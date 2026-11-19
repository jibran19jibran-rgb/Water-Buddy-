package com.waterbuddy.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var content: LinearLayout
    private lateinit var nav: LinearLayout
    private var tab = 0
    private var range = 0 // 0 = hafta, 1 = mahina, 2 = saal

    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    // ------------------------------------------------------------------ lifecycle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Pal.BG
        window.navigationBarColor = Pal.SURFACE

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setBackgroundColor(Pal.BG)

        val scroll = ScrollView(this)
        content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(20), dp(16), dp(24))
        scroll.addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
        root.addView(scroll, LinearLayout.LayoutParams(MATCH, 0, 1f))

        nav = LinearLayout(this)
        nav.orientation = LinearLayout.HORIZONTAL
        nav.setBackgroundColor(Pal.SURFACE)
        val names = listOf("🏠  Home", "📊  History", "⚙️  Settings")
        for (i in names.indices) {
            val t = TextView(this)
            t.text = names[i]
            t.gravity = Gravity.CENTER
            t.textSize = 14f
            t.setPadding(0, dp(16), 0, dp(16))
            t.setOnClickListener { tab = i; render() }
            nav.addView(t, LinearLayout.LayoutParams(0, WRAP, 1f))
        }
        root.addView(nav, LinearLayout.LayoutParams(MATCH, WRAP))
        setContentView(root)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        Scheduler.schedule(this)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        content.removeAllViews()
        when (tab) {
            0 -> home()
            1 -> history()
            else -> settings()
        }
        for (i in 0 until nav.childCount) {
            val t = nav.getChildAt(i) as TextView
            t.setTextColor(if (i == tab) Pal.ACCENT else Pal.SUB)
            t.setTypeface(null, if (i == tab) Typeface.BOLD else Typeface.NORMAL)
        }
    }

    private fun afterChange() {
        Scheduler.schedule(this)
        render()
    }

    // ------------------------------------------------------------------ small UI helpers

    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun bg(color: Int, r: Int = 16): GradientDrawable {
        val g = GradientDrawable()
        g.setColor(color)
        g.cornerRadius = dp(r).toFloat()
        return g
    }

    private fun tv(s: String, size: Float = 15f, color: Int = Pal.TEXT, bold: Boolean = false): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(null, Typeface.BOLD)
        return t
    }

    private fun LinearLayout.put(v: View, top: Int = 12, w: Int = MATCH, h: Int = WRAP): View {
        val lp = LinearLayout.LayoutParams(w, h)
        lp.topMargin = dp(top)
        addView(v, lp)
        return v
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = bg(Pal.CARD)
        c.setPadding(dp(16), dp(14), dp(16), dp(14))
        return c
    }

    private fun button(label: String, primary: Boolean = true, onClick: () -> Unit): TextView {
        val t = TextView(this)
        t.text = label
        t.textSize = 16f
        t.gravity = Gravity.CENTER
        t.setTextColor(if (primary) Color.WHITE else Pal.ACCENT)
        t.setTypeface(null, Typeface.BOLD)
        t.setPadding(dp(12), dp(14), dp(12), dp(14))
        t.background = bg(if (primary) Pal.PRIMARY else Pal.CARD, 14)
        t.setOnClickListener { onClick() }
        return t
    }

    private fun hRow(): LinearLayout {
        val r = LinearLayout(this)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        return r
    }

    private fun styledSwitch(checked: Boolean, onChange: (Boolean) -> Unit): Switch {
        val sw = Switch(this)
        sw.isChecked = checked
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        sw.thumbTintList = ColorStateList(states, intArrayOf(Pal.ACCENT, Color.parseColor("#8E7DAA")))
        sw.trackTintList = ColorStateList(states, intArrayOf(Color.parseColor("#5B3A99"), Pal.TRACK))
        sw.setOnCheckedChangeListener { _, on -> onChange(on) }
        return sw
    }

    private fun switchCard(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit): LinearLayout {
        val c = card()
        val top = hRow()
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.addView(tv(title, 17f, Pal.TEXT, true))
        col.addView(tv(sub, 13f, Pal.SUB))
        top.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        top.addView(styledSwitch(checked, onChange))
        c.addView(top)
        return c
    }

    private fun settingRow(title: String, value: String, onClick: () -> Unit): LinearLayout {
        val c = card()
        c.addView(tv(title, 13f, Pal.SUB))
        c.addView(tv(value, 17f, Pal.TEXT, true))
        c.setOnClickListener { onClick() }
        return c
    }

    private fun fmtTs(ts: Long): String = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(ts))

    private fun fmtMin(m: Int): String {
        val h = (m / 60) % 24
        val ap = if (h < 12) "AM" else "PM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%d:%02d %s", h12, m % 60, ap)
    }

    private fun entryLine(e: Entry) =
        fmtTs(e.ts) + "   " + (if (e.drink) "✅ Paani piya" else "⏭ Skip kiya")

    // ------------------------------------------------------------------ HOME

    private fun statusText(): String {
        if (!Store.enabled(this)) return "OFF — koi awaaz nahi aayegi (Meeting mode)"
        val now = System.currentTimeMillis()
        val muted = Store.mutedUntil(this)
        if (muted > now) return "🤫 Chup hai, ${fmtTs(muted)} tak"
        val n = Scheduler.nextTrigger(this) ?: return "Koi reminder baaki nahi"
        val kal = if (Store.startOfDay(n) != Store.startOfDay(now)) "Kal " else ""
        return "Agla reminder: $kal${fmtTs(n)}"
    }

    private fun home() {
        val goal = Store.goal(this)
        val today = Store.todayGlasses(this)

        content.put(tv("💧 Paani Reminder", 22f, Pal.TEXT, true), 0)
        content.put(tv(SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(Date()), 13f, Pal.SUB), 2)

        val ring = WaterRingView(this)
        ring.set(today, goal)
        content.put(ring, 8, MATCH, dp(250))

        val msg = if (today >= goal) "🎉 Aaj ka goal poora ho gaya!" else "${goal - today} glass baaki hain"
        val m = tv(msg, 15f, if (today >= goal) Pal.GOOD else Pal.SUB)
        m.gravity = Gravity.CENTER
        content.put(m, 0)

        val r = hRow()
        r.addView(button("＋  Ek glass piya") {
            Store.add(this, true); afterChange()
        }, LinearLayout.LayoutParams(0, WRAP, 2f))
        val undo = button("↩ Undo", false) {
            Store.undoLastDrink(this); afterChange()
        }
        val ulp = LinearLayout.LayoutParams(0, WRAP, 1f)
        ulp.leftMargin = dp(10)
        r.addView(undo, ulp)
        content.put(r, 12)

        // Reminders ON/OFF (meeting mode)
        content.put(switchCard("🔔 Reminders", statusText(), Store.enabled(this)) { on ->
            Store.setEnabled(this, on)
            if (!on) Store.setMutedUntil(this, 0L)
            afterChange()
        }, 16)

        // Meeting mein jaldi chup
        val now = System.currentTimeMillis()
        if (Store.enabled(this)) {
            if (Store.mutedUntil(this) > now) {
                content.put(button("🔔 Chup khatam karein", false) {
                    Store.setMutedUntil(this, 0L); afterChange()
                }, 10)
            } else {
                val mr = hRow()
                mr.addView(button("🤫 1 ghanta chup", false) {
                    Store.setMutedUntil(this, System.currentTimeMillis() + 3_600_000L); afterChange()
                }, LinearLayout.LayoutParams(0, WRAP, 1f))
                val lp2 = LinearLayout.LayoutParams(0, WRAP, 1f)
                lp2.leftMargin = dp(10)
                mr.addView(button("🤫 2 ghante chup", false) {
                    Store.setMutedUntil(this, System.currentTimeMillis() + 7_200_000L); afterChange()
                }, lp2)
                content.put(mr, 10)
            }
        }

        // Aaj ki timeline
        val day0 = Store.startOfDay(now)
        val todays = Store.entries(this).filter { it.ts >= day0 }.sortedByDescending { it.ts }
        val tc = card()
        tc.addView(tv("Aaj ki history", 17f, Pal.TEXT, true))
        if (todays.isEmpty()) {
            tc.put(tv("Abhi tak koi entry nahi.", 14f, Pal.SUB), 8)
        } else {
            for (e in todays) {
                tc.put(tv(entryLine(e), 15f, if (e.drink) Pal.TEXT else Pal.WARN), 8)
            }
        }
        content.put(tc, 16)
    }

    // ------------------------------------------------------------------ HISTORY

    private class Bucket(
        val start: Long, val end: Long, val label: String, val title: String, val days: Int
    ) {
        var glasses = 0
        var skips = 0
    }

    private fun buckets(all: List<Entry>): List<Bucket> {
        val today0 = Store.startOfDay(System.currentTimeMillis())
        val out = ArrayList<Bucket>()
        if (range < 2) {
            val n = if (range == 0) 7 else 30
            val dayFmt = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
            val wkFmt = SimpleDateFormat("EEE", Locale.getDefault())
            for (i in n - 1 downTo 0) {
                val s = Store.addDays(today0, -i)
                val e = Store.addDays(s, 1)
                val cal = Calendar.getInstance()
                cal.timeInMillis = s
                val lbl = if (range == 0) wkFmt.format(Date(s)) else cal.get(Calendar.DAY_OF_MONTH).toString()
                out.add(Bucket(s, e, lbl, dayFmt.format(Date(s)), 1))
            }
        } else {
            val longFmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            val shortFmt = SimpleDateFormat("MMM", Locale.getDefault())
            for (i in 11 downTo 0) {
                val cal = Calendar.getInstance()
                cal.timeInMillis = today0
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, -i)
                val s = cal.timeInMillis
                val dim = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.add(Calendar.MONTH, 1)
                val e = cal.timeInMillis
                val days = if (i == 0) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else dim
                out.add(Bucket(s, e, shortFmt.format(Date(s)), longFmt.format(Date(s)), days))
            }
        }
        for (b in out) {
            for (en in all) {
                if (en.ts >= b.start && en.ts < b.end) {
                    if (en.drink) b.glasses++ else b.skips++
                }
            }
        }
        return out
    }

    private fun statCard(title: String, value: String): LinearLayout {
        val c = card()
        c.addView(tv(title, 12f, Pal.SUB))
        c.addView(tv(value, 20f, Pal.TEXT, true))
        return c
    }

    private fun history() {
        val goal = Store.goal(this)
        val all = Store.entries(this)
        val today0 = Store.startOfDay(System.currentTimeMillis())

        content.put(tv("📊 History", 22f, Pal.TEXT, true), 0)

        val seg = hRow()
        val names = listOf("Hafta", "Mahina", "Saal")
        for (i in names.indices) {
            val t = TextView(this)
            t.text = names[i]
            t.gravity = Gravity.CENTER
            t.textSize = 15f
            t.setPadding(0, dp(10), 0, dp(10))
            t.setTextColor(if (i == range) Color.WHITE else Pal.SUB)
            t.background = bg(if (i == range) Pal.PRIMARY else Pal.CARD, 12)
            t.setOnClickListener { range = i; render() }
            val lp = LinearLayout.LayoutParams(0, WRAP, 1f)
            if (i > 0) lp.leftMargin = dp(8)
            seg.addView(t, lp)
        }
        content.put(seg, 12)

        val bs = buckets(all)

        // Chart
        val cc = card()
        cc.addView(tv(if (range == 2) "Har mahine ka average (glass / din)" else "Glass per din", 14f, Pal.SUB))
        val chart = BarChartView(this)
        val vals = FloatArray(bs.size) { i ->
            if (range == 2) bs[i].glasses.toFloat() / bs[i].days else bs[i].glasses.toFloat()
        }
        chart.set(vals, bs.map { it.label }, goal.toFloat(), range == 2)
        cc.put(chart, 8, MATCH, dp(190))
        cc.put(tv("- - -  Goal: $goal glass", 12f, Pal.WARN), 4)
        content.put(cc, 12)

        // Stats
        val totalG = bs.sumOf { it.glasses }
        val totalS = bs.sumOf { it.skips }
        val firstDay = if (all.isEmpty()) today0 else Store.startOfDay(all.minOf { it.ts })
        val from = maxOf(bs.first().start, firstDay)
        val effDays = Math.round((today0 - from) / 86_400_000.0).toInt() + 1
        val avg = totalG.toFloat() / effDays
        val perDay = HashMap<Long, Int>()
        for (e in all) {
            if (e.drink && e.ts >= bs.first().start) {
                val k = Store.startOfDay(e.ts)
                perDay[k] = (perDay[k] ?: 0) + 1
            }
        }
        val hit = perDay.values.count { it >= goal }

        val r1 = hRow()
        r1.addView(statCard("Average / din", String.format(Locale.US, "%.1f glass", avg)),
            LinearLayout.LayoutParams(0, WRAP, 1f))
        val l1 = LinearLayout.LayoutParams(0, WRAP, 1f)
        l1.leftMargin = dp(10)
        r1.addView(statCard("Goal poora", "$hit din"), l1)
        content.put(r1, 12)

        val r2 = hRow()
        r2.addView(statCard("Total", "$totalG glass"), LinearLayout.LayoutParams(0, WRAP, 1f))
        val l2 = LinearLayout.LayoutParams(0, WRAP, 1f)
        l2.leftMargin = dp(10)
        r2.addView(statCard("Skip kiye", "$totalS baar"), l2)
        content.put(r2, 10)

        // List
        val lc = card()
        lc.addView(tv(if (range == 2) "Mahine ke hisaab se" else "Tafseel (din par tap karein, waqt dekhein)", 14f, Pal.SUB))
        for (b in bs.reversed()) {
            val row = hRow()
            row.setPadding(0, dp(10), 0, dp(10))
            row.addView(tv(b.title, 15f), LinearLayout.LayoutParams(0, WRAP, 1f))
            val right: String
            val col: Int
            if (range < 2) {
                right = "${b.glasses}/$goal glass" + (if (b.skips > 0) " · ${b.skips} skip" else "")
                col = if (b.glasses >= goal) Pal.GOOD else if (b.glasses == 0) Pal.SUB else Pal.TEXT
                row.setOnClickListener { showDay(b, all) }
            } else {
                right = String.format(Locale.US, "avg %.1f · %d glass · %d skip",
                    b.glasses.toFloat() / b.days, b.glasses, b.skips)
                col = Pal.TEXT
            }
            row.addView(tv(right, 13f, col))
            lc.addView(row)
        }
        content.put(lc, 12)
    }

    private fun showDay(b: Bucket, all: List<Entry>) {
        val list = all.filter { it.ts >= b.start && it.ts < b.end }.sortedBy { it.ts }
        val msg = if (list.isEmpty()) "Is din koi entry nahi." else list.joinToString("\n") { entryLine(it) }
        AlertDialog.Builder(this)
            .setTitle(b.title)
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    // ------------------------------------------------------------------ SETTINGS

    private fun settings() {
        content.put(tv("⚙️ Settings", 22f, Pal.TEXT, true), 0)

        // Goal
        content.put(settingRow("Daily goal", "${Store.goal(this)} glass") {
            val opts = (4..20).toList()
            AlertDialog.Builder(this).setTitle("Daily goal (glass)")
                .setItems(opts.map { "$it glass" }.toTypedArray()) { _, i ->
                    Store.setGoal(this, opts[i]); afterChange()
                }.show()
        }, 12)

        // Active hours
        content.put(tv("Active hours (sirf in ke beech reminder aayega)", 13f, Pal.SUB), 18)
        val ah = hRow()
        ah.addView(settingRow("☀️ Uthne ka waqt", fmtMin(Store.wake(this))) {
            val w = Store.wake(this)
            TimePickerDialog(this, { _, h, m -> Store.setWake(this, h * 60 + m); afterChange() },
                w / 60, w % 60, false).show()
        }, LinearLayout.LayoutParams(0, WRAP, 1f))
        val sl = LinearLayout.LayoutParams(0, WRAP, 1f)
        sl.leftMargin = dp(10)
        ah.addView(settingRow("🌙 Sone ka waqt", fmtMin(Store.sleep(this))) {
            val s = Store.sleep(this)
            TimePickerDialog(this, { _, h, m -> Store.setSleep(this, h * 60 + m); afterChange() },
                s / 60, s % 60, false).show()
        }, sl)
        content.put(ah, 6)

        // Interval
        val ivs = listOf(30, 45, 60, 90, 120, 180)
        fun ivLabel(m: Int) = if (m < 60) "Har $m minute" else if (m % 60 == 0) "Har ${m / 60} ghanta" else "Har $m minute"
        content.put(settingRow("Kitni der baad yaad dilayein", ivLabel(Store.interval(this))) {
            AlertDialog.Builder(this).setTitle("Reminder interval")
                .setItems(ivs.map { ivLabel(it) }.toTypedArray()) { _, i ->
                    Store.setInterval(this, ivs[i]); afterChange()
                }.show()
        }, 18)

        content.put(switchCard("Goal poora to reminder band", "Aaj ka goal poora hote hi baaki reminders nahi aayenge",
            Store.stopAfterGoal(this)) { on ->
            Store.setStopAfterGoal(this, on); afterChange()
        }, 12)

        // Ringtone
        val rc = card()
        rc.addView(tv("🔔 Reminder ringtone", 13f, Pal.SUB))
        rc.addView(tv(Store.toneName(this), 17f, Pal.TEXT, true))
        val br = hRow()
        br.addView(button("📱 Phone ki tones", false) { pickPhoneTone() }, LinearLayout.LayoutParams(0, WRAP, 1f))
        val b2 = LinearLayout.LayoutParams(0, WRAP, 1f)
        b2.leftMargin = dp(10)
        br.addView(button("🎵 Apni file", false) { pickFile() }, b2)
        rc.put(br, 12)
        if (Store.toneUri(this) != null) {
            rc.put(button("Default par wapas", false) {
                Store.setTone(this, null, "Phone ki default alarm tone"); render()
            }, 8)
        }
        content.put(rc, 18)

        // Ring duration
        val secs = listOf(15, 30, 60, 120)
        content.put(settingRow("Ringing kitni der chale", "${Store.ringSeconds(this)} second") {
            AlertDialog.Builder(this).setTitle("Ringing ka dauraniya")
                .setItems(secs.map { "$it second" }.toTypedArray()) { _, i ->
                    Store.setRingSeconds(this, secs[i]); render()
                }.show()
        }, 12)

        // Test
        content.put(button("▶ Reminder abhi test karein") {
            try {
                RingService.ensureChannels(this)
                ContextCompat.startForegroundService(this, Intent(this, RingService::class.java))
            } catch (e: Exception) {
                Toast.makeText(this, "Test shuru nahi hua: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 18)

        // Permissions help
        content.put(tv("Zaroori permissions", 13f, Pal.SUB), 22)
        if (!Scheduler.canExact(this)) {
            content.put(button("⏰ Exact alarm ki ijazat dein", false) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:$packageName")))
                } catch (e: Exception) { }
            }, 8)
        }
        content.put(button("🔋 Battery optimization hatayein", false) {
            try {
                startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")))
            } catch (e: Exception) {
                try { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) { }
            }
        }, 8)

        content.put(tv(
            "Tip: Silent/Vibrate par bhi awaaz aati hai kyunke ye Alarm volume use karta hai. " +
                "Phone Settings > Sound mein Alarm volume zero na rakhein. " +
                "Meeting mein Home par Reminders OFF karein ya \"chup\" button dabayein.",
            12f, Pal.SUB), 18)
    }

    // ------------------------------------------------------------------ ringtone picking

    private fun ensureAudioPermission() {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE
        if (checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(perm), 2)
        }
    }

    private fun pickPhoneTone() {
        ensureAudioPermission()
        val i = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Reminder tone chunein")
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        i.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
        val cur = Store.toneUri(this)
        if (cur != null && cur.startsWith("content")) {
            i.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(cur))
        }
        startActivityForResult(i, 101)
    }

    private fun pickFile() {
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "audio/*"
        startActivityForResult(i, 102)
    }

    @Suppress("DEPRECATION")
    private fun pickedUri(d: Intent): Uri? =
        d.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) return
        when (requestCode) {
            101 -> {
                val u = pickedUri(data) ?: return
                val name = try {
                    RingtoneManager.getRingtone(this, u)?.getTitle(this) ?: "Chuni hui tone"
                } catch (e: Exception) {
                    "Chuni hui tone"
                }
                Store.setTone(this, u.toString(), name)
                render()
            }
            102 -> {
                val u = data.data ?: return
                Thread {
                    try {
                        var name = "Meri awaaz file"
                        contentResolver.query(u, null, null, null, null)?.use { cur ->
                            val idx = cur.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cur.moveToFirst() && idx >= 0) name = cur.getString(idx)
                        }
                        // File app ke andar copy kar lete hain, taake baad mein move/delete hone par bhi chale
                        val out = File(filesDir, "custom_tone_" + System.currentTimeMillis())
                        contentResolver.openInputStream(u)?.use { inp ->
                            out.outputStream().use { o -> inp.copyTo(o) }
                        }
                        filesDir.listFiles()
                            ?.filter { it.name.startsWith("custom_tone_") && it != out }
                            ?.forEach { it.delete() }
                        Store.setTone(this, Uri.fromFile(out).toString(), name)
                        runOnUiThread { render() }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this, "File load nahi ho saki", Toast.LENGTH_LONG).show()
                        }
                    }
                }.start()
            }
        }
    }
}
