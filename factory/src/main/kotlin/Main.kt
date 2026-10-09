import java.io.File
import java.io.IOException
import kotlinx.serialization.json.*
import kotlin.system.exitProcess

private fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}

private data class Edge(val from: String, val to: String, val label: String?)

private class AssemblyLine(private val edges: List<Edge>) {
    val machines: Set<String> = edges.flatMap { listOf(it.from, it.to) }
        .filter { it !in setOf("start", "finish") }.toSet()

    fun next(node: String, result: JsonObject?): String {
        val choices = edges.filter { it.from == node }
        choices.singleOrNull { it.label == null }?.let { return it.to }
        val fields = choices.mapNotNull { it.label?.removePrefix("not ") }.distinct()
        for (field in fields) {
            val value = (result?.get(field) as? JsonPrimitive)?.booleanOrNull
                ?: fail("The result of $node has no field \"$field\"")
            choices.firstOrNull { edge ->
                edge.label == if (value) field else "not $field"
            }?.let { return it.to }
        }
        fail("The result of $node does not select an edge")
    }

    fun resultFields(machine: String): List<String> = edges.filter { it.from == machine }
        .mapNotNull { it.label?.removePrefix("not ") }.distinct()

    companion object {
        private val edgePattern = Regex(
            """^\s*([A-Za-z][\w-]*)\s*->\s*([A-Za-z][\w-]*)\s*(?:\[\s*label\s*=\s*\"([^\"]+)\"\s*])?\s*;?\s*$"""
        )

        fun read(file: File, factoryDir: File): AssemblyLine {
            if (!file.isFile) fail("There is no assembly line")
            val edges = file.readLines().mapNotNull { line ->
                edgePattern.matchEntire(line)?.destructured?.let { (from, to, label) ->
                    Edge(from, to, label.ifBlank { null })
                }
            }
            val line = AssemblyLine(edges)
            if (edges.none { it.from == "start" }) fail("The assembly line has no start")
            if (edges.none { it.to == "finish" }) fail("The assembly line has no finish")
            for (machine in line.machines) {
                if (!File(factoryDir, machine).isDirectory) {
                    fail("There is no machine called \"$machine\"")
                }
            }
            edges.firstOrNull { it.to == "finish" && it.from != "planner" }?.let {
                fail("Only planner may lead to finish")
            }
            val reverse = edges.groupBy { it.to }
            val canFinish = mutableSetOf("finish")
            val pending = ArrayDeque<String>().apply { add("finish") }
            while (pending.isNotEmpty()) {
                reverse[pending.removeFirst()].orEmpty().forEach { edge ->
                    if (canFinish.add(edge.from)) pending.add(edge.from)
                }
            }
            line.machines.lastOrNull { it !in canFinish }?.let {
                fail("Finish cannot be reached from $it")
            }
            return line
        }
    }
}

