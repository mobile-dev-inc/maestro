//
//  AccessibilityInterfaceProxy.swift
//  maestro-driver-iosUITests
//
//  Created by Amanjeet Singh on 23/05/23.
//

import Foundation
import XCTest

class AccessibilityInterfaceProxy {
    
    private let maxDepth: Int
    private let accessibilityInterface: NSObject
    
    init(maxDepth: Int) {
        let sharedDevice = XCUIDevice.shared
        accessibilityInterface = sharedDevice.perform(NSSelectorFromString("accessibilityInterface"))
            .takeUnretainedValue() as! NSObject
        self.maxDepth = maxDepth
    }
    
    func configure() {
        let original = class_getInstanceMethod(objc_getClass("XCAXClient_iOS") as? AnyClass, Selector(("defaultParameters")))
        let replaced = class_getInstanceMethod(type(of: self),
                                               #selector(AccessibilityInterfaceProxy.replace_defaultParameters))
        guard let original = original, let replaced = replaced else { return }
        method_exchangeImplementations(original, replaced)
    }
    
    @objc func replace_defaultParameters() -> NSDictionary {
        return [
            "maxArrayCount": 2147483647,
            "maxChildren": 2147483647,
            "maxDepth": 50 /* This was int max by default */ ,
            "traverseFromParentsToChildren": 1,
            "snapshotKeyHonorModalViews": 0
        ]
    }
}
