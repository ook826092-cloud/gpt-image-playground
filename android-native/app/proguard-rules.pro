# Keep kotlinx serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Room generated code
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }

# Keep Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# Keep app data models (used by serialization)
-keep class com.gptimage.playground.data.model.** { *; }
-keep class com.gptimage.playground.data.remote.dto.** { *; }
