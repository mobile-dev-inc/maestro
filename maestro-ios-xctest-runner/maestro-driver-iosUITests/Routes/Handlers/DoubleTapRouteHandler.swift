import FlyingFox
import XCTest
import os

private let logger = Logger(subsystem: Bundle.main.bundleIdentifier!,
                            category: String(describing: DoubleTapRouteHandler.self))

@MainActor
struct DoubleTapRouteHandler: HTTPHandler {
    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()
        
        guard let requestBody = try? decoder.decode(DoubleTapRequest.self, from: request.body) else {
            let errorData = handleError(message: "incorrect request body provided")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        
        logger.info("Double tapping \(requestBody.x), \(requestBody.y)")
        
        let eventRecord = EventRecord(orientation: .portrait)
        _ = eventRecord.addPointerTouchEvent(
            at: CGPoint(x: CGFloat(requestBody.x), y: CGFloat(requestBody.y)),
            touchUpAfter: nil
        )
        _ = eventRecord.addPointerTouchEvent(
            at: CGPoint(x: CGFloat(requestBody.x), y: CGFloat(requestBody.y)),
            touchUpAfter: nil
        )

        do {
            let start = Date()
            try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord)
            let duration = Date().timeIntervalSince(start)
            logger.info("Double tapping took \(duration)")
            return HTTPResponse(statusCode: .ok)
        } catch {
            logger.error("Error double tapping: \(error)")
            return HTTPResponse(statusCode: .internalServerError)
        }
    }
    
    private func handleError(message: String) -> Data {
        logger.error("Failed to double tap - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
    

}
