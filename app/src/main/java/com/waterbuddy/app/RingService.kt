package com.waterbuddy.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

/**
 * Reminder bajata hai: ALARM audio stream par, is liye phone Silent ya Vibrate par ho tab bhi
 * awaaz aati hai (jaise alarm clock). Saath mein vibration bhi.
 */
class RingService : Service() {

    companion object {
        const val NOTIF_ID = 4242
        const val CH_RING = "water_ring"
        const val CH_QUIET = "water_quiet"

        fun ensureChannels(c: Context) {
            val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val ring = NotificationChannel(CH_RING, "Paani reminder (awaaz)", NotificationManager.IMPORTANCE_HIGH)
            ring.setSound(null, null)
            ring.enableVibration(false)
            ring.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            nm.createNotificationChannel(ring)

            val quiet = NotificationChannel(CH_QUIET, "Paani reminder (baaki)", NotificationManager.IMPORTANCE_LOW)
            quiet.setSound(null, null)
            quiet.enableVibration(false)
            nm.createNotificationChannel(quiet)
        }

        private fun actionIntent(c: Context, action: String, code: Int): PendingIntent {
            val i = Intent(c, ActionReceiver::class.java).putExtra("a", action)
            return PendingIntent.getBroadcast(
                c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun build(c: Context, ringing: Boolean): Notification {
            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            return NotificationCompat.Builder(c, if (ringing) CH_RING else CH_QUIET)
                .setSmallIcon(R.drawable.ic_stat_drop)
                .setContentTitle("💧 Paani peene ka waqt!")
                .setContentText("Kya aap ne paani pee liya?")
                .setContentIntent(open)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setColor(0xFF9B5DE5.toInt())
                .setOngoing(ringing)
                .setAutoCancel(!ringing)
                .setOnlyAlertOnce(true)
                .addAction(0, "✅ Paani pee liya", actionIntent(c, "drank", 1))
                .addAction(0, "⏭ Abhi nahi (Skip)", actionIntent(c, "skip", 2))
                .setDeleteIntent(actionIntent(c, "dismiss", 3))
                .build()
        }
    }

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var focusReq: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())

    private val timeout = Runnable {
        stopSound()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        // Awaaz band, lekin notification (do buttons ke saath) baaki rehti hai
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, build(this, false))
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannels(this)
        ServiceCompat.startForeground(
            this, NOTIF_ID, build(this, true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        )
        handler.removeCallbacksAndMessages(null)
        stopSound()
        startSound()
        handler.postDelayed(timeout, Store.ringSeconds(this) * 1000L)
        return START_NOT_STICKY
    }

    private fun startSound() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attrs).build()
            am.requestAudioFocus(req)
            focusReq = req
        } catch (_: Exception) {
        }

        val candidates = ArrayList<Uri>()
        Store.toneUri(this)?.let { candidates.add(Uri.parse(it)) }
        candidates.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
        candidates.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        candidates.add(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))

        for (u in candidates) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(attrs)
                mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
                mp.setDataSource(this, u)
                mp.isLooping = true
                mp.prepare()
                mp.start()
                player = mp
                break
            } catch (e: Exception) {
                try { mp.release() } catch (_: Exception) {}
            }
        }

        startVibration(attrs)
    }

    @Suppress("DEPRECATION")
    private fun startVibration(attrs: AudioAttributes) {
        try {
            val v: Vibrator = if (Build.VERSION.SDK_INT >= 31) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            } else {
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            vibrator = v
            if (v.hasVibrator()) {
                v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0), attrs)
            }
        } catch (_: Exception) {
        }
    }

    private fun stopSound() {
        try { player?.stop() } catch (_: Exception) {}
        try { player?.release() } catch (_: Exception) {}
        player = null
        try { vibrator?.cancel() } catch (_: Exception) {}
        vibrator = null
        try {
            focusReq?.let {
                (getSystemService(Context.AUDIO_SERVICE) as AudioManager).abandonAudioFocusRequest(it)
            }
        } catch (_: Exception) {}
        focusReq = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopSound()
        super.onDestroy()
    }
}
