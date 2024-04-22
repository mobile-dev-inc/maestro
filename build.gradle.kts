@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.google.protobuf) apply false
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.detekt)
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(11))

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    config = files("${rootDir}/detekt.yml")
}