import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Owns attempt state and the work/plan commit boundary; never reads the plan. */
internal class TaskLifecycle(
    private val maxAttempts: Int,
    private val jobs: MachineJobs,
    private val repository: TargetRepository
) {
    private var workInProgress = false
    private var workAccepted = false
    private var attempts = 0
    private var findings = JsonArray(emptyList())

    fun prepareExecution(machine: String, branchResults: Map<String, JsonObject>? = null): String {
        if (machine == "planner" && workInProgress) acceptWork()
        // Construct the prompt before beginning the attempt, preserving failure ordering.
        val prompt = if (branchResults == null) jobs.prompt(machine, workAccepted, findings)
            else jobs.meetingPrompt(machine, branchResults)
        if (machine == "doer") beginAttempt()
        return prompt
    }

    fun receiveResult(machine: String, result: JsonObject) {
        if (result["blocked"] == JsonPrimitive(true)) {
            throw FactoryBlocked("Blocked on external input at $machine. Findings: ${result["findings"] ?: "[]"}")
        }
        if (machine == "validator") {
            findings = result["findings"] as? JsonArray
                ?: fail("Could not read the validator's result")
        }
    }

    /** The deciding machine supplies retry findings, including after a fan-in. */
    fun followRoute(machine: String, next: String, result: JsonObject) {
        if (machine != "planner" && next == "doer") {
            findings = result["findings"] as? JsonArray
                ?: fail("Could not read the $machine's result")
            if (attempts >= maxAttempts) {
                fail("The task hit its limit of $maxAttempts attempts. Findings: $findings")
            }
        }
        if (machine == "planner") finishPlanning()
    }

    private fun beginAttempt() {
        workInProgress = true
        workAccepted = false
        attempts++
    }

    private fun acceptWork() {
        repository.recordTaskChanges()
        workAccepted = true
    }

    private fun finishPlanning() {
        if (workAccepted) repository.recordTaskChanges()
        workInProgress = false
        workAccepted = false
        attempts = 0
        findings = JsonArray(emptyList())
    }
}
