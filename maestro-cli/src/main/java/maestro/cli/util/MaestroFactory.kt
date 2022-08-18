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

package maestro.cli.util

import maestro.Maestro
import maestro.utils.SocketUtils.isPortOpen
import dadb.Dadb

object MaestroFactory {
    @Suppress("NAME_SHADOWING")
    fun createMaestro(platform: String?, host: String?, port: Int?): Maestro {
        val host = host ?: "localhost"
        val port = port ?: 10882

        return when (platform) {
            null -> {
                val dadb = Dadb.discover(host)

                if (dadb != null) {
                    if (isPortOpen(host, port)) {
                        throw IllegalStateException("Multiple devices found. Specify one with --platform <android|ios> --host and/or --port")
                    }
                    return Maestro.android(dadb)
                } else if (isPortOpen(host, port)) {
                    return Maestro.ios(host, port)
                } else {
                    throw IllegalStateException("No device found")
                }
            }
            "android" -> {
                val dadb = Dadb.discover(host)
                    ?: throw IllegalStateException("No Android devices found")

                return Maestro.android(dadb)
            }
            "ios" -> {
                Maestro.ios(host, port)
            }
            else -> {
                throw IllegalStateException("Unknown platform: $platform")
            }
        }
    }
}
