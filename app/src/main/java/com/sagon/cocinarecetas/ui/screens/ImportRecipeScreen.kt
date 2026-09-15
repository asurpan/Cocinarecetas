package com.sagon.cocinarecetas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModel
import com.sagon.cocinarecetas.util.SoundUtil
import android.content.Context
import androidx.core.content.edit
import com.sagon.cocinarecetas.data.model.Goals
import com.sagon.cocinarecetas.data.model.MealSuitability
import com.sagon.cocinarecetas.data.model.Nutrition
import com.sagon.cocinarecetas.data.model.NutritionValues
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecipeScreen(
    viewModel: RecipeViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Carnes") }
    var ingredients by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var cookingTime by remember { mutableStateOf("30 min") }
    
    // Campos de Salud y Filtros v3.0
    var goalHealthy by remember { mutableStateOf(true) }
    var goalWeightLoss by remember { mutableStateOf(false) }
    var goalMuscleGain by remember { mutableStateOf(false) }
    var goalDiabetic by remember { mutableStateOf(true) }
    
    var forBreakfast by remember { mutableStateOf(false) }
    var forLunch by remember { mutableStateOf(true) }
    var forDinner by remember { mutableStateOf(true) }

    // Campos Nutricionales
    var kcal by remember { mutableStateOf("450") }
    var protein by remember { mutableStateOf("20") }
    var carbs by remember { mutableStateOf("40") }
    var fat by remember { mutableStateOf("15") }

    var uploadToCloud by remember { mutableStateOf(false) }
    var adminPass by remember { mutableStateOf("") }
    var showPassError by remember { mutableStateOf(false) }
    
    val families = listOf(
        "Aperitivos", "Ensaladas", "Legumbres", "Sopas", "Arroces", 
        "Pastas", "Verduras", "Pescados", "Carnes", "Postres"
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Añadir Receta Manual") },
                navigationIcon = {
                    IconButton(onClick = {
                        SoundUtil.playBeep()
                        onBackClick()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Nueva Receta",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Escribe o pega los detalles de tu propia receta abajo. La app la organizará para ti.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Campo Título
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la receta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de Categoría (Familia)
            Text("Familia de la receta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ScrollableTabRow(
                selectedTabIndex = families.indexOf(category),
                edgePadding = 0.dp,
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                divider = {}
            ) {
                families.forEach { fam ->
                    Tab(
                        selected = category == fam,
                        onClick = { category = fam },
                        text = { Text(fam, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Tiempo
            OutlinedTextField(
                value = cookingTime,
                onValueChange = { cookingTime = it },
                label = { Text("Tiempo de cocción (ej: 45 min)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Ingredientes
            OutlinedTextField(
                value = ingredients,
                onValueChange = { ingredients = it },
                label = { Text("Ingredientes (uno por línea o separados por comas)") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                placeholder = { Text("- 500g de Lomo\n- Sal y pimienta\n- Aceite de oliva") },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Campo Preparación
            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Pasos de preparación") },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                placeholder = { Text("1. Salpimentar la carne...\n2. Calentar la sartén...") },
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN PREMIUM: FILTROS Y SALUD
            Text("Perfil de Salud y Filtros Inteligentes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Objetivos Apto:", style = MaterialTheme.typography.labelMedium)
                    FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = goalHealthy, onClick = { goalHealthy = !goalHealthy }, label = { Text("Sana") })
                        FilterChip(selected = goalWeightLoss, onClick = { goalWeightLoss = !goalWeightLoss }, label = { Text("Perder Peso") })
                        FilterChip(selected = goalMuscleGain, onClick = { goalMuscleGain = !goalMuscleGain }, label = { Text("Músculo") })
                        FilterChip(selected = goalDiabetic, onClick = { goalDiabetic = !goalDiabetic }, label = { Text("Diabéticos") })
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Momento sugerido:", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = forBreakfast, onClick = { forBreakfast = !forBreakfast }, label = { Text("Desayuno") })
                        FilterChip(selected = forLunch, onClick = { forLunch = !forLunch }, label = { Text("Almuerzo") })
                        FilterChip(selected = forDinner, onClick = { forDinner = !forDinner }, label = { Text("Cena") })
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN PREMIUM: NUTRICIÓN
            Text("Información Nutricional (por ración)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = kcal, onValueChange = { kcal = it }, label = { Text("Kcal") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small)
                OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Prot (g)") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = carbs, onValueChange = { carbs = it }, label = { Text("HC (g)") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small)
                OutlinedTextField(value = fat, onValueChange = { fat = it }, label = { Text("Grasa (g)") }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Opción de Subida a Internet (Protegida)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = uploadToCloud,
                    onCheckedChange = { uploadToCloud = it }
                )
                Text("Subir a la nube de JOSEMAN (Requiere código)")
            }

            if (uploadToCloud) {
                OutlinedTextField(
                    value = adminPass,
                    onValueChange = { 
                        adminPass = it
                        showPassError = false
                    },
                    label = { Text("Código de Administrador") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    isError = showPassError,
                    supportingText = if (showPassError) { { Text("Código incorrecto") } } else null
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    SoundUtil.playBeep()
                    
                    if (uploadToCloud && adminPass != "121212") {
                        showPassError = true
                        return@Button
                    }

                    val newRecipe = Recipe(
                        title = title.trim().uppercase(),
                        category = category,
                        ingredients = ingredients.lines().filter { it.isNotBlank() }.map { it.trim().removePrefix("-").trim() },
                        instructions = instructions.lines().filter { it.isNotBlank() }.map { it.trim() },
                        cookingTime = cookingTime.ifBlank { "30 min" },
                        healthTags = mutableListOf<String>().apply {
                            if (goalHealthy) add("sana")
                            if (goalWeightLoss) add("perder peso")
                            if (goalMuscleGain) add("músculo")
                            if (goalDiabetic) add("diabéticos")
                        },
                        goals = Goals(
                            healthy = goalHealthy,
                            weightLoss = goalWeightLoss,
                            muscleGain = goalMuscleGain,
                            diabeticFriendly = goalDiabetic
                        ),
                        mealSuitability = MealSuitability(
                            breakfast = forBreakfast,
                            lunch = forLunch,
                            dinner = forDinner
                        ),
                        nutrition = Nutrition(
                            perServing = NutritionValues(
                                kcal = kcal.toDoubleOrNull() ?: 0.0,
                                protein_g = protein.toDoubleOrNull() ?: 0.0,
                                carbohydrate_g = carbs.toDoubleOrNull() ?: 0.0,
                                fat_g = fat.toDoubleOrNull() ?: 0.0
                            )
                        )
                    )

                    scope.launch {
                        viewModel.insertInitialData(listOf(newRecipe))
                        
                        if (uploadToCloud) {
                            val success = viewModel.uploadRecipeToCloud(newRecipe)
                            if (success) {
                                snackbarHostState.showSnackbar("¡Receta guardada y subida a la nube!")
                            } else {
                                snackbarHostState.showSnackbar("Guardada local (Error en nube)")
                            }
                        } else {
                            snackbarHostState.showSnackbar("¡Receta guardada localmente!")
                        }
                        onBackClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = title.isNotBlank() && ingredients.isNotBlank() && instructions.isNotBlank()
            ) {
                Text("GUARDAR RECETA", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Tus recetas manuales aparecerán junto con las recetas oficiales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}
