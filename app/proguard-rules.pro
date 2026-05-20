# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ===== kotlinx.serialization =====
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.suvojeetsengupta.suvform.**$$serializer { *; }
-keepclassmembers class com.suvojeetsengupta.suvform.** {
    *** Companion;
}
-keepclasseswithmembers class com.suvojeetsengupta.suvform.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ===== Retrofit / OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepattributes Signature, Exceptions

# ===== Hilt / Dagger =====
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class *
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }

# ===== Compose =====
# Compose ships with proguard rules in AAR — nothing extra needed for Material3.

# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**

# ===== Coroutines =====
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===== Application classes referenced via reflection =====
-keep class com.suvojeetsengupta.suvform.SuvFormApp
