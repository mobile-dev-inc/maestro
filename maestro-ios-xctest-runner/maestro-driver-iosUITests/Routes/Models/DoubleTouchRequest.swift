import Foundation

struct DoubleTouchRequest : Codable {
    let x: Float
    let y: Float
    let interval: TimeInterval?
    let duration: TimeInterval?
}
