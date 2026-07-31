# ProGuard / R8 rules for Web to Notion
# Release builds (isMinifyEnabled = true) use these rules.

# Keep Activities referenced from AndroidManifest.xml
-keep class io.trae.webtonotion.MainActivity { *; }
-keep class io.trae.webtonotion.ShareActivity { *; }

# Kotlin metadata
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClass
-keepclassmembers class kotlin.Metadata { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Retrofit / OkHttp / kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# OkHttp logging (ensure HTTP logs survive R8 in release)
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# kotlinx.serialization — keep generated serializers and @Serializable classes
-keepattributes *Annotation*
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class io.trae.webtonotion.**$$serializer { *; }
-keep class io.trae.webtonotion.data.remote.dto.** { *; }

# WorkManager
-keep class androidx.work.ListenableWorker { *; }
-keep class * extends androidx.work.ListenableWorker { *; }

# Suppress warnings
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
