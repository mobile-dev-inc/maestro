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

    func getViewHierarchy(appId: String) throws -> Element {
        handleSystemPermisionAlert()

        let children = [
            try? springboardHierarchy(),
            try appHierarchy(XCUIApplication(bundleIdentifier: appId))
        ].compactMap { $0 }

        let root = Element(children: children)

        return root
    }

    func appHierarchy(_ xcuiApplication: XCUIApplication) throws -> Element {
        AXClientSwizzler.overwriteDefaultParameters["maxDepth"] = maxDepth
        return try elementHierarchyWithFallback(element: xcuiApplication)
    }

    func elementHierarchyWithFallback(element: XCUIElement) throws -> Element {
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

    func depth(_ hierarchy: Element) -> Int {
        guard let children = hierarchy.children
        else { return 1 }

        let max = children
            .map { child in depth(child) + 1 }
            .max()

        return max ?? 1
    }


    func springboardHierarchy() throws -> Element {
        return try elementHierarchy(xcuiElement: springboardApplication)
    }

    func elementHierarchy(xcuiElement: XCUIElement) throws -> Element {
        return try measure(message: "Take element snapshot") {
            let snapshotDictionary = try xcuiElement.snapshot().dictionaryRepresentation
            return Element(snapshotDictionary)
        }
    }
}


// TODO: Move element model to its own file

typealias AXFrame = [String: Double]
extension AXFrame {
    static var zero: Self {
        ["X": 0, "Y": 0, "Width": 0, "Height": 0]
    }
}

struct Element: Codable {
    let identifier: String
    let frame: AXFrame
    let value: String?
    let title: String?
    let label: String
    let elementType: Int
    let enabled: Bool
    let horizontalSizeClass: Int
    let verticalSizeClass: Int
    let placeholderValue: String?
    let selected: Bool
    let hasFocus: Bool
    var children: [Element]?
    let windowContextID: Double
    let displayID: Int

    init(children: [Element]) {
        self.children = children

        self.label = ""
        self.elementType = 0
        self.identifier = ""
        self.horizontalSizeClass = 0
        self.windowContextID = 0
        self.verticalSizeClass = 0
        self.selected = false
        self.displayID = 0
        self.hasFocus = false
        self.placeholderValue = nil
        self.value = nil
        self.frame = .zero
        self.enabled = false
        self.title = nil
    }

    init(_ dict: [XCUIElement.AttributeName: Any]) {
        func valueFor(_ name: String) -> Any {
            dict[XCUIElement.AttributeName(rawValue: name)] as Any
        }

        self.label = valueFor("label") as? String ?? ""
        self.elementType = valueFor("elementType") as? Int ?? 0
        self.identifier = valueFor("identifier") as? String ?? ""
        self.horizontalSizeClass = valueFor("horizontalSizeClass") as? Int ?? 0
        self.windowContextID = valueFor("windowContextID") as? Double ?? 0
        self.verticalSizeClass = valueFor("verticalSizeClass") as? Int ?? 0
        self.selected = valueFor("selected") as? Bool ?? false
        self.displayID = valueFor("displayID") as? Int ?? 0
        self.hasFocus = valueFor("hasFocus") as? Bool ?? false
        self.placeholderValue = valueFor("placeholderValue") as? String
        self.value = valueFor("value") as? String
        self.frame = valueFor("frame") as? AXFrame ?? .zero
        self.enabled = valueFor("enabled") as? Bool ?? false
        self.title = valueFor("title") as? String
        let childrenDictionaries = valueFor("children") as? [[XCUIElement.AttributeName: Any]]
        self.children = childrenDictionaries?.map { Element($0) } ?? []
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(self.identifier, forKey: .identifier)
        try container.encode(self.frame, forKey: .frame)
        try container.encodeIfPresent(self.value, forKey: .value)
        try container.encodeIfPresent(self.title, forKey: .title)
        try container.encode(self.label, forKey: .label)
        try container.encode(self.elementType, forKey: .elementType)
        try container.encode(self.enabled, forKey: .enabled)
        try container.encode(self.horizontalSizeClass, forKey: .horizontalSizeClass)
        try container.encode(self.verticalSizeClass, forKey: .verticalSizeClass)
        try container.encodeIfPresent(self.placeholderValue, forKey: .placeholderValue)
        try container.encode(self.selected, forKey: .selected)
        try container.encode(self.hasFocus, forKey: .hasFocus)
        try container.encodeIfPresent(self.children, forKey: .children)
        try container.encode(self.windowContextID, forKey: .windowContextID)
        try container.encode(self.displayID, forKey: .displayID)
    }
}
