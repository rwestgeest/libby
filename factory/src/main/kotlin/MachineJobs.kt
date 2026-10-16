import java.io.File
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject

/** Supplies run data and result schemas; machine instructions live in AGENTS.md. */
internal class MachineJobs(
    private val seed: File,
    private val plan: File,
    private val resultFields: (String) -> List<String>,
    private val repository: TargetRepository
) {
    fun prompt(machine: String, taskAccepted: Boolean, findings: JsonArray): String = """
        Read the seed at "$seed".
        The plan is at "$plan".
        Read the plan at that exact path, even when it is outside the target directory.
        When your job requires plan updates, write them at that exact path too.
        Do not create a substitute plan inside the target.
        Keep product work inside the current target directory. The factory handles commits.
        Task work committed: $taskAccepted.
        Validator findings from the previous attempt: $findings
        ${resultRequest(machine)}
        Product changes:
        ${repository.uncommittedProductChanges()}
    """.trimIndent()

    fun meetingPrompt(machine: String, results: Map<String, JsonObject>): String = """
        ${resultRequest(machine)}
        Branch results:
        ${JsonObject(results)}
    """.trimIndent()

    private fun resultRequest(machine: String): String {
        val fields = resultFields(machine)
        val schema = if (fields.isEmpty()) "Include a JSON object describing the job you did as your result."
            else "Include boolean ${if (fields.size == 1) "field" else "fields"} " +
                fields.joinToString(" and ") { "\"$it\"" } + " in your JSON result."
        return "End your answer with a single line of JSON. $schema Include an array \"findings\" in your JSON result."
    }
}
