import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS

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

    fun recordTaskChanges(runName: String? = null): String? {
        if (git("status", "--porcelain", "--", *productPathspec).isNotBlank()) {
            // Enumerate literal product paths: broad add can error on ignored, staged
            // metadata even when excluded. Never force-add ignored product files.
            val paths = git("ls-files", "--cached", "--others", "--exclude-standard", "-z",
                "--", *productPathspec).split('\u0000').filter { it.isNotEmpty() }.distinct()
                .map { ":(literal)$it" }.toTypedArray()
            git("add", "--", *paths)
            val message = "Record factory task" + recordLink(runName, null)
            git("commit", "--only", "-m", message, "--", *paths)
            return git("rev-parse", "HEAD").trim()
        }
        return null
    }

    /** Explicit selection keeps the plan opaque and private/unrelated files out of history. */
    fun validateEvidenceSelection(run: NamedRun) { selectedEvidence(run) }

    fun recordEvidence(run: NamedRun, implementation: String? = null) {
        val files = selectedEvidence(run)
        if (files.isNotEmpty()) recordFiles(files,
            "Record factory evidence (${run.name})" + recordLink(run.name, implementation))
    }

    private fun selectedEvidence(run: NamedRun): List<File> {
        run.requirePlan()
        val manifest = File(run.plan.parentFile, "evidence.list")
        if (!Files.exists(manifest.toPath(), NOFOLLOW_LINKS)) return emptyList()
        fun invalid(path: String): Nothing = fail("Invalid selected evidence path: $path")
        if (Files.isSymbolicLink(manifest.toPath()) || !manifest.isFile) invalid("evidence.list")
        val selected = manifest.readLines().filter { it.isNotBlank() }.map { path ->
            val parts = path.split('/')
            if (File(path).isAbsolute || '\\' in path || parts.any {
                    it.isEmpty() || it == "." || it == ".." || it == ".git" || it == ".assembly-lines"
                } || path == "plan.md" || path == "evidence.list") invalid(path)
            var file = run.plan.parentFile
            for (part in parts) {
                file = File(file, part)
                if (Files.isSymbolicLink(file.toPath())) invalid(path)
            }
            if (!file.isFile) invalid(path)
            file
        }
        return listOf(manifest) + selected.distinct()
    }

    /** Only this run's authoritative document, never the rest of target metadata. */
    fun recordPlan(run: NamedRun, acceptedTask: Boolean, implementation: String? = null) {
        run.requirePlan()
        val boundary = if (acceptedTask) "accepted task progress" else "planning"
        recordFiles(listOf(run.plan),
            "Record factory $boundary (${run.name})" + recordLink(run.name, implementation))
    }

    private fun recordLink(runName: String?, implementation: String?): String =
        if (runName == null) "" else buildString {
            append("\n\nFactory-Run: $runName")
            if (implementation != null) append("\nFactory-Implementation: $implementation")
        }

    private fun recordFiles(files: List<File>, message: String) {
        val root = target.toPath().toAbsolutePath().normalize()
        val paths = files.map { ":(literal)${root.relativize(it.toPath().toAbsolutePath().normalize())}" }
            .toTypedArray()
        // Selected records must survive both product pathspec exclusions and .gitignore.
        git("add", "-f", "--", *paths)
        val (output, status) = gitResult("diff", "--cached", "--quiet", "--", *paths)
        if (status > 1) fail(output.trim())
        if (status == 1) git("commit", "--only", "-m", message, "--", *paths)
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
