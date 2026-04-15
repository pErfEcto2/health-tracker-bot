# Keep kotlinx.serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keep,includedescriptorclasses class com.trackhub.**$$serializer { *; }
-keepclassmembers class com.trackhub.** {
    *** Companion;
}
-keepclasseswithmembers class com.trackhub.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit / OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-keep class kotlin.coroutines.Continuation

# Compose runtime keeps (Compose plugin already handles most of it)
