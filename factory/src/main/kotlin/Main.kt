import java.io.File

fun main(args: Array<String>) {
    val target = File(args[args.indexOf("--target") + 1])

    val process = ProcessBuilder("pi")
        .directory(target)
        .inheritIO()
        .start()

    process.waitFor()
}
