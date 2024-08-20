package maestro.cli.report

import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.img
import kotlinx.html.lang
import kotlinx.html.main
import kotlinx.html.meta
import kotlinx.html.p
import kotlinx.html.script
import kotlinx.html.stream.appendHTML
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.io.File

// TODO(bartekpacia): Decide if AI output can be considered "test output", and therefore be present in e.g. JUnit report
class HtmlAITestSuiteReporter {

    private val FlowAIOutput.htmlReportFilename
        get() = "ai-report-${flowName}.html"

    /**
     * Creates HTML files in [outputDestination] for each flow in [summary].
     */
    fun report(outputs: List<FlowAIOutput>, outputDestination: File) {
        if (!outputDestination.isDirectory) throw IllegalArgumentException("Output destination must be a directory")

        outputs.forEachIndexed { index, output ->
            val htmlContent = buildHtmlReport(outputs, index)
            val file = File(outputDestination, output.htmlReportFilename)
            // FIXME(bartekpacia): what if file doesn't exist?
            file.writeText(htmlContent)
        }
    }

    /**
     * Build HTML report for a single flow.
     *
     * Information about other flows is needed to generate links to them.
     */
    private fun buildHtmlReport(outputs: List<FlowAIOutput>, index: Int): String {
        val summary = outputs[index]

        return buildString {
            appendLine("<!DOCTYPE html>")
            appendHTML().html {
                lang = "en"

                head {
                    meta { charset = "UTF-8" }
                    meta { name = "viewport"; content = "width=device-width, initial-scale=1.0" }
                    title { +"Maestro Test Report" }
                    script { src = "https://cdn.tailwindcss.com/3.4.5" }

                    script {
                        unsafe {
                            // TODO(bartekpacia): Load it from jar resources
                            +"""
                          function myFunction() {
                            alert("Hello! I am an alert box!");
                          }

                          tailwind.config = {
                            darkMode: "media",
                            theme: {
                              extend: {
                                colors: {
                                  "gray-dark": "#110c22", // text-gray-dark
                                  "gray-medium": "#4f4b5c", // text-gray-medium
                                  "gray-1": "#f8f8f8", // surface-gray-1
                                  "gray-0": "#110C22", // surface-gray-0
                                },
                              },
                            },
                          };
                        """.trimIndent()
                        }
                    }

                    style(type = "text/tailwindcss") {
                        // TODO(bartekpacia): Load it from jar resources
                        +"""
                        @layer components {
                            body {
                              @apply dark:bg-gray-dark dark:text-gray-1 text-gray-dark;
                            }

                            .screenshot-image {
                                @apply w-64 rounded-lg border-2 border-gray-medium dark:border-gray-1;
                            }

                            .screen-card {
                                @apply flex items-start gap-4;
                            }

                            .defect-card {
                              @apply flex flex-col items-start gap-2 rounded-lg bg-[#f8f8f8] p-2 text-gray-dark dark:bg-gray-medium dark:text-gray-1;
                            }

                            .badge {
                              @apply dark:text-red-500 rounded-lg bg-[#ececec] dark:bg-gray-dark p-1 font-semibold text-gray-medium dark:text-gray-1;
                            }

                            .toggle-link {
                              @apply block border-2 border-gray-medium bg-[#ececec] px-3 py-4 text-gray-medium hover:bg-gray-medium hover:text-[#ececec];
                            }
                    
                            .divider {
                              @apply h-0.5 rounded-sm bg-gray-medium dark:bg-gray-1 py-2;
                            }

                            .btn {
                              @apply hover:text-gray-medium dark:hover:text-gray-medium;
                            }
                        }
                        """.trimIndent()
                    }
                }

                body {
                    div(classes = "flex min-h-screen flex-col") {

                        // Header area
                        div(classes = "container mx-auto py-6 space-y-2") {
                            h1(classes = "text-3xl") { +"Flow \"${summary.flowName}\" – AI output" }

                            // File chooser for different reports
                            div(classes = "group relative inline-block self-start") {
                                button(classes = "btn") { +"→ Select different report" }
                                div(classes = "absolute z-10 hidden min-w-32 group-hover:block") {
                                    outputs.forEachIndexed { outputIndex, outputFlow ->
                                        val selected = outputIndex == index

                                        a(classes = "toggle-link") {
                                            href = "./" + outputs[outputIndex].htmlReportFilename
                                            val name = outputFlow.flowFile.nameWithoutExtension
                                            val maybeSelected = if (selected) "(X)" else ""
                                            +"Report ${outputIndex + 1} $name $maybeSelected"
                                        }
                                    }
                                }
                            }

                            // Link to the flow file
                            // FIXME(bartekpacia): This path will be broken when moved across machines
                            p {
                                a(
                                    classes = "btn", href = summary.flowFile.absolutePath
                                ) {
                                    +"→ Open flow file ( ${summary.flowFile.name} )"
                                }
                            }
                        }

                        // Container for list of screenshots
                        main(classes = "container mx-auto flex flex-col gap-4") {
                            // Overall defect count for the flow
                            p {
                                val word = if (summary.defectCount == 1) "defect" else "defects"
                                +"${summary.defectCount} possible $word found"
                            }

                            // List of screenshots within flow with defects founds
                            summary.screenOutputs.forEachIndexed { screenIndex, screenSummary ->
                                div(classes = "screen-card") {
                                    img(classes = "screenshot-image") {
                                        alt = "Screenshot of the defect"
                                        // Use relative path, so when file is moved across machines, it still works
                                        src = screenSummary.screenshotPath.name.toString()
                                    }

                                    // defect-card-container
                                    div(classes = "flex flex-col gap-4") {
                                        // Defect count for the screen
                                        p(classes = "text-lg") {
                                            val word = if (screenSummary.defects.size == 1) "defect" else "defects"
                                            +"${screenSummary.defects.size} possible $word"
                                        }

                                        screenSummary.defects.forEachIndexed { i, defect ->
                                            div(classes = "defect-card") {
                                                p { +defect.reasoning }
                                                div(classes = "badge") { +defect.category }
                                            }
                                        }
                                    }
                                }

                                if (screenIndex != screenSummary.defects.size - 1) {
                                    div(classes = "divider")
                                }
                            }
                        }
                    }
                }
            }
        }

//        private fun buildScreenSummary()

//        private fun buildDefectCard()
    }

    private val FlowAIOutput.defectCount: Int
        get() = screenOutputs.flatMap { it.defects }.size
}
