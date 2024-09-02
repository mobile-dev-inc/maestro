import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("maven-publish")
    java
    alias(libs.plugins.mavenPublish)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.S01)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

tasks.named<Jar>("jar") {
    from("src/main/proto/maestro_android.proto")
}
