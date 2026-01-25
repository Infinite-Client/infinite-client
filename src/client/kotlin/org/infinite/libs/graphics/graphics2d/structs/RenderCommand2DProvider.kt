package org.infinite.libs.graphics.graphics2d.structs

/**
 * 各コマンド型ごとのプールを管理し、一元的なリストとして提供するプロバイダー。
 * Rustの Vec<RenderCommand2D> のような役割。
 */
class RenderCommand2DProvider {
    private val fillRectPool = mutableListOf<RenderCommand2D.FillRect>()
    private val fillQuadPool = mutableListOf<RenderCommand2D.FillQuad>()
    private val fillTrianglePool = mutableListOf<RenderCommand2D.FillTriangle>()
    private val textPool = mutableListOf<RenderCommand2D.Text>()
    private val textCenteredPool = mutableListOf<RenderCommand2D.TextCentered>()
    private val textRightPool = mutableListOf<RenderCommand2D.TextRight>()
    private val translatePool = mutableListOf<RenderCommand2D.Translate>()
    private val rotatePool = mutableListOf<RenderCommand2D.Rotate>()
    private val scalePool = mutableListOf<RenderCommand2D.Scale>()
    private val transformPool = mutableListOf<RenderCommand2D.Transform>()
    private val setTransformPool = mutableListOf<RenderCommand2D.SetTransform>()
    private val enableScissorPool = mutableListOf<RenderCommand2D.EnableScissor>()
    private val drawTexturePool = mutableListOf<RenderCommand2D.DrawTexture>()
    private val renderItemPool = mutableListOf<RenderCommand2D.RenderItem>()
    private val renderBlockPool = mutableListOf<RenderCommand2D.RenderBlock>()

    private val activeCommands = ArrayList<RenderCommand2D>(1024)

    // インスタンス取得用カウンタ
    private var frIdx = 0
    private var fqIdx = 0
    private var ftIdx = 0
    private var tIdx = 0
    private var tcIdx = 0
    private var trIdx = 0
    private var transIdx = 0
    private var rotIdx = 0
    private var scIdx = 0
    private var tfIdx = 0
    private var stIdx = 0
    private var esIdx = 0
    private var dtIdx = 0
    private var riIdx = 0
    private var rbIdx = 0

    fun getFillRect(): RenderCommand2D.FillRect = getOrAdd(fillRectPool, { RenderCommand2D.FillRect() }, frIdx++)

    fun getFillQuad(): RenderCommand2D.FillQuad = getOrAdd(fillQuadPool, { RenderCommand2D.FillQuad() }, fqIdx++)

    fun getFillTriangle(): RenderCommand2D.FillTriangle = getOrAdd(fillTrianglePool, { RenderCommand2D.FillTriangle() }, ftIdx++)

    fun getText(): RenderCommand2D.Text = getOrAdd(textPool, { RenderCommand2D.Text() }, tIdx++)

    fun getTextCentered(): RenderCommand2D.TextCentered = getOrAdd(textCenteredPool, { RenderCommand2D.TextCentered() }, tcIdx++)

    fun getTextRight(): RenderCommand2D.TextRight = getOrAdd(textRightPool, { RenderCommand2D.TextRight() }, trIdx++)

    fun getTranslate(): RenderCommand2D.Translate = getOrAdd(translatePool, { RenderCommand2D.Translate() }, transIdx++)

    fun getRotate(): RenderCommand2D.Rotate = getOrAdd(rotatePool, { RenderCommand2D.Rotate() }, rotIdx++)

    fun getScale(): RenderCommand2D.Scale = getOrAdd(scalePool, { RenderCommand2D.Scale() }, scIdx++)

    fun getTransform(): RenderCommand2D.Transform = getOrAdd(transformPool, { RenderCommand2D.Transform() }, tfIdx++)

    fun getSetTransform(): RenderCommand2D.SetTransform = getOrAdd(setTransformPool, { RenderCommand2D.SetTransform() }, stIdx++)

    fun getEnableScissor(): RenderCommand2D.EnableScissor = getOrAdd(enableScissorPool, { RenderCommand2D.EnableScissor() }, esIdx++)

    fun getDrawTexture(): RenderCommand2D.DrawTexture = getOrAdd(drawTexturePool, { RenderCommand2D.DrawTexture() }, dtIdx++)

    fun getRenderItem(): RenderCommand2D.RenderItem = getOrAdd(renderItemPool, { RenderCommand2D.RenderItem() }, riIdx++)

    fun getRenderBlock(): RenderCommand2D.RenderBlock = getOrAdd(renderBlockPool, { RenderCommand2D.RenderBlock() }, rbIdx++)

    fun addStatic(cmd: RenderCommand2D) {
        activeCommands.add(cmd)
    }

    private inline fun <reified T : RenderCommand2D> getOrAdd(
        pool: MutableList<T>,
        factory: () -> T,
        index: Int,
    ): T {
        val instance = if (index < pool.size) pool[index] else factory().also { pool.add(it) }
        activeCommands.add(instance)
        return instance
    }

    fun clear() {
        activeCommands.clear()
        frIdx = 0
        fqIdx = 0
        ftIdx = 0
        tIdx = 0
        tcIdx = 0
        trIdx = 0
        transIdx = 0
        rotIdx = 0
        scIdx = 0
        tfIdx = 0
        stIdx = 0
        esIdx = 0
        dtIdx = 0
        riIdx = 0
        rbIdx = 0
    }

    fun commands(): List<RenderCommand2D> = activeCommands
}
