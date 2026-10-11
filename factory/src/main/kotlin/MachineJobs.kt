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
