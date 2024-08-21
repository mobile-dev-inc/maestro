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

    implementation(libs.ktor.client.core)
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
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

//plugins.withId("com.vanniktech.maven.publish") {
//    mavenPublish {
//        sonatypeHost = "S01"
//    }
//}

//test {
//    useJUnitPlatform()
//}
