import FlyingFox
import XCTest
import os

class ScreenshotHandler : RouteHandler {
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "ScreenshotHandler")
    
    func handle(request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let fullScreenshot = XCUIScreen.main.screenshot()
        let image = fullScreenshot.image.jpegData(compressionQuality: 0.5)
        
        guard let image = image else {
            let errorData = handleError(message: "no image data received from sreenshot() operation")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        
        return HTTPResponse(statusCode: .ok, body: image)
    }
    
    private func handleError(message: String) -> Data {
        logger.error("Failed to capture simulator's screenshot - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
}
