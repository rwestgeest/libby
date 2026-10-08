package steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

private data class Task(val name: String, val done: Boolean = false)

private class Plan(private val path: Path) {
    fun entries(): List<Task> = Files.readAllLines(path)
        .filter { it.isNotBlank() }
        .map { line ->
            check(line.startsWith("- [ ] ") || line.startsWith("- [x] ")) { "Invalid task: $line" }
            Task(line.substring(6), line.startsWith("- [x] "))
        }

    fun tasks(): List<String> = entries().map {
        check(!it.done) { "Expected unfinished task: ${it.name}" }
        it.name
    }

    fun write(tasks: List<Task>) {
        Files.createDirectories(path.parent)
        Files.writeString(path, tasks.joinToString("") {
            val marker = if (it.done) "x" else " "
            "- [$marker] ${it.name}\n"
        })
    }
}

private fun runProcess(
    command: List<String>,
    dir: Path,
    environment: Map<String, String> = emptyMap()
): Pair<String, Int> {
    val builder = ProcessBuilder(command)
        .directory(dir.toFile())
        .redirectErrorStream(true)
    builder.environment().putAll(environment)
    val process = builder.start()
    val output = process.inputStream.bufferedReader().use { it.readText() }
    return output to process.waitFor()
}

private class GitRepo(private val workspace: Path) {
    fun init() {
        git("init")
        git("config", "user.name", "Factory Test")
        git("config", "user.email", "factory-test@example.invalid")
    }

    fun commit(msg: String) {
        git("commit", "--allow-empty", "-m", msg)
    }

    fun head(): String = git("rev-parse", "HEAD")

    fun logSince(commit: String, path: Path): List<String> {
        val relativePath = workspace.relativize(path).toString()
        return git(
            "log", "--format=%H", "$commit..HEAD", "--",
            relativePath, ":(exclude)$relativePath/.factory"
        ).lines().filter { it.isNotBlank() }
    }

    fun diffTree(commit: String): List<String> = git(
        "diff-tree", "--no-commit-id", "--name-only", "-r", commit
    ).lines().filter { it.isNotBlank() }

    fun git(vararg arguments: String): String {
        val (result, status) = runProcess(listOf("git") + arguments, workspace)
        check(status == 0) {
            "git ${arguments.joinToString(" ")} failed:\n$result"
        }
        return result.trim()
    }
}

