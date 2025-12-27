package org.infinite.features.utils.map

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import net.minecraft.util.ARGB
import org.infinite.InfiniteClient
import org.infinite.gui.widget.InfiniteButton
import org.infinite.libs.graphics.Graphics2D
import org.infinite.utils.rendering.transparent
import org.infinite.utils.toRadians
import kotlin.math.max
import kotlin.math.min

class FullScreenMapScreen(
    val mapFeature: MapFeature,
) : Screen(Component.nullToEmpty("Full Screen Map")) {
    private var zoom: Double = 1.0

    // マップの中心点となるワールド座標 (X, Z)
    private var centerX: Double = 0.0
    private var centerZ: Double = 0.0
    private val hyperMap: HyperMap
        get() = InfiniteClient.getFeature(HyperMap::class.java)!!

    private var dragStartX: Double = 0.0
    private var dragStartZ: Double = 0.0
    private var dragStartMouseX: Double = 0.0
    private var dragStartMouseY: Double = 0.0

    // --- 定数 ---
    private val zoomStep = 0.1
    private val minZoom = 0.1
    private val maxZoom = 5.0
    private val buttonWidth = 40
    private val buttonHeight = 20

    init {
        val player = minecraft?.player
        if (player != null) {
            centerX = player.x
            centerZ = player.z
        }
    }

    override fun init() {
        super.init()

        val buttonSpacing = 5
        var currentX = width - (buttonWidth * 3 + buttonSpacing * 3)

        // 1. ズームアウト (-) ボタン
        val zoomOutButton =
            InfiniteButton(
                currentX,
                buttonSpacing,
                buttonWidth,
                buttonHeight,
                Component.nullToEmpty("-"),
            ) {
                zoom = max(minZoom, zoom - zoomStep)
            }
        addRenderableWidget(zoomOutButton)
        currentX += buttonWidth + buttonSpacing
        // 2. ズームイン (+) ボタン
        val zoomInButton =
            InfiniteButton(
                currentX,
                buttonSpacing,
                buttonWidth,
                buttonHeight,
                Component.nullToEmpty("+"),
            ) {
                zoom = min(maxZoom, zoom + zoomStep)
            }
        addRenderableWidget(zoomInButton)
        currentX += buttonWidth + buttonSpacing

        // 3. 現在地へリセット (⌖) ボタン
        val centerButton =
            InfiniteButton(
                currentX,
                buttonSpacing,
                buttonWidth,
                buttonHeight,
                Component.nullToEmpty("⌖"),
            ) {
                val p = minecraft?.player
                if (p != null) {
                    // マップの中心をプレイヤーの現在地に戻す
                    centerX = p.x
                    centerZ = p.z
                    zoom = 1.0
                }
            }
        addRenderableWidget(centerButton)
    }

    // --- mouseClicked / mouseScrolled のロジックは変更なし ---
    override fun mouseClicked(
        click: MouseButtonEvent,
        doubled: Boolean,
    ): Boolean {
        if (click.button() == 0) { // 左クリック
            dragStartX = centerX
            dragStartZ = centerZ
            dragStartMouseX = click.x
            dragStartMouseY = click.y
        }
        return super.mouseClicked(click, doubled)
    }

    // --- mouseDragged の修正: アスペクト比を考慮したワールド移動量に修正 ---
    override fun mouseDragged(
        click: MouseButtonEvent,
        offsetX: Double,
        offsetY: Double,
    ): Boolean {
        if (click.button() == 0) { // Left mouse button
            val screenWidth = width
            val screenHeight = height

            val totalMoveX = click.x() - dragStartMouseX
            val totalMoveY = click.y() - dragStartMouseY

            val baseMapWorldRadius = hyperMap.radiusSetting.value.toDouble()
            val effectiveWorldRadius = baseMapWorldRadius / zoom

            // 画面のアスペクト比を考慮したワールドの幅と高さ
            val mapWorldWidth = effectiveWorldRadius * (screenWidth.toDouble() / screenHeight.toDouble())

            // 画面移動量 (ピクセル) をワールド座標の移動量に変換
            // (移動量 / 画面の半分) * 対応するワールド半径
            val worldMoveX = (totalMoveX / (screenWidth / 2.0)) * mapWorldWidth
            val worldMoveZ = (totalMoveY / (screenHeight / 2.0)) * effectiveWorldRadius

            centerX = dragStartX - worldMoveX
            centerZ = dragStartZ - worldMoveZ

            return true
        }
        return super.mouseDragged(click, offsetX, offsetY)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        horizontalAmount: Double,
        verticalAmount: Double,
    ): Boolean {
        if (verticalAmount > 0) {
            zoom = min(maxZoom, zoom + zoomStep)
        } else if (verticalAmount < 0) {
            zoom = max(minZoom, zoom - zoomStep)
        }
        return true
    }

    // --- render 関数の修正: 画面全体を描画し、アスペクト比を考慮した座標計算に修正 ---
    override fun render(
        context: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        delta: Float,
    ) {
        val graphics2D = Graphics2D(context, minecraft!!.deltaTracker)
        val player = minecraft!!.player ?: return

        val screenWidth = width
        val screenHeight = height

        // 1. ワールド表示範囲の計算
        val baseMapWorldRadius = hyperMap.radiusSetting.value.toDouble()
        val effectiveWorldRadius = baseMapWorldRadius / zoom // Z/Y方向のワールド表示半径

        // 画面のアスペクト比を考慮したX/Z方向のワールド表示半径
        val mapWorldWidth = effectiveWorldRadius * (screenWidth.toDouble() / screenHeight.toDouble()) // X方向のワールド表示半径
        val mapWorldHeight = effectiveWorldRadius // Z方向のワールド表示半径

        // 2. 描画設定（画面全体）
        val renderWidth = screenWidth

        // Render terrain
        renderTerrainFullScreen(graphics2D, renderWidth, screenHeight, mapWorldWidth, mapWorldHeight)

        // Render entities
        renderEntitiesFullScreen(graphics2D, renderWidth, screenHeight, mapWorldWidth, mapWorldHeight)

        // Draw player dot (画面中央を基準) を三角形で置き換える
        val playerDx = player.x - centerX
        val playerDz = player.z - centerZ

        // プレイヤーの位置を画面座標に変換
        val scaledPlayerDx = playerDx / mapWorldWidth * (renderWidth / 2.0)
        val scaledPlayerDz = playerDz / mapWorldHeight * (screenHeight / 2.0)

        val playerDotColor =
            InfiniteClient
                .theme()
                .colors.infoColor
                .transparent(255)

        // プレイヤーの位置 (画面中央を基準としたオフセット)
        val playerScreenX = (screenWidth / 2.0 + scaledPlayerDx).toFloat()
        val playerScreenY = (screenHeight / 2.0 + scaledPlayerDz).toFloat()

        // プレイヤーの向き (yaw) を取得し、ラジアンに変換
        // Minecraftの yaw は Y軸周りの回転で、-180から180度。北が-90、東が0、南が90、西が180/-180。
        // 右がX+、下がZ+と仮定して、時計回りの角度に変換
        val playerYawRadians = -toRadians(player.yRot + 90) // 北が上 (-Z) になるように調整

        val triangleSize = 8f // 三角形のサイズ (適宜調整)
        val halfSize = triangleSize / 2f

        // 三角形の頂点座標をプレイヤーを中心に計算
        // 回転の中心が playerScreenX, playerScreenY となるように translate/rotate/translate を使う
        graphics2D.pushState()
        graphics2D.translate(playerScreenX, playerScreenY)
        graphics2D.matrixStack.rotate(playerYawRadians) // Z軸周りの回転

        // (0,0) を中心とした三角形の頂点
        val tipX = 0f
        val tipY = -halfSize * 1.5f // プレイヤーの向いている方向
        val leftBaseX = -halfSize

        graphics2D.fillTriangle(
            tipX,
            tipY,
            leftBaseX,
            halfSize,
            halfSize,
            halfSize,
            playerDotColor,
        )
        graphics2D.popState()

        // 💡 方角表示 (左上にコンパクトに表示)
        val margin = 10
        val compassRadius = 20f
        compassRadius * 0.7f

        InfiniteClient.theme().colors.errorColor
        val otherColor = InfiniteClient.theme().colors.foregroundColor
        // 1. プレイヤー座標の表示 (左上)
        val currentY = margin
        if (mapFeature.showPlayerCoordinates.value) {
            val p = minecraft!!.player ?: return
            val coordsText = Component.nullToEmpty("X: %.1f Y: %.1f Z: %.1f".format(p.x, p.y, p.z))
            graphics2D.drawText(
                coordsText.string,
                margin,
                currentY,
                otherColor,
                true,
            )
        }
        super.render(context, mouseX, mouseY, delta)
    }

    // --- renderTerrainFullScreen の修正: 画面サイズとワールド半径を引数として受け取り描画 ---
    private fun renderTerrainFullScreen(
        graphics2D: Graphics2D,
        renderWidth: Int,
        renderHeight: Int,
        mapWorldWidth: Double, // X方向のワールド表示半径 (アスペクト比考慮済)
        mapWorldHeight: Double, // Z方向のワールド表示半径
    ) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        val centerBlockX = centerX.toInt()
        val centerBlockZ = centerZ.toInt()
        val centerChunkX = centerBlockX shr 4
        val centerChunkZ = centerBlockZ shr 4

        // 描画するチャンクの範囲を、画面がカバーするワールドの幅と高さから計算
        val horizontalRenderDistanceChunks = (mapWorldWidth / 16.0).toInt() + 1
        val verticalRenderDistanceChunks = (mapWorldHeight / 16.0).toInt() + 1

        val minChunkX = centerChunkX - horizontalRenderDistanceChunks
        val maxChunkX = centerChunkX + horizontalRenderDistanceChunks
        val minChunkZ = centerChunkZ - verticalRenderDistanceChunks
        val maxChunkZ = centerChunkZ + verticalRenderDistanceChunks
        val dimensionKey = MapTextureManager.dimensionKey

        val isUnderground = player.let { hyperMap.isUnderground(it.blockY) }
        val actualMode = if (isUnderground) HyperMap.Mode.Solid else hyperMap.mode.value

        val textureFileName =
            when (actualMode) {
                HyperMap.Mode.Flat -> {
                    "surface.png"
                }

                HyperMap.Mode.Solid -> {
                    val sectionY = (player.blockY / 16) * 16
                    "section_$sectionY.png"
                }
            }

        // 画面の中心から端までのピクセル距離 (X, Zそれぞれ)
        val halfRenderWidth = renderWidth / 2.0
        val halfRenderHeight = renderHeight / 2.0

        for (chunkX in minChunkX..maxChunkX) {
            for (chunkZ in minChunkZ..maxChunkZ) {
                val chunkWorldCenterX = chunkX * 16 + 8.0
                val chunkWorldCenterZ = chunkZ * 16 + 8.0

                val dx = (chunkWorldCenterX - centerX)
                val dz = (chunkWorldCenterZ - centerZ)

                // ワールド座標を画面座標にスケール (X/Z方向で異なるスケールを使用)
                val scaledDx = dx / mapWorldWidth * halfRenderWidth
                val scaledDz = dz / mapWorldHeight * halfRenderHeight

                // チャンクの描画サイズも、ワールドの幅・高さと対応する画面の幅・高さで計算
                val chunkRenderWidth = (16.0 / mapWorldWidth * halfRenderWidth).toFloat()
                val chunkRenderHeight = (16.0 / mapWorldHeight * halfRenderHeight).toFloat()

                // 描画位置は画面の中心 + スケールされたオフセット - チャンクサイズの半分
                val drawX = (halfRenderWidth + scaledDx - chunkRenderWidth / 2.0).toFloat()
                val drawY = (halfRenderHeight + scaledDz - chunkRenderHeight / 2.0).toFloat()

                var chunkIdentifier =
                    MapTextureManager.getChunkTextureIdentifier(chunkX, chunkZ, dimensionKey, textureFileName)

                if (chunkIdentifier == null) {
                    chunkIdentifier =
                        MapTextureManager.loadAndRegisterTextureFromFile(
                            chunkX,
                            chunkZ,
                            dimensionKey,
                            textureFileName,
                        )
                }

                if (chunkIdentifier != null) {
                    graphics2D.drawRotatedTexture(
                        chunkIdentifier,
                        drawX,
                        drawY,
                        chunkRenderWidth,
                        chunkRenderHeight,
                        0f,
                    )
                } else {
                    graphics2D.fill(
                        drawX.toInt(),
                        drawY.toInt(),
                        chunkRenderWidth.toInt(),
                        chunkRenderHeight.toInt(),
                        0xAA333333.toInt(), // 濃い灰色
                    )
                }
            }
        }
    }

    // --- renderEntitiesFullScreen の修正: 画面サイズとワールド半径を引数として受け取り描画 ---
    private fun renderEntitiesFullScreen(
        graphics2D: Graphics2D,
        renderWidth: Int,
        renderHeight: Int,
        mapWorldWidth: Double, // X方向のワールド表示半径
        mapWorldHeight: Double, // Z方向のワールド表示半径
    ) {
        val client = Minecraft.getInstance()
        val player = client.player ?: return

        val mobDotRadius = 2

        val screenCenterX = renderWidth / 2
        val screenCenterY = renderHeight / 2

        for (mob in hyperMap.nearbyMobs) {
            val dx = (mob.x - centerX)
            val dz = (mob.z - centerZ)

            // ワールド座標を画面座標にスケール
            val scaledDx = dx / mapWorldWidth * (renderWidth / 2.0)
            val scaledDz = dz / mapWorldHeight * (renderHeight / 2.0)

            val mobRenderX = (screenCenterX + scaledDx).toInt()
            val mobRenderY = (screenCenterY + scaledDz).toInt()

            val baseColor = (HyperMapRenderer.getBaseDotColor(mob))
            val relativeHeight = mob.y - player.y
            val maxBlendFactor = 0.5
            val blendFactor =
                (kotlin.math.abs(relativeHeight) / hyperMap.heightSetting.value).coerceIn(0.0, maxBlendFactor).toFloat()
            val blendedColor =
                when {
                    relativeHeight > 0 -> {
                        ARGB.srgbLerp(
                            blendFactor,
                            baseColor,
                            0xFFFFFFFF.toInt(),
                        )
                    }

                    relativeHeight < 0 -> {
                        ARGB.srgbLerp(
                            blendFactor,
                            baseColor,
                            0xFF000000.toInt(),
                        )
                    }

                    else -> {
                        baseColor
                    }
                }

            val alpha = HyperMapRenderer.getAlphaBasedOnHeight(mob, player.y, hyperMap.heightSetting.value)
            val finalDotColor = blendedColor.transparent(alpha)

            graphics2D.fill(
                mobRenderX - mobDotRadius,
                mobRenderY - mobDotRadius,
                mobDotRadius * 2,
                mobDotRadius * 2,
                finalDotColor,
            )
        }
    }
}
