struct InputTextRequest: Codable {
    let text: String
    let typingFrequency: Int?
    let appIds: [String]
}
