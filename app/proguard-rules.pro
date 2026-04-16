# Keep OkHttp SSE streaming (important!)
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Gson data classes
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class com.ftt.aichat.data.** { *; }

# Keep Markwon
-keep class io.noties.markwon.** { *; }

# Keep Security Crypto (EncryptedSharedPreferences)
-keep class androidx.security.crypto.** { *; }

# Keep ViewModel
-keep class * extends androidx.lifecycle.ViewModel { *; }
