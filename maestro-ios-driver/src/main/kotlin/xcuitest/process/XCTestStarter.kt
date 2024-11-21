package xcuitest.process

import java.io.File
import xcuitest.XCTestClient

interface XCTestStarter {

    fun start(xcTestRunFile: File): XCTestClient?

    fun stop()

    fun isRunning(): Boolean
}