package steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.serialization.json.*

private data class Task(val name: String, val done: Boolean = false)
private data class RunFixture(val target: Path, val seed: Path, val line: String)
private data class InvocationCalls(val planner: Long, val doer: Long, val validator: Long)

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
    private var agentArgs: List<String> = emptyList()
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
    private var currentRun = "tetris"
    private var currentLine = "careful"
    private var targetNumber = 0
    private var seedNumber = 0
    private val runs = mutableMapOf<String, RunFixture>()
    private val callsByRun = mutableMapOf<String, InvocationCalls>()

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
        targetNumber++
        val name = if (targetNumber == 1) "target" else "target-$targetNumber"
        target = Files.createDirectory(workspace.resolve(name))
    }

    @Given("a seed describing a game of Tetris")
    fun createSeed() {
        seedNumber++
        val name = if (seedNumber == 1) "seed.md" else "seed-$seedNumber.md"
        seed = Files.writeString(
            workspace.resolve(name),
            """
            Build a game of Tetris that runs in the terminal.
            Start it with npm start.
            Keep the complete display within 24 terminal rows.
            """.trimIndent()
        )
    }

    @Given("a new target, with a seed describing a game of Tetris")
    fun newTetrisTargetAndSeed() {
        newTarget()
        createSeed()
    }

    @Given("a new target, with a seed describing a game of Snake")
    fun newSnakeTargetAndSeed() {
        newTarget()
        seedNumber++
        seed = Files.writeString(
            workspace.resolve("snake-seed-$seedNumber.md"),
            "Build a game of Snake that runs in the terminal.\n"
        )
    }

    @Given("the target has the machines planner, doer and validator")
    fun targetHasStandardMachines() {
        for (name in listOf("planner", "doer", "validator")) {
            Files.createDirectories(machineDirectory(name))
        }
    }
    
    @Given("the doer does the next task in the plan")
    fun prepareDoer() {
        val folder = Files.createDirectories(workspace.resolve("doer"))
        val doer = folder.resolve("agent.py")
        Files.copy(Path.of("src/test/doubles/agent.py"), doer)
        doer.toFile().setExecutable(true)
        configureMachine("doer", "harness", doer.toString())
    }

    @Given("the planner plans the tasks alpha and beta")
    fun preparePlanner() {
        agent = workspace.resolve("agent.py")
        Files.copy(Path.of("src/test/doubles/agent.py"), agent)
        agent.toFile().setExecutable(true)
        agentArgs = listOf()
        configureMachine("planner", "harness", agent.toString())
    }

    @Given("the validator is always satisfied")
    fun prepareValidator() {
        val folder = Files.createDirectories(workspace.resolve("validator"))
        val validator = folder.resolve("validator.py")
        Files.copy(Path.of("src/test/doubles/validator.py"), validator)
        validator.toFile().setExecutable(true)
        configureMachine("validator", "harness", validator.toString())
    }

    private fun configureMachine(name: String, key: String, value: String?) {
        val file = machineDirectory(name).resolve("config.json")
        val entries = if (Files.exists(file)) {
            Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"").findAll(Files.readString(file))
                .associate { it.groupValues[1] to it.groupValues[2] }.toMutableMap()
        } else mutableMapOf()
        if (value == null) entries.remove(key) else entries[key] = value
        Files.createDirectories(file.parent)
        Files.writeString(file, entries.entries.joinToString(",\n", "{\n", "\n}\n") {
            "  \"${it.key}\": \"${it.value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        })
        if (::factoryBefore.isInitialized) {
            factoryBefore = snapshot(workspace.resolve("factory"))
            stagedBefore = repo.git("diff", "--cached", "--binary", "--", "factory")
            unstagedBefore = repo.git("diff", "--binary", "--", "factory")
        }
    }

    private fun machineDirectory(name: String): Path =
        target.resolve(".assembly-lines/.machines/$name")

    @Given("the factory allows at most three attempts per pass")
    fun threeAttemptsPerPass() {
        agentArgs = agentArgs + listOf("--max-attempts", "3")
    }

    @Given("the factory allows at most three attempts at a task")
    fun threeAttemptsAtTask() = threeAttemptsPerPass()

    @Given("the factory allows at most one attempt at a task")
    fun oneAttemptAtTask() {
        agentArgs = agentArgs + listOf("--max-attempts", "1")
    }

    @Given("the factory allows at most two attempts at a task")
    fun twoAttemptsAtTask() {
        agentArgs = agentArgs + listOf("--max-attempts", "2")
    }

    @Given("the validator rejects the first attempt of each task")
    fun rejectEachFirstAttempt() {
        Files.writeString(workspace.resolve("validator/reject-alternate"), "yes")
    }

    @Given("a custom machine called inspector")
    fun customInspector() {
        val script = workspace.resolve("inspector.py")
        Files.writeString(script, "#!/usr/bin/env python3\nprint('{}')\n")
        script.toFile().setExecutable(true)
        configureMachine("inspector", "harness", script.toString())
    }

    @Then("each task starts without previous findings")
    fun findingsReset() {
        for (call in 0..3) {
            val prompt = Files.readString(workspace.resolve("doer/calls/$call.txt"))
            if (call % 2 == 0) {
                check(prompt.contains("Validator findings from the previous attempt: []")) { prompt }
            } else {
                check(prompt.contains("Separate game logic from terminal input so it can be tested.")) { prompt }
            }
        }
    }

    @Then("the initial planner is asked only for plan status")
    fun initialPlannerStatus() {
        val prompt = Files.readString(workspace.resolve("calls/0.txt"))
        check(prompt.contains("Otherwise, report its status without changing it.")) { prompt }
        check(!prompt.contains("The task's work has been committed.")) { prompt }
    }

    @Then("it prints the stop message")
    fun stopMessage() {
        check(output.trimEnd().endsWith("factory stopped")) { output }
    }

    @Given("the validator is never satisfied")
    fun validatorNeverSatisfied() {
        Files.writeString(workspace.resolve("validator/never-satisfied"), "yes")
    }

    @Given("the validator is not satisfied the first time")
    fun validatorRejectsFirst() {
        Files.writeString(workspace.resolve("validator/reject-first"), "yes")
    }

    @Then("it reports that the pass hit its limit")
    fun passHitLimit() {
        check(exitCode != 0 && output.contains("pass hit its limit", true)) { output }
    }

    @Then("it reports that a task hit its limit")
    fun taskHitLimit() {
        check(exitCode != 0 && output.contains("task hit its limit", true)) { output }
    }

    @Then("the doer was given the validator's findings")
    fun doerReceivedFindings() {
        val retry = workspace.resolve("doer/calls/1.txt")
        check(Files.exists(retry)) { "The doer was not retried.\n$output" }
        check(Files.readString(retry).contains(
            "Separate game logic from terminal input so it can be tested."
        )) { "The retry did not include the validator's finding." }
    }

    @Given("the doer writes a file called SENTINEL")
    fun doerWritesSentinel() {
        Files.writeString(workspace.resolve("doer/product-name.txt"), "SENTINEL")
    }

    @Given("the validator says {string} before its result")
    fun validatorSaysBeforeResult(message: String) {
        Files.writeString(workspace.resolve("validator/before-result.txt"), message)
    }

    @Given("the validator appends the JSON line {string} after its result")
    fun validatorTrailingJson(line: String) {
        Files.writeString(workspace.resolve("validator/after-result.txt"), line)
    }

    @Given("the validator returns its decision in {string}")
    fun validatorDecisionField(field: String) {
        Files.writeString(workspace.resolve("validator/result-field.txt"), field)
    }

    @Given("the target has tracked product changes and private factory metadata")
    fun targetProductFixture() {
        Files.writeString(target.resolve("existing.txt"), "original product\n")
        repo.git("add", "--", "target/existing.txt")
        repo.commit("Tracked product fixture")
        Files.writeString(target.resolve("existing.txt"), "changed product\n")
        Files.createDirectories(target.resolve(".factory"))
        Files.writeString(target.resolve(".factory/private.txt"), "PRIVATE_METADATA_SENTINEL\n")
    }

    @Then("the validator sees tracked and untracked target product changes only")
    fun validatorTargetOnlyDiff() {
        val diff = validatorPrompt().substringAfter("Product changes:")
        check(diff.contains("existing.txt") && diff.contains("+changed product")) { diff }
        check(diff.contains("alpha.txt") && diff.contains("+Work for alpha")) { diff }
        for (excluded in listOf(".factory", "PRIVATE_METADATA_SENTINEL", "unrelated.txt", "untracked.txt")) {
            check(!diff.contains(excluded)) { "Unexpected $excluded in product diff:\n$diff" }
        }
    }

    @Then("all new commits touch only the target")
    fun commitsTargetOnly() {
        val commits = repo.git("rev-list", "$headBeforeRun..HEAD").lines().filter { it.isNotBlank() }
        check(commits.isNotEmpty()) { output }
        val prefix = workspace.relativize(target).toString() + "/"
        commits.forEach { commit ->
            val paths = repo.diffTree(commit)
            check(paths.isNotEmpty() && paths.all { it.startsWith(prefix) }) { "$commit: $paths" }
        }
    }

    @Then("accepted task commits bracket the planner's completion changes")
    fun acceptedCommitOrdering() {
        val commits = repo.git("rev-list", "--reverse", "$headBeforeRun..HEAD").lines()
        check(commits.size == 2) { "Expected one product commit per task: $commits\n$output" }
        fun state(call: Int) = Json.parseToJsonElement(
            Files.readString(workspace.resolve("calls/$call.git-state"))
        ).jsonObject
        val initial = state(0)
        check(initial.getValue("head").jsonPrimitive.content == headBeforeRun)
        for ((call, workIndex) in listOf(1 to 0, 2 to 1)) {
            val snapshot = state(call)
            check(snapshot.getValue("head").jsonPrimitive.content == commits[workIndex]) { snapshot }
            val task = if (call == 1) "alpha" else "beta"
            check("target/$task.txt" in repo.diffTree(commits[workIndex]))
        }
    }

    @Given("the {word} configuration is {string}")
    fun rawMachineConfiguration(machine: String, content: String) {
        Files.createDirectories(machineDirectory(machine))
        Files.writeString(machineDirectory(machine).resolve("config.json"), content)
    }

    @Given("the {word} configuration is absent")
    fun absentMachineConfiguration(machine: String) {
        Files.deleteIfExists(machineDirectory(machine).resolve("config.json"))
    }

    @Given("the chosen model is {string}")
    fun chosenModel(model: String) {
        agentArgs = agentArgs + listOf("--model", model)
    }

    @Given("the validator uses the lens {string}")
    fun validatorLens(lens: String) = configureMachine("validator", "lens", lens)

    @Given("the doer exits unsuccessfully with output")
    fun unsuccessfulDoer() {
        Files.writeString(workspace.resolve("doer/agent.py"),
            "#!/usr/bin/env python3\nimport sys\nprint('failure stdout')\nprint('failure stderr', file=sys.stderr)\nsys.exit(7)\n")
    }

    @Then("it reports the diagnostic {string}")
    fun diagnosticReported(message: String) {
        check(exitCode != 0 && output.contains(message)) { output }
    }

    @Then("every machine receives the model {string} and the target context")
    fun machineInvocationContext(model: String) {
        for (call in listOf("calls/0.json", "doer/calls/0.json", "validator/calls/0.json")) {
            val record = Json.parseToJsonElement(Files.readString(workspace.resolve(call))).jsonObject
            val args = record.getValue("args").jsonArray.map { it.jsonPrimitive.content }
            check(args.take(4) == listOf("--print", "--no-session", "--model", model)) { args }
            check(args.size == 5) { args }
            check(record.getValue("cwd").jsonPrimitive.content == target.toString()) { record }
            val prompt = args.last()
            check(prompt.contains("Read the seed at \"$seed\".")) { prompt }
            check(prompt.contains("The plan is at \"${planPath()}\".")) { prompt }
            check(prompt.contains("Work only in the current target directory. The factory handles commits.")) { prompt }
            check(prompt.contains("End your answer with a single line of JSON.")) { prompt }
        }
    }

    @Given("the doer cannot be run")
    fun doerCannotBeRun() {
        check(workspace.resolve("doer/agent.py").toFile().setExecutable(false, false)) {
            "Could not remove the test doer's execute permission"
        }
    }

    @Given("no plan")
    fun noPlan() {
        Files.deleteIfExists(planPath())
    }

    @Given("a plan with three tasks, none of them done")
    fun planWithThreeTasks() = Plan(planPath())
        .write(listOf(Task("alpha"), Task("beta"), Task("gamma")))

    @Given("a plan in which every task is done")
    fun completedPlan() {
        Plan(planPath())
            .write(listOf(Task("alpha", done = true), Task("beta", done = true)))
    }

    @Given("no harness is chosen")
    fun noHarnessChosen() {
        agentArgs = listOf()
    }

    @Given("no harness is chosen for the validator")
    fun noValidatorHarness() = configureMachine("validator", "harness", null)

    @When("the factory runs")
    fun runAssemblyLine() = runFactory()

    @When("the factory runs one pass")
    fun runOnePass() = runFactory()

    @When("the factory runs to completion")
    fun runToCompletion() = runFactory("--all")

    @When("the factory runs the {string} run")
    fun runNamed(name: String) {
        currentRun = name
        val fixture = runs.getValue(name)
        target = fixture.target
        seed = fixture.seed
        currentLine = fixture.line
        runFactory()
    }

    @When("the factory runs the {string} run, given only its name")
    fun runNamedOnly(name: String) {
        currentRun = name
        val fixture = runs.getValue(name)
        target = fixture.target
        seed = fixture.seed
        currentLine = fixture.line
        runFactory(givenOnlyRunName = true)
    }

    @When("the factory runs the {string} run with that target")
    fun runNamedWithCurrentTarget(name: String) {
        currentRun = name
        runFactory(targetOnly = true)
    }

    private fun runFactory(
        vararg options: String,
        givenOnlyRunName: Boolean = false,
        targetOnly: Boolean = false
    ) {
        val environment = setupTestEnvironment()
        ensureDefaultRun()
        val fixture = runs.getValue(currentRun)
        val targetArgument = if (absoluteTarget) target.toString()
            else workspace.relativize(target).toString()
        val settings = when {
            givenOnlyRunName -> emptyList()
            targetOnly -> if (targetChosen) listOf("--target", targetArgument) else emptyList()
            else -> buildList {
                if (targetChosen) addAll(listOf("--target", targetArgument))
                addAll(listOf("--line", fixture.line))
                if (seedChosen) addAll(listOf("--seed", workspace.relativize(seed).toString()))
            }
        }
        val command = listOf(
            workspace.resolve("factory/factory").toString(), "--run", currentRun
        ) + settings + agentArgs + options

        headBeforeRun = repo.head()
        val before = invocationCalls()
        val result = runProcess(
            command,
            workspace,
            environment
        )
        output = result.first
        exitCode = result.second
        val after = invocationCalls()
        callsByRun[currentRun] = InvocationCalls(
            after.planner - before.planner,
            after.doer - before.doer,
            after.validator - before.validator
        )
    }

    private fun ensureDefaultRun() {
        if (!Files.exists(linePath())) writeValidatedLine(currentLine)
        runs.putIfAbsent(currentRun, RunFixture(target, seed, currentLine))
    }

    private fun planPath(run: String = currentRun): Path =
        workspace.resolve("factory/runs/$run/plan.md")

    private fun linePath(line: String = currentLine, targetPath: Path = target): Path =
        targetPath.resolve(".assembly-lines/$line.dot")

    private fun writeValidatedLine(name: String) {
        Files.createDirectories(linePath(name).parent)
        Files.writeString(linePath(name), """
            digraph assembly_line {
              start -> planner
              planner -> doer [label="not complete"]
              planner -> finish [label="complete"]
              doer -> validator
              validator -> doer [label="not satisfied"]
              validator -> planner [label="satisfied"]
            }
        """.trimIndent())
    }

    private fun invocationCalls(): InvocationCalls = InvocationCalls(
        countCalls(workspace.resolve("calls")),
        countCalls(workspace.resolve("doer/calls")),
        countCalls(workspace.resolve("validator/calls"))
    )

    private fun countCalls(folder: Path): Long =
        if (!Files.isDirectory(folder)) 0 else Files.list(folder).use { stream ->
            stream.filter { it.toString().endsWith(".json") }.count()
        }

    @Given("this assembly line:")
    fun thisAssemblyLine(line: String) {
        Files.createDirectories(linePath().parent)
        Files.writeString(linePath(), line)
    }

    @Given("the target has an assembly line {string} on which the doer's work is validated")
    fun targetHasValidatedLine(name: String) {
        currentLine = name
        writeValidatedLine(name)
    }

    @Given("the target has an assembly line {string} on which the doer goes straight to the planner")
    fun targetHasQuickLine(name: String) {
        currentLine = name
        Files.createDirectories(linePath(name).parent)
        Files.writeString(linePath(name), """
            digraph assembly_line {
              start -> planner
              planner -> doer [label="not complete"]
              planner -> finish [label="complete"]
              doer -> planner
            }
        """.trimIndent())
    }

    @Given("the {string} line has been copied into the target")
    fun copyLineIntoTarget(name: String) {
        val sourceTarget = runs.getValue("tetris").target
        val source = linePath(name, sourceTarget)
        Files.createDirectories(linePath(name).parent)
        Files.copy(source, linePath(name), StandardCopyOption.REPLACE_EXISTING)
        currentLine = name
    }

    @Given("a run named {string}, on the {string} line, with that seed and target")
    fun namedRun(name: String, line: String) {
        currentRun = name
        currentLine = line
        runs[name] = RunFixture(target, seed, line)
    }

    @Given("a plan for each run with three tasks of its own, none of them done")
    fun planForEachRun() {
        runs.keys.forEach { name ->
            Plan(planPath(name)).write(listOf(Task("alpha"), Task("beta"), Task("gamma")))
        }
    }

    @Given("the {string} run has been started")
    fun runHasStarted(name: String) {
        currentRun = name
        val fixture = runs.getValue(name)
        target = fixture.target
        seed = fixture.seed
        currentLine = fixture.line
        runFactory("--check-line")
        check(exitCode == 0) { output }
    }

    @Given("the validator has been taken out, so the doer goes straight to the planner")
    fun removeValidator() {
        if (!Files.exists(linePath())) writeValidatedLine(currentLine)
        val line = Files.readString(linePath())
            .lineSequence()
            .filterNot { it.contains("validator") }
            .toMutableList()
        val closing = line.indexOfLast { it.trim() == "}" }
        line.add(if (closing < 0) line.size else closing, "  doer -> planner")
        Files.writeString(linePath(), line.joinToString("\n"))
    }

    @Given("the validator has been taken out of the {string} line, so the doer goes straight to the planner")
    fun removeValidatorFrom(line: String) {
        currentLine = line
        removeValidator()
    }

    @Given("{string} is misspelt {string} throughout the assembly line")
    fun misspellMachine(correct: String, misspelling: String) {
        Files.writeString(linePath(), Files.readString(linePath()).replace(correct, misspelling))
    }

    @Given("{string} is misspelt {string} throughout the {string} line")
    fun misspellMachineInLine(correct: String, misspelling: String, line: String) {
        currentLine = line
        misspellMachine(correct, misspelling)
    }

    @Given("the edge from validator to planner has been taken out")
    fun removeValidatorToPlanner() {
        val lines = Files.readAllLines(linePath()).filterNot {
            it.contains("validator -> planner")
        }
        Files.write(linePath(), lines)
    }

    @Given("the edges from validator are labelled {string} and {string}")
    fun relabelValidatorEdges(positive: String, negative: String) {
        if (!Files.exists(linePath())) writeValidatedLine(currentLine)
        val changed = Files.readString(linePath())
            .replace("label=\"not satisfied\"", "label=\"$negative\"")
            .replace("label=\"satisfied\"", "label=\"$positive\"")
        Files.writeString(linePath(), changed)
    }

    @When("the factory reads the assembly line")
    fun readAssemblyLine() = runFactory("--check-line")

    @Then("it accepts it")
    fun lineAccepted() { check(exitCode == 0) { output } }

    @Then("it refuses it")
    fun lineRefused() { check(exitCode != 0) { output } }

    @Then("the factory refuses")
    fun factoryRefuses() = lineRefused()

    @Then("it says the {string} run already has a target")
    fun runAlreadyHasTarget(name: String) {
        check(output.contains("The \"$name\" run already has a target")) { output }
    }

    @Then("it reports that it has no machine called {string}")
    fun missingMachineReported(name: String) {
        check(output.contains("no machine called \"$name\"", true)) { output }
    }

    @Then("it reports that finish cannot be reached from validator")
    fun unreachableFinishReported() {
        check(output.contains("finish cannot be reached from validator", true)) { output }
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

    @Then("it reports that it could not run the doer")
    fun reportsDoerCouldNotRun() {
        check(exitCode != 0 && output.contains("could not run the doer", ignoreCase = true)) {
            "Expected a doer startup error and nonzero exit code.\n$output"
        }
    }

    @Then("there is no plan")
    fun thereIsNoPlan() {
        check(!Files.exists(planPath())) {
            "Expected no plan after the factory ran.\n$output"
        }
    }

    @Then("there is no plan in the target")
    fun noPlanInTarget() {
        check(!Files.exists(target.resolve(".factory/plan.md")))
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
    fun planHasExactlyTwoTasks(first: String, second: String) =
        check(Plan(planPath()).entries().map { it.name } == listOf(first, second)) {
            "Expected only tasks $first and $second.\n$output"
        }

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

    @Then("the doer was pointed at the plan and at the seed")
    fun doerReceivedPlanAndSeed() {
        val arguments = Files.readString(workspace.resolve("doer/calls/0.txt"))
        val plan = planPath().toAbsolutePath()
        val seedPath = seed.toAbsolutePath()

        check(arguments.contains(plan.toString())) {
            "Doer was not given the plan path: $plan\n$arguments"
        }
        check(arguments.contains(seedPath.toString())) {
            "Doer was not given the seed path: $seedPath\n$arguments"
        }
    }

    @Then("the planner was asked for a result with the field {string}")
    fun plannerWasAskedForResult(field: String) {
        check(Files.exists(workspace.resolve("calls/0.txt"))) { "Planner was not called.\n$output" }
        val arguments = Files.readString(workspace.resolve("calls/0.txt"))

        check(arguments.contains("JSON", ignoreCase = true) &&
            arguments.contains("\"$field\"")) {
            "Expected a request for a JSON result with field \"$field\".\n$arguments"
        }
    }

    private fun validatorPrompt(): String {
        val call = workspace.resolve("validator/calls/0.txt")
        check(Files.exists(call)) { "Validator was not called.\n$output" }
        return Files.readString(call)
    }

    @Then("the validator was asked for a result with the fields {string} and {string}")
    fun validatorResultFields(first: String, second: String) {
        val prompt = validatorPrompt()
        check(prompt.contains("JSON", true) && prompt.contains("\"$first\"") &&
            prompt.contains("\"$second\"")) { prompt }
    }

    @Then("the validator was asked for a result with the field {string}")
    fun validatorResultField(field: String) {
        val prompt = validatorPrompt()
        check(prompt.contains("JSON", true) && prompt.contains("\"$field\"")) { prompt }
    }

    @Given("the validator's lens is testability")
    fun testabilityLens() {
        configureMachine("validator", "lens", "testability")
    }

    @Then("the validator was given {string}")
    fun validatorGiven(value: String) {
        check(validatorPrompt().contains(value)) { "Validator was not given $value" }
    }

    @Then("the validator was given the work for the second task")
    fun validatorGivenSecondTask() {
        val prompt = validatorPrompt()
        check(prompt.contains("beta.txt") && prompt.contains("Work for beta")) { prompt }
    }

    @Then("it was not given the work for the first task")
    fun validatorNotGivenFirstTask() {
        val prompt = validatorPrompt()
        check(!prompt.contains("alpha.txt") && !prompt.contains("Work for alpha")) { prompt }
    }

    private fun assertDoerCalls(expected: Long) {
        val calls = workspace.resolve("doer/calls")
        val actual = if (!Files.isDirectory(calls)) 0L else Files.list(calls).use { files ->
            files.filter { it.toString().endsWith(".json") }.count()
        }
        check(actual == expected) { "Expected $expected doer calls, found $actual.\n$output" }
    }

    @Then("the doer has been called once")
    fun doerCalledOnce() = assertDoerCalls(1)

    @Then("the doer has been called twice")
    fun doerCalledTwice() = assertDoerCalls(2)

    @Then("the doer has been called three times")
    fun doerCalledThreeTimes() = assertDoerCalls(3)

    @Then("the doer has been called four times")
    fun doerCalledFourTimes() = assertDoerCalls(4)

    @Then("the doer has not been called")
    fun doerNotCalled() = assertDoerCalls(0)

    @Then("the factory has stopped")
    fun factoryStopped() {
        // runProcess has waited for the child to exit; the sentinel is never -1 afterward.
        check(exitCode != -1) { "Factory has not exited" }
    }

    @Given("a plan whose first task is done")
    fun firstTaskDone() {
        Plan(planPath())
            .write(listOf(Task("alpha", true), Task("beta"), Task("gamma")))
        Files.writeString(target.resolve("alpha.txt"), "Work for alpha\n")
        repo.git("add", "--", workspace.relativize(target).toString())
        repo.commit("Completed first task fixture")
    }

    private fun tasks() = Plan(planPath()).entries()

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

    @Then("there are two new work commits")
    fun twoWorkCommits() { check(repo.logSince(headBeforeRun, target).size == 2) }

    @Then("each new work commit contains the work for one task")
    fun eachCommitContainsOneTask() {
        val commits = repo.logSince(headBeforeRun, target)
        check(commits.isNotEmpty()) { "Expected work commits.\n$output" }
        val targetPath = workspace.relativize(target).toString()
        commits.forEach { commit ->
            val products = repo.diffTree(commit).filterNot { it.startsWith("$targetPath/.factory/") }
            check(products.size == 1 && products.single().endsWith(".txt")) {
                "Expected one product file in $commit, found $products"
            }
        }
    }

    @Then("no new commit contains the work for the first task")
    fun noCommitContainsFirstTask() {
        val alpha = workspace.relativize(target.resolve("alpha.txt")).toString()
        check(repo.logSince(headBeforeRun, target).none { alpha in repo.diffTree(it) })
    }

    @Then("the validator has not been called")
    fun validatorNotCalled() {
        check(!Files.exists(workspace.resolve("validator/calls"))) { output }
    }

    @Then("the validator was called for the {string} run")
    fun validatorCalledForRun(name: String) {
        check(callsByRun.getValue(name).validator > 0) { callsByRun.toString() }
    }

    @Then("the validator was not called for the {string} run")
    fun validatorNotCalledForRun(name: String) {
        check(callsByRun.getValue(name).validator == 0L) { callsByRun.toString() }
    }

    @Then("the validator has been called twice")
    fun validatorCalledTwice() {
        check(callsByRun.getValue(currentRun).validator == 2L) { callsByRun.toString() }
    }

    @Then("it reports that the result of validator has no field {string}")
    fun missingValidatorResultField(field: String) {
        check(exitCode != 0 && output.contains("result of validator has no field \"$field\"", true)) {
            output
        }
    }

    @Then("the planner was called before the doer")
    fun plannerCalledBeforeDoer() {
        val order = Files.readAllLines(workspace.resolve("call-order.txt"))
        check(order.indexOf("planner") in 0 until order.indexOf("doer")) { order }
    }

    @Given("the validator answers in prose, with no result")
    fun validatorAnswersInProse() { Files.writeString(workspace.resolve("validator/no-result"), "yes") }

    @Then("it reports that it could not read the validator's result")
    fun unreadableResult() {
        check(exitCode != 0 && output.contains("could not read the validator's result", true)) { output }
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
        check(!Files.exists(workspace.resolve("doer/calls")))
        check(!Files.exists(workspace.resolve("validator/calls")))
        check(!Files.exists(workspace.resolve("bin/calls")))
    }

    @Then("there is a plan")
    fun thereIsAPlan() { check(Files.isRegularFile(planPath())) }

    @Then("the plan is plan.md in the factory's runs folder, under tetris")
    fun planInRunsFolder() {
        check(Files.isRegularFile(workspace.resolve("factory/runs/tetris/plan.md")))
    }

    @Then("each run has its own plan")
    fun eachRunHasOwnPlan() {
        val paths = runs.keys.map(::planPath)
        check(paths.distinct().size == paths.size && paths.all(Files::isRegularFile)) { paths }
    }

    @Then("each target holds only its own run's work")
    fun eachTargetHoldsOwnWork() {
        for ((name, fixture) in runs) {
            check(Files.isRegularFile(fixture.target.resolve("alpha.txt"))) { "$name has no alpha work" }
            check(Files.isRegularFile(fixture.target.resolve("beta.txt"))) { "$name has no beta work" }
            check(!Files.exists(fixture.target.resolve(".factory/plan.md")))
        }
    }

    @Then("there are no new work commits")
    fun noWorkCommits() { check(repo.logSince(headBeforeRun, target).isEmpty()) }

    private fun targetGit(vararg arguments: String): String {
        val (text, status) = runProcess(listOf("git") + arguments, target)
        check(status == 0) { text }
        return text.trim()
    }

    @Then("the committed plan matches the plan on disk")
    fun committedPlanMatches() {
        check(Files.isRegularFile(planPath()))
        check(!Files.exists(target.resolve(".factory/plan.md")))
    }

    @Then("the target has no uncommitted changes")
    fun targetClean() {
        val status = targetGit(
            "status", "--porcelain", "--", ".",
            ":(exclude).assembly-lines", ":(exclude).factory"
        )
        check(status.isEmpty()) { status }
    }

    @Then("the plan still has those three tasks")
    fun sameThreeTasks() { check(tasks().map { it.name } == listOf("alpha", "beta", "gamma")) }

    @Then("the plan is .factory\\/plan.md in the target")
    fun planInTarget() = thereIsAPlan()

    @Then("there is no plan in the factory's folder")
    fun noFactoryPlan() {
        check(!Files.exists(target.resolve(".factory/plan.md")))
    }

    @Given("the planner keeps its plan in prose")
    fun plannerProsePlan() { Files.writeString(workspace.resolve("prose-plan"), "yes") }

    @Given("the doer keeps its plan in prose")
    fun doerProsePlan() { Files.writeString(workspace.resolve("doer/prose-plan"), "yes") }

    @Then("the work for alpha and beta has been committed")
    fun alphaBetaCommitted() {
        for (name in listOf("alpha", "beta")) {
            check(targetGit("show", "HEAD:./$name.txt") == "Work for $name")
        }
        if (Files.isDirectory(target.resolve(".git"))) {
            check(targetGit("rev-list", "--count", "HEAD") == "2")
        } else {
            check(repo.logSince(headBeforeRun, target).size == 2)
        }
    }

    @Given("no target is chosen")
    fun noTargetChosen() { targetChosen = false }

    @Then("it reports that a target is required")
    fun targetRequired() { check(exitCode != 0 && output.contains("target is required", true)) { output } }

    @Given("the target folder does not exist")
    fun targetMissing() { check(target.toFile().deleteRecursively()) }

    @Given("the target is outside any Git repository")
    fun standaloneTarget() {
        val preparedTarget = target
        val outside = Files.createTempDirectory("standalone-target-")
        extraWorkspaces.add(outside)
        target = outside.resolve("product")
        preparedTarget.toFile().copyRecursively(target.toFile())
        runs[currentRun] = runs.getValue(currentRun).copy(target = target)
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
            files.filter {
                val relative = folder.relativize(it)
                Files.isRegularFile(it) && !relative.startsWith(".git") && !relative.startsWith("runs")
            }
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
            val fixture = runs.getValue(name)
            val folder = fixture.target
            check(Plan(planPath(name)).entries().all { it.done })
            for (task in listOf("alpha", "beta")) {
                val relative = workspace.relativize(folder.resolve("$task.txt")).toString()
                check(repo.git("show", "HEAD:$relative").trim() == "Work for $task")
            }
        }
    }

    private fun setupTestEnvironment(): Map<String, String> {
        val bin = Files.createDirectories(workspace.resolve("bin"))
        if (::agent.isInitialized) {
            val pi = Files.copy(agent, bin.resolve("pi"), StandardCopyOption.REPLACE_EXISTING)
            pi.toFile().setExecutable(true)
        }
        return mapOf(
            "PATH" to "$bin:${System.getenv("PATH")}",
            "FACTORY_TEST_WORKSPACE" to workspace.toString(),
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
