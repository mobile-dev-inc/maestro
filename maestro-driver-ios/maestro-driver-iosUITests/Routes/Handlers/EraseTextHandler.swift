import Foundation
import FlyingFox
import os
import XCTest
import Network

@MainActor
struct EraseTextHandler: HTTPHandler {
    private let typingFrequency = 30

    let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )

    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let requestBody = try JSONDecoder().decode(EraseTextRequest.self, from: request.body)
        let deleteText = String(repeating: XCUIKeyboardKey.delete.rawValue, count: requestBody.count)
        var eventPath = PointerEventPath.pathForTextInput()
        eventPath.type(text: deleteText, typingSpeed: typingFrequency)
        var eventRecord = EventRecord(orientation: .portrait)
        eventRecord.add(eventPath)
        try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord)

        return HTTPResponse(statusCode: .ok)
    }
}
