import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

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

    fun prepareExecution(machine: String): String {
        if (machine == "planner" && workInProgress) acceptWork()
        // Construct the prompt before beginning the attempt, preserving failure ordering.
        val prompt = jobs.prompt(machine, workAccepted, findings)
        if (machine == "doer") beginAttempt()
        return prompt
    }

    fun receiveResult(machine: String, result: JsonObject) {
        if (machine == "validator") {
            findings = result["findings"] as? JsonArray
                ?: fail("Could not read the validator's result")
        }
    }

    /** Routing is authoritative: only a validator-to-doer route exhausts retries. */
    fun followRoute(machine: String, next: String) {
        if (machine == "validator" && next == "doer" && attempts >= maxAttempts) {
            fail("The task hit its limit of $maxAttempts attempts. Findings: $findings")
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
