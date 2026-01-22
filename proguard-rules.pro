# 1. 基本設定
-dontobfuscate             # 難読化はデバッグを困難にするため、パフォーマンス目的のみならオフを推奨
-optimizationpasses 3      # 最適化を何回繰り返すか
-allowaccessmodification   # アクセス修飾子を最適化（高速化に寄与）
-dontwarn                  # 依存ライブラリの欠損警告を無視

# 2. Fabric / Minecraft 関連 (絶対に消してはいけない)
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keep class org.infinite.mixin.** { *; } # あなたのMixinパッケージ

# 3. Project Panama (jextract) 関連
# Rustとのバインディングクラスが消されると詰みます
-keep class org.infinite.nativebind.** { *; }
-keepclassmembers class org.infinite.nativebind.** {
    native <methods>;
}

# 4. Entrypoints
-keep class org.infinite.InfiniteMod { *; } # Modのメインクラス

# 5. Kotlin / OkHttp 等の動的アクセスへの対応
-keepclassmembers class * {
    @kotlin.jvm.JvmField *;
}
