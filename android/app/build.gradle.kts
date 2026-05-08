import java.net.URL
import org.gradle.api.GradleException

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.nammasanthe.ledger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nammasanthe.ledger"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isDebuggable = true
        }
    }

    // Build universal APK with all ABIs
    splits {
        abi {
            isEnable = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }
    packaging {
        resources.excludes += setOf(
            "META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
            "META-INF/INDEX.LIST",
            "META-INF/io.netty.versions.properties"
        )
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

val tessdataDir = file("src/main/assets/tessdata")
val tessdataFiles = mapOf(
    "kan.traineddata" to "https://github.com/tesseract-ocr/tessdata_best/raw/main/kan.traineddata",
    "eng.traineddata" to "https://github.com/tesseract-ocr/tessdata_best/raw/main/eng.traineddata"
)

val downloadTessdata by tasks.registering {
    outputs.dir(tessdataDir)
    doLast {
        tessdataDir.mkdirs()
        tessdataFiles.forEach { (name, url) ->
            val out = File(tessdataDir, name)
            if (!out.exists()) {
                logger.lifecycle("Downloading $name for offline OCR...")
                try {
                    URL(url).openStream().use { input ->
                        out.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (t: Throwable) {
                    throw GradleException(
                        "Failed to download $name. Place it manually in ${tessdataDir.absolutePath}",
                        t
                    )
                }
            }
        }
    }
}

tasks.named("preBuild") {
    dependsOn(downloadTessdata)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // CameraX
    val camerax = "1.3.4"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-view:$camerax")
    implementation("com.google.guava:guava:32.1.3-android")

    // ML Kit (offline text recognition for Latin + Devanagari)
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-devanagari:16.0.1")

    // ML Kit on-device translation (downloads once, then offline)
    implementation("com.google.mlkit:translate:17.0.3")

    // Tesseract for Kannada OCR (offline)
    implementation("cz.adaptech.tesseract4android:tesseract4android:4.7.0")

    // ZXing — QR code generation (encode) and scanning (decode)
    implementation("com.google.zxing:core:3.5.3")

    // Coil image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // DataStore for settings
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // HTTP client for Gemini API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.6.3")

    // Firebase (Auth, Firestore, Storage) - Offline-first with cloud sync
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.1")

    // PDF Generation for data export
    implementation("com.itextpdf:itext7-core:8.0.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
