package org.infinite.libs.graphics.graphics2d.structs

import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Block
import org.joml.Matrix3x2f
import org.joml.Vector3f

/**
 * 外側からは sealed interface として振る舞い、内部ではプールから再利用される描画コマンド。
 */
sealed interface RenderCommand2D {

    class FillRect : RenderCommand2D {
        var x = 0f
        var y = 0f
        var width = 0f
        var height = 0f
        var col0 = 0
        var col1 = 0
        var col2 = 0
        var col3 = 0

        fun set(x: Float, y: Float, w: Float, h: Float, c0: Int, c1: Int, c2: Int, c3: Int): FillRect {
            this.x = x
            this.y = y
            this.width = w
            this.height = h
            this.col0 = c0
            this.col1 = c1
            this.col2 = c2
            this.col3 = c3
            return this
        }
    }

    class FillQuad : RenderCommand2D {
        var x0 = 0f
        var y0 = 0f
        var x1 = 0f
        var y1 = 0f
        var x2 = 0f
        var y2 = 0f
        var x3 = 0f
        var y3 = 0f
        var col0 = 0
        var col1 = 0
        var col2 = 0
        var col3 = 0

        fun set(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, c0: Int, c1: Int, c2: Int, c3: Int): FillQuad {
            this.x0 = x0
            this.y0 = y0
            this.x1 = x1
            this.y1 = y1
            this.x2 = x2
            this.y2 = y2
            this.x3 = x3
            this.y3 = y3
            this.col0 = c0
            this.col1 = c1
            this.col2 = c2
            this.col3 = c3
            return this
        }
    }

    class FillTriangle : RenderCommand2D {
        var x0 = 0f
        var y0 = 0f
        var x1 = 0f
        var y1 = 0f
        var x2 = 0f
        var y2 = 0f
        var col0 = 0
        var col1 = 0
        var col2 = 0

        fun set(x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, c0: Int, c1: Int, c2: Int): FillTriangle {
            this.x0 = x0
            this.y0 = y0
            this.x1 = x1
            this.y1 = y1
            this.x2 = x2
            this.y2 = y2
            this.col0 = c0
            this.col1 = c1
            this.col2 = c2
            return this
        }
    }

    // Text系の重複を抽象化
    abstract class AbstractText : RenderCommand2D {
        var font = ""
        var text = ""
        var x = 0f
        var y = 0f
        var color = 0
        var shadow = false
        var size = 0f

        fun set(font: String, text: String, x: Float, y: Float, color: Int, shadow: Boolean, size: Float): AbstractText {
            this.font = font
            this.text = text
            this.x = x
            this.y = y
            this.color = color
            this.shadow = shadow
            this.size = size
            return this
        }
    }

    class Text : AbstractText()
    class TextCentered : AbstractText()
    class TextRight : AbstractText()

    // 変形系
    class Translate : RenderCommand2D {
        var x = 0f
        var y = 0f
        fun set(x: Float, y: Float): Translate {
            this.x = x
            this.y = y
            return this
        }
    }

    class Rotate : RenderCommand2D {
        var angle = 0f
        fun set(angle: Float): Rotate {
            this.angle = angle
            return this
        }
    }

    class Scale : RenderCommand2D {
        var x = 0f
        var y = 0f
        fun set(x: Float, y: Float): Scale {
            this.x = x
            this.y = y
            return this
        }
    }

    class Transform : RenderCommand2D {
        val vec = Vector3f()
        fun set(x: Float, y: Float, z: Float): Transform {
            vec.set(x, y, z)
            return this
        }
    }

    class SetTransform : RenderCommand2D {
        val matrix = Matrix3x2f()
        fun set(m00: Float, m10: Float, m01: Float, m11: Float, m02: Float, m12: Float): SetTransform {
            matrix.set(m00, m10, m01, m11, m02, m12)
            return this
        }
    }

    class EnableScissor : RenderCommand2D {
        var x = 0
        var y = 0
        var width = 0
        var height = 0
        fun set(x: Int, y: Int, w: Int, h: Int): EnableScissor {
            this.x = x
            this.y = y
            this.width = w
            this.height = h
            return this
        }
    }

    class DrawTexture : RenderCommand2D {
        var image: Image? = null
        var x = 0f
        var y = 0f
        var width = 0f
        var height = 0f
        var u = 0
        var v = 0
        var uWidth = 0
        var vHeight = 0
        var color = 0

        fun set(image: Image?, x: Float, y: Float, w: Float, h: Float, u: Int, v: Int, uw: Int, vh: Int, color: Int): DrawTexture {
            this.image = image
            this.x = x
            this.y = y
            this.width = w
            this.height = h
            this.u = u
            this.v = v
            this.uWidth = uw
            this.vHeight = vh
            this.color = color
            return this
        }
    }

    class RenderItem : RenderCommand2D {
        var stack: ItemStack = ItemStack.EMPTY
        var x = 0f
        var y = 0f
        var scale = 0f
        var alpha = 0f

        fun set(stack: ItemStack, x: Float, y: Float, scale: Float, alpha: Float): RenderItem {
            this.stack = stack
            this.x = x
            this.y = y
            this.scale = scale
            this.alpha = alpha
            return this
        }
    }

    class RenderBlock : RenderCommand2D {
        var block: Block? = null
        var x = 0f
        var y = 0f
        var size = 0f

        fun set(block: Block?, x: Float, y: Float, size: Float): RenderBlock {
            this.block = block
            this.x = x
            this.y = y
            this.size = size
            return this
        }
    }

    // Singleton (object) 群
    object PushTransform : RenderCommand2D
    object PopTransform : RenderCommand2D
    object ResetTransform : RenderCommand2D
    object DisableScissor : RenderCommand2D
}
