import Foundation
import XCTest
import os

struct RunningApp {
    
    #if os(tvOS)
    private static let homescreenBundleId = "com.apple.HeadBoard"
    #else
    private static let homescreenBundleId = "com.apple.springboard"
    #endif
    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier!,
        category: String(describing: Self.self)
    )
    private init() {}
    
    static func getForegroundAppId(_ appIds: [String]) -> String {
        if appIds.isEmpty {
            logger.info("Empty installed apps found")
            return ""
        }
        
        return appIds.first { appId in
            let app = XCUIApplication(bundleIdentifier: appId)
            
            return app.state == .runningForeground
        } ?? RunningApp.homescreenBundleId
    }
    
    
}
