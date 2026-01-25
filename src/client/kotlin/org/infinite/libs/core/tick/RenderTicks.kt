package org.infinite.libs.core.tick

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.resource.GraphicsResourceAllocator
import kotlinx.coroutines.runBlocking
import net.minecraft.client.Camera
import net.minecraft.client.DeltaTracker
import net.minecraft.client.gui.GuiGraphics
import org.infinite.InfiniteClient
import org.infinite.libs.graphics.graphics2d.RenderSystem2D
import org.infinite.libs.graphics.graphics3d.RenderSystem3D
import org.infinite.libs.graphics.system.ProjectionData
import org.infinite.libs.interfaces.MinecraftInterface
import org.infinite.libs.minecraft.aim.AimSystem
import org.joml.Matrix4f
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
        guiGraphics: GuiGraphics,
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
        guiGraphics: GuiGraphics,
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
        camera: Camera,
        positionMatrix: Matrix4f,
        projectionMatrix: Matrix4f,
    ) {
        val client = net.minecraft.client.Minecraft.getInstance()
        _latestProjectionData = ProjectionData(
            cameraPos = camera.position(),
            modelViewMatrix = Matrix4f(positionMatrix),
            projectionMatrix = Matrix4f(projectionMatrix),
            scaledWidth = client.window.guiScaledWidth,
            scaledHeight = client.window.guiScaledHeight,
        )
    }

    fun onLevelRendering(
        graphicsResourceAllocator: GraphicsResourceAllocator,
        deltaTracker: DeltaTracker,
        renderBlockOutline: Boolean,
        camera: Camera,
        positionMatrix: Matrix4f,
        projectionMatrix: Matrix4f,
        frustumMatrix: Matrix4f,
        gpuBufferSlice: GpuBufferSlice,
        vector4f: Vector4f,
        bl2: Boolean,
    ) {
        updateProjectionData(
            camera,
            positionMatrix,
            projectionMatrix,
        )
        val renderSystem3D = RenderSystem3D(
            graphicsResourceAllocator,
            deltaTracker,
            renderBlockOutline,
            camera,
            positionMatrix,
            projectionMatrix,
            frustumMatrix,
            gpuBufferSlice,
            vector4f, bl2,
        )
        _renderSnapShot = renderSystem3D.snapShot()
        val commands =
            runBlocking {
                return@runBlocking InfiniteClient.localFeatures.onLevelRendering()
            }
        renderSystem3D.render(commands)
    }
}
