plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.songjunyi.cardgame"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.songjunyi.cardgame.lab5"
        minSdk = 23
        targetSdk = 35
        versionCode = 5
        versionName = "5.0"
        testInstrumentationRunner = "com.songjunyi.cardgame.CardGameUiRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    testImplementation("junit:junit:4.13.2")
}
