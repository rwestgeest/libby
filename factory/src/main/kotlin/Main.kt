import java.io.File
import java.io.IOException
import kotlinx.serialization.json.*
import kotlin.system.exitProcess

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}

fun main(args: Array<String>) {
    fun option(name: String): String? = args.indexOf(name).takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1) }?.takeUnless { it.startsWith("--") }

    val seed = option("--seed")?.let { File(it).absoluteFile.normalize() }
        ?.takeIf { it.isFile } ?: fail("There is no seed")
    val target = option("--target")?.let { File(it).absoluteFile.normalize() }
        ?: fail("A target is required")
    target.mkdirs()
    val plan = File(target, ".factory/plan.md")
    val agent = option("--agent") ?: "pi"
    val doer = option("--doer") ?: agent
    val validator = option("--validator") ?: agent
    val lens = option("--lens") ?: "testability"
    val maxAttempts = option("--max-attempts")?.toInt() ?: 3

    fun gitResult(vararg arguments: String): Pair<String, Int> {
        val process = ProcessBuilder(listOf("git") + arguments)
            .directory(target).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return output to process.waitFor()
    }
    fun git(vararg arguments: String): String {
        val (output, status) = gitResult(*arguments)
        if (status != 0) fail(output.trim())
        return output
    }
    if (gitResult("rev-parse", "--show-toplevel").second != 0) git("init")

    val context = """
        Read the seed at "$seed".
        The plan is at "$plan".
        Work only in the current target directory. The factory handles commits.
        End your answer with a single line of JSON.
    """.trimIndent()

    fun runMachine(role: String, harness: String, job: String): String {
        val process = try {
            ProcessBuilder(buildList {
                add(harness)
                addAll(listOf("--print", "--no-session"))
                option("--model")?.let { addAll(listOf("--model", it)) }
                add("You are the $role.\n$context\n$job")
            })
                .directory(target)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        } catch (error: IOException) {
            fail("Could not run the $role: ${error.message}")
        }
        // The prompt is an argument; signal EOF so pi doesn't wait for piped input.
        process.outputStream.close()
        val answer = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) fail("The $role exited unsuccessfully\n$answer")
        return answer
    }

    fun readResult(answer: String, role: String): JsonObject {
        val result = answer.lineSequence().mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line) }.getOrNull()
        }.lastOrNull()
        return result as? JsonObject ?: fail("Could not read the $role's result")
    }

    fun runPlanner(job: String): Boolean {
        val answer = runMachine("planner", agent, """
            $job
            Do not implement product work.
            Include a boolean field "complete": true when no unfinished tasks remain,
            false otherwise.
        """.trimIndent())
        val result = readResult(answer, "planner")
        val complete = result["complete"] as? JsonPrimitive
        if (complete == null || complete.isString || complete.booleanOrNull == null) {
            fail("Could not read the planner's result")
        }
        println(result)
        return complete.boolean
    }

    fun validateWork(): Pair<Boolean, JsonArray> {
        val work = buildString {
            append(git("diff", "--no-ext-diff", "HEAD", "--", ".", ":(exclude).factory"))
            val newFiles = git("ls-files", "--others", "--exclude-standard", "-z", "--", ".", ":(exclude).factory")
            for (path in newFiles.split('\u0000').filter { it.isNotEmpty() }) {
                val (diff, status) = gitResult("diff", "--no-ext-diff", "--no-index", "--", "/dev/null", path)
                if (status > 1) fail(diff.trim())
                append(diff)
            }
        }
        val answer = runMachine("validator", validator, """
            Check the doer's uncommitted product work in this target, including new files.
            Review the supplied changes only; do not recheck previously committed work.
            Your validation lens is: $lens.
            Report findings only. Change neither the plan nor the product files.
            Include a boolean field "satisfied" and an array "findings" in your JSON result.
            Product changes:
            $work
        """.trimIndent())
        val result = readResult(answer, "validator")
        val satisfied = result["satisfied"] as? JsonPrimitive
        val findings = result["findings"] as? JsonArray
        if (satisfied == null || satisfied.isString || satisfied.booleanOrNull == null || findings == null) {
            fail("Could not read the validator's result")
        }
        return satisfied.boolean to findings
    }

    fun commitChanges() {
        // Both staging and committing are limited to this target, including its plan.
        if (git("status", "--porcelain", "--", ".").isNotBlank()) {
            git("add", "--", ".")
            git("commit", "--only", "-m", "Record factory pass", "--", ".")
        }
    }

    do {
        val hadPlan = plan.exists()
        val complete = runPlanner("If no plan exists, write one from the seed. Otherwise, report its status without changing it.")
        if (!hadPlan || complete) {
            commitChanges()
            if (complete || "--all" !in args) break
            continue
        }
        var findings = JsonArray(emptyList())
        var satisfied = false
        for (attempt in 1..maxAttempts) {
            runMachine("doer", doer, """
                Implement the first unfinished task in the plan.
                Validator findings from the previous attempt: $findings
                Record each finding as a subtask of the task in progress, then fix it.
                Mark resolved subtasks done. Do not create new top-level tasks for findings.
                Do not mark the task itself done; the planner will do that after the work is committed.
                Include the task description in a "task" field in your result.
            """.trimIndent())
            val verdict = validateWork()
            satisfied = verdict.first
            findings = verdict.second
            if (satisfied) break
        }
        if (!satisfied) fail("The pass hit its limit of $maxAttempts attempts. Findings: $findings")
        commitChanges()
        val finished = runPlanner("The task's work has been committed. Mark the first unfinished task done and report whether the plan is complete.")
        commitChanges()
        if (finished || "--all" !in args) break
    } while (true)
    println("factory stopped")
}
