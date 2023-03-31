import Foundation
import FlyingFox
import os

@MainActor
struct SetPermissionsHandler: HTTPHandler {
    let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )
    
    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let decoder = JSONDecoder()
        
        guard let requestBody = try? decoder.decode(SetPermissionsRequest.self, from: request.body) else {
            let errorData = handleError(message: "incorrect request body provided")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }
        
        UserDefaults.standard.set(requestBody.permissions, forKey: "permissions")
        
        return HTTPResponse(statusCode: .ok)
    }
    
    private func handleError(message: String) -> Data {
        logger.error("Failed to set app permissions - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
}
