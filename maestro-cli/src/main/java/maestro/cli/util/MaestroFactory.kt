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

    fun createMaestro(platform: String?, host: String?, port: Int?): Maestro {
        return when (platform) {
            null -> {
                try {
                    connectAndroid(host ?: "localhost")
                } catch (e: Exception) {
                    connectIos(host ?: "localhost", port ?: 10882)
                }
            }
            "android" -> {
                connectAndroid(host ?: "localhost")
            }
            "ios" -> {
                connectIos(host ?: "localhost", port ?: 10882)
            }
            else -> {
                throw IllegalStateException("Unknown platform: $platform")
            }
        }
    }

    private fun connectAndroid(host: String): Maestro {
        val dadb = Dadb.discover(host)
        return if (dadb != null) {
            Maestro.android(dadb)
        } else {
            println("No Android devices found")
            throw IllegalStateException()
        }
    }

    private fun connectIos(host: String, port: Int): Maestro {
        return Maestro.ios(host, port)
    }

    private fun findDevice(): Maestro {
        var device: Maestro? = null

        val dadb = Dadb.discover("localhost")
        if (dadb != null) {
            device = Maestro.android(dadb)
        }

        if (isPortOpen("localhost", 10882)) {
            if (device == null) {
                return Maestro.ios("localhost", 10882)
            } else {
                throw IllegalStateException("Multiple devices found. Specify one with --target")
            }
        }

        if (isPortOpen("localhost", 10883)) {
            if (device == null) {
                return Maestro.ios("localhost", 10883)
            } else {
                throw IllegalStateException("Multiple devices found. Specify one with --target")
            }
        }

        return device ?: throw IllegalStateException("No device found")
    }

}
