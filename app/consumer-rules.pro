# Audx Library - Consumer ProGuard Rules
# These rules are automatically applied to apps that use this library

# ============================================================================
# JNI - Keep all native method declarations
# ============================================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================================================
# Public API - Keep all public classes and methods
# ============================================================================

# Main Denoiser class
-keep class com.android.audx.Denoiser {
    public *;
    protected *;
}

# Denoiser.Builder
-keep class com.android.audx.Denoiser$Builder {
    public *;
}

# Data classes
-keep class com.android.audx.DenoiserResult {
    public *;
}

# Enums
-keep class com.android.audx.ModelPreset {
    *;
}

-keepclassmembers enum com.android.audx.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================================================
# Kotlin - Keep suspend functions and coroutines
# ============================================================================
-keepclassmembers class com.android.audx.Denoiser {
    ** processChunk(**, kotlin.coroutines.Continuation);
}

# ============================================================================
# Callbacks and listeners
# ============================================================================
-keep class com.android.audx.ProcessedAudioCallbackKt {
    *;
}

# Keep all callback function types
-keep class com.android.audx.** implements kotlin.jvm.functions.** {
    *;
}

# ============================================================================
# Debugging - Optional, remove in production if needed
# ============================================================================
# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Keep parameter names for better debugging
-keepattributes *Annotation*,Signature,Exception
