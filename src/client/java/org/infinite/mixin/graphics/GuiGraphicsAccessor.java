package org.infinite.mixin.graphics;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiGraphicsExtractor.class)
public interface GuiGraphicsAccessor {

  @Accessor("hoveredTextStyle")
  void setHoveredTextStyle(Style style);

  @Accessor("clickableTextStyle")
  void setClickableTextStyle(Style style);

  @Accessor("guiRenderState")
  GuiRenderState getGuiRenderState();
}
