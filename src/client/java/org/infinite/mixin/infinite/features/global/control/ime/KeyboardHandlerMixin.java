package org.infinite.mixin.infinite.features.global.control.ime;

import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

  // IMEがアクティブ（229を検知した直後など）かどうかのフラグ
  @Unique private boolean isImeActive = false;

  @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
  private void onKeyPress(long window, int scancode, KeyEvent keyEvent, CallbackInfo ci) {
    int keyCode = keyEvent.key();

    // IME操作の開始を検知
    if (keyCode == 229) {
      isImeActive = true;
      ci.cancel();
      return;
    }

    // IME操作中に Delete(261), BackSpace(259), 矢印キーなどが飛んできた場合
    // ここで ci.cancel() しないと、マイクラ側のテキストボックスが反応してしまう
    if (isImeActive) {
      // Enter(257) が押されたら IME確定とみなしてフラグを下ろす（環境により調整が必要）
      if (keyCode == 257 || keyCode == 256 /* Escape */) {
        isImeActive = false;
      } else {
        // IME操作中なら、Deleteキーなどのマイクラ側処理を遮断
        ci.cancel();
      }
    }
  }

  @Inject(method = "charTyped", at = @At("HEAD"), cancellable = true)
  private void onCharTyped(long l, CharacterEvent characterEvent, CallbackInfo ci) {
    // IME入力中の未確定文字が charTyped に流れてくるのを防ぐ
    if (isImeActive) {
      ci.cancel();
    }
  }
}
