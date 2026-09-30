// Android アプリ (:app) と、Android に依存しないコア (core/ … 独立した Gradle ビルド) の複合ビルド。
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

rootProject.name = "Hanaso"

// io.github.twatanabe1436.hanaso:core をこのディレクトリのソースで置き換える
includeBuild("core")
include(":app")
