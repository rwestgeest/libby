package steps

import io.cucumber.java.After
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.cucumber.java.en.Then
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
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
    val output = CompletableFuture.supplyAsync {
        process.inputStream.bufferedReader().use { it.readText() }
    }
    if (!process.waitFor(60, TimeUnit.SECONDS)) {
        process.descendants().use { children -> children.forEach { it.destroyForcibly() } }
        process.destroyForcibly()
        error("Process did not finish within 60 seconds: ${command.first()}")
    }
    return output.get(5, TimeUnit.SECONDS) to process.exitValue()
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
    private val environmentOverrides = mutableMapOf<String, String>()
    private val callsByRun = mutableMapOf<String, InvocationCalls>()
    private var watchedOutput = ""
    private var watcher: Process? = null
    private var watcherLog: Path? = null

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
            prepareMachineFolder(name)
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

    @Given("the pi doer returns only prose")
    fun piDoerReturnsOnlyProse() {
        val executable = workspace.resolve("doer/pi")
        Files.copy(Path.of("src/test/doubles/agent.py"), executable)
        executable.toFile().setExecutable(true)
        configureMachine("doer", "harness", executable.toString())
        Files.writeString(workspace.resolve("doer/prose-handoff"), "yes")
    }

    @Given("the pi doer uses the ACP bridge")
    fun piDoerUsesAcpBridge() {
        configureMachine("doer", "harness", "pi-rpc-acp")
        environmentOverrides["FACTORY_TEST_PI_EXECUTABLE"] = workspace.resolve("doer/pi").toString()
    }

    @Given("the format repair also returns prose")
    fun formatRepairReturnsProse() { Files.writeString(workspace.resolve("doer/repair-prose"), "yes") }

    @Given("the format repair reports missing external input")
    fun formatRepairReportsBlocked() { Files.writeString(workspace.resolve("doer/repair-blocked"), "yes") }

    @Then("each prose handoff was repaired once without tools")
    fun proseHandoffsRepairedWithoutTools() {
        val calls = workspace.resolve("doer/calls").toFile().listFiles()!!.filter { it.extension == "json" }
        check(calls.size == 4) { "Expected implementation plus repair for each task" }
        check(calls.count { "--no-tools" in it.readText() } == 2)
        check(Files.readString(target.resolve("alpha.txt")) == "Work for alpha\n")
        check(Files.readString(target.resolve("beta.txt")) == "Work for beta\n")
    }

    @Then("the original prose remains in the run record")
    fun originalProseInRecord() {
        check(Files.readString(workspace.resolve("factory/runs/$currentRun/record.jsonl"))
            .contains("Here's my summary:"))
    }

    @Given("agents format results as fenced multiline JSON")
    fun agentsFormatResultsAsFencedMultilineJson() {
        Files.writeString(workspace.resolve("pretty-json-result"), "yes\n")
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

    private fun prepareMachineFolder(name: String) {
        val directory = machineDirectory(name)
        Files.createDirectories(directory)
        if (!Files.exists(directory.resolve("AGENTS.md"))) {
            val role = if (name.startsWith("reviewer_")) "reviewer" else name
            val example = Path.of("examples/.machines/$role/AGENTS.md")
            Files.writeString(directory.resolve("AGENTS.md"),
                if (Files.exists(example)) Files.readString(example) else "Report the result of your job.\n")
            Files.writeString(directory.resolve("auth.json"), "{\"test_login\": true}\n")
            Files.writeString(directory.resolve(".gitignore"), "auth.json\n.credentials.json\n")
        }
    }

    private fun configureMachine(name: String, key: String, value: String?) {
        prepareMachineFolder(name)
        if (key == "prompt") {
            Files.writeString(machineDirectory(name).resolve("AGENTS.md"), value.orEmpty() + "\n")
            return
        }
        if (key == "lens") {
            val instructions = machineDirectory(name).resolve("AGENTS.md")
            val previous = Files.readString(instructions)
            val lens = "Your validation lens is: $value."
            val text = if (previous.contains("Your validation lens is:"))
                previous.replace(Regex("Your validation lens is: [^\n]+"), lens)
                else previous.trimEnd() + "\n" + lens + "\n"
            Files.writeString(instructions, text)
            return
        }
        val file = machineDirectory(name).resolve("config.json")
        val entries = if (Files.exists(file)) {
            Json.parseToJsonElement(Files.readString(file)).jsonObject
                .mapValues { it.value.jsonPrimitive.content }.toMutableMap()
        } else mutableMapOf()
        if (value == null) entries.remove(key) else entries[key] = value
        Files.createDirectories(file.parent)
        Files.writeString(file, JsonObject(entries.mapValues { JsonPrimitive(it.value) }).toString() + "\n")
        if (::factoryBefore.isInitialized) {
            factoryBefore = snapshot(workspace.resolve("factory"))
            stagedBefore = repo.git("diff", "--cached", "--binary", "--", "factory")
            unstagedBefore = repo.git("diff", "--binary", "--", "factory")
        }
    }

    private fun machineDirectory(name: String): Path =
        target.resolve(".assembly-lines/.machines/$name")

    private val reviewers = (1..3).map { "reviewer_$it" }

    @Given("the target has the three big brains")
    fun threeBigBrains() {
        (reviewers + "synthesiser").forEach(::prepareMachineFolder)
    }

    @Then("it reports that the branches from doer do not meet")
    fun branchesDoNotMeet() {
        check(exitCode != 0 && output.contains("branches from doer do not meet", true)) { output }
    }

    private val reviewJob = """
        Check the doer's uncommitted product work, including new files, through the configured lens.
        Review only the supplied changes. Report findings without changing the plan or product.
        Return a JSON result with a boolean "satisfied" and an array "findings".
    """.trimIndent()

    @Given("the three big brains has replaced the validator on the {string} line")
    fun replaceValidatorWithBrains(line: String) {
        threeBigBrains()
        currentLine = line
        val replacement = reviewers.joinToString("\n") { "doer -> $it\n$it -> synthesiser" }
        Files.writeString(linePath(), Files.readString(linePath())
            .replace("doer -> validator", replacement).replace("validator ->", "synthesiser ->"))
        reviewers.forEachIndexed { index, reviewer ->
            configureMachine(reviewer, "prompt", reviewJob)
            configureMachine(reviewer, "model", listOf("openai/reviewer", "anthropic/reviewer", "google/reviewer")[index])
        }
        configureMachine("synthesiser", "prompt",
            "Decide whether the reviewers' reports justify accepting the attempt. Return your decision and findings. Do not inspect or change files.")
    }

    private fun prepareReviewMachine(name: String) {
        val folder = Files.createDirectories(workspace.resolve(name))
        val harness = Files.copy(Path.of("src/test/doubles/review.py"), folder.resolve("review.py"))
        harness.toFile().setExecutable(true)
        configureMachine(name, "harness", harness.toString())
    }

    @Given("every reviewer is always satisfied")
    fun satisfiedReviewers() = reviewers.forEach(::prepareReviewMachine)

    @Given("the synthesiser is always satisfied")
    fun satisfiedSynthesiser() = prepareReviewMachine("synthesiser")

    @Given("a plan with one task, not done")
    fun oneUnfinishedTask() = Plan(planPath()).write(listOf(Task("alpha")))

    @Then("every reviewer has been called once")
    fun reviewersCalledOnce() {
        reviewers.forEach { check(countCalls(workspace.resolve("$it/calls")) == 1L) { "$it\n$output" } }
    }

    @Then("the synthesiser has been called once")
    fun synthesiserCalledOnce() {
        check(countCalls(workspace.resolve("synthesiser/calls")) == 1L) { output }
    }

    @Given("no reviewer finishes until all three have begun")
    fun reviewersWaitForPeers() {
        val barrier = Files.createDirectory(workspace.resolve("review-barrier"))
        val (message, status) = runProcess(listOf("mkfifo") + reviewers.map { barrier.resolve(it).toString() }, workspace)
        check(status == 0) { message }
    }

    private fun reviewPrompt(name: String, call: Int = 0): String {
        val path = workspace.resolve("$name/calls/$call.txt")
        check(Files.isRegularFile(path)) { "$name was not called.\n$output" }
        return Files.readString(path)
    }

    @Then("every reviewer was asked for a result")
    fun reviewersAskedForResult() {
        reviewers.forEach {
            val prompt = reviewPrompt(it)
            check(prompt.contains("JSON") && prompt.contains("result")) { prompt }
        }
    }

    private fun reviewerFinding(name: String, finding: String) {
        Files.writeString(workspace.resolve("$name/never-satisfied"), "yes")
        Files.writeString(workspace.resolve("$name/finding.txt"), finding)
    }

    @Given("every reviewer is not satisfied, with a finding of its own: {string}, {string} or {string}")
    fun allReviewerFindings(first: String, second: String, third: String) {
        reviewers.zip(listOf(first, second, third)).forEach { (name, finding) -> reviewerFinding(name, finding) }
    }

    private var inspectedPrompts = emptyList<String>()

    @Then("the synthesiser was given {string}, {string} and {string}")
    fun synthesiserGivenReports(first: String, second: String, third: String) {
        listOf(first, second, third).forEach(::synthesiserGivenReport)
        val results = Json.parseToJsonElement(reviewPrompt("synthesiser").substringAfter("Branch results:\n")).jsonObject
        check(results.keys == reviewers.toSet()) { results }
        reviewers.zip(listOf(first, second, third)).forEach { (name, finding) ->
            check(results.getValue(name).jsonObject.getValue("findings").jsonArray == JsonArray(listOf(JsonPrimitive(finding)))) { results }
        }
    }

    @Given("the second reviewer is not satisfied, with the finding {string}")
    fun secondReviewerFinding(finding: String) = reviewerFinding("reviewer_2", finding)

    @Then("the synthesiser was given {string}")
    fun synthesiserGivenReport(report: String) {
        val prompt = reviewPrompt("synthesiser")
        check(prompt.contains(report)) { prompt }
        check(!prompt.contains(seed.toString()) && !prompt.contains(planPath().toString())) { prompt }
        inspectedPrompts = listOf(prompt)
    }

    @Given("the synthesiser is never satisfied")
    fun synthesiserNeverSatisfied() {
        Files.writeString(workspace.resolve("synthesiser/never-satisfied"), "yes")
        Files.writeString(workspace.resolve("synthesiser/finding.txt"), "Synthesis: resolve the review concerns.")
    }

    @Given("the synthesiser is not satisfied the first time")
    fun synthesiserRejectsFirst() {
        Files.writeString(workspace.resolve("synthesiser/reject-first"), "yes")
        Files.writeString(workspace.resolve("synthesiser/finding.txt"), "Synthesis: resolve the review concerns.")
    }

    @Then("the doer was given the synthesiser's findings")
    fun doerReceivedSynthesis() {
        check(reviewPrompt("doer", 1).contains("Synthesis: resolve the review concerns."))
    }

    @Then("it was not given {string}")
    fun retryNotGiven(report: String) {
        val prompt = if (Files.exists(workspace.resolve("synthesiser/calls"))) reviewPrompt("doer", 1)
            else validatorPrompt()
        check(!prompt.contains(report)) { prompt }
    }

    @Given("the second reviewer answers in prose, with no result")
    fun secondReviewerNoResult() { Files.writeString(workspace.resolve("reviewer_2/no-result"), "yes") }

    @Then("it reports that it could not read reviewer_{int}'s result")
    fun reviewerUnreadable(number: Int) {
        check(exitCode != 0 && output.contains("could not read the reviewer_$number's result", true)) { output }
    }

    @Then("the synthesiser has not been called")
    fun synthesiserNotCalled() { check(countCalls(workspace.resolve("synthesiser/calls")) == 0L) { output } }

    @Given("the second reviewer cannot be run")
    fun secondReviewerCannotRun() {
        check(workspace.resolve("reviewer_2/review.py").toFile().setExecutable(false, false))
    }

    @Given("the reviewers run on Anthropic, Google and OpenAI models")
    fun reviewersOnProviders() {
        reviewers.zip(listOf("anthropic/reviewer", "google/reviewer", "openai/reviewer")).forEach { (machine, model) ->
            configureMachine(machine, "model", model)
        }
    }

    @Given("the doer says {string}, then {string}")
    fun doerSaysThen(first: String, second: String) {
        Files.writeString(workspace.resolve("doer/before-result.txt"), "$first\n")
        Files.writeString(workspace.resolve("doer/hold"), "yes")
        Files.writeString(workspace.resolve("doer/after-result.txt"), second)
    }

    @Given("the {string} run is running, with the doer part-way through an attempt")
    fun runRunningInAttempt(name: String) = beginHeldRun(name, false)

    @Given("the {string} run is running, with the doer part-way through an attempt, having said {string}")
    fun runRunningWithDoerOutput(name: String, said: String) {
        Files.writeString(workspace.resolve("doer/before-result.txt"), "$said\n")
        beginHeldRun(name, false)
    }

    @Given("the {string} run is running, with every reviewer part-way through")
    fun runRunningWithReviewers(name: String) = beginHeldRun(name, true)

    private fun beginHeldRun(name: String, reviewersHeld: Boolean) {
        currentRun = name
        if (reviewersHeld) reviewers.forEach { Files.writeString(workspace.resolve("$it/hold"), "yes") }
        else Files.writeString(workspace.resolve("doer/hold"), "yes")
        runFactory(waitForCompletion = false)
        val held = if (reviewersHeld) reviewers else listOf("doer")
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (held.any { !Files.exists(workspace.resolve("$it/ready")) } && System.nanoTime() < deadline) Thread.sleep(20)
        check(held.all { Files.exists(workspace.resolve("$it/ready")) }) { "The machines did not reach their held state.\n$output" }
    }

    @When("I watch the {string} run")
    fun watchNamedRun(name: String) {
        currentRun = name
        val out = workspace.resolve("watch-$name.txt")
        watcherLog = out
        watcher = ProcessBuilder(workspace.resolve("factory/factory").toString(), "watch", name)
            .directory(workspace.toFile()).redirectErrorStream(true).redirectOutput(out.toFile())
            .apply { environment().putAll(setupTestEnvironment()) }.start()
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(15)
        while (System.nanoTime() < deadline && watcher!!.isAlive && (!Files.exists(out) || Files.readString(out).isBlank())) Thread.sleep(20)
        val currentRunFile = workspace.resolve("factory/runs/.daemon/current")
        if (!Files.exists(currentRunFile) || Files.readString(currentRunFile).trim() != name) {
            watcher!!.waitFor(10, TimeUnit.SECONDS)
            watcher = null
        }
        watchedOutput = if (Files.exists(out)) Files.readString(out) else ""
        if (watcher?.isAlive == false) watcher = null
    }

    @Given("I am watching the {string} run as it runs")
    fun watchAsRunStarts(name: String) {
        Files.writeString(workspace.resolve("doer/hold"), "yes")
        Files.writeString(workspace.resolve("doer/before-result.txt"), "started\n")
        beginHeldRun(name, false)
        watchNamedRun(name)
    }

    @Given("I am watching the {string} run")
    fun watchRunningRun(name: String) = watchNamedRun(name)

    @When("I start the {string} run")
    fun startNamedRun(name: String) {
        currentRun = name
        val fixture = runs.getValue(name)
        target = fixture.target; seed = fixture.seed; currentLine = fixture.line
        runFactory(waitForCompletion = false)
    }

    @When("the {string} run has run to the end")
    fun releaseRun(name: String) {
        listOf("doer", "reviewer_1", "reviewer_2", "reviewer_3").forEach {
            val dir = workspace.resolve(it)
            if (Files.exists(dir.resolve("hold"))) Files.writeString(dir.resolve("release"), "yes")
        }
        runProcess(listOf(workspace.resolve("factory/factory").toString(), "wait", name), workspace, setupTestEnvironment())
        watcher?.waitFor(15, TimeUnit.SECONDS)
        watcherLog?.let { if (Files.exists(it)) watchedOutput = Files.readString(it) }
        watcher = null
        val log = workspace.resolve("factory/runs/$name/daemon.log")
        if (Files.exists(log)) output += Files.readString(log)
        val status = workspace.resolve("factory/runs/$name/status")
        if (Files.exists(status) && Files.readString(status).startsWith("failed")) exitCode = 1
    }

    @When("I have stopped the {string} run")
    fun stopRunAsFixture(name: String) {
        runProcess(listOf(workspace.resolve("factory/factory").toString(), "stop", name), workspace, setupTestEnvironment())
        listOf("doer", "reviewer_1", "reviewer_2", "reviewer_3").forEach {
            Files.deleteIfExists(workspace.resolve("$it/hold"))
        }
    }

    @When("I stop the {string} run")
    fun stopNamedRun(name: String) {
        runProcess(listOf(workspace.resolve("factory/factory").toString(), "stop", name), workspace, setupTestEnvironment())
        listOf("doer", "reviewer_1", "reviewer_2", "reviewer_3").forEach {
            Files.deleteIfExists(workspace.resolve("$it/hold"))
        }
        watcher?.waitFor(10, TimeUnit.SECONDS)
        watcherLog?.let { if (Files.exists(it)) watchedOutput = Files.readString(it) }
        watcher = null
    }

    @When("I stop the factory")
    fun stopFactory() {
        runProcess(listOf(workspace.resolve("factory/factory").toString(), "stop"), workspace, setupTestEnvironment())
    }

    @Then("the command has returned while the {string} run is running")
    fun commandReturnedRunning(name: String) {
        check(Files.readString(workspace.resolve("factory/runs/.daemon/current")).trim() == name)
        check(ProcessHandle.of(Files.readString(workspace.resolve("factory/runs/.daemon/pid")).trim().toLong()).map { it.isAlive }.orElse(false))
    }

    @Then("the factory is running")
    fun factoryRunning() = check(Files.exists(workspace.resolve("factory/runs/.daemon/factory")))

    @Then("the {string} run is running")
    fun namedRunRunning(name: String) = check(Files.readString(workspace.resolve("factory/runs/.daemon/current")).trim() == name)

    @Then("the {string} run is not running")
    fun namedRunStopped(name: String) {
        val file = workspace.resolve("factory/runs/.daemon/current")
        check(!Files.exists(file) || Files.readString(file).trim() != name)
    }

    @Then("the factory is not running")
    fun factoryNotRunning() = check(!Files.exists(workspace.resolve("factory/runs/.daemon/factory")))

    @Then("I see {string} from the doer")
    fun watchedDoerText(text: String) {
        val log = watcherLog
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (log != null && watcher?.isAlive == true && System.nanoTime() < deadline &&
            (!Files.exists(log) || !Files.readString(log).contains("doer | $text"))) Thread.sleep(20)
        val current = log?.let { if (Files.exists(it)) Files.readString(it) else "" } ?: watchedOutput
        check((current + watchedOutput).contains("doer | $text")) { current + watchedOutput }
        watchedOutput += current
    }

    @Then("I do not see {string}")
    fun watchedDoesNotContain(text: String) {
        val current = watcherLog?.let { if (Files.exists(it)) Files.readString(it) else "" } ?: watchedOutput
        check(!(current + watchedOutput).contains(text)) { current + watchedOutput }
    }

    @Then("I see {string} from reviewer_{int}, reviewer_{int} and reviewer_{int}")
    fun reviewerOutput(text: String, one: Int, two: Int, three: Int) {
        val record = Files.readString(workspace.resolve("factory/runs/$currentRun/record.jsonl"))
        listOf(one, two, three).forEach { check(record.contains("\"machine\":\"reviewer_$it\"")) { record } }
        check(record.contains(text)) { record }
    }

    @Then("it shows reviewer_{int}, reviewer_{int} and reviewer_{int} as running")
    fun reviewersRunning(one: Int, two: Int, three: Int) {
        val log = watcherLog
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
        while (log != null && System.nanoTime() < deadline &&
            (!Files.exists(log) || (1..3).any { !Files.readString(log).contains("reviewer_$it") })) Thread.sleep(20)
        val current = log?.let { if (Files.exists(it)) Files.readString(it) else "" } ?: ""
        listOf(one, two, three).forEach { check(Files.exists(workspace.resolve("reviewer_$it/ready"))) }
        listOf(one, two, three).forEach { check(current.contains("reviewer_$it")) { current } }
    }

    @Then("I see what the planner generated")
    fun plannerOutputVisible() {
        val record = Files.readString(workspace.resolve("factory/runs/$currentRun/record.jsonl"))
        check(record.contains("planner")) { record }
    }

    @Then("what I am watching shows that the {string} run was stopped")
    fun watcherShowsStopped(name: String) {
        watcherLog?.let { if (Files.exists(it)) watchedOutput = Files.readString(it) }
        check(watchedOutput.contains("run $name was stopped")) { watchedOutput }
    }

    @When("I read the {string} run's record")
    fun readRunRecord(name: String) {
        currentRun = name
        val record = workspace.resolve("factory/runs/$name/record.jsonl")
        output = if (Files.exists(record)) Files.readString(record) else ""
    }

    @Then("it shows what every machine generated")
    fun recordShowsGeneration() {
        check(output.contains("\"text\"")) { output }
        check(output.contains("planner") && output.contains("doer") && output.contains("validator")) { output }
    }

    @Then("it shows what every machine spent")
    fun recordShowsUsage() {
        check(output.contains("inputTokens") && output.contains("outputTokens")) { output }
    }

    @Then("the record is in the run's folder in the factory, not in the target")
    fun recordLocation() {
        check(Files.isRegularFile(workspace.resolve("factory/runs/$currentRun/record.jsonl")))
        check(!Files.exists(target.resolve("record.jsonl")))
    }

    @Then("it shows tokens in and tokens out for the {word}")
    fun machineTokens(machine: String) {
        val text = watchRecordOutput()
        check(Regex("tokens $machine \\d+ in / \\d+ out").containsMatchIn(text)) { text }
    }

    @Then("it shows a total for each of Anthropic, Google and OpenAI")
    fun providerTotals() {
        val text = watchRecordOutput().lowercase()
        listOf("anthropic", "google", "openai").forEach { check(text.contains(it)) { text } }
    }

    @Then("it shows a total for the whole run")
    fun totalForRun() = check(watchRecordOutput().contains("run"))

    @Then("what I watched showed the doer's tokens go up")
    fun doerTokensIncreased() {
        val matches = Regex("tokens doer (\\d+) in / (\\d+) out").findAll(watchRecordOutput()).toList()
        check(matches.size >= 2 && matches.last().groupValues[1].toLong() > matches.first().groupValues[1].toLong()) { watchRecordOutput() }
    }

    @Then("what I watched showed the run's total go up")
    fun runTokensIncreased() {
        val text = watchRecordOutput()
        val matches = Regex("run (\\d+)").findAll(text).toList()
        check(matches.size >= 2 && matches.last().groupValues[1].toLong() > matches.first().groupValues[1].toLong()) { text }
    }

    private fun watchRecordOutput(): String {
        watcherLog?.let { if (Files.exists(it)) watchedOutput = Files.readString(it) }
        return watchedOutput
    }

    @Then("it reports that it could not run reviewer_{int}")
    fun reviewerCannotRunReported(number: Int) {
        check(exitCode != 0 && output.contains("could not run the reviewer_$number", true)) { output }
    }

    @Then("every reviewer was given the work for the second task")
    fun reviewersGivenSecondTask() {
        inspectedPrompts = reviewers.map(::reviewPrompt)
        inspectedPrompts.forEach { check(it.contains("beta.txt") && it.contains("Work for beta")) { it } }
    }

    @Then("every reviewer was given the same prompt")
    fun reviewersSamePrompt() {
        val prompts = reviewers.map(::reviewPrompt)
        check(prompts.distinct().size == 1) { prompts }
        val models = reviewers.map { reviewer ->
            val args = Json.parseToJsonElement(Files.readString(workspace.resolve("$reviewer/calls/0.json")))
                .jsonObject.getValue("args").jsonArray.map { it.jsonPrimitive.content }
            args[args.indexOf("--model") + 1]
        }
        check(models.map { it.substringBefore('/') }.distinct().size == 3) { models }
    }

    @Given("the reviewers' lens is testability")
    fun reviewersTestability() = reviewers.forEach { configureMachine(it, "lens", "testability") }

    @Then("every reviewer was given {string}")
    fun reviewersGiven(value: String) {
        reviewers.forEach { check(reviewPrompt(it).contains(value)) { reviewPrompt(it) } }
    }

    @Given("the other reviewers are held at work")
    fun holdOtherReviewers() {
        val gates = Files.createDirectory(workspace.resolve("stop-gates"))
        val (message, status) = runProcess(listOf("mkfifo") + listOf("reviewer_1", "reviewer_3")
            .map { gates.resolve(it).toString() }, workspace)
        check(status == 0) { message }
    }

    @Given("the second reviewer crashes")
    fun secondReviewerCrashes() { Files.writeString(workspace.resolve("reviewer_2/crash"), "yes") }

    @Then("the held reviewers have stopped")
    fun heldReviewersStopped() {
        for (name in listOf("reviewer_1", "reviewer_3")) {
            val pid = Files.readString(workspace.resolve("$name/pid")).toLong()
            check(ProcessHandle.of(pid).map { !it.isAlive }.orElse(true)) { "$name is still running" }
        }
    }

    @Given("each branch has its own job")
    fun distinctBranchJobs() = reviewers.forEachIndexed { index, name ->
        configureMachine(name, "prompt", "Do distinct job ${index + 1} and report a JSON result.")
    }

    @Then("each branch received only its own job")
    fun distinctBranchPrompts() = reviewers.forEachIndexed { index, name ->
        val prompt = reviewPrompt(name)
        for (job in 1..3) check(prompt.contains("Do distinct job $job") == (job == index + 1)) { prompt }
    }

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
        check(prompt.contains("Task work committed: false.")) { prompt }
        check(!prompt.contains("Task work committed: true.")) { prompt }
        check(prompt.contains("Otherwise, report its status without changing it.")) { prompt }
        check(prompt.contains("Return \"complete\": false while any planned product work remains, including after creating a new plan.")) { prompt }
        check(prompt.contains("If you cannot read or write the designated plan, report the failure; do not claim completion.")) { prompt }
        check(!prompt.contains("The task's work has been committed.")) { prompt }
    }

    @Then("it prints the stop message")
    fun stopMessage() {
        check(output.trimEnd().endsWith("factory stopped")) { output }
    }

    @Given("the {word} is blocked on operator evidence")
    fun machineBlocked(machine: String) {
        Files.writeString(workspace.resolve("$machine/blocked"), "yes")
    }

    @Then("it reports the missing operator evidence")
    fun reportsBlocked() {
        val status = Files.readString(workspace.resolve("factory/runs/$currentRun/status"))
        check(status.startsWith("blocked: Blocked on external input")) { status }
        check(output.contains("Blocked on external input")) { output }
        check(output.contains("Owner must confirm signing key persistence.")) { output }
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
            check(args.take(3) == listOf("--print", "--no-session", "--no-context-files")) { args }
            check(args[args.indexOf("--model") + 1] == model) { args }
            check(args[args.indexOf("--append-system-prompt") + 1].endsWith("/AGENTS.md")) { args }
            check(record.getValue("cwd").jsonPrimitive.content == target.toString()) { record }
            val prompt = args.last()
            check(prompt.contains("Read the seed at \"$seed\".")) { prompt }
            check(prompt.contains("The plan is at \"${planPath()}\".")) { prompt }
            check(prompt.contains("Read the plan at that exact path, even when it is outside the target directory.")) { prompt }
            check(prompt.contains("When your job requires plan updates, write them at that exact path too.")) { prompt }
            check(prompt.contains("Keep product work inside the current target directory. The factory handles commits.")) { prompt }
            check(prompt.contains("End your answer with a single line of JSON.")) { prompt }
        }
    }

    @Given("the {word}'s AGENTS.md says {string}")
    fun machineInstructions(machine: String, instructions: String) {
        Files.createDirectories(machineDirectory(machine))
        Files.writeString(machineDirectory(machine).resolve("AGENTS.md"), instructions + "\n")
    }

    @Then("the doer was given {string}")
    fun doerGiven(value: String) {
        check(Files.readString(workspace.resolve("doer/calls/0.txt")).contains(value)) {
            "Doer was not given $value"
        }
    }

    private fun usePiDoer() {
        val executable = workspace.resolve("doer/pi")
        if (!Files.exists(executable)) {
            Files.copy(workspace.resolve("doer/agent.py"), executable)
            executable.toFile().setExecutable(true)
        }
        configureMachine("doer", "harness", executable.toString())
    }

    @Given("the doer has its own login")
    fun doerOwnLogin() {
        usePiDoer()
        Files.writeString(machineDirectory("doer").resolve("auth.json"), "{\"test_login\": true}\n")
    }

    @Given("the doer has no login")
    fun doerNoLogin() {
        usePiDoer()
        Files.deleteIfExists(machineDirectory("doer").resolve("auth.json"))
    }

    @Given("there is an API key for the doer's provider")
    fun doerApiKey() { environmentOverrides["ANTHROPIC_API_KEY"] = "test-api-key" }

    @Given("there is no API key for the doer's provider")
    fun noDoerApiKey() { environmentOverrides["ANTHROPIC_API_KEY"] = "" }

    private fun doerRecord(): JsonObject = Json.parseToJsonElement(
        Files.readString(workspace.resolve("doer/calls/0.json"))).jsonObject

    @Then("the doer signed in with its own login")
    fun signedInWithLogin() {
        check(doerRecord().getValue("configuration").jsonPrimitive.content == machineDirectory("doer").toString())
        check(doerRecord().getValue("auth").jsonPrimitive.content == "login")
    }

    @Then("the doer signed in with the API key")
    fun signedInWithApiKey() {
        check(doerRecord().getValue("configuration").jsonPrimitive.content == machineDirectory("doer").toString())
        check(doerRecord().getValue("auth").jsonPrimitive.content == "api-key")
    }

    @Then("the doer's login is ignored by Git")
    fun loginIgnored() {
        repo.git("check-ignore", machineDirectory("doer").resolve("auth.json").toString())
        check(repo.git("ls-files", "--", "**/auth.json", "**/.credentials.json").isEmpty())
    }

    @Then("it reports that the doer cannot sign in")
    fun cannotSignIn() = diagnosticReported("The doer cannot sign in")

    @Given("the doer uses {word}")
    fun doerHarness(harness: String) {
        val executable = workspace.resolve("doer/$harness")
        Files.copy(workspace.resolve("doer/agent.py"), executable, StandardCopyOption.REPLACE_EXISTING)
        executable.toFile().setExecutable(true)
        configureMachine("doer", "harness", executable.toString())
        if (harness == "claude") {
            Files.writeString(machineDirectory("doer").resolve("CLAUDE.md"), "@AGENTS.md\n")
            Files.writeString(machineDirectory("doer").resolve(".credentials.json"), "{\"test_login\": true}\n")
        }
    }

    @Given("personal and target instructions conflict with the machine")
    fun conflictingInstructions() {
        for (directory in listOf(workspace, target)) {
            Files.writeString(directory.resolve("AGENTS.md"), "WRONG_INSTRUCTIONS")
            Files.writeString(directory.resolve("CLAUDE.md"), "WRONG_INSTRUCTIONS")
        }
        val personal = Files.createDirectories(workspace.resolve("personal-config"))
        Files.writeString(personal.resolve("AGENTS.md"), "WRONG_INSTRUCTIONS")
        Files.writeString(personal.resolve("auth.json"), "{\"personal_login\": true}")
        for (key in listOf("PI_CODING_AGENT_DIR", "CLAUDE_CONFIG_DIR", "CODEX_HOME")) {
            environmentOverrides[key] = personal.toString()
        }
    }

    @Then("the doer ran with isolated {word} configuration")
    fun isolatedHarness(harness: String) {
        val record = doerRecord()
        val args = record.getValue("args").jsonArray.map { it.jsonPrimitive.content }
        check(record.getValue("configuration").jsonPrimitive.content == machineDirectory("doer").toString()) { record }
        val instructions = record.getValue("instructions").jsonPrimitive.content
        check(instructions.contains("ONLY_DOER_INSTRUCTIONS") && !instructions.contains("WRONG_INSTRUCTIONS")) { record }
        check(record.getValue("cwd").jsonPrimitive.content == target.toString()) { record }
        when (harness) {
            "pi" -> {
                check("--no-context-files" in args) { args }
                check(args[args.indexOf("--append-system-prompt") + 1] == machineDirectory("doer").resolve("AGENTS.md").toString()) { args }
            }
            "claude" -> check(args[args.indexOf("--setting-sources") + 1] == "user") { args }
            "codex" -> check(args.first() == "exec" && "project_doc_max_bytes=0" in args) { args }
        }
    }

    @Given("the validator has no login or API key")
    fun validatorNoCredentials() {
        val executable = workspace.resolve("validator/pi")
        Files.copy(workspace.resolve("validator/validator.py"), executable)
        executable.toFile().setExecutable(true)
        configureMachine("validator", "harness", executable.toString())
        Files.delete(machineDirectory("validator").resolve("auth.json"))
        environmentOverrides["ANTHROPIC_API_KEY"] = ""
    }

    @Then("it reports that the validator cannot sign in")
    fun validatorCannotSignIn() = diagnosticReported("The validator cannot sign in")

    @Given("the doer's model is {string}")
    fun doerModel(model: String) = configureMachine("doer", "model", model)

    @Given("the Google provider has an API key")
    fun googleApiKey() { environmentOverrides["GEMINI_API_KEY"] = "test-google-key" }

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
        targetOnly: Boolean = false,
        waitForCompletion: Boolean = true
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
        if (waitForCompletion && exitCode == 0) {
            val waitResult = runProcess(listOf(workspace.resolve("factory/factory").toString(), "wait", currentRun), workspace, environment)
            val log = workspace.resolve("factory/runs/$currentRun/daemon.log")
            output += "\n" + (if (Files.exists(log)) Files.readString(log) else "")
            if (Files.exists(workspace.resolve("factory/runs/$currentRun/status")) &&
                Files.readString(workspace.resolve("factory/runs/$currentRun/status")).startsWith("failed")) exitCode = 1
            if (waitResult.second != 0) exitCode = waitResult.second
        }
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
        val prompts = inspectedPrompts.ifEmpty { listOf(validatorPrompt()) }
        prompts.forEach { prompt ->
            check(!prompt.contains("alpha.txt") && !prompt.contains("Work for alpha")) { prompt }
        }
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
        val acp = bin.resolve("pi-rpc-acp")
        Files.writeString(acp, """#!/usr/bin/env python3
import json, os, subprocess, sys
from pathlib import Path
def send(value): print(json.dumps(value), flush=True)
session = None
for line in sys.stdin:
    try: msg = json.loads(line)
    except Exception: continue
    method, params, ident = msg.get('method'), msg.get('params', {}), msg.get('id')
    if method == 'initialize': send({'jsonrpc':'2.0','id':ident,'result':{'protocolVersion':1,'agentCapabilities':{'promptCapabilities':{}},'agentInfo':{'name':'test-pi','version':'1'},'authMethods':[]}})
    elif method == 'session/new':
        session = 'test-session'
        send({'jsonrpc':'2.0','id':ident,'result':{'sessionId':session}})
    elif method == 'session/set_model': send({'jsonrpc':'2.0','id':ident,'result':{}})
    elif method == 'session/prompt':
        prompt = '\n'.join(block.get('text','') for block in params.get('prompt',[]) if block.get('type') == 'text')
        command = [os.environ.get('FACTORY_TEST_PI_EXECUTABLE','pi'),'--print','--no-session',*sys.argv[1:],prompt]
        result = subprocess.run(command, text=True, capture_output=True, stdin=subprocess.DEVNULL)
        if result.stderr: sys.stderr.write(result.stderr)
        for part in result.stdout.splitlines():
            send({'jsonrpc':'2.0','method':'session/update','params':{'sessionId':session,'update':{'sessionUpdate':'agent_message_chunk','content':{'type':'text','text':part+'\n'}}}})
        if result.returncode:
            send({'jsonrpc':'2.0','id':ident,'error':{'code':-32603,'message':result.stdout}})
        else:
            send({'jsonrpc':'2.0','id':ident,'result':{'stopReason':'end_turn','usage':{'inputTokens':len(prompt),'outputTokens':len(result.stdout)}}})
    elif ident is not None: send({'jsonrpc':'2.0','id':ident,'result':{}})
""")
        acp.toFile().setExecutable(true)
        return mapOf(
            "ANTHROPIC_API_KEY" to "", "OPENAI_API_KEY" to "", "GEMINI_API_KEY" to "", "CODEX_API_KEY" to "",
            "PATH" to "$bin:${System.getenv("PATH")}",
            "FACTORY_TEST_WORKSPACE" to workspace.toString(),
            "GIT_CONFIG_COUNT" to "2",
            "GIT_CONFIG_KEY_0" to "user.name", "GIT_CONFIG_VALUE_0" to "Factory Test",
            "GIT_CONFIG_KEY_1" to "user.email", "GIT_CONFIG_VALUE_1" to "factory-test@example.invalid"
        ) + environmentOverrides
    }

    @After
    fun cleanUp() {
        if (::workspace.isInitialized) workspace.toFile().deleteRecursively()
        extraWorkspaces.forEach { it.toFile().deleteRecursively() }
    }
}
