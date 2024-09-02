import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    application
    id("maven-publish")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.mavenPublish)
}

application {
    applicationName = "maestro-ai-demo"
    mainClass.set("maestro.ai.DemoAppKt")
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "maestro.ai.DemoAppKt"
    }
}

dependencies {
    api(libs.kotlin.result)
    api(libs.square.okio)

    api(libs.slf4j)
    api(libs.logback) {
        exclude(group = "org.slf4j", module = "slf4j-api")
    }

    api(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.serial.json)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.clikt)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.google.truth)
    testImplementation(libs.square.mock.server)
    testImplementation(libs.junit.jupiter.params)
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
