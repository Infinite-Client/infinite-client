package org.infinite.libs.core.tick

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.resource.GraphicsResourceAllocator
import kotlinx.coroutines.runBlocking
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.chunk.ChunkSectionsToRender
import net.minecraft.client.renderer.state.level.CameraRenderState
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.graphics2d.RenderSystem2D
import org.infinite.libs.graphics.graphics3d.RenderSystem3D
import org.infinite.libs.graphics.system.ProjectionData
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.aim.AimSystem
import org.joml.Matrix4f
import org.joml.Matrix4fc
import org.joml.Vector4f

object RenderTicks : MinecraftInterface() {
    @Volatile
    private var _latestProjectionData: ProjectionData? = null
    val latestProjectionData: ProjectionData? get() = _latestProjectionData

    @Volatile
    private var _renderSnapShot: RenderSystem3D.RenderSnapshot? = null
    val renderSnapShot: RenderSystem3D.RenderSnapshot? get() = _renderSnapShot
    var fps: Float = 30f
        private set
    private var lastTime: Long = System.currentTimeMillis()
    private fun updateFps() {
        val currentTime = System.currentTimeMillis()
        val msDelta = currentTime - lastTime
        fps = 1000f / msDelta
        lastTime = currentTime
    }

    fun onStartUiRendering(
        guiGraphics: GuiGraphicsExtractor,
        deltaTracker: DeltaTracker,
    ) {
        updateFps()
        aimSystem()
        val commands =
            runBlocking {
                return@runBlocking InfiniteClient.localFeatures.onStartUiRendering(deltaTracker)
            }
        val renderSystem2D = RenderSystem2D(guiGraphics)
        renderSystem2D.render(commands)
    }

    private fun aimSystem() {
        if (!minecraft.isPaused) {
            AimSystem.process()
        }
    }

    fun onEndUiRendering(
        guiGraphics: GuiGraphicsExtractor,
        deltaTracker: DeltaTracker,
    ) {
        val commands =
            runBlocking {
                return@runBlocking InfiniteClient.localFeatures.onEndUiRendering(deltaTracker)
            }
        val renderSystem2D = RenderSystem2D(guiGraphics)
        renderSystem2D.render(commands)
    }

    private fun updateProjectionData(
        camera: CameraRenderState,
        modelViewMatrix4fc: Matrix4fc,
    ) {
        val modelViewMatrix = Matrix4f(modelViewMatrix4fc)
        val client = net.minecraft.client.Minecraft.getInstance()
        _latestProjectionData = ProjectionData(
            cameraPos = camera.pos,
            modelViewMatrix = modelViewMatrix,
            projectionMatrix = camera.projectionMatrix,
            scaledWidth = client.window.guiScaledWidth,
            scaledHeight = client.window.guiScaledHeight,
        )
    }
    fun onLevelRendering(
        resourceAllocator: GraphicsResourceAllocator,
        deltaTracker: DeltaTracker,
        renderOutline: Boolean,
        cameraState: CameraRenderState, // 新しいステート
        modelViewMatrix: Matrix4fc, // Matrix4fcに変更
        terrainFog: GpuBufferSlice,
        fogColor: Vector4f,
        shouldRenderSky: Boolean,
        chunkSectionsToRender: ChunkSectionsToRender,
    ) {
        updateProjectionData(cameraState, modelViewMatrix)
        // 4. RenderSystem3Dの初期化
        // ※RenderSystem3Dのコンストラクタもこれに合わせて修正が必要です
        val renderSystem3D = RenderSystem3D(
            resourceAllocator,
            deltaTracker,
            renderOutline,
            cameraState,
            latestProjectionData!!.modelViewMatrix,
            latestProjectionData!!.projectionMatrix,
            terrainFog,
            fogColor,
            shouldRenderSky,
            chunkSectionsToRender,
        )

        _renderSnapShot = renderSystem3D.snapShot()

        // 5. 特徴量（Features）の実行
        val commands = runBlocking {
            return@runBlocking InfiniteClient.localFeatures.onLevelRendering()
        }

        renderSystem3D.render(commands)
    }
}
