
import Foundation
import XCTest

struct PressButtonRequest: Codable {
    enum Button: String, Codable {
        case home
    }

    let button: Button

    var xctestButton: XCUIDevice.Button {
        switch button {
        case .home: return .home
        }
    }
}
