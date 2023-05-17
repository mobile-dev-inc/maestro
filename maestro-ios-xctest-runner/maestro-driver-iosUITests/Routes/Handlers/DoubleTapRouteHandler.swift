import FlyingFox
import XCTest
import os

@MainActor
struct DoubleTapRouteHandler: HTTPHandler {
    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )
    
    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()
        
        guard let requestBody = try? decoder.decode(DoubleTapRequest.self, from: request.body) else {
            let errorData = handleError(message: "incorrect request body provided")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        
        logger.info("Double tapping \(requestBody.x), \(requestBody.y)")
        
        let tap1 = EventRecord(orientation: .portrait)
        _ = tap1.addPointerTouchEvent(
            at: CGPoint(x: CGFloat(requestBody.x), y: CGFloat(requestBody.y)),
            touchUpAfter: nil
        )
        let tap2 = EventRecord(orientation: .portrait)
        _ = tap2.addPointerTouchEvent(
            at: CGPoint(x: CGFloat(requestBody.x), y: CGFloat(requestBody.y)),
            touchUpAfter: nil
        )

        do {
            let start = Date()
            try await RunnerDaemonProxy().synthesize(eventRecord: tap1)
            try await RunnerDaemonProxy().synthesize(eventRecord: tap2)
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
