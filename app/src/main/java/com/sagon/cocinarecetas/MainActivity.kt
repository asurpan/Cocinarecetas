package com.sagon.cocinarecetas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sagon.cocinarecetas.data.local.JsonAssetImporter
import com.sagon.cocinarecetas.data.local.RecipeDatabase
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.data.remote.FirestoreService
import com.sagon.cocinarecetas.data.repository.RecipeRepository
import com.sagon.cocinarecetas.ui.screens.RecipeDetailScreen
import com.sagon.cocinarecetas.ui.screens.RecipeListScreen
import com.sagon.cocinarecetas.ui.screens.WelcomeScreen
import com.sagon.cocinarecetas.ui.screens.WeeklyMenuScreen
import com.sagon.cocinarecetas.ui.screens.HealthStatsScreen
import com.sagon.cocinarecetas.ui.theme.CocinaREcetasTheme
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModel
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModelFactory
import com.sagon.cocinarecetas.util.SoundUtil
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sagon.cocinarecetas.worker.RecipeReminderWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.sagon.cocinarecetas.worker.HealthReminderWorker
import java.util.concurrent.TimeUnit
import java.util.Calendar

class MainActivity : ComponentActivity() {

    private val database by lazy { RecipeDatabase.getDatabase(this) }
    private val firestoreService by lazy { FirestoreService() }
    private val repository by lazy { RecipeRepository(database.recipeDao(), database.healthRecordDao(), firestoreService) }
    
    private val viewModel: RecipeViewModel by viewModels {
        RecipeViewModelFactory(repository, getSharedPreferences("app_prefs", MODE_PRIVATE))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        scheduleWeeklyReminder()
        scheduleHealthReminders()

        setContent {
            CocinaREcetasTheme {
                val navController = rememberNavController()
                
                // Redirigir si viene de una notificación de salud
                LaunchedEffect(intent) {
                    if (intent.getStringExtra("navigate_to") == "health_stats") {
                        navController.navigate("health_stats")
                    }
                }

                val recipes by viewModel.recipes.collectAsState()
                val prefs = remember { getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
                // Siempre mostramos la animación de bienvenida al arrancar
                var showWelcome by remember { mutableStateOf(true) }
                // Recuperamos si el aviso de responsabilidad ya fue aceptado históricamente
                val hasAcceptedDisclaimer = remember { prefs.getBoolean("show_disclaimer", true) }

                // --- CORAZÓN DE DATOS (CLOUD + LOCAL FALLBACK) ---
                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        try {
                            // 1. Carga inicial desde el nuevo JSON v3.0 si la DB está vacía
                            val localRecipes = JsonAssetImporter.loadRecipesFromAsset(this@MainActivity, "recipes.json")
                            viewModel.insertInitialData(localRecipes)
                        } catch (e: Exception) {
                            Log.e("Firebase", "Error de datos", e)
                        }
                    }
                }

                // Transición fluida animada tipo Crossfade/Fade entre bienvenida e interfaz de recetas
                AnimatedVisibility(
                    visible = showWelcome,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    WelcomeScreen(
                        skipDisclaimer = !hasAcceptedDisclaimer,
                        onStartClick = {
                            prefs.edit { putBoolean("show_disclaimer", false) }
                            showWelcome = false
                        }
                    )
                }

                AnimatedVisibility(
                    visible = !showWelcome,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    var showExitDialog by remember { mutableStateOf(false) }

                    // Intercepta el botón de atrás en la pantalla principal
                    BackHandler {
                        showExitDialog = true
                    }

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    showExitDialog = false
                                    finish() // Cierra la aplicación de forma segura
                                }) {
                                    Text("SALIR")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text("CANCELAR")
                                }
                            },
                            title = { Text("¿Salir de CocinaREcetas?") },
                            text = { Text("¿Seguro que quieres cerrar la aplicación?") }
                        )
                    }

                    RecipeApp(viewModel)
                }
            }
        }
    }

    private fun scheduleWeeklyReminder() {
        val workRequest = PeriodicWorkRequestBuilder<RecipeReminderWorker>(
            7, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weekly_recipe_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun scheduleHealthReminders() {
        // Recordatorio Mañana (aprox 9:00)
        val morningRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(calculateInitialDelay(9), TimeUnit.MINUTES).build()

        // Recordatorio Cena (aprox 21:00)
        val eveningRequest = PeriodicWorkRequestBuilder<HealthReminderWorker>(
            24, TimeUnit.HOURS
        ).setInitialDelay(calculateInitialDelay(21), TimeUnit.MINUTES).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "health_morning", ExistingPeriodicWorkPolicy.KEEP, morningRequest
        )
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "health_evening", ExistingPeriodicWorkPolicy.KEEP, eveningRequest
        )
    }

    private fun calculateInitialDelay(targetHour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) target.add(Calendar.DAY_OF_YEAR, 1)
        return (target.timeInMillis - now.timeInMillis) / (1000 * 60)
    }
}

@Composable
fun RecipeApp(viewModel: RecipeViewModel) {
    val navController = rememberNavController()
    val recipes by viewModel.recipes.collectAsState()

    NavHost(navController = navController, startDestination = "list") {
        composable("list") {
            RecipeListScreen(
                viewModel = viewModel,
                onRecipeClick = { recipe ->
                    navController.navigate("detail/${recipe.id}")
                },
                onWeeklyMenuClick = {
                    viewModel.generateWeeklyMenu()
                    navController.navigate("weekly_menu")
                },
                onHealthClick = {
                    navController.navigate("health_stats")
                }
            )
        }
        composable("health_stats") {
            HealthStatsScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
        composable("weekly_menu") {
            WeeklyMenuScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() },
                onRecipeClick = { recipe ->
                    navController.navigate("detail/${recipe.id}")
                }
            )
        }
        composable("detail/{recipeId}") { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("recipeId")?.toIntOrNull() ?: -1
            var recipe by remember { mutableStateOf<Recipe?>(null) }
            
            LaunchedEffect(recipeId) {
                recipe = viewModel.getRecipeById(recipeId)
            }
            
            recipe?.let {
                RecipeDetailScreen(
                    recipe = it,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onMarkAsConsumed = {
                        navController.navigate("health_stats") {
                            // Limpiar el historial para que al volver desde salud no regrese al detalle si no se desea
                            popUpTo("list") { saveState = true }
                        }
                    }
                )
            }
        }
    }
}
