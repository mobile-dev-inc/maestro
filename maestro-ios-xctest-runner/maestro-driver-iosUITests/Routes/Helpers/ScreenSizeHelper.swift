import XCTest

// UIKit doesn't include UIDeviceOrientation on tvOS
public enum DeviceOrientation: Int, @unchecked Sendable {
    case unknown = 0
    case portrait = 1 // Device oriented vertically, home button on the bottom
    case portraitUpsideDown = 2 // Device oriented vertically, home button on the top
    case landscapeLeft = 3 // Device oriented horizontally, home button on the right
    case landscapeRight = 4 // Device oriented horizontally, home button on the left
    case faceUp = 5 // Device oriented flat, face up
    case faceDown = 6 // Device oriented flat, face down
}

struct ScreenSizeHelper {
    static func physicalScreenSize() -> (Float, Float) {
        #if os(tvOS)
        let homescreenBundleId = "com.apple.PineBoard"
        #else
        let homescreenBundleId = "com.apple.springboard"
        #endif
        let springboardApp = XCUIApplication(bundleIdentifier: homescreenBundleId)
        let screenSize = springboardApp.frame.size
        return (Float(screenSize.width), Float(screenSize.height))
    }

    private static func actualOrientation() -> DeviceOrientation {
        #if os(tvOS)
        // Please don't rotate your AppleTV...
        let orientation = Optional(DeviceOrientation.unknown)
        #else
        let orientation = DeviceOrientation(rawValue: XCUIDevice.shared.orientation.rawValue)
        #endif

        guard let unwrappedOrientation = orientation, orientation != .unknown else {
            // If orientation is "unknown", we assume it is "portrait" to
            // work around https://stackoverflow.com/q/78932288/7009800
            return DeviceOrientation.portrait
        }
        return unwrappedOrientation
    }

    /// Takes device orientation into account.
    static func actualScreenSize() throws -> (Float, Float, DeviceOrientation) {
        let orientation = actualOrientation()

        let (width, height) = physicalScreenSize()
        let (actualWidth, actualHeight) = switch (orientation) {
        case .portrait, .portraitUpsideDown: (width, height)
        case .landscapeLeft, .landscapeRight: (height, width)
        case .faceDown, .faceUp, .unknown: throw AppError(message: "Unsupported orientation: \(orientation)")
        @unknown default: throw AppError(message: "Unsupported orientation: \(orientation)")
        }

        return (actualWidth, actualHeight, orientation)
    }

    static func orientationAwarePoint(width: Float, height: Float, point: CGPoint) -> CGPoint {
        let orientation = actualOrientation()

        return switch (orientation) {
        case .portrait: point
        case .landscapeLeft: CGPoint(x: CGFloat(width) - point.y, y: CGFloat(point.x))
        case .landscapeRight: CGPoint(x: CGFloat(point.y), y: CGFloat(height) - point.x)
        default: fatalError("Not implemented yet")
        }
    }
}
