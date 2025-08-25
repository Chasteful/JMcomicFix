-keep class net.minecraft.** { *; }
-keep class com.mojang.** { *; }
-keep class net.fabricmc.** { *; }


-keep class kotlin.** { *; }
-keepclassmembers class kotlin.** { *; }
-keepclassmembers class * { ** lambda*(...); }
-keepclassmembers class * { public static ** Companion; }

-keep class net.ccbluex.liquidbounce.** { *; } 
-keep class !net.ccbluex.jmcomicfix.** { *; }
 -keep,allowobfuscation class net.ccbluex.jmcomicfix.** { *; }

-keep class net.ccbluex.liquidbounce.common.BackgroundTexture { *; }

-keep class net.minecraft.client.texture.ReloadableTexture { *; }
-keepclassmembers class * extends net.minecraft.client.texture.ReloadableTexture { <init>(...); *; }
-keep class net.minecraft.client.texture.TextureContents { *; }
-keepclassmembers class * extends net.minecraft.client.texture.TextureContents { <init>(...); *; }

-keepclassmembers enum * { *; }

-keepattributes *Annotation*,InnerClasses,EnclosingMethod,Signature,SourceFile,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

-keep class knot.aW { *; }
-keepclassmembers class knot.aW { *; }

-dontoptimize
-dontpreverify
-dontwarn **
-dontnote **
-allowaccessmodification

-repackageclasses ''
-overloadaggressively
-useuniqueclassmembernames

-packageobfuscationdictionary dictionary.txt
-obfuscationdictionary dictionary.txt
-classobfuscationdictionary dictionary.txt

