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

    fun runPlanner(job: String): Boolean {
        val answer = runMachine("planner", agent, """
            $job
            Do not implement product work.
            Include a boolean field "complete": true when no unfinished tasks remain,
            false otherwise.
        """.trimIndent())
        val result = answer.lineSequence().mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line) }.getOrNull()
        }.lastOrNull()
        val complete = (result as? JsonObject)?.get("complete") as? JsonPrimitive
        if (complete == null || complete.isString || complete.booleanOrNull == null) {
            fail("Could not read the planner's result")
        }
        println(result)
        return complete.boolean
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
        runMachine("doer", doer, """
            Implement the first unfinished task in the plan.
            Do not mark the task done; the planner will do that after the work is committed.
            Include the task description in a "task" field in your result.
        """.trimIndent())
        commitChanges()
        val finished = runPlanner("The task's work has been committed. Mark the first unfinished task done and report whether the plan is complete.")
        commitChanges()
        if (finished || "--all" !in args) break
    } while (true)
    println("factory stopped")
}
