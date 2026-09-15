package com.sagon.cocinarecetas.worker

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sagon.cocinarecetas.MainActivity
import java.util.Calendar

class HealthReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        val message = if (hour < 12) {
            listOf(
                "¡Buenos días! Empieza el día con energía. ¿Has registrado tu peso hoy?",
                "¡A por el día! Un desayuno sano es el primer paso.",
                "¡Hoy es un gran día para cuidarte! No olvides anotar tu peso."
            ).random()
        } else {
            listOf(
                "¡Hora de la cena! Mantén el foco en tu objetivo.",
                "Día superado. ¿Cómo ha ido ese progreso hoy?",
                "¡Buenas noches! Recuerda que cada elección cuenta."
            ).random()
        }
        
        showNotification(message)
        return Result.success()
    }

    private fun showNotification(message: String) {
        val channelId = "health_reminders"
        val notificationId = 1002

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Recordatorios de Salud"
            val descriptionText = "Mensajes de ánimo y recordatorios de peso"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "health_stats")
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 1, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle("¡CocinaREcetas te anima!")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                Log.e("HealthWorker", "Sin permiso de notificación", e)
            }
        }
    }
}
