# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, uncomment and replace classes with those containing named companion objects.
#-keepattributes InnerClasses # Needed for `getDeclaredClasses`.
#-if @kotlinx.serialization.Serializable class
#com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions.
#com.example.myapplication.HasNamedCompanion2
#{
#    static **$* *;
#}
#-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
#    static <1>$$serializer INSTANCE;
#}

# Excerpt of https://raw.githubusercontent.com/square/okhttp/f408411/okhttp/src/jvmMain/resources/META-INF/proguard/okhttp3.pro
# (Licensed under Apache 2.0. No modifications.)
# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
# End of excerpt.
-dontwarn org.slf4j.impl.**

# hCaptcha related
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn lombok.**

# do not log in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

-dontwarn kotlin.**
-dontwarn org.w3c.dom.events.*
-dontwarn org.jetbrains.kotlin.di.InjectorForRuntimeDescriptorLoader

-keep class kotlin.** { *; }
-keep class org.jetbrains.kotlin.** { *; }

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}

-keepattributes InnerClasses

-keep class androidx.compose.ui.graphics.ColorKt { *; }

-keep class androidx.compose.material3.ColorScheme { *; }
-keep class androidx.compose.material3.ColorSchemeKt { *; }
-keep class androidx.compose.material3.ColorSchemeKt$* { *; }
-keepclassmembers class androidx.compose.material3.ColorSchemeKt {
    public static final <fields>;
    public <fields>;
}

-keep class chat.peptide.ndk.AstNode { *; }