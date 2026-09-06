package com.ilinetech.emergency.alert

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * © ILINE TECH BY FERAK ALADDIN
 *
 * Plays a looping alarm sound at full STREAM_ALARM volume using
 * AudioAttributes.USAGE_ALARM, which is what lets it override silent mode
 * and most Do Not Disturb configurations on Android — the same mechanism
 * used by clock/alarm apps. Paired with a looping vibration pattern for
 * devices in silent+vibrate or where DND still suppresses alarm audio
 * (some OEM DND policies do; vibration is a reasonable fallback).
 *
 * Lifecycle: call start() when a CRITICAL alert arrives, stop() when the
 * user taps "J'arrive" (see fcm.AlertAckReceiver). Per explicit product
 * decision, there is NO automatic timeout — the alarm loops indefinitely
 * until acknowledged, even if that means it runs for a long time on a
 * missed/ignored alert. (An earlier version auto-stopped after 30s; that
 * was found to defeat the entire point of a "must ring until someone
 * intervenes" emergency alarm and has been removed.) If a safety timeout
 * is wanted later, it should escalate — e.g. notify a supervisor role —
 * rather than silently going quiet, which is worse than either extreme.
 *
 * Singleton object: at most one alert alarm plays at a time by design —
 * a second CRITICAL alert arriving mid-alarm restarts the sound rather than
 * layering a second stream, so the user isn't hearing overlapping alarms.
 */
object AlertRingtonePlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    @Synchronized
    fun start(context: Context) {
        stop(context) // ensure no overlapping instance

        val appContext = context.applicationContext
        val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        val alarmUri = RingtoneManager.getActualDefaultRingtoneUri(appContext, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getValidRingtoneUri(appContext)

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(appContext, alarmUri)
            isLooping = true
            setOnPreparedListener { it.start() }
            prepareAsync()
        }

        startVibration(appContext)
    }

    @Synchronized
    fun stop(context: Context) {
        mediaPlayer?.let {
            runCatching { it.stop() }
            it.release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    private fun startVibration(context: Context) {
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = longArrayOf(0, 800, 400) // wait, buzz, pause — repeats
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // repeat from index 0
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }
}
