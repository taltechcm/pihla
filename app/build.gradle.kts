import java.io.FileInputStream
import java.util.Date
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.github.ben-manes.versions")
}

// Creates a variable called keystorePropertiesFile, and initializes it to the
// keystore.properties file.
val secretPropertiesFile = rootProject.file("secret.properties")
// Initializes a new Properties() object called keystoreProperties.
val secretProperties = Properties()
// Loads the keystore.properties file into the keystoreProperties object.
secretProperties.load(FileInputStream(secretPropertiesFile))


android {
    signingConfigs {
        getByName("debug")
        {
            keyAlias = secretProperties["keyAlias"] as String
            keyPassword = secretProperties["keyPassword"] as String
            storeFile = file(secretProperties["storeFile"] as String)
            storePassword = secretProperties["storePassword"] as String
        }
        /*
        getByName("debug") {
            storeFile = file("../keystore.jks")
            storePassword = "taltech"
            keyAlias = "key"
            keyPassword = "taltech"
        }
        */
    }
    namespace = "ee.taltech.aireapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "ee.taltech.aireapplication"
        minSdk = 30
        targetSdk = 34
        // max is 2100000000
        versionCode = getVersionCode()
        versionName = "1.0-$versionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "TEMI_BACKEND_API_KEY", secretProperties["TEMI_BACKEND_API_KEY"] as String)
        }
        debug {
            buildConfigField("String", "TEMI_BACKEND_API_KEY", secretProperties["TEMI_BACKEND_API_KEY"] as String)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

fun getVersionCode(offset: Int = 0): Int {
    return (Date().time / 1000).toInt()
}

dependencies {

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("androidx.activity:activity-ktx:1.10.0")
    implementation("androidx.activity:activity:1.10.0")
    // implementation("androidx.activity:activity:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")



    implementation("com.robotemi:sdk:1.135.1")



    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.zeugmasolutions.localehelper:locale-helper-android:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.android.volley:volley:1.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")

    val ktorVersion = "3.0.3"

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-resources:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("org.slf4j:slf4j-simple:2.0.16")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")


    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")

    implementation("com.airbnb.android:lottie:6.6.0")
    implementation("com.github.gkonovalov.android-vad:silero:2.0.7")

}