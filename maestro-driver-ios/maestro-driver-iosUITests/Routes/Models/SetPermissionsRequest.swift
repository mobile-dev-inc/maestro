import Foundation

enum PermissionValue: String, Codable {
    case allow
    case deny
    case unset
}

struct Permission: Codable {
    let type: String
    let value: PermissionValue
}

struct SetPermissionsRequest: Codable {
    let permissions: [Permission]
}
