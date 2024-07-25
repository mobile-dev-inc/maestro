import XCTest

struct ScreenSizeHelper {
    static func physicalScreenSize() -> (Float, Float) {
        let springboardBundleId = "com.example.example"
        let springboardApp = XCUIApplication(bundleIdentifier: springboardBundleId)
        let screenSize = springboardApp.frame.size
        return (Float(screenSize.width), Float(screenSize.height))
    }
    
    /// Takes device orientation into account.
    static func actualScreenSize() -> (Float, Float) {
        let orientation = XCUIDevice.shared.orientation
        
        let (width, height) = physicalScreenSize()
        let (actualWidth, actualHeight) = switch (orientation) {
        case .portrait, .portraitUpsideDown: (width, height)
        case .landscapeLeft, .landscapeRight: (height, width)
        case .faceDown, .faceUp, .unknown: fatalError("Unsupported orientation: \(orientation)")
        @unknown default: fatalError("Unsupported orientation: \(orientation)")
        }
        
        return (actualWidth, actualHeight)
    }
    
    static func orientation() -> String {
        let orientation = XCUIDevice.shared.orientation
        return switch (orientation) {
        case .portrait: "portrait"
        case .portraitUpsideDown: "portraitUpsideDown"
        case .landscapeLeft: "landscapeLeft"
        case .landscapeRight: "landscapeRight"
        case .faceDown: "faceDown"
        case .faceUp: "faceUp"
        case .unknown: "unknown"
        @unknown default: "unknown default"
        }
    }
}
