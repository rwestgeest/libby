import java.io.File
import java.io.IOException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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

    fun prompt(name: String): String? = (read(name)["prompt"] as? JsonPrimitive)?.contentOrNull

    fun model(name: String): String? = (read(name)["model"] as? JsonPrimitive)?.contentOrNull
}

/** Runs one machine in the target and decodes its last JSON line. */
internal class MachineRunner(
    private val target: File,
    private val configuration: MachineConfiguration,
    private val model: String?
) {
    fun run(name: String, prompt: String): JsonObject = run(name, prompt, null)

    fun runBranches(prompts: Map<String, String>): Map<String, JsonObject> {
        val executor = Executors.newFixedThreadPool(prompts.size)
        val completed = ExecutorCompletionService<Pair<String, JsonObject>>(executor)
        val group = BranchProcesses()
        try {
            prompts.forEach { (name, prompt) ->
                completed.submit(Callable { name to run(name, prompt, group) })
            }
            val results = mutableMapOf<String, JsonObject>()
            repeat(prompts.size) {
                val (name, result) = try { completed.take().get() }
                catch (error: ExecutionException) { throw error.cause!! }
                results[name] = result
            }
            return prompts.keys.associateWith { results.getValue(it) }
        } finally {
            group.stop()
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun run(name: String, prompt: String, group: BranchProcesses?): JsonObject {
        val harness = configuration.harness(name)
        val process = try {
            val builder = ProcessBuilder(buildList {
                add(harness)
                addAll(listOf("--print", "--no-session"))
                (configuration.model(name) ?: model)?.let { addAll(listOf("--model", it)) }
                add(prompt)
            })
                .directory(target)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
            if (group == null) builder.start() else group.start(builder)
        } catch (error: IOException) {
            fail("Could not run the $name: ${error.message}")
        }
        try {
            process.outputStream.close()
            val answer = process.inputStream.bufferedReader().use { it.readText() }
            if (process.waitFor() != 0) fail("The $name exited unsuccessfully\n$answer")
            return readResult(answer, name)
        } finally {
            if (process.isAlive) process.destroyForcibly()
        }
    }

    private fun readResult(answer: String, machine: String): JsonObject {
        val result = answer.lineSequence().mapNotNull { line ->
            runCatching { Json.parseToJsonElement(line) }.getOrNull()
        }.lastOrNull()
        return result as? JsonObject ?: fail("Could not read the $machine's result")
    }

    private class BranchProcesses {
        private val processes = mutableListOf<Process>()
        private var stopped = false

        @Synchronized fun start(builder: ProcessBuilder): Process {
            if (stopped) throw InterruptedException("Fan out stopped")
            return builder.start().also { processes.add(it) }
        }

        @Synchronized fun stop() {
            stopped = true
            processes.forEach { process ->
                process.descendants().use { children -> children.forEach { it.destroyForcibly() } }
                if (process.isAlive) process.destroyForcibly()
            }
        }
    }
}
