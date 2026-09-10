import FlyingFox
import XCTest
import os

@MainActor
struct DoubleTouchRouteHandler: HTTPHandler {
    private static let defaultInterval: TimeInterval = 0.1

    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()

        guard let requestBody = try? await decoder.decode(DoubleTouchRequest.self, from: request.bodyData) else {
            NSLog("Invalid request for double tapping")
            return AppError(type: .precondition, message: "incorrect request body provided for doubleTouch route").httpResponse
        }

        let (width, height) = ScreenSizeHelper.physicalScreenSize()
        let point = ScreenSizeHelper.orientationAwarePoint(
            width: width,
            height: height,
            point: CGPoint(x: CGFloat(requestBody.x), y: CGFloat(requestBody.y))
        )
        let (x, y) = (point.x, point.y)
        let interval = requestBody.interval ?? Self.defaultInterval

        NSLog("Double tapping \(x), \(y) with interval \(interval)s")

        do {
            let eventRecord = EventRecord(orientation: ScreenSizeHelper.currentInterfaceOrientation())
            _ = eventRecord.addDoubleTapEvent(
                at: CGPoint(x: CGFloat(x), y: CGFloat(y)),
                interval: interval,
                holdDuration: requestBody.duration
            )
            let start = Date()
            try await RunnerDaemonProxy().synthesize(eventRecord: eventRecord)
            let duration = Date().timeIntervalSince(start)
            NSLog("Double tapping took \(duration)")
            return HTTPResponse(statusCode: .ok)
        } catch {
            NSLog("Error double tapping: \(error)")
            return AppError(message: "Error double tapping point: \(error.localizedDescription)").httpResponse
        }
    }
}
