import XCTest
import FlyingFox

class maestro_driver_iosUITests: XCTestCase {
    private static var swizzledOutIdle = false
    private static var swizzledParameters = false

    override func setUpWithError() throws {
        // Disable waiting for quiescence
        if !maestro_driver_iosUITests.swizzledOutIdle { // ensure the swizzle only happens once
            let original = class_getInstanceMethod(objc_getClass("XCUIApplicationProcess") as? AnyClass, Selector(("waitForQuiescenceIncludingAnimationsIdle:")))
            let replaced = class_getInstanceMethod(type(of: self),
                                                   #selector(maestro_driver_iosUITests.replace_waitForQuiescenceIncludingAnimationsIdle))
            
            guard let original = original, let replaced = replaced else { return }
            
            method_exchangeImplementations(original, replaced)
            maestro_driver_iosUITests.swizzledOutIdle = true
        }
     
        // swizzle for decreasing maxDepth. Hardcoded to be 50 inside
        if !maestro_driver_iosUITests.swizzledParameters {
            AccessibilityInterfaceProxy(maxDepth: 50).configure()
            maestro_driver_iosUITests.swizzledParameters = true
        }

        // In UI tests it is usually best to stop immediately when a failure occurs.
        continueAfterFailure = false

        // In UI tests it’s important to set the initial state - such as interface orientation - required for your tests before they run. The setUp method is a good place to do this.
    }
    
    @objc func replace_waitForQuiescenceIncludingAnimationsIdle() {
        return
    }
    
    func testHttpServer() async throws {
        let server = XCTestHTTPServer()
        try await server.start()
    }
    
    @MainActor
    func testSwipeOrHierarchy() async throws {
        let sharedDevice = XCUIDevice.shared
        let accessibilityInterface = sharedDevice.perform(NSSelectorFromString("accessibilityInterface"))
            .takeRetainedValue() as! NSObject
        
        // defaultParameters
        let defaultParametersSelector = NSSelectorFromString("defaultParameters")
        let defaultParametersImp = accessibilityInterface.method(for: defaultParametersSelector)
        typealias defaultParametersMethod = @convention(c) (NSObject, Selector) -> NSMutableDictionary
        
        let defaultParametersMethodCall = unsafeBitCast(defaultParametersImp, to: defaultParametersMethod.self)
        let params = defaultParametersMethodCall(accessibilityInterface, defaultParametersSelector)
        
        print("Using Param \(params)")
        
        let app = XCUIApplication(bundleIdentifier: "com.chatloop.my")
        let anyXCUIElementQuery = app.children(matching: XCUIElement.ElementType.any)
        print(try anyXCUIElementQuery.firstMatch.snapshot().dictionaryRepresentation)
    }
    
    @objc func replace_defaultParameters() -> NSDictionary {
        return [
            "maxArrayCount": 2147483647,
            "maxChildren": 2147483647,
            "maxDepth": 70,
            "traverseFromParentsToChildren": 1,
            "snapshotKeyHonorModalViews": 0
        ]
    }
    
}
