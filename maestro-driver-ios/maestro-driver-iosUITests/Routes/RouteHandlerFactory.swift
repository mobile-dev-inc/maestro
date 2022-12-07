import Foundation

class RouteHandlerFactory {
    class func createRouteHandler(route: Route) -> RouteHandler {
        switch route {
        case .subTree:
            return SubTreeRouteHandler()
        case .getRunningApp:
            return GetRunningAppRouteHandler()
        }
    }
}
