package com.sagon.cocinarecetas.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sagon.cocinarecetas.MainActivity
import com.sagon.cocinarecetas.R
import com.sagon.cocinarecetas.data.local.RecipeDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecipeReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = RecipeDatabase.getDatabase(applicationContext)
            val recipeDao = database.recipeDao()
            
            // Obtenemos una receta aleatoria
            val randomRecipes = recipeDao.getRandomRecipesSample(1)
            if (randomRecipes.isEmpty()) return@withContext Result.failure()
            
            val recipe = randomRecipes[0]
            
            showNotification(recipe.title, recipe.id)
            
            Result.success()
        } catch (e: Exception) {
            android.util.Log.e("RecipeWorker", "Error enviando notificación", e)
            Result.retry()
        }
    }

    private fun showNotification(recipeTitle: String, recipeId: Int) {
        val channelId = "recipe_reminders"
        val notificationId = 1001

        // Crear el canal de notificación para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sugerencias de Cocina"
            val descriptionText = "Invitaciones semanales para cocinar nuevas recetas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app en la receta específica
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("recipeId", recipeId)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_today) // Cambiar por icono de la app luego
            .setContentTitle("¡Hora de cocinar!")
            .setContentText("¿Qué tal si hoy preparas: $recipeTitle?")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(applicationContext)) {
            // Verificación de permisos para Android 13+ (Omitida aquí por simplicidad del flujo, 
            // pero se asume que el usuario los concederá)
            try {
                notify(notificationId, builder.build())
            } catch (e: SecurityException) {
                android.util.Log.e("RecipeWorker", "Sin permiso de notificación", e)
            }
        }
    }
}
