# Export Engine Module Consumer ProGuard Rules
# These rules will be automatically applied to consumer modules

# Keep all export related public APIs
-keep public class com.habittracker.export.** { public *; }

# Keep data models used in export
-keep class com.habittracker.export.data.model.** { *; }

# Keep exception classes
-keep class com.habittracker.export.domain.exception.** { *; }

# Preserve enum names for export formats
-keepnames class com.habittracker.export.domain.model.ExportFormat
