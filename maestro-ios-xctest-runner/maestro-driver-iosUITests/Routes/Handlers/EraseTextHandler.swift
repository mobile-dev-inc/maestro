import Foundation
import FlyingFox
import os
import XCTest
import Network

@MainActor
struct EraseTextHandler: HTTPHandler {
    private let typingFrequency = 30

    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )

    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        guard let requestBody = try? JSONDecoder().decode(EraseTextRequest.self, from: request.body) else {
            let errorData = handleError(message: "incorrect request body provided")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        
        let appId = RunningApp.getForegroundAppId(requestBody.appIds)
        await waitUntilKeyboardIsPresented(appId: appId)
        
        // due to different keyboard input listener events (i.e. autocorrection or hardware keyboard connection)
        // caret might jump onto the beginning of the text field
        let warmupDeleteText = XCUIKeyboardKey.delete.rawValue
        var warmupEventPath = PointerEventPath.pathForTextInput()
        warmupEventPath.type(text: warmupDeleteText, typingSpeed: 1)
        let eventRecord1 = EventRecord(orientation: .portrait)
        _ = eventRecord1.add(warmupEventPath)
        try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord1)
        
        if (requestBody.charactersToErase > 1) {
            let deleteText = String(repeating: XCUIKeyboardKey.delete.rawValue, count: requestBody.charactersToErase - 1)
            var eventPath = PointerEventPath.pathForTextInput()
            eventPath.type(text: deleteText, typingSpeed: typingFrequency)
            let eventRecord2 = EventRecord(orientation: .portrait)
            _ = eventRecord2.add(eventPath)
            try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord2)
        }

        return HTTPResponse(statusCode: .ok)
    }

    private func handleError(message: String) -> Data {
        logger.error("Failed - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
    
    private func waitUntilKeyboardIsPresented(appId: String?) async {
        try? await TimeoutHelper.repeatUntil(timeout: 1, delta: 0.2) {
            guard let appId = appId else { return true }

            return XCUIApplication(bundleIdentifier: appId).keyboards.firstMatch.exists
        }
    }
}
