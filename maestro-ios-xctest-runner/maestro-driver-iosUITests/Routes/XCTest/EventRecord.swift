import Foundation
import UIKit

@objc
final class EventRecord: NSObject {
    let eventRecord: NSObject
    static let defaultTapDuration = 0.1

    enum Style: String {
        case singeFinger = "Single-Finger Touch Action"
        case multiFinger = "Multi-Finger Touch Action"
    }

    init(orientation: UIInterfaceOrientation, style: Style = .singeFinger) {
        eventRecord = objc_lookUpClass("XCSynthesizedEventRecord")?.alloc()
            .perform(
                NSSelectorFromString("initWithName:interfaceOrientation:"),
                with: style.rawValue,
                with: orientation
            )
            .takeUnretainedValue() as! NSObject
    }

    func addPointerTouchEvent(at point: CGPoint, touchUpAfter: TimeInterval?) -> Self {
        var path = PointerEventPath.pathForTouch(at: point)
        path.offset += touchUpAfter ?? Self.defaultTapDuration
        path.liftUp()
        return add(path)
    }

    // Two paths, not one re-pressed path: re-pressing after a lift reads as a single
    // continuous touch and never raises tapCount to 2, so no double-tap recognizer fires.
    func addDoubleTapEvent(at point: CGPoint, interval: TimeInterval, holdDuration: TimeInterval? = nil) -> Self {
        let hold = holdDuration ?? Self.defaultTapDuration

        var first = PointerEventPath.pathForTouch(at: point)
        first.offset += hold
        first.liftUp()

        var second = PointerEventPath.pathForTouch(at: point, offset: hold + interval)
        second.offset += hold
        second.liftUp()

        return add(first).add(second)
    }

    func addSwipeEvent(start: CGPoint, end: CGPoint, duration: TimeInterval) -> Self {
        var path = PointerEventPath.pathForTouch(at: start)
        path.offset += Self.defaultTapDuration
        path.moveTo(point: end)
        path.offset += duration
        path.liftUp()
        return add(path)
    }

    func add(_ path: PointerEventPath) -> Self {
        let selector = NSSelectorFromString("addPointerEventPath:")
        let imp = eventRecord.method(for: selector)
        typealias Method = @convention(c) (NSObject, Selector, NSObject) -> ()
        let method = unsafeBitCast(imp, to: Method.self)
        method(eventRecord, selector, path.path)
        return self
    }
}
