package steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import java.nio.file.Files
import java.nio.file.Path

class FactorySteps {
    private lateinit var workspace: Path
    private lateinit var target: Path

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

    @After
    fun cleanUp() {
        if (::workspace.isInitialized) {
            workspace.toFile().deleteRecursively()
        }
    }
}