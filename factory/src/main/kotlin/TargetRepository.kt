import java.io.File

/** Owns Git operations scoped to a target, even inside a containing repository. */
internal class TargetRepository(private val target: File) {
    private val productPathspec = arrayOf(".", ":(exclude).factory", ":(exclude).assembly-lines")

    fun prepare() {
        if (gitResult("rev-parse", "--show-toplevel").second != 0) git("init")
    }

    fun uncommittedProductChanges(): String = buildString {
        if (gitResult("rev-parse", "--verify", "HEAD").second == 0) {
            append(git("diff", "--no-ext-diff", "HEAD", "--", *productPathspec))
        }
        val newFiles = git(
            "ls-files", "--others", "--exclude-standard", "-z", "--", *productPathspec
        )
        for (path in newFiles.split('\u0000').filter { it.isNotEmpty() }) {
            val (diff, status) = gitResult("diff", "--no-ext-diff", "--no-index", "--", "/dev/null", path)
            if (status > 1) fail(diff.trim())
            append(diff)
        }
    }

    fun recordTaskChanges() {
        if (git("status", "--porcelain", "--", *productPathspec).isNotBlank()) {
            git("add", "--", *productPathspec)
            git("commit", "--only", "-m", "Record factory task", "--", *productPathspec)
        }
    }

    private fun git(vararg arguments: String): String {
        val (output, status) = gitResult(*arguments)
        if (status != 0) fail(output.trim())
        return output
    }

    private fun gitResult(vararg arguments: String): Pair<String, Int> {
        val process = ProcessBuilder(listOf("git") + arguments)
            .directory(target).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        return output to process.waitFor()
    }
}
