/** Coordinates collaborators; graph routing remains independent of task acceptance. */
internal fun runFactory(
    assemblyLine: AssemblyLine,
    runner: MachineRunner,
    lifecycle: TaskLifecycle,
    repository: TargetRepository
) {
    var machine = assemblyLine.next("start", null)
    var branchResults: Map<String, kotlinx.serialization.json.JsonObject>? = null
    while (machine != "finish") {
        val result = runner.run(machine, lifecycle.prepareExecution(machine, branchResults))
        branchResults = null
        println(result)
        lifecycle.receiveResult(machine, result)
        val branches = assemblyLine.branches(machine)
        if (branches.isNotEmpty()) {
            val prompts = branches.associateWith { lifecycle.prepareExecution(it) }
            branchResults = runner.runBranches(prompts)
            branchResults.forEach { (branch, report) ->
                println(report)
                lifecycle.receiveResult(branch, report)
            }
            machine = assemblyLine.meetingPoint(machine)
            continue
        }
        val next = assemblyLine.next(machine, result)
        lifecycle.followRoute(machine, next, result)
        machine = next
    }
    repository.recordTaskChanges()
    println("factory stopped")
}
