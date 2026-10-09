# BrainRot ProGuard rules
# Keep Capacitor and WebView bridge
-keep class com.getcapacitor.** { *; }
-keep class com.brainrot.app.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*

# Keep Cordova if used
-keep class org.apache.cordova.** { *; }
-keep class org.chromium.** { *; }

# Keep native plugins
-keep class com.capacitorjs.plugins.** { *; }

# WebView
-keepclassmembers class fqcn.of.javascript.interface.for.webview {
   public *;
}
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,EnclosingMethod,Signature,InnerClasses

# For debugging, keep line numbers
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Don't warn about missing classes
-dontwarn android.webkit.**
-dontwarn com.getcapacitor.**
-dontwarn org.apache.cordova.**
