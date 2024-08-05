import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.vanniktech.maven.publish.SonatypeHost

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    alias(libs.plugins.vanniktech.publish)
}

dependencies {
    api(libs.square.okio)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.google.truth)
}

// From https://jakewharton.com/kotlins-jdk-release-compatibility-flag

val javaVersion = JavaVersion.VERSION_1_8
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjdk-release=$javaVersion")
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
