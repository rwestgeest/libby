import java.io.File
import java.io.IOException
import kotlinx.serialization.json.*

/** Reads configuration on demand, so changes between machine calls remain visible. */
internal class MachineConfiguration(private val machinesDirectory: File) {
    private fun read(name: String): JsonObject {
        val file = File(machinesDirectory, "$name/config.json")
        if (!file.isFile) return JsonObject(emptyMap())
        return runCatching { Json.parseToJsonElement(file.readText()).jsonObject }
            .getOrElse { fail("Could not read the configuration for $name") }
    }

    fun harness(name: String): String =
        (read(name)["harness"] as? JsonPrimitive)?.contentOrNull ?: "pi"

    fun validationLens(name: String): String =
        (read(name)["lens"] as? JsonPrimitive)?.contentOrNull ?: "testability"
}

/** Runs one machine in the target and decodes its last JSON line. */
internal class MachineRunner(
    private val target: File,
    private val configuration: MachineConfiguration,
    private val model: String?
) {
    fun run(name: String, prompt: String): JsonObject {
        val harness = configuration.harness(name)
        val process = try {
            ProcessBuilder(buildList {
                add(harness)
                addAll(listOf("--print", "--no-session"))
                model?.let { addAll(listOf("--model", it)) }
                add(prompt)
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
        return readResult(answer, name)
    }

    private fun readResult(answer: String, machine: String): JsonObject {
        val result = answer.lineSequence().mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line) }.getOrNull()
        }.lastOrNull()
        return result as? JsonObject ?: fail("Could not read the $machine's result")
    }
}
