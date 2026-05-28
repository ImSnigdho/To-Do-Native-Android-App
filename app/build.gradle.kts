plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  id("com.google.gms.google-services") version "4.4.1"
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.todo.wxyzkp"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation("com.google.firebase:firebase-database")
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("fixColors") {
    doLast {
        val file = file("src/main/java/com/example/ui/TodoScreens.kt")
        var content = file.readText()
        content = content.replace("Color(0xFF1F1A18)", "MaterialTheme.colorScheme.onSurface")
        content = content.replace("Color(0xFF514441)", "MaterialTheme.colorScheme.onSurfaceVariant")
        content = content.replace("Color(0xFF85736E)", "MaterialTheme.colorScheme.onSurfaceVariant")
        content = content.replace("Color(0xFFF5DED8)", "MaterialTheme.colorScheme.surfaceVariant")
        content = content.replace("Color(0xFFFFDBD1)", "MaterialTheme.colorScheme.primaryContainer")
        content = content.replace("Color(0xFFFFDBCC)", "MaterialTheme.colorScheme.secondaryContainer")
        content = content.replace("Color(0xFFFFF1EE)", "MaterialTheme.colorScheme.surfaceVariant")
        content = content.replace("Color(0xFF8F4C38)", "MaterialTheme.colorScheme.primary")
        content = content.replace("Color(0xFFD9411E)", "MaterialTheme.colorScheme.error")
        content = content.replace("Color(0xFF4C150A)", "MaterialTheme.colorScheme.onErrorContainer")
        content = content.replace("Color(0xFFFFB4A5)", "MaterialTheme.colorScheme.errorContainer")
        content = content.replace("Color(0xFF5D4038)", "MaterialTheme.colorScheme.outline") // Just placeholders to fix any left over
        content = content.replace("Color(0xFFECEFF1)", "MaterialTheme.colorScheme.onSurfaceVariant")
        content = content.replace("Color(0xFF00B0FF)", "MaterialTheme.colorScheme.primary")
        content = content.replace("Color(0xFF2196F3)", "MaterialTheme.colorScheme.primary")
        content = content.replace("Color(0xFF00E676)", "MaterialTheme.colorScheme.secondary")
        content = content.replace("Color(0xFFD32F2F)", "MaterialTheme.colorScheme.error")
        content = content.replace("Color(0xFFFF5252)", "MaterialTheme.colorScheme.error")
        content = content.replace("Color(0xFFFDF8F6)", "MaterialTheme.colorScheme.background")
        content = content.replace("Color(0xFF040209)", "MaterialTheme.colorScheme.background")
        content = content.replace("Color(0xFF0F0E17)", "MaterialTheme.colorScheme.background")
        content = content.replace("Color(0xFF7C4DFF)", "MaterialTheme.colorScheme.primary")
        content = content.replace("Color(0xFFFFF9F8)", "MaterialTheme.colorScheme.surface")
        
        content = content.replace("Color.White", "MaterialTheme.colorScheme.surface")
        content = content.replace("Color.Black", "MaterialTheme.colorScheme.onSurface")
        content = content.replace("Color.DarkGray", "MaterialTheme.colorScheme.onSurfaceVariant")
        content = content.replace("Color.Gray", "MaterialTheme.colorScheme.onSurfaceVariant")
        content = content.replace("Color.LightGray", "MaterialTheme.colorScheme.surfaceVariant")
        content = content.replace("Color.Red", "MaterialTheme.colorScheme.error")
        content = content.replace("Color.Green", "MaterialTheme.colorScheme.secondary")
        content = content.replace("Color.Blue", "MaterialTheme.colorScheme.primary")
        content = content.replace("Color.Transparent", "Color.Transparent")
        
        content = content.replace(Regex("Color\\(0x[0-9A-Fa-f]+\\)"), "MaterialTheme.colorScheme.outline")
        
        file.writeText(content)
    }
}

