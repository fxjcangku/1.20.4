-dontoptimize
-dontwarn **
-ignorewarnings
-keep class com.example.addon.AddonTemplate { *; }
-keep class com.example.addon.JeraddonModule { *; }
-keep class com.example.addon.JeraddonWelcomeService { *; }
-keep class com.example.addon.modules.** { public <init>(); }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepclassmembers class * {
    @meteordevelopment.orbit.EventHandler <methods>;
}
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keep @org.spongepowered.asm.mixin.gen.Invoker interface * { *; }
-keepclassmembers enum com.example.addon.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public java.lang.String toString();
}
-adaptclassstrings
-repackageclasses 'com.example.addon.internal'
