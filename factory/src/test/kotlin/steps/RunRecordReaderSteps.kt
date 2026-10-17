package steps

import RunRecordReader
import io.cucumber.java.en.Then
import java.nio.file.Files
import java.nio.file.StandardOpenOption.APPEND
import kotlinx.serialization.json.Json

class RunRecordReaderSteps {
    @Then("the run record reader preserves Unicode and waits for complete appended events")
    fun readGrowingRecord() {
        val file = Files.createTempFile("run-record-", ".jsonl")
        try {
            val reader = RunRecordReader(file.toFile())
            val first = "{\"text\":\"review — 🍀\"}"
            val second = "{\"text\":\"次\"}"
            Files.writeString(file, "$first\n")
            check(reader.readLines() == listOf(first))
            check(reader.readLines().isEmpty())
            val bytes = "$second\n".toByteArray(Charsets.UTF_8)
            // End a poll inside a multibyte character as well as inside JSON.
            val split = "{\"text\":\"".toByteArray().size + 1
            Files.write(file, bytes.copyOfRange(0, split), APPEND)
            check(reader.readLines().isEmpty())
            Files.write(file, bytes.copyOfRange(split, bytes.size - 1), APPEND)
            check(reader.readLines().isEmpty())
            Files.writeString(file, "\n$first\n", APPEND)
            val remaining = reader.readLines()
            check(remaining == listOf(second, first))
            remaining.forEach { Json.parseToJsonElement(it) }
            check(reader.readLines().isEmpty())
        } finally {
            Files.deleteIfExists(file)
        }
    }
}
