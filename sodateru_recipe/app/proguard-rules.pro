# kotlinx.serialization: @Serializable クラスの serializer() を残す
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class io.github.twatanabe1436.sodateru.core.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.twatanabe1436.sodateru.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Anthropic Java SDK: 同梱の META-INF/proguard ルールに加え、Android に無いクラスへの参照を無視する
-dontwarn com.github.victools.jsonschema.**
-dontwarn io.swagger.v3.oas.annotations.**
-dontwarn org.apache.hc.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn java.beans.**
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn com.standardwebhooks.**
-dontwarn javax.naming.**
-dontwarn sun.misc.**

# Jackson はリフレクションで SDK のモデル・(デ)シリアライザを生成するため、SDK と Jackson のクラスは名前・メンバーごと残す。
# (縮小すると「Class xxx has no default (no arg) constructor」で API 呼び出しが失敗した)
-keep class com.anthropic.** { *; }
-keep class com.fasterxml.jackson.** { *; }
-keep class kotlin.reflect.jvm.internal.** { *; }
