import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess
import kotlinx.serialization.json.*

internal open class FactoryFailure(message: String) : RuntimeException(message)
internal class FactoryBlocked(message: String) : FactoryFailure(message)
internal fun fail(message: String): Nothing = throw FactoryFailure(message)

fun main(args: Array<String>) {
    try { executeFactory(args) }
    catch (error: FactoryFailure) { System.err.println(error.message); exitProcess(1) }
}

private fun executeFactory(args: Array<String>) {
    val factoryDir = (System.getenv("FACTORY_DIR")?.let(::File) ?: File(System.getProperty("user.dir"))).absoluteFile
    when (args.firstOrNull()) {
        "watch" -> { watchRun(factoryDir, args.getOrNull(1) ?: fail("A run name is required")); return }
        "stop" -> { stopRun(factoryDir, args.getOrNull(1)); return }
        "wait" -> { waitRun(factoryDir, args.getOrNull(1) ?: fail("A run name is required")); return }
        "--execute-run" -> { executeNamedRun(factoryDir, args.getOrNull(1) ?: fail("A run name is required"), args.drop(2).toTypedArray()); return }
    }
    fun option(name: String): String? = args.indexOf(name).takeIf { it >= 0 }?.let { args.getOrNull(it + 1) }
    val runName = option("--run") ?: fail("A run is required")
    val namedRun = RunStore(factoryDir).open(runName, option("--target"), option("--line"), option("--seed"))
    val assemblyLine = readAssemblyLine(namedRun.assemblyLineFile, namedRun.machinesDirectory)
    if ("--check-line" in args) {
        assemblyLine.validate { if (!File(namedRun.machinesDirectory, it).isDirectory) fail("There is no machine called \"$it\"") }
        println("Assembly line accepted")
        return
    }
    val configuration = MachineConfiguration(namedRun.machinesDirectory)
    val model = option("--model")
    assemblyLine.validate { configuration.requireCredentials(it, model) }
    val control = RunControl(factoryDir)
    if (control.currentRun() != null && control.isAlive()) fail("refused: run ${control.currentRun()} is running")
    control.start(runName, args)
    println("started run $runName")
}

private fun executeNamedRun(factoryDir: File, runName: String, args: Array<String>) {
    val pidFile = File(factoryDir, "runs/.daemon/pid")
    val ownPid = ProcessHandle.current().pid()
    var attempts = 0
    while (attempts++ < 100 && (!pidFile.isFile || pidFile.readText().trim().toLongOrNull() != ownPid)) Thread.sleep(10)
    Thread.sleep(250)
    val namedRun = RunStore(factoryDir).open(runName, null, null, null)
    val assemblyLine = readAssemblyLine(namedRun.assemblyLineFile, namedRun.machinesDirectory)
    val model = args.toList().let { it.indexOf("--model").takeIf { i -> i >= 0 }?.let { i -> it.getOrNull(i + 1) } }
    val configuration = MachineConfiguration(namedRun.machinesDirectory)
    assemblyLine.validate { configuration.requireCredentials(it, model) }
    val repository = TargetRepository(namedRun.target)
    repository.prepare()
    val control = RunControl(factoryDir)
    val runner = MachineRunner(
        namedRun.target, configuration, model,
        onOutput = { machine, text, usage -> control.event(runName, machine, text, usage) },
        onMachineState = { machine, running -> control.machineState(runName, machine, running) }
    )
    val jobs = MachineJobs(namedRun.seed, namedRun.plan, assemblyLine::resultFields)
    val maxAttempts = args.toList().let { it.indexOf("--max-attempts").takeIf { i -> i >= 0 }?.let { i -> it.getOrNull(i + 1)?.toIntOrNull() } } ?: 3
    try {
        runFactory(assemblyLine, runner, TaskLifecycle(maxAttempts, jobs, repository))
        control.finished(runName, "finished")
    } catch (error: Throwable) {
        control.finished(runName, if (error is FactoryBlocked) "blocked" else "failed", error.message)
        if (error is FactoryFailure) throw error
        throw error
    }
}

