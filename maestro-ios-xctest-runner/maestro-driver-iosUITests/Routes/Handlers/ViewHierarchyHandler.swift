import FlyingFox
import XCTest
import os

@MainActor
struct ViewHierarchyHandler: HTTPHandler {

    #if os(tvOS)
    private static let homescreenBundleId = "com.apple.HeadBoard"
    #else
    private static let homescreenBundleId = "com.apple.springboard"
    #endif
    private let springboardApplication = XCUIApplication(bundleIdentifier: Self.homescreenBundleId)
    private let snapshotMaxDepth = 60

    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )

    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> HTTPResponse {
        guard let requestBody = try? JSONDecoder().decode(ViewHierarchyRequest.self, from: request.body) else {
            return AppError(type: .precondition, message: "incorrect request body provided").httpResponse
        }

        do {
            let runningAppIds = requestBody.appIds
            let app = getForegroundApp(runningAppIds)
            guard let app = app else {
                let springboardHierarchy = try elementHierarchy(xcuiElement: springboardApplication)
                let springBoardViewHierarchy = ViewHierarchy.init(axElement: springboardHierarchy, depth: springboardHierarchy.depth())
                let body = try JSONEncoder().encode(springBoardViewHierarchy)
                return HTTPResponse(statusCode: .ok, body: body)
            }

            let appViewHierarchy = try logger.measure(message: "View hierarchy snapshot for \(app)") {
                try getAppViewHierarchy(app: app, excludeKeyboardElements: requestBody.excludeKeyboardElements)
            }
            let viewHierarchy = ViewHierarchy.init(axElement: appViewHierarchy, depth: appViewHierarchy.depth())
            
            let body = try JSONEncoder().encode(viewHierarchy)
            return HTTPResponse(statusCode: .ok, body: body)
        } catch let error as AppError {
            logger.error("AppError in handleRequest, Error:\(error)");
            return error.httpResponse
        } catch let error {
            logger.error("Error in handleRequest, Error:\(error)");
            return AppError(message: "Snapshot failure while getting view hierarchy. Error: \(error.localizedDescription)").httpResponse
        }
    }

    func getForegroundApp(_ runningAppIds: [String]) -> XCUIApplication? {
        runningAppIds
            .map { XCUIApplication(bundleIdentifier: $0) }
            .first { app in app.state == .runningForeground }
    }

    func getAppViewHierarchy(app: XCUIApplication, excludeKeyboardElements: Bool) throws -> AXElement {
        SystemPermissionHelper.handleSystemPermissionAlertIfNeeded(springboardApplication: springboardApplication)
        // Fetch the view hierarchy of the springboard application
        // to make it possible to interact with the home screen.
        // Ignore any errors on fetching the springboard hierarchy.
        let springboardHierarchy: AXElement?
        do {
            springboardHierarchy = try elementHierarchy(xcuiElement: springboardApplication)
        } catch {
            logger.error("Springboard hierarchy failed to fetch: \(error)")
            springboardHierarchy = nil
        }

        let appHierarchy = try getHierarchyWithFallback(app)
        
        let keyboard = app.keyboards.firstMatch
        if (excludeKeyboardElements && keyboard.exists) {
            let filteredChildren = appHierarchy.filterAllChildrenNotInKeyboardBounds(keyboard.frame)
            return AXElement(children: [
                springboardHierarchy,
                AXElement(children: filteredChildren),
            ].compactMap { $0 })
        }

        return AXElement(children: [
            springboardHierarchy,
            appHierarchy,
        ].compactMap { $0 })
    }

    func getHierarchyWithFallback(_ element: XCUIElement) throws -> AXElement {
        logger.info("Starting getHierarchyWithFallback for element: \(element.debugDescription)")

        do {
            var hierarchy = try elementHierarchy(xcuiElement: element)
            logger.info("Successfully retrieved element hierarchy. Depth: \(hierarchy.depth())")

            if hierarchy.depth() < snapshotMaxDepth {
                return hierarchy
            }
            let count = try element.snapshot().children.count
            var children: [AXElement] = []
            for i in 0..<count {
              let element = element.descendants(matching: .other).element(boundBy: i).firstMatch
              children.append(try getHierarchyWithFallback(element))
            }
            hierarchy.children = children
            return hierarchy
        } catch let error {
            guard isIllegalArgumentError(error) else {
                logger.error("Snapshot failure, cannot return view hierarchy due to \(error.localizedDescription)")
                throw AppError(message: error.localizedDescription)
            }

            logger.error("Snapshot failure, getting recovery element for fallback")
            AXClientSwizzler.overwriteDefaultParameters["maxDepth"] = snapshotMaxDepth
            // In apps with bigger view hierarchys, calling
            // `XCUIApplication().snapshot().dictionaryRepresentation` or `XCUIApplication().allElementsBoundByIndex`
            // throws "Error kAXErrorIllegalArgument getting snapshot for element <AXUIElementRef 0x6000025eb660>"
            // We recover by selecting the first child of the app element,
            // which should be the window, and continue from there.

            let recoveryElement = try findRecoveryElement(element.children(matching: .any).firstMatch)
            let hierarchy = try getHierarchyWithFallback(recoveryElement)

            // When the application element is skipped, try to fetch
            // the keyboard, alert and other custom element hierarchies separately.
            if let element = element as? XCUIApplication {
                let keyboard = logger.measure(message: "Fetch keyboard hierarchy") {
                    keyboardHierarchy(element)
                }

                let alerts = logger.measure(message: "Fetch alert hierarchy") {
                    fullScreenAlertHierarchy(element)
                }

                let other = try logger.measure(message: "Fetch other custom element from window") {
                    try customWindowElements(element)
                }
                return AXElement(children: [
                    other,
                    keyboard,
                    alerts,
                    hierarchy
                ].compactMap { $0 })
            }

            return hierarchy
        }
    }

    private func isIllegalArgumentError(_ error: Error) -> Bool {
        error.localizedDescription.contains("Error kAXErrorIllegalArgument getting snapshot for element")
    }

    private func keyboardHierarchy(_ element: XCUIApplication) -> AXElement? {
        guard element.keyboards.firstMatch.exists else {
            return nil
        }
        
        let keyboard = element.keyboards.firstMatch
        return try? elementHierarchy(xcuiElement: keyboard)
    }
    
    private func customWindowElements(_ element: XCUIApplication) throws -> AXElement? {
        let windowElement = element.children(matching: .any).firstMatch
        if try windowElement.snapshot().children.count > 1 {
            return nil
        }
        return try? elementHierarchy(xcuiElement: windowElement)
    }

    func fullScreenAlertHierarchy(_ element: XCUIApplication) -> AXElement? {
        guard element.alerts.firstMatch.exists else {
            return nil
        }
        
        let alert = element.alerts.firstMatch
        return try? elementHierarchy(xcuiElement: alert)
    }

    private func findRecoveryElement(_ element: XCUIElement) throws -> XCUIElement {
        if try element.snapshot().children.count > 1 {
            return element
        }
        let firstOtherElement = element.children(matching: .other).firstMatch
        if (firstOtherElement.exists) {
            return try findRecoveryElement(firstOtherElement)
        } else {
            return element
        }
    }

    private func elementHierarchy(xcuiElement: XCUIElement) throws -> AXElement {
        let snapshotDictionary = try xcuiElement.snapshot().dictionaryRepresentation
        return AXElement(snapshotDictionary)
    }
}
