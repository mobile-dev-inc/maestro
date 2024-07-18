import FlyingFox
import XCTest
import os

@MainActor
struct TouchRouteHandler: HTTPHandler {
    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )
    
    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()
      
        let orientation = XCUIDevice.shared.orientation
        
        logger.info("XCUIDevice.shared.orientation: \(String(describing: orientation))")
      
        guard let requestBody = try? decoder.decode(TouchRequest.self, from: request.body) else {
            return AppError(type: .precondition, message: "incorrect request body provided for tap route").httpResponse
        }
        
        let (width, height) = ScreenSizeHelper.physicalScreenSize()
        
        let (x, y) = switch (orientation) {
        case .portrait: (requestBody.x, requestBody.y)
        case .landscapeLeft: (width - requestBody.y, requestBody.x)
        case .landscapeRight: (requestBody.y, height - requestBody.x)
        default: fatalError("Not implemented yet")
        }

        if requestBody.duration != nil {
            logger.info("Long pressing \(x), \(y) for \(requestBody.duration!)s")
        } else {
            logger.info("Tapping \(x), \(y)")
        }

        do {
            let eventRecord = EventRecord(orientation: .portrait)
            _ = eventRecord.addPointerTouchEvent(
                at: CGPoint(x: CGFloat(x), y: CGFloat(y)),
                touchUpAfter: requestBody.duration
            )
            let start = Date()
            try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord)
            let duration = Date().timeIntervalSince(start)
            logger.info("Tapping took \(duration)")
            return HTTPResponse(statusCode: .ok)
        } catch {
            logger.error("Error tapping: \(error)")
            return AppError(message: "Error tapping point: \(error.localizedDescription)").httpResponse
        }
    }
}
