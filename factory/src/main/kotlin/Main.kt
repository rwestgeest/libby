import java.io.File

fun main(args: Array<String>) {
    val target = File(args[args.indexOf("--target") + 1])
    val seed = File(args[args.indexOf("--seed") + 1]).absoluteFile
    val plan = File(target, ".factory/plan.md").absoluteFile

    val prompt = """
        Read the seed at "$seed".
        Keep your plan at "$plan".
        If no plan exists, write a plan without implementing any tasks.
        Otherwise, implement the first unfinished task and mark it done.
        """.trimIndent()

    val agentIndex = args.indexOf("--agent")
    val agent = if (agentIndex >= 0) args[agentIndex + 1] else "pi"

    val process = try {
        ProcessBuilder(agent, "--print", "--no-session", prompt)
            .directory(target)
            .inheritIO()
            .start()
    } catch (error: java.io.IOException) {
        System.err.println("Could not run the agent: ${error.message}")
        kotlin.system.exitProcess(1)
    }

    process.waitFor()

    fun git(vararg arguments: String) {
        val command = ProcessBuilder(listOf("git") + arguments)
            .directory(target)
            .inheritIO()
            .start()

        check(command.waitFor() == 0) {
            "git ${arguments.joinToString(" ")} failed"
        }
    }

    git("add", "--", ".")
    git("commit", "--only", "-m", "Record factory pass", "--", ".")
}
