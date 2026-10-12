import java.io.File
import java.io.IOException

/** Target-owned adapter must run the complete required suite, not a validator proxy. */
internal class RequiredTests(private val target: File) {
    fun requirePassing() {
        val process = try {
            ProcessBuilder(File(target, "test").absolutePath)
                .directory(target).redirectErrorStream(true).start()
        } catch (error: IOException) {
            fail("Required tests failed: Cannot run target ./test: ${error.message}")
        }
        process.inputStream.bufferedReader().use { input ->
            input.forEachLine { println(it) }
        }
        val status = process.waitFor()
        if (status != 0) fail("Required tests failed: target ./test exited $status; acceptance blocked")
    }
}
