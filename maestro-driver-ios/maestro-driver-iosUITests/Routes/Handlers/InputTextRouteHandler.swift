import FlyingFox
import XCTest
import os

class InputTextRouteHandler : RouteHandler {
    
    private let logger = Logger(subsystem: Bundle.main.bundleIdentifier!, category: "InputTextRouteHandler")
    
    func handle(request: FlyingFox.HTTPRequest) async throws -> FlyingFox.HTTPResponse {
        let decoder = JSONDecoder()
        
        guard let requestBody = try? decoder.decode(InputTextRequest.self, from: request.body) else {
            let errorData = handleError(message: "incorrect request body provided")
            return HTTPResponse(statusCode: HTTPStatusCode.badRequest, body: errorData)
        }

        return try await withCheckedThrowingContinuation { continuation in

            let ObjC = ObjCRuntime()
            let daemonSession = ObjC.XCTRunnerDaemonSession.sharedSession()
            let proxy = daemonSession.daemonProxy().handle as! NSObject

            // ObjCRuntime does not work correctly because
            // class_getInstanceMethod(object_getClass(proxy), "_XCT_sendString:maximumFrequency:completion:") returns null
            // while proxy.method(for: NSSelectorFromString("_XCT_sendString:maximumFrequency:completion:")) does work
            //
            //            let proxy = daemonSession.daemonProxy()
            //            proxy._XCT_sendString(requestBody.text, maximumFrequency: 1, completion: { (error: Error?) in
            //                if let error = error {
            //                    continuation.resume(with: .failure(error))
            //                } else {
            //                    continuation.resume(with: .success(HTTPResponse(statusCode: .ok)))
            //                }
            //            })

            typealias sendStringMethod = @convention(c) (NSObject, Selector, NSString, Int, @escaping (Error?) -> ()) -> ()

            let selector = NSSelectorFromString("_XCT_sendString:maximumFrequency:completion:")
            let methodIMP = proxy.method(for: selector)

            let method = unsafeBitCast(methodIMP, to: sendStringMethod.self)
            method(proxy, selector, requestBody.text as NSString, 1, { error in
                if let error = error {
                    continuation.resume(with: .failure(error))
                } else {
                    continuation.resume(with: .success(HTTPResponse(statusCode: .ok)))
                }
            })
        }
    }

    private func handleError(message: String) -> Data {
        logger.error("Failed to input text - \(message)")
        let jsonString = """
         { "errorMessage" : \(message) }
        """
        return Data(jsonString.utf8)
    }
    
}
