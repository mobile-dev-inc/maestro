package maestro.cli.report

import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.img
import kotlinx.html.lang
import kotlinx.html.main
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.stream.appendHTML
import kotlinx.html.title
import okio.Sink
import okio.buffer

// TODO(bartekpacia): Decide if AI output can be considered "test output", and therefore be present in e.g. JUnit report
class HtmlAITestSuiteReporter {

    fun report(summary: SingleFlowAIOutput, out: Sink) {
        val bufferedOut = out.buffer()
        val htmlContent = buildHtmlReport(summary)
        bufferedOut.writeUtf8(htmlContent)
        bufferedOut.close()
    }

    private fun buildHtmlReport(summary: SingleFlowAIOutput): String {
        return buildString {
            appendLine("<!DOCTYPE html>")
            appendHTML().html {
                lang = "en"

                head {
                    title { +"Maestro Test Report" }
                    script { src = "https://cdn.tailwindcss.com/3.4.5" }
                }

                body {
                    div(classes = "flex min-h-screen flex-col") {

                        div(classes = "container mx-auto py-6 space-y-2") {
                            h1(classes = "text-3xl") {
                                +"Flow \"${summary.flowName}\" – AI output"
                            }
                            p {
                                +"Flow file: "
                                a(
                                    // FIXME(bartekpacia): This path will be broken when moved across machines
                                    href = summary.flowFile.absolutePath
                                ) {
                                    +summary.flowFile.name
                                }
                            }
                        }
                        main(classes = "container mx-auto flex flex-col gap-4") {
                            p(classes = "text-[#4f4b5c]") { +"10 possible defects found" }

                            summary.screenOutputs.forEach { singleScreenOutput ->
                                div(classes = "flex items-start gap-4 bg-white") {
                                    p(classes = "text-lg") {
                                        +"${singleScreenOutput.defects.size} possible defects"
                                    }

                                    singleScreenOutput.defects.forEach { defect ->
                                        img(classes = "w-64 rounded-lg border-2 border-[#4f4b5c]") {
                                            alt = "Screenshot of the defect"
                                            // Use relative path, so when file is moved across machines, it still works
                                            src = singleScreenOutput.screenshotPath.name.toString()
                                        }

                                        div(classes = "flex flex-col gap-4") {
                                            div(classes = "flex flex-col items-start gap-2 rounded-lg bg-[#f8f8f8] p-2") {
                                                p(classes = "text-[#110c22]") {
                                                    +defect.reasoning
                                                }

                                                div(classes = "rounded-lg bg-[#ececec] p-1 font-semibold text-[#4f4b5c]") {
                                                    +defect.category
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
