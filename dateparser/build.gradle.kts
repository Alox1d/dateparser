import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

group = "io.github.alox1d"
version = providers.gradleProperty("releaseVersion").getOrElse("0.1.0")

kotlin {
    explicitApi()
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


mavenPublishing {
    coordinates(group.toString(), "dateparser-kotlin", version.toString())
    publishToMavenCentral()
    if (!providers.gradleProperty("localPublication").map(String::toBoolean).getOrElse(false)) {
        signAllPublications()
    }
    pom {
        name.set("DateParser Kotlin")
        description.set("Offline Russian and English natural-language date/time parser for Kotlin Multiplatform")
        inceptionYear.set("2020")
        url.set("https://github.com/Alox1d/dateparser-kotlin")
        licenses { license { name.set("MIT License"); url.set("https://opensource.org/license/mit/"); distribution.set("repo") } }
        developers { developer { id.set("Alox1d"); name.set("Alexander Baranov"); url.set("https://github.com/Alox1d") } }
        scm {
            url.set("https://github.com/Alox1d/dateparser-kotlin")
            connection.set("scm:git:https://github.com/Alox1d/dateparser-kotlin.git")
            developerConnection.set("scm:git:ssh://git@github.com/Alox1d/dateparser-kotlin.git")
        }
    }
}

publishing {
    repositories { maven { name = "Test"; url = rootProject.layout.buildDirectory.dir("test-repository").get().asFile.toURI() } }
}

tasks.withType<Jar>().matching { it.name.contains("javadoc", ignoreCase = true) }.configureEach {
    from(rootProject.file("README.md"))
    from(rootProject.file("README.ru.md"))
    from(rootProject.file("THIRD_PARTY_NOTICES.md"))
}
