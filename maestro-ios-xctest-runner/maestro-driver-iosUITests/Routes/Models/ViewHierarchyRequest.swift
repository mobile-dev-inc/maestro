import Foundation

struct ViewHierarchyRequest: Codable {
    let appIds: [String]
    let filterOutKeyboardElements: Bool
}
