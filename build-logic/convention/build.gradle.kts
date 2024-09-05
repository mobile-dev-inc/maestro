import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "dev.mobile.maestro"

// Configure the build-logic plugins to target JDK 17
// This matches the JDK used to build the project, and is not related to what is running on device.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// kotlin {
//     compilerOptions {
//         jvmTarget = JvmTarget.JVM_17
//     }
// }

gradlePlugin {
    plugins {
        register("jvmLibrary") {
            id = "dev.mobile.maestro.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
    }
}
