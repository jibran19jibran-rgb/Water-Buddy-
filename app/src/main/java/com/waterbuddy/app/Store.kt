package com.waterbuddy.app

import android.content.Context
import java.util.Calendar

data class Entry(val ts: Long, val drink: Boolean)

object Store {
    private fun sp(c: Context) =
        c.applicationContext.getSharedPreferences("water", Context.MODE_PRIVATE)

    // ---------- Settings ----------
    fun enabled(c: Context) = sp(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) { sp(c).edit().putBoolean("enabled", v).apply() }

    fun goal(c: Context) = sp(c).getInt("goal", 8)
    fun setGoal(c: Context, v: Int) { sp(c).edit().putInt("goal", v).apply() }

    /** Uthne ka waqt, minutes since midnight */
    fun wake(c: Context) = sp(c).getInt("wake", 7 * 60)
    fun setWake(c: Context, v: Int) { sp(c).edit().putInt("wake", v).apply() }

    /** Sone ka waqt, minutes since midnight */
    fun sleep(c: Context) = sp(c).getInt("sleep", 23 * 60)
    fun setSleep(c: Context, v: Int) { sp(c).edit().putInt("sleep", v).apply() }

    fun interval(c: Context) = sp(c).getInt("interval", 60)
    fun setInterval(c: Context, v: Int) { sp(c).edit().putInt("interval", v).apply() }

    fun mutedUntil(c: Context) = sp(c).getLong("muted", 0L)
    fun setMutedUntil(c: Context, v: Long) { sp(c).edit().putLong("muted", v).apply() }

    fun stopAfterGoal(c: Context) = sp(c).getBoolean("stop_goal", true)
    fun setStopAfterGoal(c: Context, v: Boolean) { sp(c).edit().putBoolean("stop_goal", v).apply() }

    fun ringSeconds(c: Context) = sp(c).getInt("ring_sec", 30)
    fun setRingSeconds(c: Context, v: Int) { sp(c).edit().putInt("ring_sec", v).apply() }

    fun toneUri(c: Context): String? = sp(c).getString("tone_uri", null)
    fun toneName(c: Context): String = sp(c).getString("tone_name", null) ?: "Phone ki default alarm tone"
    fun setTone(c: Context, uri: String?, name: String) {
        sp(c).edit().putString("tone_uri", uri).putString("tone_name", name).apply()
    }

    // ---------- Log (har entry: "timestamp,D" = piya, "timestamp,S" = skip) ----------
    @Synchronized
    fun entries(c: Context): List<Entry> {
        val raw = sp(c).getString("log", "") ?: ""
        if (raw.isEmpty()) return emptyList()
        val out = ArrayList<Entry>()
        for (line in raw.split('\n')) {
            val p = line.split(',')
            if (p.size == 2) {
                val ts = p[0].toLongOrNull() ?: continue
                out.add(Entry(ts, p[1] == "D"))
            }
        }
        return out
    }

    @Synchronized
    fun add(c: Context, drink: Boolean, ts: Long = System.currentTimeMillis()) {
        val cur = sp(c).getString("log", "") ?: ""
        val line = "$ts," + (if (drink) "D" else "S")
        sp(c).edit().putString("log", if (cur.isEmpty()) line else "$cur\n$line").apply()
    }

    @Synchronized
    fun undoLastDrink(c: Context) {
        val list = entries(c).toMutableList()
        val dayStart = startOfDay(System.currentTimeMillis())
        val idx = list.indexOfLast { it.drink && it.ts >= dayStart }
        if (idx >= 0) {
            list.removeAt(idx)
            val raw = list.joinToString("\n") { "${it.ts}," + (if (it.drink) "D" else "S") }
            sp(c).edit().putString("log", raw).apply()
        }
    }

    fun todayGlasses(c: Context): Int {
        val dayStart = startOfDay(System.currentTimeMillis())
        return entries(c).count { it.drink && it.ts >= dayStart }
    }

    // ---------- Date helpers ----------
    fun startOfDay(ts: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun addDays(ts: Long, n: Int): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = ts
        cal.add(Calendar.DAY_OF_YEAR, n)
        return cal.timeInMillis
    }
}
