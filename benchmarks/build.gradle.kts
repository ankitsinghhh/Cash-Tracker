plugins {
    id("com.android.test")
    id("androidx.baselineprofile")
}

android {
    namespace = "com.example.benchmarks"
    compileSdk { version = release(36) { minorApiLevel = 1 } }
    targetProjectPath = ":app"
    defaultConfig {
        minSdk = 29
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation("androidx.benchmark:benchmark-macro-junit4:1.4.1")
    implementation("androidx.test.ext:junit:1.3.0")
    implementation("androidx.test:runner:1.6.2")
    implementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
