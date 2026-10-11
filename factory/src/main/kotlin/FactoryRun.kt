/** Coordinates collaborators; graph routing remains independent of task acceptance. */
internal fun runFactory(
    assemblyLine: AssemblyLine,
    runner: MachineRunner,
    lifecycle: TaskLifecycle,
    repository: TargetRepository
) {
    var machine = assemblyLine.next("start", null)
    while (machine != "finish") {
        val result = runner.run(machine, lifecycle.prepareExecution(machine))
        println(result)
        lifecycle.receiveResult(machine, result)
        val next = assemblyLine.next(machine, result)
        lifecycle.followRoute(machine, next)
        machine = next
    }
    // Commits belong to lifecycle boundaries, not arbitrary routes to finish.
    println("factory stopped")
}
