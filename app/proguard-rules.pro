# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Preserve file names and line numbers in release for actionable crash reports
-keepattributes SourceFile,LineNumberTable,Signature

# Temporarily keep our app classes unobfuscated to diagnose crashes (can be relaxed later)
-keep class com.habittracker.** { *; }

# Keep Compose runtime/UI to get meaningful stack traces from release builds while we diagnose
-keep class androidx.compose.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.material.** { *; }

# Gson generic types via TypeToken require Signature and these keeps for reflection-based parsing
-keep class com.google.gson.reflect.TypeToken
-keep class * extends com.google.gson.reflect.TypeToken

# --- Release shrink fixes for annotation-only deps (safe) ---
# Google Tink and some AndroidX libs reference these compile-time annotations
# which are not required at runtime. Suppress warnings so R8 doesn't fail.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.j2objc.annotations.**
-dontwarn org.checkerframework.**

# If you ever see keep suggestions generated under
# app/build/outputs/mapping/release/missing_rules.txt,
# review and migrate the minimal necessary rules here.

# --- Gson / TypeToken: preserve generic signatures and subclasses ---
# Required so TypeToken retains parameterized type info after shrinking/obfuscation
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Keep TypeToken itself
-keep class com.google.gson.reflect.TypeToken { *; }

# Also keep every (anonymous) subclass that captures the generic parameter
-keep class * extends com.google.gson.reflect.TypeToken { *; }
