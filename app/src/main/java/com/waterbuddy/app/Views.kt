package com.waterbuddy.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.View
import java.util.Locale

object Pal {
    val BG = Color.parseColor("#120A1F")
    val SURFACE = Color.parseColor("#1E1233")
    val CARD = Color.parseColor("#2A1B47")
    val PRIMARY = Color.parseColor("#9B5DE5")
    val ACCENT = Color.parseColor("#C77DFF")
    val TEXT = Color.parseColor("#EDE7F6")
    val SUB = Color.parseColor("#B39DDB")
    val GOOD = Color.parseColor("#7BE0AD")
    val WARN = Color.parseColor("#FFB86B")
    val TRACK = Color.parseColor("#3A2A5C")
}

/** Gol progress ring: kitne glass piye / goal. */
class WaterRingView(c: Context) : View(c) {
    private var value = 0
    private var goal = 8
    private val d = c.resources.displayMetrics.density

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Pal.TRACK; strokeWidth = 18 * d
    }
    private val prog = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = Pal.ACCENT; strokeWidth = 18 * d; strokeCap = Paint.Cap.ROUND
    }
    private val big = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Pal.TEXT; textAlign = Paint.Align.CENTER; textSize = 64 * d
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Pal.SUB; textAlign = Paint.Align.CENTER; textSize = 16 * d
    }

    fun set(v: Int, g: Int) {
        value = v; goal = g; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val size = minOf(width, height) - 40 * d
        val r = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        canvas.drawArc(r, -90f, 360f, false, track)
        val frac = (value.toFloat() / goal).coerceIn(0f, 1f)
        if (frac > 0f) canvas.drawArc(r, -90f, 360f * frac, false, prog)
        canvas.drawText(value.toString(), cx, cy + 14 * d, big)
        canvas.drawText("/ $goal glass", cx, cy + 44 * d, small)
        val pct = (value * 100f / goal).toInt()
        canvas.drawText("$pct%", cx, cy - 56 * d, small)
    }
}

/** Seedha bar chart: har bar = ek din (ya mahina), dashed line = goal. */
class BarChartView(c: Context) : View(c) {
    private var values = FloatArray(0)
    private var labels: List<String> = emptyList()
    private var goal = 8f
    private var decimals = false
    private val d = c.resources.displayMetrics.density

    private val bar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = Pal.SUB; textSize = 11 * d
    }
    private val valTxt = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER; color = Pal.TEXT; textSize = 11 * d
    }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Pal.WARN; style = Paint.Style.STROKE; strokeWidth = 1.5f * d
        pathEffect = DashPathEffect(floatArrayOf(8 * d, 6 * d), 0f)
    }

    fun set(v: FloatArray, l: List<String>, g: Float, dec: Boolean) {
        values = v; labels = l; goal = g; decimals = dec; invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val n = values.size
        if (n == 0) return
        val left = 6 * d
        val right = width - 6 * d
        val top = 20 * d
        val bottom = height - 24 * d
        val maxV = maxOf(goal * 1.15f, values.max() * 1.1f, 1f)
        val slot = (right - left) / n
        val bw = slot * 0.62f
        val step = if (n > 14) 5 else 1

        for (i in 0 until n) {
            val v = values[i]
            val h = if (v <= 0f) 3 * d else (bottom - top) * v / maxV
            val l = left + slot * i + (slot - bw) / 2
            bar.color = when {
                v <= 0f -> Pal.TRACK
                v >= goal -> Pal.GOOD
                else -> Pal.PRIMARY
            }
            canvas.drawRoundRect(RectF(l, bottom - h, l + bw, bottom), 4 * d, 4 * d, bar)
            if (n <= 12 && v > 0f) {
                val s = if (decimals) String.format(Locale.US, "%.1f", v) else v.toInt().toString()
                canvas.drawText(s, l + bw / 2, bottom - h - 4 * d, valTxt)
            }
            if ((n - 1 - i) % step == 0) {
                canvas.drawText(labels.getOrElse(i) { "" }, l + bw / 2, height - 6 * d, txt)
            }
        }

        val gy = bottom - (bottom - top) * goal / maxV
        val p = Path()
        p.moveTo(left, gy)
        p.lineTo(right, gy)
        canvas.drawPath(p, line)
    }
}
