import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.egepaksoy.kiss"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.egepaksoy.kiss"
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = "0.5.0"
    }

    // Sabit imza: güncellemeler cihaz sahibi (Device Owner) uygulamasının üzerine kurulabilsin.
    // Anahtar repoda tutulmaz. Yol ve parolalar ortam değişkenlerinden gelir
    // (KISS_KEYSTORE, KISS_STORE_PASSWORD, KISS_KEY_PASSWORD); yoksa yerel keystore/kiss.jks denenir.
    val keystoreFile = (System.getenv("KISS_KEYSTORE") ?: "keystore/kiss.jks").let { rootProject.file(it) }
    val storePass = System.getenv("KISS_STORE_PASSWORD")
    val hasKey = keystoreFile.exists() && storePass != null

    signingConfigs {
        if (hasKey) create("kiss") {
            storeFile = keystoreFile
            storePassword = storePass
            keyAlias = System.getenv("KISS_KEY_ALIAS") ?: "kiss"
            keyPassword = System.getenv("KISS_KEY_PASSWORD") ?: storePass
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Anahtar yoksa debug anahtarıyla imzalanır (sadece deneme için; güncellemeler uyumsuz olur).
            signingConfig = if (hasKey) signingConfigs.getByName("kiss") else signingConfigs.getByName("debug")
        }
        debug {
            if (hasKey) signingConfig = signingConfigs.getByName("kiss")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            it.systemProperty("robolectric.dependency.repo.url", "https://maven-central.storage-download.googleapis.com/maven2")
            it.systemProperty("kiss.shots", System.getenv("KISS_SHOTS") ?: layout.buildDirectory.dir("shots").get().asFile.path)
        }
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
