import FlyingFox
import XCTest
import os

@MainActor
struct InputTextRouteHandler : HTTPHandler {
    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )
    
    private let defaultTypingSpeed = 10
    // in case of exceeding 50 symbols maestro will rely on copy-paste functionality
    private let maxDefaultInputCharacters = 50

    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()

        guard let requestBody = try? decoder.decode(InputTextRequest.self, from: request.body) else {
            return errorResponse(message: "incorrect request body provided")
        }

        do {
            let start = Date()

            var eventPath = PointerEventPath.pathForTextInput()
            let typingSpeed = calculateTypingSpeed(for: requestBody.text)
            logger.info("Typing text \"\(requestBody.text)\" with speed \(typingSpeed) characters per second")
            eventPath.type(text: requestBody.text, typingSpeed: typingSpeed)
            let eventRecord = EventRecord(orientation: .portrait)
            _ = eventRecord.add(eventPath)
            try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord)

            let duration = Date().timeIntervalSince(start)
            logger.info("Text input duration took \(duration)")
            return HTTPResponse(statusCode: .ok)
        } catch {
            logger.error("Error inputting text '\(requestBody.text)': \(error)")
            return errorResponse(message: "internal error")
        }
    }

    private func errorResponse(message: String) -> HTTPResponse {
        logger.error("Failed to input text - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        let errorData = Data(jsonString.utf8)
        return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
    }
    
    private func calculateTypingSpeed(for text: String) -> Int {
        let maxCommandTimeSeconds = 5.0
        let timeWithDefaultSpeed = Double(text.count) / Double(defaultTypingSpeed)

        if timeWithDefaultSpeed < maxCommandTimeSeconds {
            return defaultTypingSpeed
        } else {
            return Int((Double(text.count) / maxCommandTimeSeconds).rounded())
        }
    }
}
