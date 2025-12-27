package org.infinite.libs.gui.screen

import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import org.infinite.InfiniteClient
import org.infinite.global.ConfigurableGlobalFeature
import org.infinite.global.GlobalFeature
import org.infinite.global.GlobalFeatureCategory
import org.infinite.libs.graphics.Graphics2D
import org.infinite.libs.gui.widget.InfiniteBlockColorListField
import org.infinite.libs.gui.widget.InfiniteBlockListField
import org.infinite.libs.gui.widget.InfiniteEntityListField
import org.infinite.libs.gui.widget.InfiniteGlobalFeatureToggle
import org.infinite.libs.gui.widget.InfinitePlayerListField
import org.infinite.libs.gui.widget.InfiniteScrollableContainer
import org.infinite.libs.gui.widget.InfiniteSelectionListField
import org.infinite.libs.gui.widget.InfiniteSettingTextField
import org.infinite.libs.gui.widget.InfiniteSettingToggle
import org.infinite.libs.gui.widget.InfiniteSlider
import org.infinite.libs.gui.widget.TabButton
import org.infinite.libs.gui.widget.ThemeTileContainer
import org.infinite.settings.FeatureSetting
import org.infinite.utils.rendering.transparent
import org.lwjgl.glfw.GLFW

class GlobalSettingsScreen(
    parentScreen: Screen?,
) : Screen(Component.literal("Infinite Client Global Settings")) {
    private val parent: Screen? = parentScreen
    private var selectedCategory: GlobalFeatureCategory? = null
    private var initialCategoryName: String? = null
    private val sections: MutableMap<GlobalFeatureCategory, Section> = mutableMapOf()
    private val categories = InfiniteClient.globalFeatureCategories.toMutableList()

    class Section(
        val tab: TabButton,
        val contents: InfiniteScrollableContainer,
    )

    override fun init() {
        super.init()
        sections.clear()

        val tabSpacing = 2
        // タブの幅を計算。最小幅を確保しつつ、テキストに合わせて調整
        val tabWidth =
            categories.maxOf { font.width(it.name) + tabSpacing * 2 }.coerceAtLeast(width / categories.size)
        val tabHeight = (font.lineHeight + tabSpacing) * 2
        val totalTabsWidth = (tabWidth + tabSpacing) * categories.size - tabSpacing
        var x = (this.width - totalTabsWidth) / 2

        categories.forEach { category ->
            val tabButton =
                TabButton(
                    x,
                    0,
                    tabWidth,
                    tabHeight,
                    // カテゴリ名をローカライズして表示
                    Component.literal(category.name),
                ) {
                    selectedCategory = category
                    updateCategoryContent() // クリックされたらコンテンツを切り替え
                    updateTabButtonStates()
                }

            val contents = InfiniteScrollableContainer(0, tabHeight, width, height - tabHeight, generateWidgets(category))

            x += tabWidth + tabSpacing
            sections[category] = Section(tabButton, contents)

            // タブボタンのみをselectableChildとして追加
            addWidget(tabButton)
        }

        selectedCategory =
            initialCategoryName
                ?.let { desired -> categories.find { it.name.equals(desired, ignoreCase = true) } }
                ?: categories.firstOrNull()
        updateCategoryContent()
        updateTabButtonStates()
    }

    // 選択されたカテゴリのコンテンツを有効化/無効化する新しいメソッド
    private fun updateCategoryContent() {
        sections.forEach { (category, section) ->
            val container = section.contents
            if (category == selectedCategory) {
                // 選択されているカテゴリのコンテンツを有効化
                container.visible = true
                addWidget(container) // childrenリストに追加することでフォーカス対象とする
            } else {
                // 選択されていないカテゴリのコンテンツを無効化
                container.visible = false
                removeWidget(container) // childrenリストから削除することでフォーカス対象外とする
            }
            container.isFocused = category == selectedCategory // フォーカス状態を設定
        }
    }

    private fun updateTabButtonStates() {
        sections
            .map { it.value }
            .forEach { it.tab.isHighlighted = it.tab.message.string == getCategoryDisplayName(selectedCategory) }
    }

    private fun getCategoryDisplayName(category: GlobalFeatureCategory?): String =
        category?.let {
            if (it.name == "Themes") {
                "Themes"
            } else {
                Component.translatable("infinite.global_category.${it.name.lowercase()}").string
            }
        } ?: ""

    override fun render(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        // 背景を描画
        val graphics2D = Graphics2D(context)
        graphics2D.fill(
            0,
            0,
            graphics2D.width,
            graphics2D.height,
            InfiniteClient
                .theme()
                .colors.backgroundColor
                .transparent(100),
        )
        sections.forEach { (category, section) ->
            val selected = category == selectedCategory
            section.tab.isHighlighted = selected
            section.tab.render(context, mouseX, mouseY, delta)
            if (selected) {
                section.contents.render(context, mouseX, mouseY, delta)
            }
        }
        super.render(context, mouseX, mouseY, delta)
    }

    override fun keyPressed(input: KeyEvent): Boolean {
        when (input.key) {
            GLFW.GLFW_KEY_ESCAPE -> {
                this.onClose()
            }

            GLFW.GLFW_KEY_LEFT -> {
                selectPreviousCategory()
            }

            GLFW.GLFW_KEY_RIGHT -> {
                selectNextCategory()
            }

            else -> {
                return super.keyPressed(input)
            }
        }
        return true
    }

    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean = super.mouseClicked(click, doubled)

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean = super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)

    override fun mouseDragged(
        click: MouseButtonEvent,
        offsetX: Double,
        offsetY: Double,
    ): Boolean = super.mouseDragged(click, offsetX, offsetY)

    override fun mouseReleased(click: MouseButtonEvent): Boolean = super.mouseReleased(click)

    override fun charTyped(input: CharacterEvent): Boolean = super.charTyped(input)

    private fun selectPreviousCategory() {
        if (categories.isEmpty()) return
        val currentIndex = categories.indexOf(selectedCategory)
        val newIndex = if (currentIndex <= 0) categories.size - 1 else currentIndex - 1
        selectedCategory = categories[newIndex]
        updateCategoryContent() // コンテンツを切り替える
        updateTabButtonStates()
    }

    private fun selectNextCategory() {
        if (categories.isEmpty()) return
        val currentIndex = categories.indexOf(selectedCategory)
        val newIndex = if (currentIndex >= categories.size - 1) 0 else currentIndex + 1
        selectedCategory = categories[newIndex]
        updateCategoryContent() // コンテンツを切り替える
        updateTabButtonStates()
    }

    private fun generateWidgets(category: GlobalFeatureCategory): MutableList<AbstractWidget> {
        val allCategoryWidgets = mutableListOf<AbstractWidget>()
        val contentWidth = width - 40
        val padding = 5
        val defaultWidgetHeight = 20

        category.features.forEach { feature ->
            val featureDescription = Component.translatable(feature.descriptionKey).string

            // isEnabledトグルボタンを追加
            allCategoryWidgets.add(
                InfiniteGlobalFeatureToggle(
                    0, // x は ScrollableContainer が設定
                    0, // y は ScrollableContainer が設定
                    contentWidth,
                    defaultWidgetHeight,
                    feature,
                    false,
                    featureDescription,
                ),
            )

            // 概要ウィジェットと設定ウィジェットの間のスペーサー
            allCategoryWidgets.add(
                object : AbstractWidget(0, 0, contentWidth, 10, Component.empty()) {
                    override fun renderWidget(
                        context: GuiGraphics,
                        mouseX: Int,
                        mouseY: Int,
                        delta: Float,
                    ) {
                        // スペーサー、自身の視覚的レンダリングは不要
                    }

                    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
                },
            )
            allCategoryWidgets.addAll(generateWidgets(feature))

            // 各フィーチャー間の視覚的な区切りとして大きなスペーサーを追加
            allCategoryWidgets.add(
                object : AbstractWidget(0, 0, contentWidth, padding, Component.empty()) {
                    override fun renderWidget(
                        context: GuiGraphics,
                        mouseX: Int,
                        mouseY: Int,
                        delta: Float,
                    ) {
                    }

                    override fun updateWidgetNarration(builder: NarrationElementOutput) {}
                },
            )
        }
        return allCategoryWidgets
    }

    private fun generateWidgets(feature: GlobalFeature<out ConfigurableGlobalFeature>): MutableList<AbstractWidget> {
        val settingWidgets = mutableListOf<AbstractWidget>()
        val widgetWidth = width - 40 // ScrollableContainerの幅に合わせるため、仮の値
        val defaultWidgetHeight = 20
        val sliderWidgetHeight = 35
        val blockListFieldHeight = height / 2

        feature.instance.settings.forEach { setting ->
            when (setting) {
                is FeatureSetting.BooleanSetting -> {
                    settingWidgets.add(InfiniteSettingToggle(0, 0, widgetWidth, defaultWidgetHeight, setting))
                }

                is FeatureSetting.IntSetting, is FeatureSetting.FloatSetting, is FeatureSetting.DoubleSetting -> {
                    settingWidgets.add(InfiniteSlider(0, 0, widgetWidth, sliderWidgetHeight, setting))
                }

                is FeatureSetting.StringSetting -> {
                    settingWidgets.add(InfiniteSettingTextField(0, 0, widgetWidth, defaultWidgetHeight, setting))
                }

                is FeatureSetting.StringListSetting -> {
                    settingWidgets.add(InfiniteSelectionListField(0, 0, widgetWidth, defaultWidgetHeight, setting))
                }

                is FeatureSetting.EnumSetting<*> -> {
                    settingWidgets.add(InfiniteSelectionListField(0, 0, widgetWidth, defaultWidgetHeight, setting))
                }

                is FeatureSetting.BlockIDSetting -> {
                    settingWidgets.add(InfiniteSettingTextField(0, 0, widgetWidth, defaultWidgetHeight, setting))
                }

                is FeatureSetting.EntityIDSetting -> {
                    settingWidgets.add(InfiniteSettingTextField(0, 0, widgetWidth, defaultWidgetHeight, setting))
                }

                is FeatureSetting.BlockListSetting -> {
                    settingWidgets.add(InfiniteBlockListField(0, 0, widgetWidth, blockListFieldHeight, setting))
                }

                is FeatureSetting.EntityListSetting -> {
                    settingWidgets.add(InfiniteEntityListField(0, 0, widgetWidth, blockListFieldHeight, setting))
                }

                is FeatureSetting.PlayerListSetting -> {
                    settingWidgets.add(InfinitePlayerListField(0, 0, widgetWidth, blockListFieldHeight, setting))
                }

                is FeatureSetting.BlockColorListSetting -> {
                    settingWidgets.add(InfiniteBlockColorListField(0, 0, widgetWidth, blockListFieldHeight, setting))
                }
            }
        }
        val customWidgets =
            feature.instance.getCustomWidgets().map { widget ->
                if (widget is ThemeTileContainer) {
                    widget.width = widgetWidth
                    widget.height = widget.calculateHeight(widgetWidth)
                }
                widget
            }
        settingWidgets.addAll(customWidgets) // Add custom widgets
        return settingWidgets
    }

    override fun onClose() {
        this.minecraft?.setScreen(this.parent)
    }
}
