import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.*
import java.awt.event.KeyAdapter
import java.awt.event.MouseAdapter
import java.awt.event.MouseMotionAdapter
import java.lang.System.exit
import javax.swing.WindowConstants
import kotlin.time.ExperimentalTime

val Frame = Array<Array<Paint>>(400) { Array<Paint>(400) { Paint() } }
val FrameRect = Array<Array<Rect>>(400) { a -> Array<Rect>(400) { b -> Rect(4f * b, 4f * a, 4f * (b + 1), 4f * (a + 1)) } }

data class Menu(var rect: Rect, var tilesNum: Int)

fun responseMenu(menu: Menu, foo: (it: Int) -> Unit) {
    if (State.newclick) {
        if (State.clickedY - menu.rect.top < 0 || State.clickedY - menu.rect.bottom > 0)
            return
        if (State.clickedX - menu.rect.left < 0 || State.clickedX - menu.rect.right > 0)
            return
        State.newclick = false
        foo(((State.clickedX - menu.rect.left) / menu.tilesNum).toInt())
    }
}

fun drawMenu(paint: Paint, menu: Menu, canvas: Canvas, foo: (it: Int, rect: Rect) -> Unit) {
    canvas.drawRect(menu.rect, paint)
    var left = menu.rect.left
    repeat(menu.tilesNum) {
        val tileRect = Rect.makeXYWH(left, menu.rect.top, menu.rect.height, menu.rect.height)
        canvas.drawRect(tileRect, paint)
        foo(it, tileRect)
        left += menu.rect.height
    }
}

fun main() {
    createWindow("Paint")
}

fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()

    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseListener(MouseAdapter)
    window.layer.addMouseMotionListener(MouseMotionAdapter)
    window.layer.addKeyListener(KeyAdapter)

    window.preferredSize = Dimension(400, 400)
    window.minimumSize = Dimension(100,100)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}

class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 20f)
    var paint = Paint().apply {
        color = 0xFFFFFFFFL.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val paintMenu = Paint().apply {
        color = 0xFF9BC730L.toInt()
        mode = PaintMode.STROKE
        strokeWidth = 1f
    }
    val paintBack = Paint().apply {
        color = 0xFF000000L.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val menu = Menu(Rect.makeXYWH(0f, 0f, 300f, 50f), 2)

    @ExperimentalTime
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        val w = (width / contentScale).toInt()
        val h = (height / contentScale).toInt()

        if (State.pressed) {
            Frame[(State.pressedY / 4).toInt()][(State.pressedX / 4).toInt()] = paint
        }

        for (i in 0 until 400) {
            for (j in 0 until 400) {
                canvas.drawRect(FrameRect[i][j], Frame[i][j])
            }
        }

        drawMenu(paintMenu, menu, canvas) { it: Int, rect: Rect -> { } }
        //responseMenu(menu) { it: Int -> { if(it == 1) { paint = Paint().apply {
        //    color = 0xFFFFFFFFL.toInt()
        //    mode = PaintMode.FILL
        //    strokeWidth = 1f
        //} } else {
        //    paint = Paint().apply {
        //        color = 0xFF000000L.toInt()
        //        mode = PaintMode.FILL
        //        strokeWidth = 1f
        //    } } } }

        layer.needRedraw()
    }
}

object State {
    var mouseX = 0f
    var mouseY = 0f
    var clickedX = 0f
    var clickedY = 0f
    var newclick = false
    var pressedX = 0f
    var pressedY = 0f
    var pressed = false
}

object MouseAdapter : MouseAdapter() {
    override fun mouseClicked(event: MouseEvent) {
        State.newclick = true
        State.pressedX = event.x.toFloat()
        State.pressedY = event.y.toFloat()
    }

    override fun mousePressed(event: MouseEvent) {
        State.pressed = true
    }

    override fun mouseReleased(event: MouseEvent) {
        State.pressed = false
    }
}

object MouseMotionAdapter : MouseMotionAdapter() {
    override fun mouseMoved(event: MouseEvent) {
        State.mouseX = event.x.toFloat()
        State.mouseY = event.y.toFloat()
    }

    override fun mouseDragged(event: MouseEvent) {
        State.mouseX = event.x.toFloat()
        State.mouseY = event.y.toFloat()
        State.pressedX = event.x.toFloat()
        State.pressedY = event.y.toFloat()
    }
}

object KeyAdapter : KeyAdapter() {
    override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == 27)
            exit(0)
    }
}
