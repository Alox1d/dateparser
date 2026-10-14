import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("com.vanniktech.maven.publish")
}

group = "io.github.alox1d"
version = providers.gradleProperty("releaseVersion").getOrElse("0.1.1")

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
    configure(KotlinMultiplatform(javadocJar = JavadocJar.None()))
    coordinates(group.toString(), "dateparser-kotlin", version.toString())
    publishToMavenCentral()
    if (!providers.gradleProperty("localPublication").map(String::toBoolean).getOrElse(false)) {
        signAllPublications()
    }
    pom {
        name.set("DateParser Kotlin")
        description.set("Offline English and Russian natural-language date/time parser for Kotlin Multiplatform")
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

val documentationJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(rootProject.file("README.md"))
    from(rootProject.file("README.ru.md"))
    from(rootProject.file("THIRD_PARTY_NOTICES.md"))
    from(rootProject.file("LICENSE"))
}

publishing.publications.withType<MavenPublication>().configureEach {
    artifact(documentationJar)
}

tasks.withType<Jar>().configureEach {
    from(rootProject.file("LICENSE")) { into("META-INF") }
    from(rootProject.file("THIRD_PARTY_NOTICES.md")) { into("META-INF") }
}
