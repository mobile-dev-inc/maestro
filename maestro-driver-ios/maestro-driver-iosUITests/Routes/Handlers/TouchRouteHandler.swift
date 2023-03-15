import FlyingFox
import XCTest
import os

private let logger = Logger(subsystem: Bundle.main.bundleIdentifier!,
                            category: String(describing: TouchRouteHandler.self))

@MainActor
final class TouchRouteHandler: HTTPHandler {
    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()
        
        guard let requestBody = try? decoder.decode(TouchRequest.self, from: request.body) else {
            let errorData = handleError(message: "incorrect request body provided")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        
        let duration = handleDuration(duration: requestBody.durationInMs)
        if duration != nil {
            logger.info("Tapping \(requestBody.x), \(requestBody.y) for \(duration!)s")
        } else {
            logger.info("Tapping \(requestBody.x), \(requestBody.y)")
        }
        
        var eventRecord = EventRecord(orientation: .portrait)
        eventRecord.addPointerTouchEvent(
            at: CGPoint(x: CGFloat(requestBody.x), y: CGFloat(requestBody.y)),
            touchUpAfter: duration
        )

        do {
            let start = Date()
            try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord)
            let duration = Date().timeIntervalSince(start)
            logger.info("Tapping took \(duration)")
            return HTTPResponse(statusCode: .ok)
        } catch {
            logger.error("Error tapping: \(error)")
            return HTTPResponse(statusCode: .internalServerError)
        }
    }
    
    private func handleDuration(duration: Int?) -> TimeInterval? {
        guard let duration else {
            return nil
        }
        return TimeInterval(Double(duration) / 1000)
    }
    
    private func handleError(message: String) -> Data {
        logger.error("Failed to tap - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
    
}
