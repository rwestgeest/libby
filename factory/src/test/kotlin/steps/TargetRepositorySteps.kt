package steps

import TargetRepository
import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import java.io.File
import java.nio.file.Files

class TargetRepositorySteps {
    private var workspace: File? = null
    private lateinit var target: File
    private lateinit var repository: TargetRepository
    private var changes = ""
    private var head = ""
    private var staged = ""
    private var unstaged = ""

    private fun git(dir: File, vararg args: String): String {
        val process = ProcessBuilder(listOf("git") + args).directory(dir)
            .redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        check(process.waitFor() == 0) { output }
        return output.trim()
    }

    @Given("a standalone repository target")
    fun standalone() {
        workspace = Files.createTempDirectory("repository-test-").toFile()
        target = File(workspace, "product").apply { mkdirs() }
        repository = TargetRepository(target)
    }

    @When("the target repository is prepared twice")
    fun prepareTwice() {
        repository.prepare()
        repository.prepare()
    }

    @Then("Git is rooted at the target")
    fun rootedAtTarget() {
        check(File(git(target, "rev-parse", "--show-toplevel")).canonicalFile == target.canonicalFile)
    }

    @Given("a repository target with mixed changes in a containing repository")
    fun mixedChanges() {
        standalone()
        val root = workspace!!
        git(root, "init")
        git(root, "config", "user.name", "Repository Test")
        git(root, "config", "user.email", "repository-test@example.invalid")
        File(target, "tracked.txt").writeText("original\n")
        File(root, "outside.txt").writeText("original\n")
        git(root, "add", ".")
        git(root, "commit", "-m", "Fixture")
        head = git(root, "rev-parse", "HEAD")
        File(target, "tracked.txt").writeText("tracked product change\n")
        git(target, "add", "tracked.txt")
        File(target, "new product.txt").writeText("new product change\n")
        File(target, ".factory").mkdirs()
        File(target, ".factory/plan.md").writeText("private plan change\n")
        File(target, ".gitignore").writeText("ignored.txt\n")
        File(target, "ignored.txt").writeText("ignored product change\n")
        File(root, "outside.txt").writeText("staged outside change\n")
        git(root, "add", "outside.txt")
        File(root, "outside.txt").writeText("unstaged outside change\n")
        staged = git(root, "diff", "--cached", "--", "outside.txt")
        unstaged = git(root, "diff", "--", "outside.txt")
        repository.prepare()
        check(!File(target, ".git").exists())
    }

    @When("the repository collects product changes")
    fun collect() { changes = repository.uncommittedProductChanges() }

    @Then("the collected changes contain only tracked and new target products")
    fun productChangesOnly() {
        check(changes.contains("+tracked product change")) { changes }
        check(changes.contains("+new product change")) { changes }
        check(changes.contains("new product.txt")) { changes }
        for (excluded in listOf(".factory", "private plan change", "outside.txt", "ignored product change")) {
            check(!changes.contains(excluded)) { changes }
        }
    }

    @When("the repository records task changes twice")
    fun recordTwice() {
        repository.recordTaskChanges()
        repository.recordTaskChanges()
    }

    @Then("one task commit includes product and plan changes but not outside work")
    fun taskCommit() {
        val root = workspace!!
        check(git(root, "rev-list", "--count", "$head..HEAD") == "1")
        check(git(root, "log", "-1", "--format=%s") == "Record factory task")
        val paths = git(root, "diff-tree", "--no-commit-id", "--name-only", "-r", "HEAD").lines()
        check(paths.toSet() == setOf("product/.gitignore",
            "product/new product.txt", "product/tracked.txt")) { paths }
        check(git(
            target, "status", "--porcelain", "--", ".",
            ":(exclude).factory", ":(exclude).assembly-lines"
        ).isEmpty())
    }

    @Then("outside staged and unstaged changes are preserved")
    fun outsideUnchanged() {
        val root = workspace!!
        check(git(root, "diff", "--cached", "--", "outside.txt") == staged)
        check(git(root, "diff", "--", "outside.txt") == unstaged)
    }

    @After
    fun cleanUp() { workspace?.deleteRecursively() }
}
