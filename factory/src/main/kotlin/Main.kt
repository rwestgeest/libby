import java.io.File
import kotlinx.serialization.json.*
import kotlin.system.exitProcess

internal fun fail(message: String): Nothing {
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

    val repository = TargetRepository(target)
    repository.prepare()

    val configuration = MachineConfiguration(factoryDir)
    val runner = MachineRunner(target, configuration, option("--model"))
    val jobs = MachineJobs(seed, plan, configuration, assemblyLine::resultFields, repository)

    var node = assemblyLine.next("start", null)
    var taskInProgress = false
    var taskAccepted = false
    var attempts = 0
    var findings = JsonArray(emptyList())

    while (node != "finish") {
        if (node == "planner" && taskInProgress) {
            repository.recordTaskChanges()
            taskAccepted = true
        }

        val prompt = jobs.prompt(node, taskAccepted, findings)

        if (node == "doer") {
            taskInProgress = true
            taskAccepted = false
            attempts++
        }
        val result = runner.run(node, prompt)
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
            if (taskAccepted) repository.recordTaskChanges()
            taskInProgress = false
            taskAccepted = false
            attempts = 0
            findings = JsonArray(emptyList())
        }
        node = next
    }
    repository.recordTaskChanges()
    println("factory stopped")
}
