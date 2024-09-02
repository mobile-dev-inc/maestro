import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
}

application {
    applicationName = "maestro-example"
    mainClass.set("MainKt")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.withType<KotlinCompilationTask<KotlinJvmCompilerOptions>>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjdk-release=1.8")
    }
}

dependencies {
    implementation("dev.mobile:maestro-client:1.38.1")
    implementation("dev.mobile:maestro-orchestra:1.38.1")
    implementation("dev.mobile:maestro-ios:1.38.1")
}
