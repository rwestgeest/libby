import java.io.File
import kotlinx.serialization.json.*
import kotlin.system.exitProcess

internal fun fail(message: String): Nothing {
    System.err.println(message)
    exitProcess(1)
}

fun main(args: Array<String>) {
    fun option(name: String): String? = args.indexOf(name).takeIf { it >= 0 }
        ?.let { args.getOrNull(it + 1) }?.takeUnless { it.startsWith("--") }

    val factoryDir = System.getenv("FACTORY_DIR")?.let(::File)
        ?: File(System.getProperty("user.dir"))
    val assemblyLine = readAssemblyLine(
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
