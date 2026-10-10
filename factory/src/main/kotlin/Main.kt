import java.io.File
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
    val runName = option("--run") ?: fail("A run is required")
    val namedRun = RunStore(factoryDir).open(
        runName,
        option("--target"),
        option("--line"),
        option("--seed")
    )
    val assemblyLine = readAssemblyLine(namedRun.assemblyLineFile, namedRun.machinesDirectory)
    if ("--check-line" in args) {
        println("Assembly line accepted")
        return
    }

    val maxAttempts = option("--max-attempts")?.toInt() ?: 3

    val repository = TargetRepository(namedRun.target)
    repository.prepare()

    val configuration = MachineConfiguration(namedRun.machinesDirectory)
    val runner = MachineRunner(namedRun.target, configuration, option("--model"))
    val jobs = MachineJobs(
        namedRun.seed,
        namedRun.plan,
        configuration,
        assemblyLine::resultFields,
        repository
    )

    val lifecycle = TaskLifecycle(maxAttempts, jobs, repository)
    runFactory(assemblyLine, runner, lifecycle, repository)
}
