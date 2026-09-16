package com.sagon.cocinarecetas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.automirrored.twotone.MenuBook
import androidx.compose.material.icons.twotone.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModel
import com.sagon.cocinarecetas.util.SoundUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipe: Recipe,
    viewModel: RecipeViewModel,
    onBackClick: () -> Unit,
    onMarkAsConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var notesText by remember(recipe.id) { mutableStateOf(recipe.notes) }
    val checkedIngredients = remember(recipe.id) { mutableStateMapOf<String, Boolean>() }
    val missingIngredients = recipe.ingredients.filter { !(checkedIngredients[it] ?: false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = recipe.title, style = MaterialTheme.typography.titleMedium, lineHeight = 18.sp)
                        Text(recipe.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = { SoundUtil.playBeep(); onBackClick() },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(8.dp).size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.TwoTone.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        SoundUtil.playBeep()
                        val shareText = "Receta: ${recipe.title}\n\nIngredientes:\n${recipe.ingredients.joinToString("\n") { "• $it" }}\n\nPreparación:\n${recipe.instructions.joinToString("\n") { "• $it" }}"
                        val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, shareText); type = "text/plain" }
                        context.startActivity(Intent.createChooser(sendIntent, "Enviar receta a..."))
                    }) {
                        Icon(Icons.TwoTone.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface)
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()
        var isEditingNotes by remember { mutableStateOf(false) }
        
        LaunchedEffect(notesText) {
            if (isEditingNotes && scrollState.maxValue > 0) scrollState.animateScrollTo(scrollState.maxValue)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp).imePadding()
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.TwoTone.AccessTime, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Tiempo de preparación: ${recipe.cookingTime}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Efecto de parpadeo/pulso para el botón
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.03f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(800, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )

                    // Botón premium para registrar en la dieta diaria - Ahora optimizado
                    Button(
                        onClick = {
                            SoundUtil.playBeep()
                            viewModel.addNutritionFromRecipe(recipe)
                            onMarkAsConsumed()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp) // Reducido de 56/64 a 52 para que sea más estilizado
                            .graphicsLayer(scaleX = scale, scaleY = scale),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.large,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                    ) {
                        Icon(Icons.TwoTone.DoneAll, contentDescription = null, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AÑADIR A MI DIETA DE HOY", 
                            fontSize = 14.sp, 
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        NutrientItem("Kcal", "${recipe.nutrition.perServing.kcal?.toInt() ?: "---"}")
                        NutrientItem("Proteína", "${recipe.nutrition.perServing.protein_g?.toInt() ?: "---"}g")
                        NutrientItem("Carbos", "${recipe.nutrition.perServing.carbohydrate_g?.toInt() ?: "---"}g")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // --- EL SEMÁFORO DEL ORDEN (BIO-SECUENCIACIÓN) ---
                    Text(
                        text = "EL SEMÁFORO DEL ORDEN (CÓMO COMER)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Sigue este orden para evitar el cansancio y no guardar grasa:",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black.copy(alpha = 0.05f), MaterialTheme.shapes.medium)
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().height(12.dp)) {
                            Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFF43A047))) // Vegetales
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(modifier = Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFF1E88E5))) // Proteína
                            Spacer(modifier = Modifier.width(2.dp))
                            Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFFFB8C00))) // Arroz/Pasta
                        }
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("1º VEGETALES", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text("2º PROTEÍNA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                            Text("3º ARROZ / PASTA", fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // --- PROTOCOLOS DE CIENCIA FÁCIL ---
                    val scienceInstructions = recipe.instructions.filter { 
                        it.contains("TIP", ignoreCase = true) || 
                        it.contains("CONSEJO", ignoreCase = true) || 
                        it.contains("CIENCIA", ignoreCase = true) 
                    }
                    
                    if (scienceInstructions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4C3)),
                            border = BorderStroke(2.dp, Color(0xFFC0CA33))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.TwoTone.Psychology, contentDescription = null, tint = Color(0xFF33691E))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CIENCIA FÁCIL PARA TI", fontWeight = FontWeight.Black, color = Color(0xFF33691E))
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                scienceInstructions.forEach { tip ->
                                    val readableTip = tip
                                        .replace("TIP CIENCIA 2025 (Pérdida de peso):", "EL TRUCO DEL DÍA DESPUÉS:")
                                        .replace("CONSEJO ALMIDÓN RESISTENTE:", "EL TRUCO DEL DÍA DESPUÉS:")
                                        .replace("TIP ADELGAZAMIENTO 2025:", "EL FRENO DEL HAMBRE:")
                                        .replace("CIENCIA DIABETES 2025:", "AZÚCAR BAJO CONTROL:")
                                        .replace("TIP SALUD:", "PROTOCOLO SALUD:")
                                        .replace("TIP MUSCULACIÓN 2025:", "MÚSCULO DE ÉLITE:")
                                        .replace("CIENCIA SALUD 2025:", "LONGEVIDAD ACTIVA:")
                                    
                                    Text(
                                        text = readableTip,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF33691E),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "Fuentes: Nature Metabolism 2024 / Cornell University",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = Color(0xFF689F38),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }

                    val parsedMinutes = remember(recipe.cookingTime) {
                        val numbers = "\\d+".toRegex().findAll(recipe.cookingTime).map { it.value.toInt() }.toList()
                        if (numbers.isNotEmpty()) numbers.last() else 15
                    }

                    var totalSecondsLeft by remember(recipe.id) { mutableStateOf(parsedMinutes * 60) }
                    var isTimerRunning by remember { mutableStateOf(false) }
                    var showTimePicker by remember { mutableStateOf(false) }

                    LaunchedEffect(isTimerRunning, totalSecondsLeft) {
                        if (isTimerRunning && totalSecondsLeft > 0) {
                            delay(1000L)
                            totalSecondsLeft -= 1
                            if (totalSecondsLeft == 0) {
                                isTimerRunning = false
                                SoundUtil.startLoopingAlarm(context)
                            }
                        }
                    }

                    val minutesDisplay = totalSecondsLeft / 60
                    val secondsDisplay = totalSecondsLeft % 60
                    val timeString = String.format(Locale.getDefault(), "%02d:%02d", minutesDisplay, secondsDisplay)

                    if (showTimePicker) {
                        AlertDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = { TextButton(onClick = { showTimePicker = false }) { Text("CERRAR") } },
                            title = { Text("Ajustar Tiempo") },
                            text = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = timeString,
                                        style = MaterialTheme.typography.displayMedium,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        // Bloque de -5 y -1
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            var isDec5Pressed by remember { mutableStateOf(false) }
                                            LaunchedEffect(isDec5Pressed) {
                                                if (isDec5Pressed) {
                                                    var first = true
                                                    while (isDec5Pressed) {
                                                        if (totalSecondsLeft >= 300) totalSecondsLeft -= 300
                                                        SoundUtil.playBeep()
                                                        delay(if (first) 500L else 200L)
                                                        first = false
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { if (totalSecondsLeft >= 300) totalSecondsLeft -= 300; SoundUtil.playBeep() },
                                                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                                    detectTapGestures(onPress = { isDec5Pressed = true; try { awaitRelease() } finally { isDec5Pressed = false } })
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                                            ) { Text("-5 min", fontWeight = FontWeight.Bold) }
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            var isDec1Pressed by remember { mutableStateOf(false) }
                                            LaunchedEffect(isDec1Pressed) {
                                                if (isDec1Pressed) {
                                                    var first = true
                                                    while (isDec1Pressed) {
                                                        if (totalSecondsLeft >= 60) totalSecondsLeft -= 60
                                                        SoundUtil.playBeep()
                                                        delay(if (first) 500L else 150L)
                                                        first = false
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { if (totalSecondsLeft >= 60) totalSecondsLeft -= 60; SoundUtil.playBeep() },
                                                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                                    detectTapGestures(onPress = { isDec1Pressed = true; try { awaitRelease() } finally { isDec1Pressed = false } })
                                                }
                                            ) { Text("-1 min") }
                                        }

                                        // Bloque de +1 y +5
                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                            var isInc5Pressed by remember { mutableStateOf(false) }
                                            LaunchedEffect(isInc5Pressed) {
                                                if (isInc5Pressed) {
                                                    var first = true
                                                    while (isInc5Pressed) {
                                                        totalSecondsLeft += 300
                                                        SoundUtil.playBeep()
                                                        delay(if (first) 500L else 200L)
                                                        first = false
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { totalSecondsLeft += 300; SoundUtil.playBeep() },
                                                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                                    detectTapGestures(onPress = { isInc5Pressed = true; try { awaitRelease() } finally { isInc5Pressed = false } })
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) { Text("+5 min", fontWeight = FontWeight.Bold) }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            var isInc1Pressed by remember { mutableStateOf(false) }
                                            LaunchedEffect(isInc1Pressed) {
                                                if (isInc1Pressed) {
                                                    var first = true
                                                    while (isInc1Pressed) {
                                                        totalSecondsLeft += 60
                                                        SoundUtil.playBeep()
                                                        delay(if (first) 500L else 150L)
                                                        first = false
                                                    }
                                                }
                                            }
                                            Button(
                                                onClick = { totalSecondsLeft += 60; SoundUtil.playBeep() },
                                                modifier = Modifier.fillMaxWidth().pointerInput(Unit) {
                                                    detectTapGestures(onPress = { isInc1Pressed = true; try { awaitRelease() } finally { isInc1Pressed = false } })
                                                }
                                            ) { Text("+1 min") }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Mantén pulsado para ajustar rápido", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                    
                                    Spacer(modifier = Modifier.height(24.dp))
                                    
                                    // Botón dinámico de prueba de sonido
                                    val isAlarmActive by SoundUtil.isAlarmActive
                                    
                                    Button(
                                        onClick = { 
                                            if (isAlarmActive) {
                                                SoundUtil.stopAlarm()
                                            } else {
                                                SoundUtil.startLoopingAlarm(context)
                                                // Detenemos automáticamente a los 10 segundos si no hace nada
                                                scope.launch {
                                                    delay(10000)
                                                    if (SoundUtil.isAlarmActive.value) SoundUtil.stopAlarm()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAlarmActive) Color.Red else MaterialTheme.colorScheme.secondary
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (isAlarmActive) Icons.TwoTone.VolumeOff else Icons.TwoTone.VolumeUp, 
                                            contentDescription = null
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isAlarmActive) "DETENER PRUEBA" else "PROBAR SONIDO ALARMA")
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Controles de ajuste rápido (+/- 1 min) + Display del tiempo
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { 
                                    SoundUtil.playBeep()
                                    if (totalSecondsLeft >= 60) totalSecondsLeft -= 60 
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.TwoTone.RemoveCircleOutline, 
                                    contentDescription = "Menos 1 min",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            Column(
                                modifier = Modifier
                                    .clickable { SoundUtil.playBeep(); showTimePicker = true }
                                    .padding(horizontal = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Temporizador", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = timeString, 
                                    style = MaterialTheme.typography.headlineLarge, 
                                    fontWeight = FontWeight.ExtraBold, 
                                    color = if (totalSecondsLeft == 0) Color.Red else MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            IconButton(
                                onClick = { 
                                    SoundUtil.playBeep()
                                    totalSecondsLeft += 60 
                                },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.TwoTone.AddCircleOutline, 
                                    contentDescription = "Más 1 min",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        if (totalSecondsLeft == 0) {
                            Button(
                                onClick = { SoundUtil.stopAlarm(); totalSecondsLeft = parsedMinutes * 60 },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("DETENER ALARMA", fontWeight = FontWeight.Black)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilledIconButton(onClick = { SoundUtil.playBeep(); isTimerRunning = !isTimerRunning }) {
                                    Icon(if (isTimerRunning) Icons.TwoTone.Pause else Icons.TwoTone.PlayArrow, contentDescription = null)
                                }
                                OutlinedIconButton(onClick = { SoundUtil.playBeep(); isTimerRunning = false; totalSecondsLeft = parsedMinutes * 60 }) {
                                    Icon(Icons.TwoTone.Refresh, contentDescription = null)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                shape = MaterialTheme.shapes.extraLarge
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(text = "Ingredientes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    recipe.ingredients.forEach { ingredient ->
                        val isChecked = checkedIngredients[ingredient] ?: false
                        IngredientItem(ingredient = ingredient, checked = isChecked, onCheckedChange = { checkedIngredients[ingredient] = it })
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (missingIngredients.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Lista de la Compra", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                            IconButton(onClick = {
                                SoundUtil.playBeep()
                                val listText = "🛒 *Lista de la Compra para: ${recipe.title}*\n\n" + 
                                    missingIngredients.joinToString("\n") { "• $it" } + 
                                    "\n\nGenerado por CocinaREcetas 🥘"
                                val sendIntent = Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, listText); type = "text/plain" }
                                context.startActivity(Intent.createChooser(sendIntent, "Compartir lista con..."))
                            }) {
                                Icon(Icons.TwoTone.Share, contentDescription = "Compartir lista", tint = MaterialTheme.colorScheme.tertiary)
                            }
                        }
                        missingIngredients.forEach { ingredient ->
                            Text(text = "• $ingredient", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Paso a Paso", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            
            val scienceKeywords = listOf("TIP", "CONSEJO", "CIENCIA", "TRUCO", "PASEO", "SINERGIA", "RENDIMIENTO", "ESTRATEGIA", "ORDEN", "ALMIDÓN")
            val prepSteps = recipe.instructions.filter { step ->
                !scienceKeywords.any { kw -> step.contains(kw, ignoreCase = true) }
            }
            
            if (prepSteps.isEmpty()) {
                Text("Consulta las notas o ciencia para la preparación de este básico.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                prepSteps.forEachIndexed { index, instruction ->
                    PreparationStep(index + 1, instruction)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "Tus Notas", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = notesText,
                onValueChange = { notesText = it; isEditingNotes = true },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
                trailingIcon = {
                    if (notesText != recipe.notes) {
                        IconButton(onClick = { SoundUtil.playBeep(); viewModel.updateRecipeNotes(recipe, notesText) }) {
                            Icon(Icons.Filled.Save, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun NutrientItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun IngredientItem(ingredient: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { SoundUtil.playBeep(); onCheckedChange(it) })
        Text(text = ingredient, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
fun PreparationStep(stepNumber: Int, instruction: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(28.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = stepNumber.toString(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Text(text = instruction, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}
