package steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import java.nio.file.Files
import java.nio.file.Path

class FactorySteps {
    private lateinit var workspace: Path
    private lateinit var target: Path
    private lateinit var seed: Path
    private lateinit var agent: Path
    private var chooseAgent = true
    private var output = ""
    private var exitCode = -1
    private lateinit var headBeforeRun: String

    @Given("a copy of the factory")
    fun copyFactory() {
        workspace = Files.createTempDirectory("factory-test-")
        val source = Path.of("").toAbsolutePath().toFile()
        val copy = workspace.resolve("factory").toFile()

        copy.mkdirs()
        // Each example owns its compiled code; only dependency JARs are shared.
        source.resolve("target/classes").copyRecursively(copy.resolve("target/classes"))
        source.resolve("target/runtime-classpath.txt")
            .copyTo(copy.resolve("target/runtime-classpath.txt"))
        source.resolve("factory").copyTo(copy.resolve("factory"))
        copy.resolve("factory").setExecutable(true)
        git("init")
        git("config", "user.name", "Factory Test")
        git("config", "user.email", "factory-test@example.invalid")
        git("commit", "--allow-empty", "-m", "Initial test state")
    }

    @Given("a new target")
    fun newTarget() {
        target = Files.createDirectory(workspace.resolve("target"))
    }

    @Given("a seed describing a game of Tetris")
    fun createSeed() {
        seed = Files.writeString(
            workspace.resolve("seed.md"),
            """
            Build a game of Tetris that runs in the terminal.
            Start it with npm start.
            Keep the complete display withing 24 terminal rows.
            """.trimIndent()
        )
    }

    @Given("the agent plans the tasks alpha and beta, and does one task a pass")
    fun prepareAgent() {
        agent = workspace.resolve("agent.py")
        Files.copy(Path.of("src/test/doubles/agent.py"), agent)
        agent.toFile().setExecutable(true)
    }

    @Given("the agent writes a file called SENTINEL")
    fun agentWritesSentinel() {
        Files.writeString(workspace.resolve("product-name.txt"), "SENTINEL")
    }

    @Given("the agent cannot be run")
    fun agentCannotBeRun() {
        check(agent.toFile().setExecutable(false, false)) {
            "Could not remove the test agent's execute permission"
        }
    }

    @Given("no plan")
    fun noPlan() {
        Files.deleteIfExists(target.resolve(".factory/plan.md"))
    }

    @Given("a plan with three tasks, none of them done")
    fun planWithThreeTasks() {
        val plan = target.resolve(".factory/plan.md")
        Files.createDirectories(plan.parent)
        Files.writeString(
            plan,
            """
            - [ ] alpha
            - [ ] beta
            - [ ] gamma
            """.trimIndent() + "\n"
        )
    }

    @Given("no harness is chosen")
    fun noHarnessChosen() {
        chooseAgent = false
    }

    @When("the factory runs one pass")
    fun runOnePass() {
        val bin = Files.createDirectory(workspace.resolve("bin"))
        val pi = Files.copy(agent, bin.resolve("pi"))
        pi.toFile().setExecutable(true)

        val command = mutableListOf(
            workspace.resolve("factory/factory").toString(),
            "--seed", seed.toString(),
            "--target", target.toString()
        )
        if (chooseAgent) {
            command.addAll(listOf("--agent", agent.toString()))
        }

        val builder = ProcessBuilder(command)
            .directory(workspace.toFile())
            .redirectErrorStream(true)

        builder.environment()["PATH"] =
            "$bin:${System.getenv("PATH")}"

        headBeforeRun = git("rev-parse", "HEAD")
        val process = builder.start()
        output = process.inputStream.bufferedReader().use { it.readText() }
        exitCode = process.waitFor()
    }

    @Then("pi has been called")
    fun piHasBeenCalled() {
        check(Files.exists(workspace.resolve("bin/calls/0.json"))) {
            "Expected pi to be called. Exit code: $exitCode\n$output"
        }
    }

    @Then("the chosen agent has been called")
    fun chosenAgentHasBeenCalled() {
        check(Files.exists(workspace.resolve("calls/0.json"))) {
            "Expected the chosen agent to be called. Exit code: $exitCode\n$output"
        }
    }

    @Then("pi has not been called")
    fun piHasNotBeenCalled() {
        check(!Files.exists(workspace.resolve("bin/calls/0.json"))) {
            "Expected pi not to be called.\n$output"
        }
    }

    @Then("it reports that it could not run the agent")
    fun reportsAgentCouldNotRun() {
        check(output.contains("could not run the agent", ignoreCase = true)) {
            "Expected an agent startup error message.\n$output"
        }
    }

    @Then("there is no plan")
    fun thereIsNoPlan() {
        check(!Files.exists(target.resolve(".factory/plan.md"))) {
            "Expected no plan after the factory ran.\n$output"
        }
    }

    @Then("there are no new commits")
    fun thereAreNoNewCommits() {
        val headAfterRun = git("rev-parse", "HEAD")
        check(headAfterRun == headBeforeRun) {
            "Expected no new commits, but HEAD changed " +
                "from $headBeforeRun to $headAfterRun"
        }
    }

    @Then("the plan has the tasks {string} and {string}, and no others")
    fun planHasExactlyTwoTasks(first: String, second: String) {
        val lines = Files.readAllLines(target.resolve(".factory/plan.md"))
            .filter { it.isNotBlank() }

        check(lines == listOf("- [ ] $first", "- [ ] $second")) {
            "Expected only tasks $first and $second, but found:\n" +
                lines.joinToString("\n")
        }
    }

    private fun git(vararg arguments: String): String {
        val process = ProcessBuilder(listOf("git") + arguments)
            .directory(workspace.toFile())
            .redirectErrorStream(true)
            .start()

        val result = process.inputStream.bufferedReader().use {
            it.readText()
        }
        check(process.waitFor() == 0) {
            "git ${arguments.joinToString(" ")} failed:\n$result"
        }
        return result.trim()
    }

    @After
    fun cleanUp() {
        if (::workspace.isInitialized) {
            workspace.toFile().deleteRecursively()
        }
    }
}