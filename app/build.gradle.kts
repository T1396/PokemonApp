import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
    id("com.apollographql.apollo3") version "3.8.3"
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.pokinfo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.pokinfo"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val keystoreFile = project.rootProject.file("apikeys.properties")
        val properties = Properties()
        properties.load(keystoreFile.inputStream())
        val localApiUrl = properties.getProperty("API_URL_LOCAL")
        val localGraphQLUrl = properties.getProperty("GRAPH_URL_LOCAL")
        val webClientId = properties.getProperty("WEB_CLIENT_ID")

        buildConfigField("String", "localApiUrl", localApiUrl)
        buildConfigField("String", "localGraphqlUrl", localGraphQLUrl)
        buildConfigField("String", "webClientId", webClientId)
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}





dependencies {

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    val retrofitVersion = "2.11.0"
    val roomVersion = "2.6.1"
    implementation("com.ultramegasoft.radarchart:radar-chart:0.1.5")

    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    //noinspection LifecycleAnnotationProcessorWithJava8
    annotationProcessor ("androidx.lifecycle:lifecycle-compiler:2.7.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation(("androidx.legacy:legacy-support-v4:1.0.0"))

    // Google Auth Id ...
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    // Biometric
    implementation("androidx.biometric:biometric:1.1.0")


    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.android.gms:play-services-auth-api-phone:18.0.2") // Überprüfen Sie die genaue Version
    implementation("com.google.firebase:firebase-firestore:24.11.1")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
    implementation ("com.firebaseui:firebase-ui-auth:8.0.2")
    // Also add the dependency for the Google Play services library and specify its version
    implementation ("com.faltenreich:skeletonlayout:5.0.0")



    implementation("io.coil-kt:coil:2.6.0")
    implementation("com.apollographql.apollo3:apollo-runtime:3.8.3")
    implementation("com.apollographql.apollo3:apollo-http-cache:3.8.3")

    //Retrofit und Moshi
    implementation("com.squareup.retrofit2:retrofit:$retrofitVersion")
    implementation("com.squareup.retrofit2:converter-moshi:$retrofitVersion")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation ("com.squareup.retrofit2:adapter-rxjava2:2.11.0")
    //Interceptor
    implementation ("com.squareup.okhttp3:logging-interceptor:4.12.0")
    //Room
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    implementation("com.google.code.gson:gson:2.10.1")


}

apollo {
    service("service") {
        packageName.set("com.example.pokeinfo.data.graphModel")
    }
}

