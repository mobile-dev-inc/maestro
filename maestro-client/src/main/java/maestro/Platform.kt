package maestro

import com.fasterxml.jackson.annotation.JsonValue

enum class Platform(@JsonValue val platformName: String) {
    ANDROID("Android"),
    IOS("iOS"),
    WEB("Web")
}