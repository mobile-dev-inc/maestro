import Foundation
import FlyingFox
import os
import XCTest
import Network

@MainActor
struct PressButtonHandler: HTTPHandler {

    let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )

    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let requestBody = try JSONDecoder().decode(PressButtonRequest.self, from: request.body)
        
        switch requestBody.button {
        case .home:
            XCUIDevice.shared.press(.home)
        case .lock:
            XCUIDevice.shared.perform(NSSelectorFromString("pressLockButton"))
        }

        return HTTPResponse(statusCode: .ok)
    }
}
