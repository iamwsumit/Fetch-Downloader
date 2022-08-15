# Add any ProGuard configurations specific to this
# extension here.

-keep public class com.sumit1334.fetchdownloader.FetchDownloader {
    public *;
 }

 -keep public class com.tonyodev.**

-keeppackagenames gnu.kawa**, gnu.expr**
-optimizationpasses 4
-allowaccessmodification
-mergeinterfacesaggressively

-repackageclasses 'com/sumit1334/fetchdownloader/repack'
-flattenpackagehierarchy
-dontpreverify
-dontwarn com.tonyodev**
-dontwarn androidx.room**
