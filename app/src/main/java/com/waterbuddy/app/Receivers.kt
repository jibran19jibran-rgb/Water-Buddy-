package com.waterbuddy.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Alarm ka waqt hone par chalta hai: agla alarm lagata hai aur ringing shuru karta hai. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Scheduler.schedule(context) // pehle agla reminder set
        if (!Store.enabled(context)) return
        if (System.currentTimeMillis() < Store.mutedUntil(context)) return
        if (Store.stopAfterGoal(context) && Store.todayGlasses(context) >= Store.goal(context)) return

        try {
            ContextCompat.startForegroundService(context, Intent(context, RingService::class.java))
        } catch (e: Exception) {
            // Agar system ne service start na karne di to kam az kam notification dikha do
            RingService.ensureChannels(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(RingService.NOTIF_ID, RingService.build(context, false))
        }
    }
}

/** Notification ke buttons: "Paani pee liya" / "Abhi nahi (Skip)". Ringing band + history mein entry. */
class ActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.getStringExtra("a")) {
            "drank" -> Store.add(context, true)
            "skip" -> Store.add(context, false)
        }
        context.stopService(Intent(context, RingService::class.java))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(RingService.NOTIF_ID)
        Scheduler.schedule(context)
    }
}

/** Phone restart ya app update ke baad reminders dobara lagata hai. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Scheduler.schedule(context)
    }
}
