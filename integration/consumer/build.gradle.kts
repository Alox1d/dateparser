import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform") version "2.3.20"
    id("com.android.kotlin.multiplatform.library") version "8.13.2"
}
repositories {
    if (!providers.gradleProperty("centralOnly").map(String::toBoolean).getOrElse(false)) {
        maven { url = uri(providers.gradleProperty("parserRepository").getOrElse("../../build/test-repository")) }
    }
    mavenCentral()
    google()
}
kotlin {
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) } }
    android {
        namespace = "io.github.alox1d.dateparser.consumer"
        compileSdk = 35
        minSdk = 26
        withHostTest { }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
    }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies { implementation("io.github.alox1d:dateparser-kotlin:${providers.gradleProperty("parserVersion").getOrElse("0.1.1")}") }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("androidHostTest").dependencies { implementation(kotlin("test-junit")) }
    }
}
