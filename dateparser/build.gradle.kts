import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    android {
        namespace = "io.github.alox1d.dateparser"
        compileSdk = 35
        minSdk = 26
        withHostTest { }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) }
    }
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_1_8) } }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies { api("org.jetbrains.kotlinx:kotlinx-datetime:0.8.0") }
        commonTest.dependencies { implementation(kotlin("test")) }
        getByName("androidHostTest").dependencies { implementation(kotlin("test-junit")) }
    }
}
