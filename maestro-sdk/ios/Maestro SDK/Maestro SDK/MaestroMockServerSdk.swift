import Foundation

public class MaestroMockServerSdk {
    
    public func url(baseUrl: String) -> String {
        guard let projectId = MaestroSdk.projectId else {
            fatalError("projectId is not initialized. Did you call MaestroSdk.setup()?")
        }
        
        let sessionId = getSessionId()
        
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
    
    func getSessionId() -> String {
        var sessionId = ""
        let semaphore = DispatchSemaphore(value: 0)
        
        guard let url = URL(string: "http://localhost:22087/sessionInfo") else {
            fatalError("failed to build url to fetch sessionInfo data")
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        
        let session = URLSession.shared
        let task = session.dataTask(with: request) { data, response, error in
            if let error = error {
                fatalError("error to fetch driver http server \(error)")
            } else if let httpResponse = response as? HTTPURLResponse,
                      (200...299).contains(httpResponse.statusCode),
                      let data = data {
                do {
                    let json = try JSONSerialization.jsonObject(with: data, options: []) as? [String: Any]
                    print("json \(String(describing: json))")
                    if let session = json?["sessionId"] as? String {
                        sessionId = session
                    }
                } catch {
                    print(error)
                }
            } else {
                fatalError("invalid response \(String(describing: error))")
            }
            semaphore.signal()
        }
        
        task.resume()
        semaphore.wait()
        
        return sessionId
    }
}
