import java.io.File
import kotlinx.serialization.json.*

internal data class NamedRun(
    val name: String,
    val target: File,
    val seed: File,
    val lineName: String,
    val directory: File
) {
    val plan: File get() = File(directory, "plan.md")
    val assemblyLineFile: File get() = File(target, ".assembly-lines/$lineName.dot")
    val machinesDirectory: File get() = File(target, ".assembly-lines/.machines")
}

internal class RunStore(private val factoryDir: File) {
    fun open(
        name: String,
        targetArgument: String?,
        lineArgument: String?,
        seedArgument: String?
    ): NamedRun {
        if (!Regex("[A-Za-z0-9][A-Za-z0-9._-]*").matches(name)) {
            fail("The run name is invalid")
        }
        val directory = File(factoryDir, "runs/$name")
        val settings = File(directory, "settings.json")
        val existing = if (settings.isFile) readSettings(settings) else null

        val target = resolveTarget(name, targetArgument, existing)
        val line = resolveValue(name, "assembly line", lineArgument, existing?.lineName)
        val seed = resolveSeed(name, seedArgument, existing)

        if (!seed.isFile) fail("There is no seed")
        target.mkdirs()

        if (existing == null) {
            directory.mkdirs()
            settings.writeText(buildJsonObject {
                put("target", target.path)
                put("line", line)
                put("seed", seed.path)
            }.toString() + "\n")
        }
        return NamedRun(name, target, seed, line, directory)
    }

    private fun resolveTarget(name: String, argument: String?, existing: StoredRun?): File {
        if (existing == null && argument == null) fail("A target is required")
        val stored = existing?.target?.let(::File)
        val supplied = argument?.let(::absoluteFile)
        if (stored != null && supplied != null && stored != supplied) {
            fail("The \"$name\" run already has a target")
        }
        return stored ?: supplied!!
    }

    private fun resolveSeed(name: String, argument: String?, existing: StoredRun?): File {
        if (existing == null && argument == null) fail("There is no seed")
        val stored = existing?.seed?.let(::File)
        val supplied = argument?.let(::absoluteFile)
        if (stored != null && supplied != null && stored != supplied) {
            fail("The \"$name\" run already has a seed")
        }
        return stored ?: supplied!!
    }

    private fun resolveValue(name: String, label: String, supplied: String?, stored: String?): String {
        if (stored == null && supplied == null) fail("An assembly line is required")
        if (stored != null && supplied != null && stored != supplied) {
            fail("The \"$name\" run already has an $label")
        }
        return stored ?: supplied!!
    }

    private fun absoluteFile(path: String): File = File(path).absoluteFile.normalize()

    private fun readSettings(file: File): StoredRun = runCatching {
        val json = Json.parseToJsonElement(file.readText()).jsonObject
        StoredRun(
            File(json.getValue("target").jsonPrimitive.content).absoluteFile.normalize().path,
            json.getValue("line").jsonPrimitive.content,
            File(json.getValue("seed").jsonPrimitive.content).absoluteFile.normalize().path
        )
    }.getOrElse { fail("Could not read the run settings") }

    private data class StoredRun(val target: String, val lineName: String, val seed: String)
}
