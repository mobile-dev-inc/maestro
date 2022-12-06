import FlyingFox
import XCTest

class SubTreeRouteHandler: RouteHandler {
    func handle(request: HTTPRequest) async throws -> FlyingFox.HTTPResponse {
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
}
