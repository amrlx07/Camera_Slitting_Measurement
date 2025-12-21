plugins {
    id("com.android.library")
    id("kotlin-android") // Tambahkan ini biar support Kotlin
}

android {
    namespace = "org.opencv" // Wajib pakai tanda sama dengan (=)
    compileSdk = 35          // Sesuaikan dengan SDK di laptopmu

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("java/src")
            aidl.srcDirs("java/src")
            res.srcDirs("java/src")
            manifest.srcFile("java/AndroidManifest.xml")

            jniLibs.srcDirs("native/libs")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")

    // Jika nanti butuh cameraX di dalam module ini, bisa tambah disini
}