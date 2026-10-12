import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** Owns attempt state and the work/plan commit boundary; never reads the plan. */
internal class TaskLifecycle(
    private val maxAttempts: Int,
    private val jobs: MachineJobs,
    private val repository: TargetRepository,
    private val run: NamedRun,
    private val requiredTests: RequiredTests = RequiredTests(run.target)
) {
    private var workInProgress = false
    private var workAccepted = false
    private var implementationCommit: String? = null
    private var plannerComplete = false
    private var attempts = 0
    private var findings = JsonArray(emptyList())

    fun prepareExecution(machine: String): String {
        if (machine == "planner") run.preparePlanning()
        if (machine == "doer") run.requirePlan()
        if (machine == "planner" && workInProgress) acceptWork()
        // Construct the prompt before beginning the attempt, preserving failure ordering.
        val prompt = jobs.prompt(machine, workAccepted, findings)
        if (machine == "doer") beginAttempt()
        return prompt
    }

    fun receiveResult(machine: String, result: JsonObject) {
        if (machine == "planner") {
            run.requirePlan()
            plannerComplete = (result["complete"] as? JsonPrimitive)?.booleanOrNull == true
        }
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
        requiredTests.requirePassing()
        repository.validateEvidenceSelection(run)
        implementationCommit = repository.recordTaskChanges(run.name)
        repository.recordEvidence(run, implementationCommit)
        workAccepted = true
    }

    private fun finishPlanning() {
        // Recheck after planner edits and on completed resumption; no cached waiver.
        if (workAccepted || plannerComplete) requiredTests.requirePassing()
        repository.recordEvidence(run, implementationCommit)
        repository.recordPlan(run, workAccepted, implementationCommit)
        implementationCommit = null
        workInProgress = false
        workAccepted = false
        attempts = 0
        findings = JsonArray(emptyList())
    }
}
