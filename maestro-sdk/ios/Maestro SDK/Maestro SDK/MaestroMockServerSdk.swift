import Foundation

public class MaestroMockServerSdk {
    
    public func url(baseUrl: String) -> String {
        guard let projectId = MaestroSdk.projectId else {
            fatalError("projectId is not initialized. Did you call MaestroSdk.setup()?")
        }
        
        let sessionIdFromEnvVar = ProcessInfo.processInfo.environment["MAESTRO_SESSION_ID"]

        var sessionId = NSUUID().uuidString
        if (sessionIdFromEnvVar != nil) {
            if let uuid = UUID(uuidString: sessionIdFromEnvVar!) {
                sessionId = uuid.uuidString
            }
        }
        
        var payloadBuilder = ""
        payloadBuilder += projectId + "\n"
        payloadBuilder += sessionId + "\n"
        payloadBuilder += baseUrl + "\n"
        
        let sessionPayload = Data(payloadBuilder.utf8).base64EncodedString()
        
        let url = "https://mock.mobile.dev/\(sessionPayload)"
        
        if (url.hasSuffix("/")) {
            return url
        } else {
            return url + "/"
        }
    }
    
}
