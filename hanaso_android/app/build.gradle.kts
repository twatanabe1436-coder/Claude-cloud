plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// 公開用の署名鍵 (signing/README.md)。パスワードは GitHub Actions の secret から環境変数で渡す。
val releaseKeystorePassword: String? = System.getenv("HANASO_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }

android {
    namespace = "io.github.twatanabe1436.hanaso"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.twatanabe1436.hanaso"
        minSdk = 26
        targetSdk = 36
        versionCode = 7
        versionName = "0.3.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystorePassword != null) {
            create("release") {
                storeFile = rootProject.file("signing/hanaso-release.p12")
                storeType = "pkcs12"
                storePassword = releaseKeystorePassword
                keyAlias = "hanaso"
                keyPassword = releaseKeystorePassword
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            // Claude SDK は Jackson のリフレクションで JSON を読み書きするので、難読化・縮小はしない
            isMinifyEnabled = false
            // 配布用は固定の鍵で署名する (同じ鍵でないと上書き更新できない)。
            // パスワードがない環境 (手元・テスト) では debug 鍵で署名する。
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
    }

    lint {
        // CI のログに出せるようテキスト版のレポートも作る (build/reports/lint-results-debug.txt)
        textReport = true
        // Claude SDK の依存 (Jackson など) が Android にない java.beans 等を参照しているが、実行時には使われない
        disable += "InvalidPackage"
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*",
                "/META-INF/*.version",
                "/META-INF/versions/9/previous-compilation-data.bin",
                "**/module-info.class",
            )
        }
    }
}

dependencies {
    implementation("io.github.twatanabe1436.hanaso:core")

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
    implementation(libs.androidx.compose.material.icons.core)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.okhttp.mockwebserver)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