class FactorySteps {
    private lateinit var workspace: Path
    private lateinit var repo: GitRepo
    private lateinit var target: Path
    private lateinit var seed: Path
    private lateinit var agent: Path
    private lateinit var agentArgs: List<String>
    private var output = ""
    private var exitCode = -1
    private lateinit var headBeforeRun: String
    private lateinit var workCommit: String
    private var seedChosen = true
    private var targetChosen = true
    private var absoluteTarget = false
    private val extraWorkspaces = mutableListOf<Path>()
    private val targetSnapshots = mutableMapOf<String, Map<String, String>>()
    private lateinit var factoryBefore: Map<String, String>
    private var stagedBefore = ""
    private var unstagedBefore = ""

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
        repo = GitRepo(workspace)
        repo.init()
        repo.commit("Initial test state")
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
            Keep the complete display within 24 terminal rows.
            """.trimIndent()
        )
    }
    
    @Given("the doer does the next task in the plan")
    fun prepareDoer() {
        val folder = Files.createDirectories(workspace.resolve("doer"))
        val doer = folder.resolve("agent.py")
        Files.copy(Path.of("src/test/doubles/agent.py"), doer)
        doer.toFile().setExecutable(true)
        agentArgs = agentArgs + listOf("--doer", doer.toString())
    }

    @Given("the planner plans the tasks alpha and beta")
    fun preparePlanner() {
        agent = workspace.resolve("agent.py")
        Files.copy(Path.of("src/test/doubles/agent.py"), agent)
        agent.toFile().setExecutable(true)
        agentArgs = listOf("--agent", agent.toString())
    }

    @Given("the validator is always satisfied")
    fun prepareValidator() {
        val folder = Files.createDirectories(workspace.resolve("validator"))
        val validator = folder.resolve("validator.py")
        Files.copy(Path.of("src/test/doubles/validator.py"), validator)
        validator.toFile().setExecutable(true)
        agentArgs = agentArgs + listOf("--validator", validator.toString())
    }

    @Given("the agent writes a file called SENTINEL")
    fun agentWritesSentinel() {
        Files.writeString(workspace.resolve("product-name.txt"), "SENTINEL")
    }

    @Given("the agent says {string} before its result")
    fun agentSaysBeforeResult(message: String) {
        Files.writeString(workspace.resolve("before-result.txt"), message)
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
    fun planWithThreeTasks() = Plan(target.resolve(".factory/plan.md"))
        .write(listOf(Task("alpha"), Task("beta"), Task("gamma")))

    @Given("a plan in which every task is done")
    fun completedPlan() {
        Plan(target.resolve(".factory/plan.md"))
            .write(listOf(Task("alpha", done = true), Task("beta", done = true)))
        // Already-finished work has an existing committed plan.
        repo.git("add", "--", workspace.relativize(target).toString())
        repo.commit("Completed plan fixture")
    }

    @Given("no harness is chosen")
    fun noHarnessChosen() {
        agentArgs = listOf()
    }
    @When("the factory runs one pass")
    fun runOnePass() = runFactory()

    @When("the factory runs to completion")
    fun runToCompletion() = runFactory("--all")

    private fun runFactory(vararg options: String) {
        val environment = setupTestEnvironment()
        val targetArgument = if (absoluteTarget) target.toString()
            else workspace.relativize(target).toString()
        val command = listOf(workspace.resolve("factory/factory").toString()) +
            (if (seedChosen) listOf("--seed", workspace.relativize(seed).toString()) else emptyList()) +
            (if (targetChosen) listOf("--target", targetArgument) else emptyList()) +
            agentArgs + options

        headBeforeRun = repo.head()
        val result = runProcess(
            command,
            workspace,
            environment
        )
        output = result.first
        exitCode = result.second
    }

    @Then("pi has been called")
    fun piHasBeenCalled() {
        check(Files.exists(workspace.resolve("bin/calls/0.json"))) {
            "Expected pi to be called. Exit code: $exitCode\n$output"
        }
    }

    @Then("the doer's chosen harness has been called")
    fun chosenDoerHasBeenCalled() {
        check(Files.exists(workspace.resolve("doer/calls/0.json"))) {
            "Expected the chosen doer to be called. Exit code: $exitCode\n$output"
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
        val headAfterRun = repo.head()
        check(headAfterRun == headBeforeRun) {
            "Expected no new commits, but HEAD changed " +
                "from $headBeforeRun to $headAfterRun"
        }
    }

    @Then("the plan has the tasks {string} and {string}, and no others")
    fun planHasExactlyTwoTasks(first: String, second: String) = check(Plan(target.resolve(".factory/plan.md")).tasks() == listOf(first, second)) { "Expected only unfinished tasks $first and $second.\n$output" }

    @Then("there is one new work commit")
    fun oneNewWorkCommit() {
        val commits = repo.logSince(headBeforeRun, target)

        check(commits.size == 1) {
            "Expected one new work commit, found ${commits.size}.\n$output"
        }
        workCommit = commits.single()
    }

    @Then("its only product file is SENTINEL")
    fun onlyProductFileIsSentinel() {
        val targetPath = workspace.relativize(target).toString()
        val productFiles = repo.diffTree(workCommit).filter {
            !it.startsWith("$targetPath/.factory/")
        }

        check(productFiles == listOf("$targetPath/SENTINEL")) {
            "Expected only SENTINEL as a product file, found: $productFiles"
        }
    }

    @Then("the agent was pointed at the plan and at the seed")
    fun agentReceivedPlanAndSeed() {
        val arguments = Files.readString(workspace.resolve("calls/0.txt"))
        val plan = target.resolve(".factory/plan.md").toAbsolutePath()
        val seedPath = seed.toAbsolutePath()

        check(arguments.contains(plan.toString())) {
            "Agent was not given the plan path: $plan\n$arguments"
        }
        check(arguments.contains(seedPath.toString())) {
            "Agent was not given the seed path: $seedPath\n$arguments"
        }
    }

    @Then("the agent was asked for a result with the field {string}")
    fun agentWasAskedForResult(field: String) {
        val arguments = Files.readString(workspace.resolve("calls/0.txt"))

        check(arguments.contains("JSON", ignoreCase = true) &&
            arguments.contains("\"$field\"")) {
            "Expected a request for a JSON result with field \"$field\".\n$arguments"
        }
    }

    @Then("the agent has been called once")
    fun agentCalledOnce() {
        val calls = workspace.resolve("calls")
        check(Files.isDirectory(calls)) { "No agent calls recorded.\n$output" }
        Files.list(calls).use { files ->
            check(files.filter { it.toString().endsWith(".json") }.count() == 1L)
        }
    }

    @Then("the factory has stopped")
    fun factoryStopped() {
        // runProcess has waited for the child to exit; the sentinel is never -1 afterward.
        check(exitCode != -1) { "Factory has not exited" }
    }

    @Given("a plan whose first task is done")
    fun firstTaskDone() = Plan(target.resolve(".factory/plan.md"))
        .write(listOf(Task("alpha", true), Task("beta"), Task("gamma")))

    private fun tasks() = Plan(target.resolve(".factory/plan.md")).entries()

    @Then("the plan shows the first task as done")
    fun firstDone() { check(tasks().first().done) }

    @Then("the plan shows the other two as not done")
    fun otherTwoNotDone() {
        check(tasks().size == 3 && tasks().drop(1).all { !it.done })
    }

    @Then("the plan shows the first two tasks as done")
    fun firstTwoDone() {
        check(tasks().size >= 2 && tasks().take(2).all { it.done })
    }

    @Then("the plan shows every task as done")
    fun everyTaskDone() { check(tasks().isNotEmpty() && tasks().all { it.done }) }

    @Then("the plan shows every task as not done")
    fun everyTaskNotDone() { check(tasks().isNotEmpty() && tasks().none { it.done }) }

    @Then("it contains the work for the first task")
    fun containsFirstTask() = containsTask("alpha")

    @Then("it contains the work for the second task")
    fun containsSecondTask() = containsTask("beta")

    private fun containsTask(name: String) {
        val path = workspace.relativize(target.resolve("$name.txt")).toString()
        check(path in repo.diffTree(workCommit))
        check(repo.git("show", "$workCommit:$path").contains("Work for $name"))
    }

    @Then("there are three new work commits")
    fun threeWorkCommits() { check(repo.logSince(headBeforeRun, target).size == 3) }

    @Given("the agent answers in prose, with no result")
    fun agentAnswersInProse() { Files.writeString(workspace.resolve("no-result"), "yes") }

    @Then("it reports that it could not read the agent's result")
    fun unreadableResult() {
        check(exitCode != 0 && output.contains("could not read the agent's result", true)) { output }
    }

    @Given("no seed is chosen")
    fun noSeedChosen() { seedChosen = false }

    @Given("the seed has been deleted")
    fun seedDeleted() { Files.delete(seed) }

    @Then("it reports that there is no seed")
    fun noSeedReported() { check(exitCode != 0 && output.contains("no seed", true)) { output } }

    @Then("no agent has been called")
    fun noAgentCalled() {
        check(!Files.exists(workspace.resolve("calls")))
        check(!Files.exists(workspace.resolve("bin/calls")))
    }

    @Then("there is a plan")
    fun thereIsAPlan() { check(Files.isRegularFile(target.resolve(".factory/plan.md"))) }

    @Then("there are no new work commits")
    fun noWorkCommits() { check(repo.logSince(headBeforeRun, target).isEmpty()) }

    private fun targetGit(vararg arguments: String): String {
        val (text, status) = runProcess(listOf("git") + arguments, target)
        check(status == 0) { text }
        return text.trim()
    }

    @Then("the committed plan matches the plan on disk")
    fun committedPlanMatches() {
        check(targetGit("show", "HEAD:./.factory/plan.md") ==
            Files.readString(target.resolve(".factory/plan.md")).trim())
    }

    @Then("the target has no uncommitted changes")
    fun targetClean() { check(targetGit("status", "--porcelain", "--", ".").isEmpty()) }

    @Then("the plan still has those three tasks")
    fun sameThreeTasks() { check(tasks().map { it.name } == listOf("alpha", "beta", "gamma")) }

    @Then("the plan is .factory\\/plan.md in the target")
    fun planInTarget() = thereIsAPlan()

    @Then("there is no plan in the factory's folder")
    fun noFactoryPlan() {
        check(!Files.exists(workspace.resolve("factory/.factory/plan.md")))
        check(!Files.exists(workspace.resolve("factory/plan.md")))
    }

    @Given("the agent keeps its plan in prose")
    fun prosePlan() { Files.writeString(workspace.resolve("prose-plan"), "yes") }

    @Then("the work for alpha and beta has been committed")
    fun alphaBetaCommitted() {
        for (name in listOf("alpha", "beta")) {
            check(targetGit("show", "HEAD:./$name.txt") == "Work for $name")
        }
        check(repo.logSince(headBeforeRun, target).size == 2)
    }

    @Given("no target is chosen")
    fun noTargetChosen() { targetChosen = false }

    @Then("it reports that a target is required")
    fun targetRequired() { check(exitCode != 0 && output.contains("target is required", true)) { output } }

    @Given("the target folder does not exist")
    fun targetMissing() { check(target.toFile().deleteRecursively()) }

    @Given("the target is outside any Git repository")
    fun standaloneTarget() {
        val outside = Files.createTempDirectory("standalone-target-")
        extraWorkspaces.add(outside)
        target = outside.resolve("product")
        absoluteTarget = true
    }

    @Then("the target uses the containing repository")
    fun usesContainingRepo() {
        check(Path.of(targetGit("rev-parse", "--show-toplevel")).toRealPath() == workspace.toRealPath())
        check(!Files.exists(target.resolve(".git")))
    }

    @Then("the target is a Git repository")
    fun targetIsRepo() {
        check(Files.isDirectory(target.resolve(".git")))
        check(Path.of(targetGit("rev-parse", "--show-toplevel")).toRealPath() == target.toRealPath())
    }

    private fun snapshot(folder: Path): Map<String, String> {
        if (!Files.exists(folder)) return emptyMap()
        return Files.walk(folder).use { files ->
            files.filter { Files.isRegularFile(it) && !folder.relativize(it).startsWith(".git") }
                .toList().associate { file ->
                    folder.relativize(file).toString() to
                        MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
                            .joinToString("") { "%02x".format(it) }
                }
        }
    }

    @Given("the factory has staged and unstaged changes")
    fun unrelatedChanges() {
        val file = workspace.resolve("factory/unrelated.txt")
        Files.writeString(file, "original\n")
        repo.git("add", "--", "factory/unrelated.txt")
        repo.commit("Unrelated fixture")
        Files.writeString(file, "staged\n")
        repo.git("add", "--", "factory/unrelated.txt")
        Files.writeString(file, "unstaged\n")
        Files.writeString(workspace.resolve("factory/untracked.txt"), "leave me alone")
        factoryBefore = snapshot(workspace.resolve("factory"))
        stagedBefore = repo.git("diff", "--cached", "--binary", "--", "factory")
        unstagedBefore = repo.git("diff", "--binary", "--", "factory")
    }

    @Then("the factory's own files and unrelated uncommitted changes are as they were")
    fun unrelatedUnchanged() {
        check(snapshot(workspace.resolve("factory")) == factoryBefore)
        check(repo.git("diff", "--cached", "--binary", "--", "factory") == stagedBefore)
        check(repo.git("diff", "--binary", "--", "factory") == unstagedBefore)
    }

    @When("the factory builds the target {string} to completion")
    fun buildTarget(name: String) {
        target = workspace.resolve(name)
        absoluteTarget = false
        runFactory("--all")
        check(exitCode == 0) { output }
        targetSnapshots.putIfAbsent(name, snapshot(target))
    }

    @When("the factory builds the same target using its absolute path")
    fun rebuildAbsolute() {
        absoluteTarget = true
        runFactory("--all")
        check(exitCode == 0) { output }
    }

    @Then("the target {string} is unchanged")
    fun targetUnchanged(name: String) {
        check(snapshot(workspace.resolve(name)) == targetSnapshots.getValue(name))
    }

    @Then("the targets {string} and {string} each have their own completed plan and committed work")
    fun independentTargets(first: String, second: String) {
        for (name in listOf(first, second)) {
            val folder = workspace.resolve(name)
            check(Plan(folder.resolve(".factory/plan.md")).entries().all { it.done })
            for (task in listOf("alpha", "beta")) {
                check(repo.git("show", "HEAD:$name/$task.txt").trim() == "Work for $task")
            }
            check(repo.git("show", "HEAD:$name/.factory/plan.md").trim() ==
                Files.readString(folder.resolve(".factory/plan.md")).trim())
            check(repo.git("status", "--porcelain", "--", name).isEmpty())
        }
    }

    private fun setupTestEnvironment(): Map<String, String> {
        val bin = Files.createDirectories(workspace.resolve("bin"))
        val pi = Files.copy(agent, bin.resolve("pi"), StandardCopyOption.REPLACE_EXISTING)
        pi.toFile().setExecutable(true)
        return mapOf(
            "PATH" to "$bin:${System.getenv("PATH")}",
            "GIT_CONFIG_COUNT" to "2",
            "GIT_CONFIG_KEY_0" to "user.name", "GIT_CONFIG_VALUE_0" to "Factory Test",
            "GIT_CONFIG_KEY_1" to "user.email", "GIT_CONFIG_VALUE_1" to "factory-test@example.invalid"
        )
    }

    @After
    fun cleanUp() {
        if (::workspace.isInitialized) workspace.toFile().deleteRecursively()
        extraWorkspaces.forEach { it.toFile().deleteRecursively() }
    }
}
