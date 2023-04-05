import Foundation
import FlyingFox
import os
import XCTest
import Network

@MainActor
struct SessionInfoHandler: HTTPHandler {

    let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )
    
    private let sessionId = UUID()

    func handleRequest(_ request: HTTPRequest) async throws -> HTTPResponse {
        let sessionInfo = SessionInfoResponse(
            sessionId: sessionId
        )

        let responseBody = try! JSONEncoder().encode(sessionInfo)
        return HTTPResponse(statusCode: .ok, body: responseBody)
    }

    private func handleError(message: String) -> Data {
        logger.error("Failed - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
}
