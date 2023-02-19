buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.2.1")
    }
}

plugins {
    id("com.vanniktech.maven.publish") version "0.19.0"
    id("io.gitlab.arturbosch.detekt") version "1.19.0"
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
}

repositories {
    google()
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    config = files("${rootDir}/detekt.yml")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}