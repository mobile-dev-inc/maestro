@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.detekt)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    config = files("${rootDir}/detekt.yml")
}
