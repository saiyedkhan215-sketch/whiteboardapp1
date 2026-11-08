# ProGuard rules for Production Release (2026)

# Keep Room entities and DAOs
-keep class androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Moshi models & codegen classes
-keep class com.example.data.model.** { *; }
-keep class * implements com.squareup.moshi.JsonAdapter

# Keep Retrofit interfaces
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep ML Kit Digital Ink and Text Recognition models
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
-keep class com.google.android.gms.internal.mlkit_** { *; }
-dontwarn com.google.android.gms.internal.mlkit_**
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.tasks.**

# Keep Compose UI
-keep class androidx.compose.ui.** { *; }

# Keep AdMob
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

