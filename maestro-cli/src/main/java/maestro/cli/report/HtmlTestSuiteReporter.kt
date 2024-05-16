package maestro.cli.report

import maestro.cli.model.TestExecutionSummary
import okio.Sink
import okio.buffer
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.FileReader
import maestro.cli.report.TestDebugReporter

class HtmlTestSuiteReporter : TestSuiteReporter {
    override fun report(summary: TestExecutionSummary, out: Sink) {
        val bufferedOut = out.buffer()
        val htmlContent = buildHtmlReport(summary)
        bufferedOut.writeUtf8(htmlContent)
        bufferedOut.close()
    }

    private fun readLogFile(): Array<String> {
        val debugOutputPath = TestDebugReporter.getDebugOutputPath()
        val filePathLog = "${debugOutputPath}/maestro.log"
        var reader: BufferedReader? = null
        var failedStep = emptyArray<String>()
    
        try {
            reader = BufferedReader(FileReader(filePathLog))
            var line: String = ""
    
            while (reader.readLine().also { line = it } != null) {
                // Process each line
                if(line.contains("FAILED") && !line.contains("Run")){
                    failedStep += line
                }
            }
        } catch (e: Exception) {
        } finally {
            try {
                reader?.close()
            } catch (e: Exception) {
                println("An error occurred while closing the file: ${e.message}")
            }
        }
        return failedStep
    }

    private fun buildHtmlReport(summary: TestExecutionSummary): String {
    val htmlBuilder = StringBuilder()
    val failedStep = readLogFile()
    var idx = 0

    htmlBuilder.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Maestro Test Report</title><link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\"></head><body>")
    htmlBuilder.append("<div class=\"container py-5\"><div class=\"card mb-4\"><br><center><h1>Test Execution Summary</h1></center>")
    htmlBuilder.append("<div class=\"card-body\">Device Name: ${summary.deviceName}<br>Test Result Summary: ${if (summary.passed) "PASSED" else "FAILED"}<br>Total Suite: ${summary.suites.size}<br>Total Test: ${summary.suites.sumOf { it.flows.size }}</div></div>")

    for (suite in summary.suites) {
        htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-body\"><center><b>Test Suite</b></center><br>Summary: ${if (suite.passed) "PASSED" else "FAILED"}<br>Duration: ${suite.duration}<br>Test Count: ${suite.flows.size}<br><br>")
        for (flow in suite.flows) {
            htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-body\">")
            htmlBuilder.append("<h5 class=\"card-title\">${flow.name}</h5>")
            htmlBuilder.append("<p class=\"card-text\">Status: ${flow.status}<br>Duration: ${flow.duration}<br>File Name: ${flow.fileName}</p>")
            if (flow.failure != null) {
                htmlBuilder.append("<p class=\"card-text text-danger\">${failedStep[idx]}</p>")
                htmlBuilder.append("<p class=\"card-text text-danger\">${flow.failure.message}</p>")
                idx++
            }
            htmlBuilder.append("</div></div>")
        }
    }
    htmlBuilder.append("</div></div></div></body></html>")
    return htmlBuilder.toString()
    }
}