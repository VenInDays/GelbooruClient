# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keep class com.gelbooru.client.data.model.** { *; }
-keep class com.gelbooru.client.scraping.** { *; }
-dontwarn org.jsoup.**
-keep class org.jsoup.** { *; }
