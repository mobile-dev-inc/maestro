package maestro.cli.command

import kotlinx.coroutines.runBlocking
import maestro.cli.util.FileDownloader
import org.fusesource.jansi.Ansi
import org.rauschig.jarchivelib.ArchiverFactory
import picocli.CommandLine
import picocli.CommandLine.Option
import java.io.File
import java.util.concurrent.Callable

@CommandLine.Command(
    name = "download-samples",
)
class DownloadSamplesCommand : Callable<Int> {

    @Option(names = ["-o", "--output"], description = ["Output directory"])
    private var outputDirectory: File? = null

    override fun call(): Int {
        val folder = ensureSamplesFolder()
        val samplesFile = File("maestro-samples.zip")

        runBlocking {
            try {
                downloadSamplesZip(samplesFile)

                val archiver = ArchiverFactory.createArchiver(samplesFile)
                archiver.extract(samplesFile, folder)

                message("✅ Samples downloaded to $folder/")
            } finally {
                samplesFile.delete()
            }
        }

        return 0
    }

    private suspend fun downloadSamplesZip(file: File) {
        val progressView = DownloadProgress(20)

        FileDownloader
            .downloadFile(
                SAMPLES_URL,
                file
            ).collect {
                when (it) {
                    is FileDownloader.DownloadResult.Success -> {
                        // Do nothing
                    }
                    is FileDownloader.DownloadResult.Error -> {
                        println("Error: ${it.message}") // TODO handle errors
                    }
                    is FileDownloader.DownloadResult.Progress -> {
                        progressView.set(it.progress)
                    }
                }
            }
    }

    private fun ensureSamplesFolder(): File {
        val outputDir = outputDirectory
            ?: File("samples")

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }

        return outputDir
    }

    class DownloadProgress(private val width: Int) {

        private var progressWidth: Int? = null

        fun set(progress: Float) {
            val progressWidth = (progress * width).toInt()
            if (progressWidth == this.progressWidth) return
            this.progressWidth = progressWidth
            val ansi = Ansi.ansi()
            ansi.cursorToColumn(0)
            ansi.fgCyan()
            repeat(progressWidth) { ansi.a("█") }
            repeat(width - progressWidth) { ansi.a("░") }
            print(ansi)
        }
    }

    companion object {

        private const val SAMPLES_URL = "https://github.com/mobile-dev-inc/maestro/releases/download/cli-1.9.0/maestro-1.9.0.zip"

        private fun message(message: String) {
            println(
                Ansi.ansi()
                    .render("\n")
                    .fgDefault()
                    .render(message)
            )
        }

    }

}