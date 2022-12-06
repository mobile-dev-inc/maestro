import FlyingFox

class AppStateRouteHandler: RouteHandler {
    func handle(request: HTTPRequest) async throws -> HTTPResponse {
        // TODO (as): Add code to get the app state from bundle id
        return HTTPResponse(statusCode: .ok)
    }
}
