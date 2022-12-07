import FlyingFox
import XCTest

class GetRunningAppRouteHandler: RouteHandler {
    private static let springboardBundleId = "com.apple.springboard"
    
    func handle(request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        
        guard let requestBody = try? decoder.decode(GetRunningAppRequest.self, from: request.body) else {
            throw ServerError.GetRunningAppRequestSerializeFailure
        }
        
        let runningAppId = requestBody.appIds.first { appId in
            let app = XCUIApplication(bundleIdentifier: appId)
            
            return app.state == .runningForeground
        }
        
        let response = ["runningAppBundleId": runningAppId ?? GetRunningAppRouteHandler.springboardBundleId]
        
        guard let responseData = try? JSONSerialization.data(
            withJSONObject: response,
            options: .prettyPrinted
        ) else {
            print("Serialization of app state failed")
            throw ServerError.GetRunningAppResponseSerializeFailure
        }
        
        return HTTPResponse(statusCode: .ok, body: responseData)
    }
}
