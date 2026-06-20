plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.kotlin.ksp)
  alias(libs.plugins.hilt)
}

// Read properties from peer-stream/.env
val envFile = file("${project.rootDir}/../peer-stream/.env")
val envMap = mutableMapOf<String, String>()
if (envFile.exists()) {
    envFile.forEachLine { line ->
        if (line.contains("=") && !line.startsWith("#")) {
            val parts = line.split("=", limit = 2)
            val key = parts[0].trim()
            var value = parts[1].trim()
            // Strip quotes
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length - 1)
            } else if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length - 1)
            }
            envMap[key] = value
        }
    }
}

val envSupabaseUrl = envMap["VITE_SUPABASE_URL"] ?: ""
val envSupabaseKey = envMap["VITE_SUPABASE_PUBLISHABLE_KEY"] ?: ""
val envOpenRouterKey = envMap["VITE_Open_Router"] ?: ""

android {
    namespace = "com.example.pulse"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.pulse"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        
        buildConfigField("String", "SUPABASE_URL", "\"$envSupabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$envSupabaseKey\"")
        buildConfigField("String", "OPEN_ROUTER_API_KEY", "\"$envOpenRouterKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

ksp {
    arg("room.schemaLocation", "C:/Users/hp/pulse_schemas")
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Dependency Injection (Hilt)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.hilt.navigation.compose)

  // Room Local Cache Database
  implementation(libs.room.runtime)
  implementation(libs.room.ktx)
  ksp(libs.room.compiler)

  // Supabase Multiplatform Kotlin SDK
  implementation(platform(libs.supabase.bom))
  implementation(libs.supabase.gotrue)
  implementation(libs.supabase.postgrest)
  implementation(libs.supabase.realtime)
  implementation(libs.supabase.storage)

  // Ktor Client for OpenRouter Event Streaming
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)

  // Image & Video Multimedia
  implementation(libs.coil.compose)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.ui)
}
