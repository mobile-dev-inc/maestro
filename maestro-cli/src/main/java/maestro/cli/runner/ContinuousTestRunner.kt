/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package maestro.cli.runner

import maestro.Maestro
import org.fusesource.jansi.Ansi.ansi
import org.fusesource.jansi.AnsiConsole
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future

class ContinuousTestRunner(
    private val maestro: Maestro,
    private val testFile: File,
) {

    fun run() {
        AnsiConsole.systemInstall()
        println(ansi().eraseScreen())

        val view = ResultView()

        val fileWatcher = FileWatcher()
        val executor = Executors.newSingleThreadExecutor()
        var future: Future<*>? = null

        maestro.use { maestro ->
            val commandRunner = MaestroCommandRunner(
                maestro = maestro,
                view = view,
            )

            fileWatcher.register(testFile.toPath()) {
                cancelFuture(future)

                future = executor.submit {
                    commandRunner.run(testFile)
                }
            }

            fileWatcher.start()
        }
    }

    private fun cancelFuture(future: Future<*>?) {
        try {
            future?.cancel(true)
            future?.get()
        } catch (ignored: Exception) {
            // Do nothing
        }
    }
}
