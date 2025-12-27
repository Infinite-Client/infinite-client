package org.infinite.libs.gui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.infinite.features.Feature
import org.infinite.libs.feature.ConfigurableFeature
import org.infinite.libs.gui.widget.InfiniteBlockColorListField
import org.infinite.libs.gui.widget.InfiniteBlockListField
import org.infinite.libs.gui.widget.InfiniteButton
import org.infinite.libs.gui.widget.InfiniteEntityListField
import org.infinite.libs.gui.widget.InfinitePlayerListField
import org.infinite.libs.gui.widget.InfiniteScrollableContainer
import org.infinite.libs.gui.widget.InfiniteSelectionListField
import org.infinite.libs.gui.widget.InfiniteSettingTextField
import org.infinite.libs.gui.widget.InfiniteSettingToggle
import org.infinite.libs.gui.widget.InfiniteSlider
import org.infinite.settings.FeatureSetting
import org.infinite.utils.rendering.alpha

class FeatureSettingsScreen(
    private val parent: Screen,
    private val feature: Feature<out ConfigurableFeature>,
) : Screen(Component.literal(feature.name)) {
    private var savedPageIndex: Int = 0

    // 遅延初期化を維持
    private lateinit var scrollableContainer: InfiniteScrollableContainer

    override fun onClose() {
        minecraft?.setScreen(parent)
    }

    override fun init() {
        super.init()

        if (parent is InfiniteScreen) {
            savedPageIndex = parent.pageIndex
        }

        val settingWidgets = mutableListOf<AbstractWidget>()
        var currentY = 50
        val widgetWidth = width - 40
        val defaultWidgetHeight = 20
        val sliderWidgetHeight = 35 // Increased height for sliders
        val blockListFieldHeight = height / 2
        val padding = 5

        feature.instance.settings.forEach { setting ->
            when (setting) {
                is FeatureSetting.BooleanSetting -> {
                    settingWidgets.add(InfiniteSettingToggle(20, currentY, widgetWidth, defaultWidgetHeight, setting))
                    currentY += defaultWidgetHeight + padding
                }

                is FeatureSetting.IntSetting, is FeatureSetting.FloatSetting, is FeatureSetting.DoubleSetting -> {
                    settingWidgets.add(InfiniteSlider(20, currentY, widgetWidth, sliderWidgetHeight, setting))
                    currentY += sliderWidgetHeight + padding
                }

                is FeatureSetting.StringSetting -> {
                    settingWidgets.add(
                        InfiniteSettingTextField(
                            20,
                            currentY,
                            widgetWidth,
                            defaultWidgetHeight,
                            setting,
                        ),
                    )
                    currentY += defaultWidgetHeight + padding
                }

                is FeatureSetting.StringListSetting -> {
                    settingWidgets.add(
                        InfiniteSelectionListField(
                            20,
                            currentY,
                            widgetWidth,
                            defaultWidgetHeight,
                            setting,
                        ),
                    )
                    currentY += defaultWidgetHeight + padding
                }

                is FeatureSetting.EnumSetting<*> -> {
                    settingWidgets.add(
                        InfiniteSelectionListField(
                            20,
                            currentY,
                            widgetWidth,
                            defaultWidgetHeight,
                            setting,
                        ),
                    )
                    currentY += defaultWidgetHeight + padding
                }

                is FeatureSetting.BlockIDSetting -> {
                    settingWidgets.add(
                        InfiniteSettingTextField(
                            20,
                            currentY,
                            widgetWidth,
                            defaultWidgetHeight,
                            setting,
                        ),
                    )
                    currentY += defaultWidgetHeight + padding
                }

                is FeatureSetting.EntityIDSetting -> {
                    settingWidgets.add(
                        InfiniteSettingTextField(
                            20,
                            currentY,
                            widgetWidth,
                            defaultWidgetHeight,
                            setting,
                        ),
                    )
                    currentY += defaultWidgetHeight + padding
                }

                is FeatureSetting.BlockListSetting -> {
                    settingWidgets.add(InfiniteBlockListField(20, currentY, widgetWidth, blockListFieldHeight, setting))
                    currentY += blockListFieldHeight + padding
                }

                is FeatureSetting.EntityListSetting -> {
                    settingWidgets.add(
                        InfiniteEntityListField(
                            20,
                            currentY,
                            widgetWidth,
                            blockListFieldHeight,
                            setting,
                        ),
                    )
                    currentY += blockListFieldHeight + padding
                }

                is FeatureSetting.PlayerListSetting -> {
                    settingWidgets.add(
                        InfinitePlayerListField(
                            20,
                            currentY,
                            widgetWidth,
                            blockListFieldHeight,
                            setting,
                        ),
                    )
                    currentY += blockListFieldHeight + padding
                }

                is FeatureSetting.BlockColorListSetting -> {
                    settingWidgets.add(
                        InfiniteBlockColorListField(
                            20,
                            currentY,
                            widgetWidth,
                            blockListFieldHeight,
                            setting,
                        ),
                    )
                    currentY += blockListFieldHeight + padding
                }
            }
        }

        val scrollableContainer =
            InfiniteScrollableContainer(
                20,
                50,
                width - 40,
                height - 100,
                settingWidgets,
            )
        addRenderableWidget(scrollableContainer)
        this.scrollableContainer = scrollableContainer

        addRenderableWidget(
            InfiniteButton(
                width / 2 - 50,
                height - 30,
                100,
                20,
                Component.literal("Close"),
            ) {
                if (parent is InfiniteScreen) {
                    InfiniteScreen.selectedPageIndex = savedPageIndex
                }
                this.minecraft?.setScreen(parent)
            },
        )
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (scrollableContainer.mouseClicked(click, doubled)) return true
        return super.mouseClicked(click, doubled)
    }

    override fun mouseDragged(
        click: MouseButtonEvent,
        offsetX: Double,
        offsetY: Double,
    ): Boolean {
        if (scrollableContainer.mouseDragged(click, offsetX, offsetY)) return true
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseReleased(click: MouseButtonEvent): Boolean {
        if (scrollableContainer.mouseReleased(click)) return true
        return super.mouseReleased(click)
    }

    // --- スクロールイベント (ParentElement.java の新しいシグネチャに合わせる) ---

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        // InfiniteScrollableContainer は古い amount 引数 (垂直スクロール) を期待すると仮定し、verticalAmount を転送
        if (scrollableContainer.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    // --- キーボードイベント (ParentElement.java の新しいシグネチャに合わせる) ---

    // ParentElement.java で確認された KeyInput シグネチャを使用
    override fun keyPressed(input: KeyEvent): Boolean {
        // scrollableContainer の古い keyPressed(keyCode, scanCode, modifiers) に転送
        if (scrollableContainer.keyPressed(input)) return true
        return super.keyPressed(input)
    }

    // ParentElement.java で確認された CharInput シグネチャを使用
    override fun charTyped(input: CharacterEvent): Boolean {
        // scrollableContainer の古い charTyped(chr, modifiers) に転送
        if (scrollableContainer.charTyped(input)) return true
        return super.charTyped(input)
    }

    // --- レンダリングなど (変更なし) ---

    override fun render(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        // 背景の描画 (半透明の黒)
        context.fill(
            0,
            0,
            width,
            height,
            InfiniteClient
                .currentColors()
                .backgroundColor
                .alpha(128),
        )

        context.drawCenteredString(
            font,
            Component.literal(feature.name),
            width / 2,
            20,
            InfiniteClient
                .currentColors()
                .foregroundColor,
        )
        context.drawCenteredString(
            font,
            Component.translatable(feature.descriptionKey),
            width / 2,
            35,
            InfiniteClient
                .currentColors()
                .secondaryColor,
        )

        // ウィジェットの描画 (scrollableContainerを含む)
        super.render(context, mouseX, mouseY, delta)
    }

    override fun isPauseScreen(): Boolean = false
}
