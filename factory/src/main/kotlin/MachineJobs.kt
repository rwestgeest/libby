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
                If no plan exists, write one from the seed. Otherwise, report its status without changing it.
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
                    Report findings only. Change neither the plan nor the product files.
                    ${resultRequest(machine)} Include an array "findings" in your JSON result.
                    Product changes:
                    ${repository.uncommittedProductChanges()}
                """.trimIndent()
            }
            else -> resultRequest(machine)
        }
        return "You are the $machine.\n$context\n$job"
    }

    private fun resultRequest(machine: String): String {
        val fields = resultFields(machine)
        if (fields.isEmpty()) return "Include an empty JSON object as your result."
        return "Include boolean ${if (fields.size == 1) "field" else "fields"} " +
            fields.joinToString(" and ") { "\"$it\"" } + " in your JSON result."
    }
}
