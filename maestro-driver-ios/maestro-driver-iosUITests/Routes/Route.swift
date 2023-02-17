import FlyingFox

enum Route: String, CaseIterable {
    case subTree = "/subTree?appId=*"
    case runningApp = "/runningApp"
    case swipe = "/swipe?appId=*"
    case inputText = "/inputText?appId=*"
    case touch = "/touch?appId=*"
    case screenshot
    case isScreenStatic
    
    func toHTTPRoute() -> HTTPRoute {
        switch self {
        case .subTree:
            return HTTPRoute(rawValue)
        case .runningApp:
            return HTTPRoute(method: .POST,
                             path: rawValue)
        case .swipe:
            return HTTPRoute(method: .POST,
                             path: rawValue)
        case .inputText:
            return HTTPRoute(method: .POST,
                             path: rawValue)
        case .touch:
            return HTTPRoute(method: .POST,
                             path: rawValue)
        case .screenshot:
            return HTTPRoute(rawValue)
        case .isScreenStatic:
            return HTTPRoute(rawValue)
        }
    }
}
