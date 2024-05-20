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
    
        try {
            reader = BufferedReader(FileReader(filePathLog))
            var line: String = ""
    
            while (reader.readLine().also { line = it } != null) {
                // Process each line
                if(line.contains("m.cli.runner.TestSuiteInteractor") && !line.contains("Run") && !line.contains("Define variables") && !line.contains("Apply configuration")){
                    if(line.contains("COMPLETED") || line.contains("FAILED")){
                        testStep += line.replace("[INFO ] m.cli.runner.TestSuiteInteractor ", "")
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

    private fun getFailedTest(summary: TestExecutionSummary): Array<String>{
        var failedTest = emptyArray<String>()
        for (suite in summary.suites) {
            for(flow in suite.flows){
                if(flow.status.toString() == "ERROR"){
                    failedTest += flow.name
                }
            }
        }
        return failedTest
    }

    private fun buildHtmlReport(summary: TestExecutionSummary): String {
    val htmlBuilder = StringBuilder()
    val testSteps = getTestStep()
    var idx = 0
    var failedTest = getFailedTest(summary)

    htmlBuilder.append("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><title>Maestro Test Report</title><link href=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css\" rel=\"stylesheet\" integrity=\"sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH\" crossorigin=\"anonymous\"></head><body>")

    for (suite in summary.suites) {
        htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-body\"><center><h1>Test Execution Summary</h1></center><br>Test Result: ${if (suite.passed) "PASSED" else "FAILED"}<br>Duration: ${suite.duration}<br><br>")
        htmlBuilder.append("<div class=\"card-group mb-4\"><div class=\"card\"><div class=\"card-body\"><center><h5 class=\"card-title\">Total Test</h5><h3 class=\"card-text\">${suite.flows.size}</h3></center></div></div>")
        htmlBuilder.append("<div class=\"card text-white bg-danger\"><div class=\"card-body\"><center><h5 class=\"card-title\">Failed Test</h5><h3 class=\"card-text\">${failedTest.size}</h3></center></div></div>")
        htmlBuilder.append("<div class=\"card text-white bg-success\"><div class=\"card-body\"><center><h5 class=\"card-title\">Success Test</h5><h3 class=\"card-text\">${suite.flows.size - failedTest.size}</h3></center></div></div></div>")
        if(failedTest.size != 0) htmlBuilder.append("<div class=\"card border-danger mb-3\"><div class=\"card-body text-danger\"><b>Failed Test</b><br><p class=\"card-text\">")
        for(test in failedTest){
            htmlBuilder.append("${test}<br>")
        }
        htmlBuilder.append("</p></div></div>")
        for (flow in suite.flows) {
            val buttonClass = if (flow.status.toString() == "ERROR") "btn btn-danger" else "btn btn-success"
            htmlBuilder.append("<div class=\"card mb-4\"><div class=\"card-header\"><h5 class=\"mb-0\">")
            htmlBuilder.append("<button class=\"$buttonClass\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#${flow.name}\" aria-expanded=\"false\" aria-controls=\"${flow.name}\">${flow.name} : ${flow.status}</button></h5></div>")
            htmlBuilder.append("<div id=\"${flow.name}\" class=\"collapse\"><div class=\"card-body\">")

            htmlBuilder.append("<p class=\"card-text\">Status: ${flow.status}<br>Duration: ${flow.duration}<br>File Name: ${flow.fileName}</p>")
            if (flow.failure != null) {
                htmlBuilder.append("<p class=\"card-text text-danger\">${flow.failure.message}</p>")
            }
            htmlBuilder.append("<div class=\"accordion\"><div class=\"accordion-item\"><h5 class=\"accordion-header\"><button class=\"accordion-button border-danger\" type=\"button\" data-bs-toggle=\"collapse\" data-bs-target=\"#step-${flow.name}\" aria-expanded=\"false\" aria-controls=\"step-${flow.name}\">Test Step Details</button></h5>")
            htmlBuilder.append("<div id=\"step-${flow.name}\" class=\"collapse\"><div class=\"accordion-body\" style=\"max-height: 200px; overflow-y: auto;\">")
            for(step in testSteps[idx]){
                if(step.contains("FAILED")) htmlBuilder.append("<p class=\"text-danger\">${step}</p>") else htmlBuilder.append("${step}<br>")
            }
            htmlBuilder.append("</div></div></div></div>")
            
            htmlBuilder.append("</div></div></div>")
            idx++
        }
    }
    htmlBuilder.append("<script src=\"https://cdn.jsdelivr.net/npm/@popperjs/core@2.11.7/dist/umd/popper.min.js\"></script><script src=\"https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.min.js\"></script>")
    htmlBuilder.append("</div></div></div></body></html>")
    return htmlBuilder.toString()
    }
}