fun main(args: Array<String>) {
    fun option(name: String): String? = args.indexOf(name).takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1) }?.takeUnless { it.startsWith("--") }

    val factoryDir = System.getenv("FACTORY_DIR")?.let(::File)
        ?: File(System.getProperty("user.dir"))
    val assemblyLine = AssemblyLine.read(
        File(factoryDir, "assembly-line/line.dot"), factoryDir
    )
    if ("--check-line" in args) {
        println("Assembly line accepted")
        return
    }

    val seed = option("--seed")?.let { File(it).absoluteFile.normalize() }
        ?.takeIf { it.isFile } ?: fail("There is no seed")
    val target = option("--target")?.let { File(it).absoluteFile.normalize() }
        ?: fail("A target is required")
    target.mkdirs()
    val plan = File(target, ".factory/plan.md")
    val maxAttempts = option("--max-attempts")?.toInt() ?: 3

    fun machineConfig(name: String): JsonObject {
        val file = File(factoryDir, "$name/config.json")
        if (!file.isFile) return JsonObject(emptyMap())
        return runCatching { Json.parseToJsonElement(file.readText()).jsonObject }
            .getOrElse { fail("Could not read the configuration for $name") }
    }

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

    fun runMachine(name: String, job: String): String {
        val config = machineConfig(name)
        val harness = (config["harness"] as? JsonPrimitive)?.contentOrNull ?: "pi"
        val process = try {
            ProcessBuilder(buildList {
                add(harness)
                addAll(listOf("--print", "--no-session"))
                option("--model")?.let { addAll(listOf("--model", it)) }
                add("You are the $name.\n$context\n$job")
            })
                .directory(target)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        } catch (error: IOException) {
            fail("Could not run the $name: ${error.message}")
        }
        process.outputStream.close()
        val answer = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) fail("The $name exited unsuccessfully\n$answer")
        return answer
    }

    fun readResult(answer: String, machine: String): JsonObject {
        val result = answer.lineSequence().mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line) }.getOrNull()
        }.lastOrNull()
        return result as? JsonObject ?: fail("Could not read the $machine's result")
    }

    fun resultRequest(machine: String): String {
        val fields = assemblyLine.resultFields(machine)
        if (fields.isEmpty()) return "Include an empty JSON object as your result."
        return "Include boolean ${if (fields.size == 1) "field" else "fields"} " +
            fields.joinToString(" and ") { "\"$it\"" } + " in your JSON result."
    }

    fun productDiff(): String = buildString {
        append(git("diff", "--no-ext-diff", "HEAD", "--", ".", ":(exclude).factory"))
        val newFiles = git("ls-files", "--others", "--exclude-standard", "-z", "--", ".", ":(exclude).factory")
        for (path in newFiles.split('\u0000').filter { it.isNotEmpty() }) {
            val (diff, status) = gitResult("diff", "--no-ext-diff", "--no-index", "--", "/dev/null", path)
            if (status > 1) fail(diff.trim())
            append(diff)
        }
    }

    fun commitChanges() {
        if (git("status", "--porcelain", "--", ".").isNotBlank()) {
            git("add", "--", ".")
            git("commit", "--only", "-m", "Record factory task", "--", ".")
        }
    }

    var node = assemblyLine.next("start", null)
    var taskInProgress = false
    var taskAccepted = false
    var attempts = 0
    var findings = JsonArray(emptyList())

    while (node != "finish") {
        if (node == "planner" && taskInProgress) {
            commitChanges()
            taskAccepted = true
        }

        val job = when (node) {
            "planner" -> if (taskAccepted) """
                The task's work has been committed. Mark the first unfinished task done and report whether the plan is complete.
                Do not implement product work.
                ${resultRequest(node)}
            """.trimIndent() else """
                If no plan exists, write one from the seed. Otherwise, report its status without changing it.
                Do not implement product work.
                ${resultRequest(node)}
            """.trimIndent()
            "doer" -> """
                Implement the first unfinished task in the plan.
                Validator findings from the previous attempt: $findings
                Record each finding as a subtask of the task in progress, then fix it.
                Mark resolved subtasks done. Do not create new top-level tasks for findings.
                Do not mark the task itself done; the planner will do that after the work is committed.
                ${resultRequest(node)}
            """.trimIndent()
            "validator" -> {
                val lens = (machineConfig(node)["lens"] as? JsonPrimitive)?.contentOrNull ?: "testability"
                """
                    Check the doer's uncommitted product work in this target, including new files.
                    Review the supplied changes only; do not recheck previously committed work.
                    Your validation lens is: $lens.
                    Report findings only. Change neither the plan nor the product files.
                    ${resultRequest(node)} Include an array "findings" in your JSON result.
                    Product changes:
                    ${productDiff()}
                """.trimIndent()
            }
            else -> resultRequest(node)
        }

        if (node == "doer") {
            taskInProgress = true
            taskAccepted = false
            attempts++
        }
        val result = readResult(runMachine(node, job), node)
        println(result)
        if (node == "validator") {
            findings = result["findings"] as? JsonArray
                ?: fail("Could not read the validator's result")
        }
        val next = assemblyLine.next(node, result)
        if (node == "validator" && next == "doer" && attempts >= maxAttempts) {
            fail("The task hit its limit of $maxAttempts attempts. Findings: $findings")
        }
        if (node == "planner") {
            if (taskAccepted) commitChanges()
            taskInProgress = false
            taskAccepted = false
            attempts = 0
            findings = JsonArray(emptyList())
        }
        node = next
    }
    commitChanges()
    println("factory stopped")
}
