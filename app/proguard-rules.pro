# NexPass - ProGuard Rules for Release Builds
# Security-focused password manager - Keep all cryptographic classes

# ===========================
# Cryptography & Security
# ===========================

# Keep all cryptography classes - CRITICAL for security
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class javax.security.** { *; }
-keepclassmembers class javax.crypto.** { *; }

# Keep Android Keystore classes
-keep class android.security.keystore.** { *; }
-keepclassmembers class android.security.keystore.** { *; }

# Keep BiometricPrompt
-keep class androidx.biometric.** { *; }

# SQLCipher
-keep class net.sqlcipher.** { *; }
-keep class net.sqlcipher.database.** { *; }

# ===========================
# Ktor Client
# ===========================

-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Ktor serialization
-keep class io.ktor.serialization.** { *; }
-keep class io.ktor.client.** { *; }

# ===========================
# Kotlinx Serialization
# ===========================

-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep Serializer classes
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}

-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-keep @kotlinx.serialization.Serializable class ** {
    *;
}

-keepclassmembers @kotlinx.serialization.Serializable class ** {
    # lookup for plugin generated serializable classes
    *** Companion;
    # lookup for serializable objects
    *** INSTANCE;
    # keep fields for serialization
    <fields>;
}

# ===========================
# Room Database
# ===========================

# Keep Room classes
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

# Keep Room annotations
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

-dontwarn androidx.room.paging.**

# ===========================
# Koin Dependency Injection
# ===========================

-keep class org.koin.** { *; }
-keep class org.koin.core.** { *; }
-keepclassmembers class org.koin.** { *; }

# ===========================
# Coroutines
# ===========================

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ===========================
# Jetpack Compose
# ===========================

# Keep Compose runtime
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }

# Keep Composable functions
-keepclassmembers class ** {
    @androidx.compose.runtime.Composable <methods>;
}

# ===========================
# NexPass Application Classes
# ===========================

# Keep all domain models (they are data classes used in the app)
-keep class com.nexpass.passwordmanager.domain.model.** { *; }

# Keep all DTOs (used for network serialization)
-keep class com.nexpass.passwordmanager.data.remote.dto.** { *; }

# Keep all database entities
-keep class com.nexpass.passwordmanager.data.local.entity.** { *; }

# Keep AutofillService implementation - CRITICAL
-keep class com.nexpass.passwordmanager.autofill.** { *; }
-keepclassmembers class com.nexpass.passwordmanager.autofill.** { *; }

# Keep security layer implementations
-keep class com.nexpass.passwordmanager.security.** { *; }
-keepclassmembers class com.nexpass.passwordmanager.security.** { *; }

# Keep repository implementations
-keep class com.nexpass.passwordmanager.data.repository.** { *; }

# Keep use cases
-keep class com.nexpass.passwordmanager.domain.usecase.** { *; }

# ===========================
# General Android
# ===========================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(***);
}

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ===========================
# Debugging & Optimization
# ===========================

# Remove logging in release builds (except errors)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove debugging code
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void check*(...);
    public static void throw*(...);
}

# Optimization
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# ===========================
# Security Hardening
# ===========================

# Obfuscate class names (but keep important security classes identifiable for debugging)
-repackageclasses 'com.nexpass.obfuscated'

# Remove source file names and line numbers for extra security
# WARNING: This makes crash debugging harder - disable during beta testing
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# ===========================
# Warnings to Suppress
# ===========================

# Suppress warnings for missing classes that are not needed at runtime
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**

# Keep Google Tink crypto library (used by AndroidX Security)
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# ===========================
# Keep Stack Traces Readable
# ===========================

# Keep exception messages and stack traces
-keepattributes Exceptions
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
