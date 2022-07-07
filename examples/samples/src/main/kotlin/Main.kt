import conductor.Conductor
import conductor.orchestra.AssertCommand
import conductor.orchestra.ConductorCommand
import conductor.orchestra.ElementSelector
import conductor.orchestra.LaunchAppCommand
import conductor.orchestra.Orchestra
import conductor.orchestra.TapOnElementCommand
import dadb.Dadb

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

fun main() {
    executeAndroidCommands()
    executeIOSCommands()
}

private fun executeAndroidCommands() {
    val dadb = Dadb.create("localhost", 5555)
    val conductor = Conductor.android(dadb)
    val launchAppCommand = ConductorCommand(launchAppCommand = LaunchAppCommand("dev.mobile.sample"))
    val tapViewDetailsCommand = ConductorCommand(
        tapOnElement = TapOnElementCommand(ElementSelector(textRegex = "VIEW DETAILS"))
    )
    val assertCommand = ConductorCommand(
        assertCommand = AssertCommand(ElementSelector(textRegex = "Here is the detailed content"))
    )
    conductor.use {
        Orchestra(it).executeCommands(
            listOf(
                launchAppCommand,
                tapViewDetailsCommand,
                assertCommand
            )
        )
    }
}

private fun executeIOSCommands() {
    val conductor = Conductor.ios("localhost", 10883)
    val launchAppCommand = ConductorCommand(
        launchAppCommand = LaunchAppCommand("ch.admin.bag.covidcertificate.verifier.dev")
    )
    val tapOnElementCommand = ConductorCommand(
        tapOnElement = TapOnElementCommand(
            ElementSelector(textRegex = "HOW IT WORKS")
        )
    )
    val assertCommand = ConductorCommand(
        assertCommand = AssertCommand(
            ElementSelector(textRegex = "HOW IT WORKS")
        )
    )
    conductor.use {
        Orchestra(conductor).executeCommands(
            listOf(launchAppCommand, tapOnElementCommand, assertCommand)
        )
    }
}