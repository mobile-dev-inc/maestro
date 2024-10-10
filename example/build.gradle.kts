import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
}

application {
    applicationName = "maestro-example"
    mainClass.set("MainKt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(KotlinCompile::class.java).configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        freeCompilerArgs.addAll("-Xjdk-release=1.8")
    }
}

dependencies {
    implementation("dev.mobile:maestro-client:1.38.1")
    implementation("dev.mobile:maestro-orchestra:1.38.1")
    implementation("dev.mobile:maestro-ios:1.38.1")
}
