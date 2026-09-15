package com.sagon.cocinarecetas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.twotone.ArrowBack
import androidx.compose.material.icons.twotone.ChevronRight
import androidx.compose.material.icons.twotone.Lightbulb
import androidx.compose.material.icons.twotone.Refresh
import androidx.compose.material.icons.twotone.RestaurantMenu
import androidx.compose.material.icons.twotone.WbTwilight
import androidx.compose.material.icons.twotone.WbSunny
import androidx.compose.material.icons.twotone.NightsStay
import androidx.compose.material.icons.twotone.Share
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModel
import com.sagon.cocinarecetas.util.SoundUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyMenuScreen(
    viewModel: RecipeViewModel,
    onBackClick: () -> Unit,
    onRecipeClick: (Recipe) -> Unit
) {
    val weeklyMenu by viewModel.weeklyMenu.collectAsState()
    val selectedTag by viewModel.selectedHealthTag.collectAsState()
    val days = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
    val healthTags = listOf("Sana", "Perder peso", "Músculo", "Diabéticos")
    var showMethodology by remember { mutableStateOf(false) }

    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plan Nutricional Semanal") },
                navigationIcon = {
                    Surface(
                        onClick = {
                            SoundUtil.playBeep()
                            onBackClick()
                        },
                        shape = androidx.compose.foundation.shape.CircleShape,
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
                        val header = if (selectedTag.isNotEmpty()) "🥘 *Mi Plan Nutricional ($selectedTag)*\n\n" else "🥘 *Mi Plan Nutricional CocinaREcetas*\n\n"
                        val shareText = StringBuilder(header)
                        weeklyMenu.toSortedMap().forEach { (index, dayMenu) ->
                            val dayName = days.getOrElse(index) { "Día ${index + 1}" }
                            shareText.append("*$dayName* (${dayMenu.totalKcal.toInt()} kcal | ${dayMenu.totalProtein.toInt()}g prot)\n")
                            shareText.append("🌅 Desayuno: ${dayMenu.breakfast.title}\n")
                            shareText.append("☀️ Almuerzo: ${dayMenu.lunch.title}\n")
                            shareText.append("🌙 Cena: ${dayMenu.dinner.title}\n\n")
                        }
                        shareText.append("Generado por CocinaREcetas DE JOSEMAN 👨‍🍳")
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText.toString())
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir Menú"))
                    }) {
                        Icon(Icons.TwoTone.Share, contentDescription = "Compartir menú", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        SoundUtil.playBeep()
                        showMethodology = true
                    }) {
                        Icon(Icons.TwoTone.Lightbulb, contentDescription = "Metodología", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        SoundUtil.playBeep()
                        viewModel.generateWeeklyMenu()
                    }) {
                        Icon(Icons.TwoTone.Refresh, contentDescription = "Regenerar menú", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding)) {
            // Selector de objetivo de salud en el menú
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(healthTags) { tag ->
                        FilterChip(
                            selected = selectedTag == tag,
                            onClick = {
                                SoundUtil.playBeep()
                                viewModel.onHealthTagToggle(tag)
                                viewModel.generateWeeklyMenu()
                            },
                            label = { Text(tag) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (showMethodology) {
                AlertDialog(
                    onDismissRequest = { showMethodology = false },
                    confirmButton = {
                        TextButton(onClick = { showMethodology = false }) { Text("ENTENDIDO") }
                    },
                    title = { Text("🧬 Metodología Científica") },
                    text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            MethodologyItem("🌓 Ritmo Circadiano", "Almuerzos complejos y cenas ligeras para optimizar el sueño y la digestión.")
                            MethodologyItem("📉 Índice Glucémico", "Priorizamos la creación de almidón resistente enfriando hidratos para controlar la insulina.")
                            MethodologyItem("🧪 Biodisponibilidad", "Sugerimos sinergias como Vitamina C con Legumbres para maximizar la absorción de Hierro.")
                            MethodologyItem("🔄 Variedad Inteligente", "Rotación obligatoria de familias para asegurar un perfil completo de aminoácidos y vitaminas.")
                            MethodologyItem("💰 Cocina de Aprovechamiento", "Nuestro algoritmo prioriza recetas que compartan ingredientes perecederos. ¡Ahorra dinero y evita el desperdicio!")
                            MethodologyItem("💸 Economía Circular", "Algoritmo de aprovechamiento de ingredientes para reducir el desperdicio y ahorrar.")
                        }
                    }
                )
            }

            // Banner Promocional de Cocina de Aprovechamiento
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.TwoTone.Lightbulb,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            "¡NUEVO: AHORRO INTELIGENTE! 💰",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            "Tu menú ahora prioriza recetas que comparten ingredientes para que ahorres dinero y no tires nada.",
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(weeklyMenu.entries.toList()) { entry ->
                    val dayName = days.getOrElse(entry.key) { "Día ${entry.key + 1}" }
                    val dayMenu = entry.value
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.TwoTone.RestaurantMenu, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = dayName.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                // Badge Nutricional del día
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "${dayMenu.totalKcal.toInt()} kcal | ${dayMenu.totalProtein.toInt()}g prot",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            MealItem("DESAYUNO", Icons.TwoTone.WbTwilight, MaterialTheme.colorScheme.tertiary, dayMenu.breakfast, onRecipeClick, onRefresh = { viewModel.refreshMeal(entry.key, "BREAKFAST") })
                            Spacer(modifier = Modifier.height(14.dp))
                            MealItem("ALMUERZO", Icons.TwoTone.WbSunny, MaterialTheme.colorScheme.secondary, dayMenu.lunch, onRecipeClick, onRefresh = { viewModel.refreshMeal(entry.key, "LUNCH") })
                            Spacer(modifier = Modifier.height(14.dp))
                            MealItem("CENA", Icons.TwoTone.NightsStay, MaterialTheme.colorScheme.primary, dayMenu.dinner, onRecipeClick, onRefresh = { viewModel.refreshMeal(entry.key, "DINNER") })

                            if (dayMenu.tips.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(20.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Column {
                                    dayMenu.tips.forEach { tip ->
                                        Row(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.TwoTone.Lightbulb, 
                                                contentDescription = null, 
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = tip.replace("💡 ", ""),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MethodologyItem(title: String, description: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = description, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun MealItem(label: String, icon: ImageVector, iconColor: Color, recipe: Recipe, onClick: (Recipe) -> Unit, onRefresh: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SoundUtil.playBeep()
                onClick(recipe)
            },
        shape = MaterialTheme.shapes.medium,
        color = iconColor.copy(alpha = 0.05f),
        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = iconColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = iconColor,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = recipe.title, 
                    style = MaterialTheme.typography.bodyMedium, 
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )
            }
            IconButton(
                onClick = {
                    SoundUtil.playBeep()
                    onRefresh()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.TwoTone.Refresh, 
                    contentDescription = "Cambiar receta", 
                    modifier = Modifier.size(22.dp), 
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.TwoTone.ChevronRight, 
                contentDescription = null, 
                modifier = Modifier.size(24.dp), 
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        }
    }
}
