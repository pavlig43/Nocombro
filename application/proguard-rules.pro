# Keep Compose generated classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Keep Koin
-keep class org.koin.** { *; }
-keepinterface org.koin.** { *; }

# Keep Decompose
-keep class com.arkivanov.decompose.** { *; }
-keepinterface com.arkivanov.decompose.** { *; }

# Keep serialization
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepclassmembers class ru.pavlig43.** {
    <init>(...);
}

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
