package com.waterbuddy.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object Scheduler {
    private const val REQ = 7

    /**
     * Agla reminder ka waqt. Reminders sirf "Active hours" (uthne se sone tak) mein aate hain:
     * uthne ke waqt + interval, + 2*interval ... sone ke waqt tak.
     */
    fun nextTrigger(c: Context, now: Long = System.currentTimeMillis()): Long? {
        if (!Store.enabled(c)) return null

        var minStart = maxOf(now, Store.mutedUntil(c))
        if (Store.stopAfterGoal(c) && Store.todayGlasses(c) >= Store.goal(c)) {
            // Aaj ka goal poora: kal se dobara
            minStart = maxOf(minStart, Store.addDays(Store.startOfDay(now), 1))
        }

        val interval = Store.interval(c) * 60_000L
        val wake = Store.wake(c)
        var sleep = Store.sleep(c)
        if (sleep <= wake) sleep += 1440 // raat 12 ke baad sona

        var best: Long? = null
        for (d in -1..2) {
            val day = Store.addDays(Store.startOfDay(now), d)
            val start = day + wake * 60_000L
            val end = day + sleep * 60_000L
            var t = start + interval
            while (t <= end) {
                if (t > minStart) {
                    if (best == null || t < best) best = t
                    break
                }
                t += interval
            }
        }
        return best
    }

    private fun pending(c: Context): PendingIntent =
        PendingIntent.getBroadcast(
            c, REQ, Intent(c, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun canExact(c: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pending(c)
        am.cancel(pi)
        val t = nextTrigger(c) ?: return
        try {
            if (canExact(c)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi)
        }
    }
}
