# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *** INSTANCE; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** serializer(...);
}
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable <methods>;
}

# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Socket.IO
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-keep class org.json.** { *; }

# Hilt
-keep class dagger.hilt.** { *; }
-keepclassmembers class * {
    @dagger.hilt.* <methods>;
    @javax.inject.* <methods>;
}

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# App models (keep for serialization)
-keep class com.bbsrevival.data.models.** { *; }
-keep class com.bbsrevival.data.api.**Data { *; }
-keep class com.bbsrevival.data.api.**Result { *; }
