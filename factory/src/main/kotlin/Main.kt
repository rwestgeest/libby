import java.io.File

fun main(args: Array<String>) {
    val target = File(args[args.indexOf("--target") + 1])

    val agentIndex = args.indexOf("--agent")
    val agent = if (agentIndex >= 0) args[agentIndex + 1] else "pi"

    val process = try {
        ProcessBuilder(agent)
            .directory(target)
            .inheritIO()
            .start()
    } catch (error: java.io.IOException) {
        System.err.println("Could not run the agent: ${error.message}")
        kotlin.system.exitProcess(1)
    }

    process.waitFor()
}
