# Keep kotlinx.serialization generated serializers for nav routes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class dev.sam.countri.** {
    *** Companion;
}
-keepclasseswithmembers class dev.sam.countri.** {
    kotlinx.serialization.KSerializer serializer(...);
}
