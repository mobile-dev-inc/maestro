package xcuitest.installer

import java.io.File

interface XCTestInstaller {
    /**
     * Installs the XCTest Runner
     *
     * @return the file where the XCTest was installed
     */
    fun install(): File

    fun uninstall()
}
