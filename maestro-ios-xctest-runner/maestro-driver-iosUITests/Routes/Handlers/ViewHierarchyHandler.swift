import FlyingFox
import XCTest
import os

@MainActor
struct ViewHierarchyHandler: HTTPHandler {

    let maxDepth = 60
    let springboardApplication = XCUIApplication(bundleIdentifier: "com.apple.springboard")

    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )

    func measure<T>(message: String, _ block: () throws -> T) rethrows -> T {
        let start = Date()
        logger.info("\(message) - start")

        let result = try block()

        let duration = Date().timeIntervalSince(start)
        logger.info("\(message) - duration \(duration)")

        return result
    }
    
    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> HTTPResponse {
        guard let requestBody = try? JSONDecoder().decode(ViewHierarchyRequest.self, from: request.body) else {
            let errorInfo = [
                "errorMessage": "incorrect request body provided",
            ]
            let body = try? JSONEncoder().encode(errorInfo)
            return HTTPResponse(statusCode: .badRequest, body: body ?? Data())
        }

        let appId = requestBody.appId

        do {
            let viewHierarchy = try measure(message: "View hierarchy snapshot for \(appId)") {
                try getViewHierarchy(appId: appId)
            }

            let body = try JSONEncoder().encode(viewHierarchy)

            // TODO: Remove debug print here
            print(String(data: body, encoding: .utf8)!)

            return HTTPResponse(statusCode: .ok, body: body)
        } catch {
            print(error)
            let errorInfo = [
                "errorMessage": "Snapshot failure while getting view hierarchy. Error: \(error)",
            ]
            let body = try? JSONEncoder().encode(errorInfo)
            return HTTPResponse(statusCode: .internalServerError, body: body ?? Data())
        }
    }

    func handleSystemPermisionAlert() {
        SystemPermissionHelper.handleSystemPermissionAlertIfNeeded(springboardApplication: springboardApplication)
    }

    func getViewHierarchy(appId: String) throws -> AXElement {
        handleSystemPermisionAlert()

        let children = [
            try? springboardHierarchy(),
            try appHierarchy(XCUIApplication(bundleIdentifier: appId))
        ].compactMap { $0 }

        let root = AXElement(children: children)

        return root
    }

    func appHierarchy(_ xcuiApplication: XCUIApplication) throws -> AXElement {
        AXClientSwizzler.overwriteDefaultParameters["maxDepth"] = maxDepth
        return try elementHierarchyWithFallback(element: xcuiApplication)
    }

    func elementHierarchyWithFallback(element: XCUIElement) throws -> AXElement {
        do {
            var hierarchy = try elementHierarchy(xcuiElement: element)
            let hierarchyDepth = depth(hierarchy)

            if hierarchyDepth < maxDepth {
                logger.info("Hierarchy dept below maxdepth \(hierarchyDepth)")
                return hierarchy
            }

            // depth == maxdepth handling from here:

            let elementChildren = measure(message: "Get element children") {
                element
                    .children(matching: .any)
                    .allElementsBoundByIndex
            }

            hierarchy.children = try elementChildren
                .map { try elementHierarchyWithFallback(element: $0) }

            return hierarchy
        } catch {
            // In apps with bigger view hierarchys
            // calling XCUIApplication().snapshot().dictionaryRepresentation or XCUIApplication().allElementsBoundByIndex
            // throws "Error kAXErrorIllegalArgument getting snapshot for element <AXUIElementRef 0x6000025eb660>"
            // We recover by selecting the first child of the element,
            // which should be the app window, and continue from there.

            let firstChild = element
                .children(matching: .any)
                .firstMatch
            let childHierarchy = try elementHierarchyWithFallback(element: firstChild)

            return childHierarchy
        }

    }

    func depth(_ hierarchy: AXElement) -> Int {
        guard let children = hierarchy.children
        else { return 1 }

        let max = children
            .map { child in depth(child) + 1 }
            .max()

        return max ?? 1
    }


    func springboardHierarchy() throws -> AXElement {
        return try elementHierarchy(xcuiElement: springboardApplication)
    }

    func elementHierarchy(xcuiElement: XCUIElement) throws -> AXElement {
        return try measure(message: "Take element snapshot") {
            let snapshotDictionary = try xcuiElement.snapshot().dictionaryRepresentation
            return AXElement(snapshotDictionary)
        }
    }
}
