# Fix R8 error: Missing class okhttp3.internal.Util
-dontwarn okhttp3.internal.Util
-keep class okhttp3.internal.Util { *; }
