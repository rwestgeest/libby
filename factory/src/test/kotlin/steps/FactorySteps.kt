package steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import java.nio.file.Files
import java.nio.file.Path

class FactorySteps {
    private lateinit var workspace: Path
    private lateinit var target: Path
    private lateinit var seed: Path
    private lateinit var agent: Path
    private var chooseAgent = true

    @Given("a copy of the factory")
    fun copyFactory() {
        workspace = Files.createTempDirectory("factory-test-")
        val source = Path.of("").toAbsolutePath().toFile()
        val copy = workspace.resolve("factory").toFile()

        copy.mkdirs()
        source.resolve("pom.xml").copyTo(copy.resolve("pom.xml"))
        source.resolve("src/main").copyRecursively(copy.resolve("src/main"))
        source.resolve("factory").copyTo(copy.resolve("factory"))
        copy.resolve("factory").setExecutable(true)
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

    @Given("no harness is chosen")
    fun noHarnessChosen() {
        chooseAgent = false
    }

    @After
    fun cleanUp() {
        if (::workspace.isInitialized) {
            workspace.toFile().deleteRecursively()
        }
    }
}