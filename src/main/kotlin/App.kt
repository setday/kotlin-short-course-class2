import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.swing.Swing
import kotlinx.datetime.*
import org.jetbrains.skija.*
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkiaRenderer
import org.jetbrains.skiko.SkiaWindow
import java.awt.Dimension
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.lang.String.format
import javax.swing.WindowConstants
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.time.ExperimentalTime

fun main() {
    createWindow("Klock")
}

fun createWindow(title: String) = runBlocking(Dispatchers.Swing) {
    val window = SkiaWindow()
    window.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
    window.title = title

    window.layer.renderer = Renderer(window.layer)
    window.layer.addMouseMotionListener(MouseMotionAdapter)

    window.preferredSize = Dimension(800, 600)
    window.minimumSize = Dimension(100,100)
    window.pack()
    window.layer.awaitRedraw()
    window.isVisible = true
}

class Renderer(val layer: SkiaLayer): SkiaRenderer {
    val typeface = Typeface.makeFromFile("fonts/JetBrainsMono-Regular.ttf")
    val font = Font(typeface, 40f)
    val paint = Paint().apply {
        color = 0xff9BC730L.toInt()
        mode = PaintMode.FILL
        strokeWidth = 1f
    }
    val clockFill = Paint().apply {
        color = 0xFFFFFFFF.toInt()
    }
    val clockFillHover = Paint().apply {
        color = 0xFFE4FF01.toInt()
    }
    val clockStroke = Paint().apply {
        color = 0xFF000000.toInt()
        mode = PaintMode.STROKE
        strokeWidth = 1f
    }
    val clockStrokeS = Paint().apply {
        color = 0xFFFF0000.toInt()
        mode = PaintMode.STROKE
        strokeWidth = 1f
    }
    val clockStrokeMH = Paint().apply {
        color = 0xFF0000FF.toInt()
        mode = PaintMode.STROKE
        strokeWidth = 3f
    }

    @ExperimentalTime
    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        val contentScale = layer.contentScale
        canvas.scale(contentScale, contentScale)
        val w = (width / contentScale).toInt()
        val h = (height / contentScale).toInt()

        val centerX = w/2f
        val centerY = h/2f
        val clockRadius = min(w, h)/2f - 5
        val tickLen = 15f

        displayClockFace(canvas, centerX, centerY, clockRadius, tickLen)

        val now = Clock.System.now()
        val timeZone = TimeZone.currentSystemDefault()
        val midnight = Clock.System.todayAt(timeZone).atStartOfDayIn(timeZone)
        val msTime = (now - midnight).inWholeMilliseconds

        displayClockHands(canvas, centerX, centerY, clockRadius, tickLen, msTime)

        displayTime(canvas, now.toLocalDateTime(timeZone))

        layer.needRedraw()
    }

    private fun displayTime(canvas: Canvas, localDateTime: LocalDateTime) {
        val text = format("%02d:%02d:%02d", localDateTime.hour, localDateTime.minute, localDateTime.second)

        canvas.drawString(text, State.mouseX, State.mouseY, font, paint)
    }

    private fun displayClockFace(canvas: Canvas, centerX: Float, centerY: Float, clockRadius: Float, tickLen: Float) {

        val x = centerX - clockRadius
        val y = centerY - clockRadius

        val hover = distanceSq(centerX, centerY, State.mouseX, State.mouseY) <= clockRadius*clockRadius

        val fill = if (hover) clockFillHover else clockFill

        val clockRect = Rect.makeXYWH(x, y, clockRadius * 2, clockRadius * 2)
        canvas.drawOval(clockRect, fill)
        canvas.drawOval(clockRect, clockStroke)
        clockTicks(canvas, clockStroke, centerX, tickLen/3, centerY, clockRadius, 60)
        clockTicks(canvas, clockStroke, centerX, tickLen, centerY, clockRadius, 12)
    }

    private fun displayClockHands(canvas: Canvas, centerX: Float, centerY: Float, clockRadius: Float, tickLen: Float,
                                  msTime: Long) {

        val secShare = msTime/60000f
        // Секундная стрелка
        clockHand(canvas, clockStrokeS, centerX, centerY, secShare, clockRadius - tickLen)
        // Минутная стрелка
        clockHand(canvas, clockStrokeMH, centerX, centerY, secShare/60, clockRadius - tickLen)
        // Часовая стрелка
        clockHand(canvas, clockStrokeMH, centerX, centerY, secShare/60/12, (clockRadius - tickLen)/2)
    }

    private fun clockTicks(
        canvas: Canvas,
        clockStroke: Paint,
        centerX: Float,
        tickLen: Float,
        centerY: Float,
        clockRadius: Float,
        qty: Int
    ) {
        var angle = 0f
        while (angle < 2f * Math.PI) {
            canvas.drawLine(
                (centerX + (clockRadius - tickLen) * cos(angle)),
                (centerY - (clockRadius - tickLen) * sin(angle)),
                (centerX + clockRadius * cos(angle)),
                (centerY - clockRadius * sin(angle)),
                clockStroke
            )
            angle += (2.0 * Math.PI / qty).toFloat()
        }
    }

    private fun clockHand(canvas: Canvas, stroke: Paint, centerX: Float, centerY: Float, clockShare: Float, length: Float) {
        val angle = ((0.5 - clockShare * 2) * Math.PI).toFloat()
        canvas.drawLine(centerX, centerY,
            centerX + length * cos(angle),
            centerY - length * sin(angle),
            stroke)
    }
}

object State {
    var mouseX = 0f
    var mouseY = 0f
}

object MouseMotionAdapter : MouseMotionAdapter() {
    override fun mouseMoved(event: MouseEvent) {
        State.mouseX = event.x.toFloat()
        State.mouseY = event.y.toFloat()
    }
}

fun distanceSq(x1: Float, y1: Float, x2: Float, y2: Float) = (x1-x2)*(x1-x2) + (y1-y2)*(y1-y2)