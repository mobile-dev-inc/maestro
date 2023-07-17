//
//  RunningApp.swift
//  maestro-driver-iosUITests
//
//  Created by Amanjeet Singh on 17/07/23.
//

import Foundation
import XCTest

struct RunningApp {
    
    private static let springboardBundleId = "com.apple.springboard"
    private init() {}
    
    static func getForegroundAppId(_ appIds: [String]) -> String {
        return appIds.first { appId in
            let app = XCUIApplication(bundleIdentifier: appId)
            
            return app.state == .runningForeground
        } ?? RunningApp.springboardBundleId
    }
    
    
}