private class RunControl(private val factoryDir: File) {
    private val state = File(factoryDir, "runs/.daemon")
    private val factoryMarker = File(state, "factory")
    private fun runDir(name: String) = File(factoryDir, "runs/$name")
    fun currentRun(): String? = File(state, "current").takeIf { it.isFile }?.readText()?.trim()?.takeIf(String::isNotEmpty)
    fun isAlive(): Boolean {
        val pid = File(state, "pid").takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() ?: return false
        return ProcessHandle.of(pid).map { it.isAlive }.orElse(false)
    }
    fun start(name: String, args: Array<String>) {
        state.mkdirs(); runDir(name).mkdirs()
        factoryMarker.writeText("running\n")
        File(state, "current").writeText("$name\n")
        File(runDir(name), "status").writeText("running\n")
        val java = File(System.getProperty("java.home"), "bin/java").path
        val process = ProcessBuilder(listOf(java, "-cp", System.getProperty("java.class.path"), "MainKt", "--execute-run", name) + args)
            .directory(factoryDir).redirectInput(ProcessBuilder.Redirect.from(File("/dev/null")))
            .redirectOutput(ProcessBuilder.Redirect.appendTo(File(runDir(name), "daemon.log")))
            .redirectError(ProcessBuilder.Redirect.appendTo(File(runDir(name), "daemon.log"))).start()
        File(state, "pid").writeText("${process.pid()}\n")
    }
    fun event(name: String, machine: String, text: String, usage: Pair<Long, Long>?) {
        val file = File(runDir(name), "record.jsonl")
        val provider = runCatching {
            val target = Json.parseToJsonElement(File(runDir(name), "settings.json").readText()).jsonObject
                .getValue("target").jsonPrimitive.content
            val config = File(File(target, ".assembly-lines/.machines/$machine"), "config.json")
            Json.parseToJsonElement(config.readText()).jsonObject["model"]?.jsonPrimitive?.content?.substringBefore('/')
        }.getOrNull()?.let {
            when (it.lowercase()) { "openai-codex" -> "openai"; "google-gemini" -> "google"; else -> it.lowercase() }
        }
        val item = buildJsonObject {
            put("type", "output"); put("machine", machine); put("text", text)
            put("inputTokens", usage?.first ?: text.length.toLong()); put("outputTokens", usage?.second ?: (text.length / 2).toLong())
            provider?.let { put("provider", it) }
        }.toString()
        Files.writeString(file.toPath(), "$item\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
    fun machineState(name: String, machine: String, running: Boolean) {
        val file = File(runDir(name), "record.jsonl")
        val item = buildJsonObject { put("type", "machine-state"); put("machine", machine); put("running", running) }.toString()
        Files.writeString(file.toPath(), "$item\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
    fun runStatus(name: String, text: String) {
        val file = File(runDir(name), "record.jsonl")
        val item = buildJsonObject { put("type", "run-status"); put("text", text) }.toString()
        Files.writeString(file.toPath(), "$item\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
    fun finished(name: String, status: String, error: String? = null) {
        File(runDir(name), "status").writeText(status + (error?.let { ": $it" } ?: "") + "\n")
        File(state, "current").delete(); File(state, "pid").delete()
    }
}

internal class RunRecordReader(private val file: File) {
    private var offset = 0L

    fun readLines(): List<String> {
        if (!file.isFile) return emptyList()
        return RandomAccessFile(file, "r").use { input ->
            input.seek(offset)
            val lines = mutableListOf<String>()
            val pending = java.io.ByteArrayOutputStream()
            while (true) {
                val byte = input.read()
                if (byte == -1) break
                if (byte == 10) {
                    lines.add(pending.toString(Charsets.UTF_8))
                    pending.reset()
                    offset = input.filePointer
                } else pending.write(byte)
            }
            lines
        }
    }
}

private fun watchRun(factoryDir: File, name: String) {
    val control = RunControl(factoryDir); val runFolder = File(factoryDir, "runs/$name")
    val file = File(runFolder, "record.jsonl")
    val records = RunRecordReader(file)
    val totalsByMachine = mutableMapOf<String, Pair<Long, Long>>()
    val providerTotals = mutableMapOf<String, Long>()
    val runningMachines = linkedSetOf<String>()
    var wholeRun = 0L
    while (true) {
        run {
            records.readLines().filter(String::isNotBlank).forEach { line ->
                val item = Json.parseToJsonElement(line).jsonObject
                if (item["type"]?.jsonPrimitive?.content == "run-status") {
                    println(item.getValue("text").jsonPrimitive.content)
                    return@forEach
                }
                if (item["type"]?.jsonPrimitive?.content == "machine-state") {
                    val machine = item.getValue("machine").jsonPrimitive.content
                    if (item.getValue("running").jsonPrimitive.boolean) runningMachines.add(machine) else runningMachines.remove(machine)
                    println(if (runningMachines.isEmpty()) "finished $machine" else "running: ${runningMachines.joinToString(", ")}")
                    return@forEach
                }
                val machine = item.getValue("machine").jsonPrimitive.content
                val generated = item.getValue("text").jsonPrimitive.content
                if (generated.isNotEmpty()) println("$machine | $generated")
                val previous = totalsByMachine.getOrDefault(machine, 0L to 0L)
                val total = (previous.first + item.getValue("inputTokens").jsonPrimitive.long) to
                    (previous.second + item.getValue("outputTokens").jsonPrimitive.long)
                totalsByMachine[machine] = total
                val spent = item.getValue("inputTokens").jsonPrimitive.long + item.getValue("outputTokens").jsonPrimitive.long
                wholeRun += spent
                item["provider"]?.jsonPrimitive?.content?.let { providerTotals[it] = providerTotals.getOrDefault(it, 0L) + spent }
                if (generated.isNotEmpty()) println("tokens $machine ${total.first} in / ${total.second} out")
                if (generated.isNotEmpty() || item["inputTokens"]?.jsonPrimitive?.long != 0L || item["outputTokens"]?.jsonPrimitive?.long != 0L)
                    println("tokens ${providerTotals.entries.joinToString("   ") { "${it.key} ${it.value}" }}   run $wholeRun")
            }
        }
        if (control.currentRun() != name || !control.isAlive()) break
        Thread.sleep(100)
    }
    var statusChecks = 0
    while (statusChecks++ < 30 && File(runFolder, "status").takeIf { it.isFile }?.readText()?.trim().orEmpty().startsWith("running")) Thread.sleep(20)
    val settings = runCatching { Json.parseToJsonElement(File(runFolder, "settings.json").readText()).jsonObject }.getOrNull()
    val target = settings?.get("target")?.jsonPrimitive?.content?.let(::File)
    if (target != null && file.isFile) {
        val configuredProviders = target.let { root -> File(root, ".assembly-lines/.machines").listFiles()?.filter { it.isDirectory }.orEmpty() }
            .mapNotNull { machine -> runCatching { Json.parseToJsonElement(File(machine, "config.json").readText()).jsonObject["model"]?.jsonPrimitive?.content?.substringBefore('/')?.lowercase() }.getOrNull() }
            .map { when (it) { "openai-codex" -> "openai"; "google-gemini" -> "google"; else -> it } }.distinct()
        if (configuredProviders.isNotEmpty()) println(configuredProviders.joinToString("   ") { provider -> "$provider ${providerTotals.getOrDefault(provider, 0L)}" } + "   run $wholeRun")
    }
    if (File(runFolder, "status").takeIf { it.isFile }?.readText()?.trim()?.startsWith("stopped") == true) println("run $name was stopped")
}

private fun stopRun(factoryDir: File, name: String?) {
    val state = File(factoryDir, "runs/.daemon"); val active = File(state, "current").takeIf { it.isFile }?.readText()?.trim()
    if (active == null) {
        if (name != null) fail("There is no running run named $name")
        File(state, "factory").delete()
        println("factory stopped"); return
    }
    if (name != null && name != active) fail("There is no running run named $name")
    RunControl(factoryDir).runStatus(active, "run $active was stopped")
    val pid = File(state, "pid").readText().trim().toLong()
    ProcessHandle.of(pid).ifPresent { process ->
        process.descendants().use { it.forEach(ProcessHandle::destroy) }
        process.destroy()
        try { process.onExit().get(5, TimeUnit.SECONDS) } catch (_: Exception) { process.destroyForcibly() }
    }
    File(factoryDir, "runs/$active/status").writeText("stopped\n")
    File(state, "current").delete(); File(state, "pid").delete()
    if (name == null) File(state, "factory").delete()
    println(if (name == null) "factory stopped" else "stopped run $active")
}

private fun waitRun(factoryDir: File, name: String) {
    val control = RunControl(factoryDir)
    while (control.currentRun() == name && control.isAlive()) Thread.sleep(50)
}
