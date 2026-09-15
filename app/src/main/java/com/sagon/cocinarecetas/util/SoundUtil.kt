package com.sagon.cocinarecetas.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.sagon.cocinarecetas.R

object SoundUtil {
    private var toneGenerator: ToneGenerator? = null
    private var mediaPlayer: MediaPlayer? = null

    fun playBeep() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
        }
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
    }

    fun playBoilingSound(context: Context) {
        try {
            stopBoilingSound()
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.h)
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stopBoilingSound() {
        mediaPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        mediaPlayer = null
    }

    private var alarmPlayer: MediaPlayer? = null
    var isAlarmActive = mutableStateOf(false)

    fun playBubbleSound() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 40)
        }
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 60)
    }

    fun startLoopingAlarm(context: Context) {
        if (alarmPlayer?.isPlaying == true) return
        
        try {
            stopAlarm()
            alarmPlayer = MediaPlayer.create(context.applicationContext, R.raw.z)
            alarmPlayer?.isLooping = true
            
            // Configuración moderna de audio
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            alarmPlayer?.setAudioAttributes(audioAttributes)
            
            // Aseguramos volumen máximo
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                AudioManager.FLAG_SHOW_UI
            )
            
            alarmPlayer?.start()
            isAlarmActive.value = true
            Log.d("SoundUtil", "Alarma iniciada con éxito")
        } catch (e: Exception) {
            Log.e("SoundUtil", "Error al iniciar la alarma", e)
        }
    }

    fun stopAlarm() {
        try {
            alarmPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            alarmPlayer = null
            isAlarmActive.value = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playTimerFinishedSound() {
        // Mantenemos esta para la prueba rápida, pero usando el nuevo sonido una vez
        // (Nota: playTimerFinishedSound se usa en el botón de PROBAR SONIDO)
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        }
        Thread {
            val tones = listOf(ToneGenerator.TONE_DTMF_1, ToneGenerator.TONE_DTMF_5, ToneGenerator.TONE_DTMF_9, ToneGenerator.TONE_DTMF_A)
            tones.forEach { tone -> toneGenerator?.startTone(tone, 150); Thread.sleep(120) }
            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_D, 500)
        }.start()
    }
}
