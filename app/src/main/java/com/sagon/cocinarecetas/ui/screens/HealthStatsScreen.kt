package com.sagon.cocinarecetas.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagon.cocinarecetas.data.local.JsonAssetImporter
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModel
import com.sagon.cocinarecetas.data.model.HealthRecord
import com.sagon.cocinarecetas.data.model.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthStatsScreen(
    viewModel: RecipeViewModel,
    onBackClick: () -> Unit
) {
    val records by viewModel.filteredHealthRecords.collectAsState()
    val period by viewModel.selectedPeriod.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val allRecords by viewModel.allHealthRecords.collectAsState()
    val scope = rememberCoroutineScope()
    
    val today = System.currentTimeMillis() / (24 * 60 * 60 * 1000) * (24 * 60 * 60 * 1000)
    val todayRecord = allRecords.find { it.date == today } ?: HealthRecord(today)

    var showAddWeightDialog by remember { mutableStateOf(false) }
    var showAddCalDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Mi Salud y Evolución", 
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Ayuda", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar Perfil")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Tarjeta de Ayuda Rápida (Solo se muestra si es necesario o fija)
            if (todayRecord.caloriesConsumed == 0) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Consejo: Entra en una receta y pulsa 'MARCAR COMO CONSUMIDO' para ver cómo se llenan estos gráficos.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Resumen de Hoy
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Consumo Hoy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                            Text(
                                text = "${todayRecord.caloriesConsumed} / ${profile.dailyCalorieTarget} kcal", 
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                            CircularProgressIndicator(
                                progress = { (todayRecord.caloriesConsumed.toFloat() / profile.dailyCalorieTarget).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 8.dp,
                                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                            )
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Puedes ajustar calorías manualmente si has comido algo extra o menos de lo planeado.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        lineHeight = 12.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Botones de incremento rápido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { viewModel.addCalories(-50) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("-50", fontWeight = FontWeight.Bold) }
                        
                        Button(
                            onClick = { viewModel.addCalories(50) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("+50", fontWeight = FontWeight.Bold) }
                        
                        Button(
                            onClick = { viewModel.addCalories(100) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            shape = MaterialTheme.shapes.medium
                        ) { Text("+100", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            // Selector de Periodo
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = period == "Semana",
                    onClick = { viewModel.onPeriodChange("Semana") },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("Semana") }
                SegmentedButton(
                    selected = period == "Mes",
                    onClick = { viewModel.onPeriodChange("Mes") },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("Mes") }
            }

            // --- NUEVO: CONTADOR DE DIVERSIDAD DE PLANTAS (CIENCIA 2025) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                border = BorderStroke(1.dp, Color(0xFF8BC34A))
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "DIVERSIDAD DE PLANTAS SEMANAL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF33691E)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simulación de contador (En una versión futura se vinculará a los ingredientes reales)
                    val plantsThisWeek = (allRecords.take(7).size * 3 + 4).coerceIn(0, 30) 
                    
                    Text(
                        text = "$plantsThisWeek / 30",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF33691E)
                    )
                    
                    LinearProgressIndicator(
                        progress = { plantsThisWeek / 30f },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 8.dp),
                        color = Color(0xFF8BC34A),
                        trackColor = Color(0xFFDCEDC8)
                    )
                    
                    Text(
                        "CIENCIA 2025: Comer 30 tipos de plantas distintas a la semana mejora tu microbioma y protege tus células contra el envejecimiento.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = Color(0xFF558B2F),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // Dashboard de Macronutrientes
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Balance Nutricional (Hoy)", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Basado en estimaciones promedio", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MacroCircle(
                            current = todayRecord.proteinConsumed,
                            target = profile.dailyProteinTarget.toFloat(),
                            label = "Proteínas",
                            color = Color(0xFF42A5F5),
                            modifier = Modifier.size(80.dp)
                        )
                        MacroCircle(
                            current = todayRecord.carbsConsumed,
                            target = (profile.dailyCalorieTarget * 0.5 / 4).toFloat(), // Estimado 50% carbs
                            label = "Carbos",
                            color = Color(0xFFFFA726),
                            modifier = Modifier.size(80.dp)
                        )
                        MacroCircle(
                            current = todayRecord.fatConsumed,
                            target = (profile.dailyCalorieTarget * 0.3 / 9).toFloat(), // Estimado 30% grasas
                            label = "Grasas",
                            color = Color(0xFFEF5350),
                            modifier = Modifier.size(80.dp)
                        )
                    }
                }
            }

            // Gráfico de Peso
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Evolución de Peso", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val weightRecords = records.filter { it.weight != null }
                    if (weightRecords.size >= 2) {
                        LineChart(
                            data = weightRecords.map { it.weight!! },
                            target = profile.weight, // Podríamos usar un peso objetivo aquí
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    } else {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            Text("Registra tu peso para ver el progreso mensual.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Actual: ${profile.weight} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        IconButton(onClick = { showAddWeightDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
            }

            // Gráfico de Calorías
            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Consumo Calórico (${period})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (records.isNotEmpty()) {
                        BarChart(
                            data = records.map { it.caloriesConsumed.toFloat() },
                            target = profile.dailyCalorieTarget.toFloat(),
                            modifier = Modifier.fillMaxWidth().height(150.dp)
                        )
                    }
                    
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Objetivo: ${profile.dailyCalorieTarget} kcal", fontWeight = FontWeight.Medium)
                        IconButton(onClick = { showAddCalDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Guía de Salud 🩺", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. Registro Automático:", fontWeight = FontWeight.Bold)
                    Text("Al pulsar 'Marcar como consumido' en una receta, sumaremos sus calorías y nutrientes automáticamente.", fontSize = 13.sp)
                    
                    Text("2. Evolución de Peso:", fontWeight = FontWeight.Bold)
                    Text("Usa el botón '+' en la tarjeta de peso para anotar tu pesaje. Verás una línea que muestra si subes o bajas.", fontSize = 13.sp)
                    
                    Text("3. Macronutrientes:", fontWeight = FontWeight.Bold)
                    Text("Los círculos de colores te indican si llevas una dieta compensada en Proteínas, Carbohidratos y Grasas.", fontSize = 13.sp)
                    
                    HorizontalDivider()
                    Text("Nota: Todos los cálculos son ESTIMADOS basados en raciones promedio y en tu perfil.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            },
            confirmButton = { TextButton(onClick = { showHelpDialog = false }) { Text("ENTENDIDO") } }
        )
    }

    if (showAddWeightDialog) {
        var currentWeight by remember { mutableFloatStateOf(profile.weight) }
        AlertDialog(
            onDismissRequest = { showAddWeightDialog = false },
            title = { Text("Registrar Peso") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = String.format("%.1f kg", currentWeight), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { currentWeight -= 0.1f }, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        IconButton(onClick = { currentWeight += 0.1f }, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { currentWeight -= 1f }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("-1 kg")
                        }
                        Button(onClick = { currentWeight += 1f }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("+1 kg")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.updateWeight(currentWeight); showAddWeightDialog = false }) { Text("GUARDAR") } },
            dismissButton = { TextButton(onClick = { showAddWeightDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showAddCalDialog) {
        var caloriesToAdd by remember { mutableIntStateOf(100) }
        AlertDialog(
            onDismissRequest = { showAddCalDialog = false },
            title = { Text("Añadir Calorías") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "$caloriesToAdd kcal", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        IconButton(onClick = { if (caloriesToAdd > 10) caloriesToAdd -= 10 }, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)) {
                            Icon(Icons.Default.Remove, contentDescription = null)
                        }
                        IconButton(onClick = { caloriesToAdd += 10 }, modifier = Modifier.background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.medium)) {
                            Icon(Icons.Default.Add, contentDescription = null)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Button(onClick = { if (caloriesToAdd > 50) caloriesToAdd -= 50 }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("-50")
                        }
                        Button(onClick = { caloriesToAdd += 50 }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            Text("+50")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { viewModel.addCalories(caloriesToAdd); showAddCalDialog = false }) { Text("AÑADIR") } },
            dismissButton = { TextButton(onClick = { showAddCalDialog = false }) { Text("CANCELAR") } }
        )
    }

    if (showEditProfileDialog) {
        var age by remember { mutableIntStateOf(profile.age) }
        var weightTarget by remember { mutableFloatStateOf(profile.weight) }
        var height by remember { mutableFloatStateOf(profile.height) }
        var gender by remember { mutableStateOf(profile.gender) }
        
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Configurar Mi Perfil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tus datos ayudan a estimar objetivos saludables.", fontSize = 12.sp, color = Color.Gray)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Género", fontWeight = FontWeight.Bold)
                        Row {
                            FilterChip(
                                selected = gender == "Hombre",
                                onClick = { gender = "Hombre" },
                                label = { Text("Hombre") }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = gender == "Mujer",
                                onClick = { gender = "Mujer" },
                                label = { Text("Mujer") }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Edad", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (age > 10) age-- }) { Icon(Icons.Default.Remove, null) }
                            Text("$age", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { if (age < 100) age++ }) { Icon(Icons.Default.Add, null) }
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Peso (kg)", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { weightTarget -= 0.5f }) { Icon(Icons.Default.Remove, null) }
                            Text("${weightTarget}", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { weightTarget += 0.5f }) { Icon(Icons.Default.Add, null) }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Altura (cm)", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { height -= 1f }) { Icon(Icons.Default.Remove, null) }
                            Text("${height.toInt()}", style = MaterialTheme.typography.titleMedium)
                            IconButton(onClick = { height += 1f }) { Icon(Icons.Default.Add, null) }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    // ESPACIO PARA FUTURAS CONFIGURACIONES
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Versión del Recetario: 3.0 Premium (Maestro)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray.copy(alpha = 0.5f)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Cálculo automático de calorías (Mifflin-St Jeor)
                    val s = if (gender == "Hombre") 5 else -161
                    val baseKcal = (10 * weightTarget + 6.25 * height - 5 * age + s).toInt()
                    viewModel.saveUserProfile(profile.copy(
                        age = age,
                        weight = weightTarget,
                        height = height,
                        gender = gender,
                        dailyCalorieTarget = baseKcal
                    ))
                    showEditProfileDialog = false
                }) { Text("CALCULAR Y ACEPTAR") }
            }
        )
    }
}

