# Add project specific ProGuard rules here.
-keep class com.sofastream.app.api.** { *; }
-keep class com.sofastream.app.data.model.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**
