# ProGuard rules for core-architecture module
# Add rules as needed for Room, coroutines, etc.
-dontwarn kotlin.**
-dontwarn androidx.**
-keep class androidx.room.** { *; }
-keep class kotlinx.coroutines.** { *; }
 
# Gson TypeToken rules (for converters using anonymous subclasses)
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken { *; }
