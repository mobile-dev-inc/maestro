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

    private fun getTestStep(): Array<Array<String>> {
        val debugOutputPath = TestDebugReporter.getDebugOutputPath()
        val filePathLog = "${debugOutputPath}/maestro.log"
        var reader: BufferedReader? = null
        var testStep = emptyArray<String>()
        var testSteps = emptyArray<Array<String>>()
        var failedStep = emptyArray<String>()
    
        try {
            reader = BufferedReader(FileReader(filePathLog))
            var line: String = ""
    
            while (reader.readLine().also { line = it } != null) {
                // Process each line
                if(line.contains("m.cli.runner.TestSuiteInteractor") && !line.contains("Run") && !line.contains("Define variables") && !line.contains("Apply configuration")){
                    if(line.contains("COMPLETED") || line.contains("FAILED")){
                        testStep += line
                    }
                }
                if(line.contains("m.cli.runner.TestSuiteInteractor - Stop") && line.contains("COMPLETED")){
                    testSteps += testStep
                    testStep = emptyArray<String>()
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
        return testSteps
    }

    private fun buildHtmlReport(summary: TestExecutionSummary): String {
    val htmlBuilder = StringBuilder()
    // val failedStep = readLogFile()
    val testSteps = getTestStep()
    var idx = 0

    htmlBuilder.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Maestro Test Report</title><link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\"></head><body>")
    htmlBuilder.append("<div class=\"container py-5\"><div class=\"card mb-4\"><br><center><h1>Test Execution Summary</h1></center>")
    htmlBuilder.append("<div class=\"card-body\">Device Name: ${summary.deviceName}<br>Test Result Summary: ${if (summary.passed) "PASSED" else "FAILED"}<br>Total Suite: ${summary.suites.size}<br>Total Test: ${summary.suites.sumOf { it.flows.size }}</div></div>")

    for (suite in summary.suites) {
        htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-body\"><center><b>Test Suite</b></center><br>Summary: ${if (suite.passed) "PASSED" else "FAILED"}<br>Duration: ${suite.duration}<br>Test Count: ${suite.flows.size}<br><br>")
        for (flow in suite.flows) {
            val buttonClass = if (flow.status.toString() == "ERROR") "btn btn-danger" else "btn btn-success"
            htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-header\"><h5 class=\"mb-0\">")
            htmlBuilder.append("<button class=\"$buttonClass\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#${flow.name}\" aria-expanded=\"false\" aria-controls=\"${flow.name}\">${flow.name} : ${flow.status}</button></h5></div>")
            htmlBuilder.append("<div id=\"${flow.name}\" class=\"collapse\"><div class=\"card-body\">")

            htmlBuilder.append("<p class=\"card-text\">Status: ${flow.status}<br>Duration: ${flow.duration}<br>File Name: ${flow.fileName}</p>")
            if (flow.failure != null) {
                // htmlBuilder.append("<p class=\"card-text text-danger\">${failedStep[idx]}</p>")
                htmlBuilder.append("<p class=\"card-text text-danger\">${flow.failure.message}</p>")
                // idx++
            }
            htmlBuilder.append("<div id=\"accordion\"><div class=\"card\"><div class=\"card-header\" id=\"headingOne\"><h5 class=\"mb-0\"><button class=\"btn btn-link\" data-toggle=\"collapse show\" data-target=\"#test-step\" aria-expanded=\"true\" aria-controls=\"test-step\">Detail Steps</button></h5></div>")
            htmlBuilder.append("<div id=\"test-step\" class=\"collapse show\" aria-labelledby=\"headingOne\" data-parent=\"#accordion\"><div class=\"card-body\"><p class=\"card-text\">")
            for(step in testSteps[idx]){
                htmlBuilder.append("${step}<br>")
            }
            htmlBuilder.append("</p></div></div></div></div>")
            htmlBuilder.append("</div></div></div>")
            idx++
        }
    }
    htmlBuilder.append("<script src=\"https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.7/dist/umd/popper.min.js\"></script><script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js\"></script>")
    htmlBuilder.append("</div></div></div></body></html>")
    return htmlBuilder.toString()
    }
}
