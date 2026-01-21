// package org.infinite.mixin.infinite.features.global.control.ime;
//
// import net.minecraft.client.KeyboardHandler;
// import net.minecraft.client.input.CharacterEvent;
// import net.minecraft.client.input.KeyEvent;
// import org.spongepowered.asm.mixin.Mixin;
// import org.spongepowered.asm.mixin.injection.At;
// import org.spongepowered.asm.mixin.injection.Inject;
// import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//
// @Mixin(KeyboardHandler.class)
// public abstract class KeyboardHandlerMixin {
//
//    /**
//     * keyPressの冒頭でIME入力を検知し、Minecraft側の処理（移動、ジャンプ、デバッグ機能等）を遮断します。
//     * Windows OSではIME操作中、仮想キーコードは 229 (VK_PROCESSKEY) になります。
//     */
////    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
////    private void onKeyPress(long l, int i, KeyEvent keyEvent, CallbackInfo ci) {
////        // keyEvent.key() を使用して、OSから送られてきたキーコードを確認
////        if (keyEvent.key() == 229) {
////            // IMEがイベントを消費しているため、Minecraft側の処理をすべてスキップ
////            ci.cancel();
////        }
////    }
//
//    /**
//     * 文字入力イベントのハンドリング。
//     * 通常、IME入力中は keyPress 側で 229 が送られるため、
//     * ここでの特別な処理は不要なことが多いですが、一応フックを残しておきます。
//     */
//    @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
//    private void onCharTyped(long l, CharacterEvent characterEvent, CallbackInfo ci) {
//        // IME確定後の文字は characterEvent.codepoint() としてここに届きます。
//        // 基本的にここはMinecraftのスクリーン（EditBox等）に渡すべきなので、
//        // 特殊なフィルタリングが必要ない限りは cancel() しないことを推奨します。
//    }
// }
