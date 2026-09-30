pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SodateruRecipe"

// Android に依存しない計算・解析ロジック。Android SDK なしで単体ビルド・テストできるよう別ビルドにしている
// (cd core を含むディレクトリで ./gradlew -p core test)。
includeBuild("core")
include(":app")