@Composable
fun MacroCircle(current: Float, target: Float, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = modifier) {
            CircularProgressIndicator(
                progress = { (current / target).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                color = color,
                strokeWidth = 6.dp,
                trackColor = color.copy(alpha = 0.1f)
            )
            Text("${current.toInt()}g", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
    }
}

@Composable
fun LineChart(data: List<Float>, target: Float, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val max = maxOf(data.maxOrNull() ?: 1f, target) * 1.05f
        val min = minOf(data.minOrNull() ?: 0f, target) * 0.95f
        val range = max - min
        
        // Línea de objetivo
        val targetY = size.height - (size.height * ((target - min) / range))
        drawLine(Color.Gray.copy(alpha = 0.3f), Offset(0f, targetY), Offset(size.width, targetY), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))

        val path = Path()
        data.forEachIndexed { index, value ->
            val x = if (data.size > 1) size.width * (index.toFloat() / (data.size - 1)) else size.width / 2
            val y = size.height - (size.height * ((value - min) / range))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
    }
}

@Composable
fun BarChart(data: List<Float>, target: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val max = maxOf(data.maxOrNull() ?: 0f, target) * 1.2f
        val barWidth = size.width / (data.size * 1.5f).coerceAtLeast(1f)
        val spacing = (size.width - (barWidth * data.size)) / (data.size + 1).coerceAtLeast(1)
        
        data.forEachIndexed { index, value ->
            val x = spacing + (index * (barWidth + spacing))
            val h = if (max > 0) size.height * (value / max) else 0f
            val y = size.height - h
            drawRect(color = if (value > target && target > 0) Color(0xFFEF9A9A) else Color(0xFFA5D6A7), topLeft = Offset(x, y), size = Size(barWidth, h))
        }
    }
}
