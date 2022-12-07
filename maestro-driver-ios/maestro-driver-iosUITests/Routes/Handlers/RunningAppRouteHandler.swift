import FlyingFox
import XCTest

class RunningAppRouteHandler: RouteHandler {
    private static let springboardBundleId = "com.apple.springboard"
    
    func handle(request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        
        let str = String(decoding: request.body, as: UTF8.self)
        print(str)
        
        guard let requestBody = try? decoder.decode(RunningAppRequest.self, from: request.body) else {
            throw ServerError.RunningAppRequestSerializeFailure
        }
        
        let runningAppId = requestBody.appIds.first { appId in
            let app = XCUIApplication(bundleIdentifier: appId)
            
            return app.state == .runningForeground
        }
        
        let response = ["runningAppBundleId": runningAppId ?? RunningAppRouteHandler.springboardBundleId]
        
        guard let responseData = try? JSONSerialization.data(
            withJSONObject: response,
            options: .prettyPrinted
        ) else {
            print("Serialization of runningAppBundleId failed")
            throw ServerError.RunningAppResponseSerializeFailure
        }
        
        return HTTPResponse(statusCode: .ok, body: responseData)
    }
}
