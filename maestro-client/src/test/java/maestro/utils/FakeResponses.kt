package maestro.utils

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer

fun getBadResponse(): MockResponse {
    val mockResponse = MockResponse().apply {
        setResponseCode(401)
        setBody("This is bad request")
    }
    return mockResponse
}

fun enqueueBadResponses(mockWebServer: MockWebServer, enqueueTime: Int) {
    when (enqueueTime) {
        1 -> {
            // timeout after first
            val mockResponse = getBadResponse()
            mockWebServer.enqueue(mockResponse)
        }
        2 -> {
            // timeout after second
            val firstMockResponse = getBadResponse()
            val secondMockResponse = getBadResponse()
            mockWebServer.enqueue(firstMockResponse)
            mockWebServer.enqueue(secondMockResponse)
        }
        3 -> {
            // 401 in all three cases, but timeout in last attempt
            val firstMockResponse = getBadResponse()
            val secondMockResponse = getBadResponse()
            val thirdMockResponse = getBadResponse()
            mockWebServer.enqueue(firstMockResponse)
            mockWebServer.enqueue(secondMockResponse)
            mockWebServer.enqueue(thirdMockResponse)
        }
        else -> {
            // not enqueue to straightaway timeout
        }
    }
}