import java.io.File
import kotlinx.serialization.json.JsonArray

/** Owns shared prompt context and the small set of role-specific instructions. */
internal class MachineJobs(
    seed: File,
    plan: File,
    private val configuration: MachineConfiguration,
    private val resultFields: (String) -> List<String>,
    private val repository: TargetRepository
) {
    private val context = """
        Read the seed at "$seed".
        The plan is at "$plan".
        Work only in the current target directory. The factory handles commits.
        An explicitly requested behavior change in the seed or recorded product-owner instructions takes precedence over an older, conflicting test expectation.
        This authorization includes local updates to superseded fetched specs; no upstream course change is required.
        Record the old expectation, the requested replacement, and the reason for each test change in the run record.
        If the intended behavior is ambiguous, report the question rather than inventing a requirement.
        A red phase during TDD is expected; acceptance still requires all required tests to pass.
        End your answer with a single line of JSON.
    """.trimIndent()

    fun prompt(machine: String, taskAccepted: Boolean, findings: JsonArray): String {
        val job = when (machine) {
            "planner" -> if (taskAccepted) """
                The task's work has been committed. Mark the first unfinished task done and report whether the plan is complete.
                Do not implement product work.
                ${resultRequest(machine)}
            """.trimIndent() else """
                If no plan exists, write one from the seed at the exact supplied plan path, never an alternative location.
                Otherwise, report its status without changing it.
                Use the existing plan's recorded progress on retries and resumption; do not regenerate it from the seed.
                Keep the seed unchanged. All plan and progress writes must stay inside the target.
                Do not implement product work.
                ${resultRequest(machine)}
            """.trimIndent()
            "doer" -> """
                Implement the first unfinished task in the plan.
                Use TDD: add or update the test to assert the requested behavior, run it and inspect the result, then change the implementation and rerun the required suite.
                You are authorized and expected to update conflicting tests and their step definitions inside the target, including fetched specs when they express the superseded behavior.
                Confirm that red reflects the intended behavior change, not a broken fixture or syntax error. If the implementation already satisfies the revised test, record that observation without manufacturing a failure.
                Do not delete, skip, filter out, or weaken checks merely to get green; preserve coverage of requirements that still apply.
                Validator findings from the previous attempt: $findings
                Record each finding as a subtask of the task in progress, then fix it.
                Mark resolved subtasks done. Do not create new top-level tasks for findings.
                Do not mark the task itself done; the planner will do that after the work is committed.
                ${resultRequest(machine)}
            """.trimIndent()
            "validator" -> {
                val lens = configuration.validationLens(machine)
                """
                    Check the doer's uncommitted product work in this target, including new files.
                    Review the supplied changes only; do not recheck previously committed work.
                    Your validation lens is: $lens.
                    Review changed tests against the requested behavior, not the superseded expectation; reject weakened coverage of requirements that still apply.
                    Check the recorded reason for test changes and the required-suite results as well as the configured lens.
                    The supplied target-owned plan is the work record, not product acceptance evidence.
                    Task/subtask status updates are permitted; they do not prove the product meets the seed.
                    Report findings only. Change neither the plan nor the product files.
                    ${resultRequest(machine)} Include an array "findings" in your JSON result.
                    Product changes:
                    ${repository.uncommittedProductChanges()}
                """.trimIndent()
            }
            else -> resultRequest(machine)
        }
        val acceptance = if (machine == "planner") """
            Plan the necessary test updates as part of the authorized change, rather than declaring obsolete expectations an external blocker.
            On resumption, preserve historical observations but use current product-owner decisions to supersede obsolete blockers and plan restrictions.
            Distinguish completed automated work from outstanding manual acceptance required by the seed.
            Record acceptance evidence and outstanding checks in the target-owned plan or target-local documents referenced by it.
            An unavailable or unperformed manual check remains explicitly outstanding; never infer a pass from automated work or task status.
            Report complete only when required acceptance is supported by recorded evidence, not merely when implementation tasks are done.
            On resumption, read the plan and its referenced target-local evidence, including product-owner acceptance; preserve recorded progress and outstanding checks.
            Do not require factory-side acceptance edits or invent evidence. Completion decisions remain yours.
        """.trimIndent() else ""
        return "You are the $machine.\n$context\n$job\n$acceptance"
    }

    private fun resultRequest(machine: String): String {
        val fields = resultFields(machine)
        if (fields.isEmpty()) return "Include an empty JSON object as your result."
        return "Include boolean ${if (fields.size == 1) "field" else "fields"} " +
            fields.joinToString(" and ") { "\"$it\"" } + " in your JSON result."
    }
}
