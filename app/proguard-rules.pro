# Proguard rules for NowShowing TV
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.google.gson.** { *; }
-keep class com.nowshowing.tv.data.model.** { *; }
-keep class com.nowshowing.tv.domain.model.** { *; }
-keep class androidx.room.** { *; }
-keepclassmembers class * {
    @androidx.room.* *;
}
-keep class androidx.media3.** { *; }
