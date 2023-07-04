import FlyingFox
import XCTest
import os

extension NSException: Error {}

@MainActor
struct ViewHierarchyHandler: HTTPHandler {

    private static let springboardBundleId = "com.apple.springboard"
    private let springboardApplication = XCUIApplication(bundleIdentifier: Self.springboardBundleId)

    private let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )

    func handleRequest(_ request: FlyingFox.HTTPRequest) async throws -> HTTPResponse {
        guard let requestBody = try? JSONDecoder().decode(ViewHierarchyRequest.self, from: request.body) else {
            return AppError(type: .precondition, message: "incorrect request body provided").httpResponse
        }

        let runningAppIds = requestBody.appIds
        let appId = getRunningAppId(runningAppIds)

        do {
            if appId == ViewHierarchyHandler.springboardBundleId {
                let springboardHierarchy = try elementHierarchy(xcuiElement: springboardApplication)
                let body = try JSONEncoder().encode(springboardHierarchy)
                return HTTPResponse(statusCode: .ok, body: body)
            } else {
                let viewHierarchy = try logger.measure(message: "View hierarchy snapshot for \(appId)") {
                    try getViewHierarchy(appId: appId)
                }

                let body = try JSONEncoder().encode(viewHierarchy)

                return HTTPResponse(statusCode: .ok, body: body)
            }
        } catch let error as AppError {
            return error.httpResponse
        } catch let error {
            return AppError(message: "Snapshot failure while getting view hierarchy. Error: \(error.localizedDescription)").httpResponse
        }
    }

    func getViewHierarchy(appId: String) throws -> AXElement {
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

        let appHierarchy = try appHierarchy(XCUIApplication(bundleIdentifier: appId))

        return AXElement(children: [
            springboardHierarchy,
            appHierarchy,
        ].compactMap { $0 })
    }
    
    func getRunningAppId(_ runningAppIds: [String]) -> String {
        return runningAppIds.first { appId in
            let app = XCUIApplication(bundleIdentifier: appId)
            
            return app.state == .runningForeground
        } ?? ViewHierarchyHandler.springboardBundleId
    }

    func appHierarchy(_ xcuiApplication: XCUIApplication) throws -> AXElement {
        return try elementHierarchyWithFallback(element: xcuiApplication)
    }

    func elementHierarchyWithFallback(element: XCUIElement) throws -> AXElement {
        do {
            let hierarchy = try elementHierarchy(xcuiElement: element)
            return hierarchy
        } catch let error {
            guard isIllegalArgumentError(error) else {
                logger.error("Snapshot failure, cannot return view hierarchy due to \(error.localizedDescription)")
                throw AppError(message: error.localizedDescription)
            }

            logger.error("Snapshot failure, getting recovery element for fallback")
            // In apps with bigger view hierarchys, calling
            // `XCUIApplication().snapshot().dictionaryRepresentation` or `XCUIApplication().allElementsBoundByIndex`
            // throws "Error kAXErrorIllegalArgument getting snapshot for element <AXUIElementRef 0x6000025eb660>"
            // We recover by selecting the first child of the app element,
            // which should be the window, and continue from there.

            let recoveryElement = findRecoveryElement(element)
            let hierarchy = try elementHierarchyWithFallback(element: recoveryElement)

            // When the application element is skipped, try to fetch
            // the keyboard and alert hierarchies separately.
            if let element = element as? XCUIApplication {
                let keyboard = try? logger.measure(message: "Fetch keyboard hierarchy") {
                    try keyboardHierarchy(element)
                }

                let alerts = try? logger.measure(message: "Fetch alert hierarchy", {
                    try fullScreenAlertHierarchy(element)
                })

                return AXElement(children: [
                    hierarchy,
                    keyboard,
                    alerts
                ].compactMap { $0 })
            }

            return hierarchy
        }
    }

    private func isIllegalArgumentError(_ error: Error) -> Bool {
        error.localizedDescription.contains("Error kAXErrorIllegalArgument getting snapshot for element")
    }

    private func keyboardHierarchy(_ element: XCUIApplication) throws -> AXElement? {
        if !element.keyboards.firstMatch.exists {
            return nil
        }
        
        let keyboard = try objcTry {
             element.keyboards.firstMatch
        }

        return try elementHierarchy(xcuiElement: keyboard)
    }

    func fullScreenAlertHierarchy(_ element: XCUIApplication) throws -> AXElement? {
        if !element.alerts.firstMatch.exists {
            return nil
        }
        
        let alert = try objcTry {
            element.alerts.firstMatch
        }

        return try elementHierarchy(xcuiElement: alert)
    }

    let useFirstParentWithMultipleChildren = false
    private func findRecoveryElement(_ element: XCUIElement) -> XCUIElement {
        if !useFirstParentWithMultipleChildren {
            do {
                return try objcTry{
                    element
                        .children(matching: .any)
                        .firstMatch
                }
            } catch {
                return element.windows.firstMatch
            }
        } else {
            if element.children(matching: .any).count > 1 {
                return element
            } else {
                return findRecoveryElement(element.children(matching: .any).firstMatch)
            }
        }
    }

    private func elementHierarchy(xcuiElement: XCUIElement) throws -> AXElement {
        let snapshotDictionary = try xcuiElement.snapshot().dictionaryRepresentation
        return AXElement(snapshotDictionary)
    }
}
