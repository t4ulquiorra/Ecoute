plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.ecoute.innertube"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.encoding)
    implementation(libs.ktor.serialization.json)
    implementation(libs.brotli)
    implementation(libs.newpipeextractor)
    implementation(libs.timber)
    testImplementation(testLibs.junit)
    coreLibraryDesugaring(libs.desugaring)
}

configurations.all {
    resolutionStrategy {
        force("org.mozilla:rhino:1.7.15")
    }
}
