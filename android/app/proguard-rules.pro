# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep our serializable models
-keep @kotlinx.serialization.Serializable class com.gptimage.playground.** { *; }
-keepclassmembers class com.gptimage.playground.** {
    *** Companion;
}
-keepclasseswithmembers class com.gptimage.playground.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep model classes used by reflection in JSON parsing
-keep class com.gptimage.playground.data.model.** { *; }
