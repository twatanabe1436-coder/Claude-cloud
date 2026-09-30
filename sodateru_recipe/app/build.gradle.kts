plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// 署名鍵。CI では Secrets から復元した keystore を環境変数で渡す (README 参照)。
// 無い場合はデバッグ鍵で署名する (インストールはできるが、鍵が変わると上書き更新できない)。
val releaseKeystore = System.getenv("SODATERU_KEYSTORE_FILE")?.let(::file)?.takeIf { it.exists() }

android {
    namespace = "io.github.twatanabe1436.sodateru"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.twatanabe1436.sodateru"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("SODATERU_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SODATERU_KEY_ALIAS")
                keyPassword = System.getenv("SODATERU_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (releaseKeystore != null) signingConfigs.getByName("release") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // 端末内 OCR (ML Kit) のネイティブライブラリが CPU の種類ごとに入って APK が大きくなるので、
    // 配布用はほぼすべての現行スマホに合う arm64-v8a 版を分けて作る (universal 版はすべての CPU 向け)。
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // CI のログに出せるようテキスト版のレポートも作る (build/reports/lint-results-debug.txt)
        textReport = true
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/*.kotlin_module",
                "/META-INF/versions/9/OSGI-INF/MANIFEST.MF",
            )
        }
    }
}

dependencies {
    // Android に依存しない計算・解析ロジック (../core、composite build)
    implementation("io.github.twatanabe1436.sodateru:core")

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // 端末内の文字認識 (日本語、モデル同梱なのでオフラインで動く)
    implementation(libs.mlkit.text.japanese)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
