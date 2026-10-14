plugins { kotlin("multiplatform") version "2.3.20" }
repositories {
    maven { url = uri(providers.gradleProperty("parserRepository").getOrElse("../../build/test-repository")) }
    mavenCentral()
}
kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies { implementation("io.github.alox1d:dateparser-kotlin:${providers.gradleProperty("parserVersion").getOrElse("0.1.0")}") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
