//
//  maestro_driver_iosUITests.swift
//  maestro-driver-iosUITests
//
//  Created by Amanjeet Singh on 28/11/22.
//

import XCTest
import FlyingFox
import maestro_driver_ios

class maestro_driver_iosUITests: XCTestCase {

    override func setUpWithError() throws {
        // Put setup code here. This method is called before the invocation of each test method in the class.

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }

    override func tearDownWithError() throws {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
    }

    func testHttpServer() async throws {
        let server = HTTPServer(port: 9080)
        let subTreeRoute = HTTPRoute("/subTree?appId=*")
        await server.appendRoute(subTreeRoute) { request in
            guard let appId = request.query["appId"] else {
                return HTTPResponse(statusCode: HTTPStatusCode.badRequest)
            }
            let viewHierarchyDictionaryResult = await MainActor.run {
                try? XCUIApplication(bundleIdentifier: appId).snapshot().dictionaryRepresentation
            }
            guard let viewHierarchyDictionary = viewHierarchyDictionaryResult else {
                print("Cannot return view hierarchy, throwing exception..")
                throw ServerError.ApplicationSnapshotFailure
            }
            guard let hierarchyJsonData = try? JSONSerialization.data(
                withJSONObject: viewHierarchyDictionary,
                options: .prettyPrinted
            ) else {
                print("Serialization of view hierarchy failed")
                throw ServerError.SnapshotSerializeFailure
            }
            return HTTPResponse(statusCode: .ok, body: hierarchyJsonData)
        }
        try await server.start()
    }
    
    enum ServerError: Error {
        case ApplicationSnapshotFailure
        case SnapshotSerializeFailure
    }
}
