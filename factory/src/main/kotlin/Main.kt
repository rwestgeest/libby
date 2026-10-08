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

    val prompt = """
        Read the seed at "$seed".
        Keep your plan at "$plan".
        If no plan exists, write a plan without implementing any tasks.
        Otherwise, implement the first unfinished task and mark it done.
        Work only in the current target directory. The factory handles commits.
        End your answer with a single line of JSON.
        Include a boolean field "complete": true when no unfinished tasks
        remain, false otherwise. If you implemented a task, also include
        its description in a "task" field.
    """.trimIndent()

    do {
        val process = try {
            ProcessBuilder(buildList {
                add(agent)
                addAll(listOf("--print", "--no-session"))
                option("--model")?.let { addAll(listOf("--model", it)) }
                add(prompt)
            })
                .directory(target)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        } catch (error: IOException) {
            fail("Could not run the agent: ${error.message}")
        }
        // The prompt is an argument; signal EOF so pi doesn't wait for piped input.
        process.outputStream.close()
        val answer = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) fail("Agent exited unsuccessfully\n$answer")
        val result = answer.lineSequence().mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line) }.getOrNull()
        }.lastOrNull()
        val complete = (result as? JsonObject)?.get("complete") as? JsonPrimitive
        if (complete == null || complete.isString || complete.booleanOrNull == null) {
            fail("Could not read the agent's result")
        }

        // Both staging and committing are limited to this target, including its plan.
        if (git("status", "--porcelain", "--", ".").isNotBlank()) {
            git("add", "--", ".")
            git("commit", "--only", "-m", "Record factory pass", "--", ".")
        }
        println(result)
        if (complete.boolean || "--all" !in args) break
    } while (true)
    println("factory stopped")
}
