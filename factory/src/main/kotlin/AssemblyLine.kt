import java.io.File
import kotlinx.serialization.json.*

internal data class AssemblyEdge(val from: String, val to: String, val label: String?)

/** File syntax and factory-directory availability belong to loading, not routing. */
internal fun readAssemblyLine(file: File, factoryDir: File): AssemblyLine {
    if (!file.isFile) fail("There is no assembly line")
    val edgePattern = Regex(
        """^\s*([A-Za-z][\w-]*)\s*->\s*([A-Za-z][\w-]*)\s*(?:\[\s*label\s*=\s*\"([^\"]+)\"\s*])?\s*;?\s*$"""
    )
    val edges = file.readLines().mapNotNull { line ->
        edgePattern.matchEntire(line)?.destructured?.let { (from, to, label) ->
            AssemblyEdge(from, to, label.ifBlank { null })
        }
    }
    return AssemblyLine(edges).also { line ->
        line.validate { machine ->
            if (!File(factoryDir, machine).isDirectory) {
                fail("There is no machine called \"$machine\"")
            }
        }
    }
}

/** Owns graph rules and routing; callers never need its edges or machine set. */
internal class AssemblyLine(private val edges: List<AssemblyEdge>) {
    private val machines: Set<String> = edges.flatMap { listOf(it.from, it.to) }
        .filter { it !in setOf("start", "finish") }.toSet()

    fun validate(requireMachineAvailable: (String) -> Unit) {
        if (edges.none { it.from == "start" }) fail("The assembly line has no start")
        if (edges.none { it.to == "finish" }) fail("The assembly line has no finish")
        machines.forEach(requireMachineAvailable)
        edges.firstOrNull { it.to == "finish" && it.from != "planner" }?.let {
            fail("Only planner may lead to finish")
        }
        val reverse = edges.groupBy { it.to }
        val canFinish = mutableSetOf("finish")
        val pending = ArrayDeque<String>().apply { add("finish") }
        while (pending.isNotEmpty()) {
            reverse[pending.removeFirst()].orEmpty().forEach { edge ->
                if (canFinish.add(edge.from)) pending.add(edge.from)
            }
        }
        machines.lastOrNull { it !in canFinish }?.let {
            fail("Finish cannot be reached from $it")
        }
    }

    fun next(node: String, result: JsonObject?): String {
        val choices = edges.filter { it.from == node }
        choices.singleOrNull { it.label == null }?.let { return it.to }
        val fields = resultFields(node)
        for (field in fields) {
            val value = (result?.get(field) as? JsonPrimitive)?.booleanOrNull
                ?: fail("The result of $node has no field \"$field\"")
            choices.firstOrNull { edge ->
                edge.label == if (value) field else "not $field"
            }?.let { return it.to }
        }
        fail("The result of $node does not select an edge")
    }

    fun resultFields(machine: String): List<String> = edges.filter { it.from == machine }
        .mapNotNull { it.label?.removePrefix("not ") }.distinct()
}
