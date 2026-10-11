import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import kotlinx.serialization.json.*

internal data class NamedRun(
    val name: String,
    val target: File,
    val seed: File,
    val lineName: String,
    val directory: File
) {
    val plan: File get() = File(target, planLocation(name))
    val assemblyLineFile: File get() = File(target, ".assembly-lines/$lineName.dot")
    val machinesDirectory: File get() = File(target, ".assembly-lines/.machines")

    fun preparePlanning() {
        validatePlanLocation(target, seed, name)
        if (!plan.parentFile.isDirectory && !plan.parentFile.mkdirs()) {
            fail("Could not create the authoritative plan directory: ${plan.parentFile}")
        }
    }

    fun requirePlan() {
        validatePlanLocation(target, seed, name)
        if (!plan.isFile) {
            fail("Planning did not establish the authoritative plan: $plan. Product work cannot proceed.")
        }
    }
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
        if (Files.exists(File(directory, "plan.md").toPath(), NOFOLLOW_LINKS) ||
            (existing != null && existing.planLocation != planLocation(name))) {
            fail("The \"$name\" run has legacy or ambiguous plan ownership; resumption is unsupported. No plan was migrated.")
        }
        validatePlanLocation(target, seed, name)
        target.mkdirs()

        if (existing == null) {
            directory.mkdirs()
            settings.writeText(buildJsonObject {
                put("target", target.path)
                put("line", line)
                put("seed", seed.path)
                put("planLocation", planLocation(name))
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
            File(json.getValue("seed").jsonPrimitive.content).absoluteFile.normalize().path,
            json["planLocation"]?.jsonPrimitive?.content
        )
    }.getOrElse { fail("Could not read the run settings") }

    private data class StoredRun(
        val target: String, val lineName: String, val seed: String, val planLocation: String?
    )
}

/** No aliases are allowed in the work-record path, even aliases pointing inside the target. */
private fun validatePlanLocation(target: File, seed: File, name: String) {
    val root = target.canonicalFile.toPath()
    val plan = root.resolve(planLocation(name))
    var component = root
    for (part in root.relativize(plan)) {
        component = component.resolve(part)
        if (Files.isSymbolicLink(component)) fail("The target plan path must not contain filesystem aliases")
        if (Files.exists(component, NOFOLLOW_LINKS) &&
            (if (component == plan) !Files.isRegularFile(component) else !Files.isDirectory(component))) {
            fail("The target plan path has an incompatible filesystem entry")
        }
    }
    if (seed.canonicalFile.toPath() == plan ||
        (Files.exists(plan) && Files.isSameFile(seed.toPath(), plan))) {
        fail("The target plan must be separate from the seed")
    }
}

private fun planLocation(name: String): String = ".factory/runs/$name/plan.md"
