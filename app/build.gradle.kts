plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.nexora.player'
    compileSdk 34

    defaultConfig {
        applicationId "com.nexora.player"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        release {
            def ksFile = System.getenv("KEYSTORE_FILE") ? file(System.getenv("KEYSTORE_FILE")) : null
            if (ksFile?.exists()) {
                storeFile     ksFile
                storePassword System.getenv("KEYSTORE_PASSWORD")
                keyAlias      System.getenv("KEY_ALIAS")
                keyPassword   System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            minifyEnabled     true
            shrinkResources   true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = '17' }
    buildFeatures {
        viewBinding true
        buildConfig true
    }
}

dependencies {
    def media3_version = "1.2.1"
    def nav_version    = "2.7.7"

    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'

    // Navigation
    implementation "androidx.navigation:navigation-fragment-ktx:$nav_version"
    implementation "androidx.navigation:navigation-ui-ktx:$nav_version"

    // Lifecycle
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-livedata-ktx:2.7.0'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'

    // Media3 / ExoPlayer
    implementation "androidx.media3:media3-exoplayer:$media3_version"
    implementation "androidx.media3:media3-ui:$media3_version"
    implementation "androidx.media3:media3-session:$media3_version"
    implementation "androidx.media3:media3-common:$media3_version"

    // Images
    implementation 'com.github.bumptech.glide:glide:4.16.0'

    // UI extras
    implementation 'androidx.recyclerview:recyclerview:1.3.2'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.1.0'
    implementation 'de.hdodenhof:circleimageview:3.1.0'
    implementation 'androidx.preference:preference-ktx:1.2.1'

    // Coroutines
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
