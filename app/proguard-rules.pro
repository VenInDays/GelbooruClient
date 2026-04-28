# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# ---- Project data models & scraping classes ----
-keep class com.gelbooru.client.data.model.** { *; }
-keep class com.gelbooru.client.scraping.** { *; }
-keep class com.gelbooru.client.network.** { *; }
-keep class com.gelbooru.client.service.** { *; }

# ---- Jsoup (HTML parsing) ----
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }
-keepclassmembers class org.jsoup.** { *; }

# ---- OkHttp (network) ----
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep class okio.internal.** { *; }

# ---- Gson (JSON serialization) ----
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ---- Kotlin Coroutines ----
-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---- Coil (Image loading) ----
-dontwarn coil.**
-keep class coil.** { *; }

# ---- AndroidX / Compose ----
-keep class androidx.compose.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.datastore.** { *; }
-dontwarn androidx.compose.**
-dontwarn androidx.datastore.**

# ---- Enum (used in data models) ----
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
