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

    fun directory(name: String): File = File(machinesDirectory, name).absoluteFile

    fun instructions(name: String): File = File(directory(name), "AGENTS.md")

    fun model(name: String): String? = (read(name)["model"] as? JsonPrimitive)?.contentOrNull

    fun kind(name: String): String = File(harness(name)).name

    fun apiKeyVariable(name: String, fallbackModel: String?): String {
        (read(name)["apiKeyEnv"] as? JsonPrimitive)?.contentOrNull?.let { return it }
        return when (kind(name)) {
            "codex" -> "CODEX_API_KEY"
            "claude" -> "ANTHROPIC_API_KEY"
            else -> {
                val provider = (model(name) ?: fallbackModel).orEmpty()
                    .takeIf { '/' in it }?.substringBefore('/') ?: ""
                when (provider) {
                    "google", "google-gemini" -> "GEMINI_API_KEY"
                    "openai-codex" -> "OPENAI_API_KEY"
                    "" -> "ANTHROPIC_API_KEY"
                    else -> provider.uppercase().replace('-', '_') + "_API_KEY"
                }
            }
        }
    }

    fun requireCredentials(name: String, fallbackModel: String?) {
        if (!instructions(name).isFile) fail("There are no instructions for $name")
        // Ordinary executable machines need no coding-agent login.
        if (kind(name) !in setOf("pi", "claude", "codex")) return
        val login = File(directory(name), if (kind(name) == "claude") ".credentials.json" else "auth.json")
        if (!login.isFile && System.getenv(apiKeyVariable(name, fallbackModel)).isNullOrBlank()) {
            fail("The $name cannot sign in: supply its own login or ${apiKeyVariable(name, fallbackModel)}")
        }
    }

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
            val directory = configuration.directory(name)
            val selectedModel = configuration.model(name) ?: model
            val builder = ProcessBuilder(buildList {
                add(harness)
                when (configuration.kind(name)) {
                    "claude" -> addAll(listOf("--print", "--no-session-persistence", "--setting-sources", "user"))
                    "codex" -> addAll(listOf("exec", "-c", "project_doc_max_bytes=0"))
                    else -> addAll(listOf("--print", "--no-session", "--no-context-files",
                        "--append-system-prompt", configuration.instructions(name).path))
                }
                selectedModel?.let { addAll(listOf("--model", it)) }
                add(prompt)
            })
                .directory(target)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
            // A parent agent may itself have a redirected configuration directory.
            listOf("PI_CODING_AGENT_DIR", "CLAUDE_CONFIG_DIR", "CODEX_HOME",
                "ANTHROPIC_AUTH_TOKEN", "ANTHROPIC_OAUTH_TOKEN", "CLAUDE_CODE_OAUTH_TOKEN").forEach {
                builder.environment().remove(it)
            }
            val variable = when (configuration.kind(name)) {
                "claude" -> "CLAUDE_CONFIG_DIR"
                "codex" -> "CODEX_HOME"
                else -> "PI_CODING_AGENT_DIR"
            }
            builder.environment()[variable] = directory.path
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
