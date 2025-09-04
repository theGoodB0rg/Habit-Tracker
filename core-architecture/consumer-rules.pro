# Consumer ProGuard rules for core-architecture module
# Add rules as needed for consumers of this library
-dontwarn kotlin.**
-dontwarn androidx.**
-keep class androidx.room.** { *; }
-keep class kotlinx.coroutines.** { *; }
