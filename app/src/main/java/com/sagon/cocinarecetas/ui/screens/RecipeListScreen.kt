package com.sagon.cocinarecetas.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.rounded.Info
import com.sagon.cocinarecetas.data.local.JsonAssetImporter
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.ui.viewmodel.RecipeViewModel
import com.sagon.cocinarecetas.util.SoundUtil

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecipeListScreen(
    viewModel: RecipeViewModel,
    onRecipeClick: (Recipe) -> Unit,
    onWeeklyMenuClick: () -> Unit,
    onHealthClick: () -> Unit
) {
    val context = LocalContext.current
    val recipes by viewModel.recipes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val selectedTag by viewModel.selectedHealthTag.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    var showOnlyFavorites by remember { mutableStateOf(false) }
    var recipeToHide by remember { mutableStateOf<Recipe?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) } // Para el menú de los 3 puntos

    val healthTags = listOf(
        "Sana", "Perder peso", "Músculo", "Diabéticos"
    )

    val families = listOf(
        "Aperitivos", "Ensaladas", "Legumbres", "Sopas", "Arroces", 
        "Pastas", "Verduras", "Pescados", "Carnes", "Postres", "Salsas"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "CocinaREcetas", 
                        style = MaterialTheme.typography.titleLarge, // Más grande y profesional
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                actions = {
                    IconButton(onClick = { SoundUtil.playBeep(); onWeeklyMenuClick() }) {
                        Icon(Icons.Rounded.CalendarMonth, "Menú Semanal", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { SoundUtil.playBeep(); onHealthClick() }) {
                        Icon(Icons.Rounded.Check, "Salud", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = { SoundUtil.playBeep(); showOnlyFavorites = !showOnlyFavorites }) {
                        Icon(
                            imageVector = if (showOnlyFavorites) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favoritos",
                            tint = if (showOnlyFavorites) Color.Red else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    // --- MENÚ DE TRES PUNTOS PARA LO MENOS FRECUENTE ---
                    Box {
                        IconButton(onClick = { SoundUtil.playBeep(); showMenu = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert, 
                                contentDescription = "Más", 
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Información Legal") },
                                onClick = { 
                                    showMenu = false
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://asurpan.github.io/Cocinarecetas/privacy.html"))
                                    context.startActivity(intent)
                                },
                                leadingIcon = { Icon(Icons.Rounded.Info, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Resetear App") },
                                onClick = { 
                                    showMenu = false
                                    showResetDialog = true 
                                },
                                leadingIcon = { Icon(Icons.Rounded.RestartAlt, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Column {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    // Lista de Sugerencias Automáticas
                    if (searchSuggestions.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchSuggestions) { suggestion ->
                                SuggestionChip(
                                    onClick = { 
                                        SoundUtil.playBeep()
                                        viewModel.onSearchQueryChange(suggestion) 
                                    },
                                    label = { Text(suggestion, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }

                    val healthScrollState = rememberLazyListState()
                    // Efecto de sonido al deslizar filtros de salud
                    LaunchedEffect(healthScrollState.firstVisibleItemIndex) {
                        if (healthScrollState.isScrollInProgress) {
                            SoundUtil.playBeep()
                        }
                    }

                    LazyRow(
                        state = healthScrollState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(healthTags) { tag ->
                            FilterChip(
                                selected = selectedTag == tag,
                                onClick = {
                                    SoundUtil.playBeep()
                                    viewModel.onHealthTagToggle(tag)
                                },
                                label = { Text(tag) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                leadingIcon = if (selectedTag == tag) {
                                    { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            // Selector de Familias (Categorías) con estilo más limpio
            Text(
                text = "FAMILIAS DE RECETAS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            val familiesScrollState = rememberLazyListState()
            // Efecto de sonido al deslizar familias
            LaunchedEffect(familiesScrollState.firstVisibleItemIndex) {
                if (familiesScrollState.isScrollInProgress) {
                    SoundUtil.playBeep()
                }
            }

            LazyRow(
                state = familiesScrollState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(families) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            SoundUtil.playBeep()
                            viewModel.onCategoryToggle(category)
                        },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f)
                        )
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isLoading) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Buscando recetas...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (recipes.isEmpty() && (searchQuery.isNotEmpty() || selectedTag.isNotEmpty() || selectedCategory.isNotEmpty())) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("No hay recetas con estos filtros", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                viewModel.onSearchQueryChange("")
                                viewModel.onHealthTagToggle("")
                                viewModel.onCategoryToggle("")
                            }) {
                                Text("Ver todas las recetas")
                            }
                        }
                    }
                } else {
                    items(
                        if (showOnlyFavorites) recipes.filter { it.isFavorite } else recipes
                    ) { recipe ->
                        RecipeItem(
                            recipe = recipe,
                            onClick = { onRecipeClick(recipe) },
                            onLongClick = { recipeToHide = recipe },
                            onFavoriteToggle = { viewModel.toggleFavorite(recipe) }
                        )
                    }
                }

                if (!isLoading && recipes.isNotEmpty() && recipes.size < 5 && (searchQuery.isNotEmpty() || selectedTag.isNotEmpty() || selectedCategory.isNotEmpty())) {
                    item {
                        TextButton(
                            onClick = {
                                viewModel.onSearchQueryChange("")
                                viewModel.onHealthTagToggle("")
                                viewModel.onCategoryToggle("")
                            },
                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                        ) {
                            Text("¿Quieres ver más? Quitar todos los filtros")
                        }
                    }
                }
            }
        }
    }

    if (recipeToHide != null) {
        AlertDialog(
            onDismissRequest = { recipeToHide = null },
            title = { Text("Ocultar receta") },
            text = { Text("¿Deseas ocultar esta receta localmente?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        recipeToHide?.let { viewModel.deleteRecipeLocally(it) }
                        recipeToHide = null
                    }
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { recipeToHide = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Resetear aplicación") },
            text = { Text("Se restaurarán todas las recetas que hayas ocultado previamente. ¿Deseas continuar?") },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(
                        onClick = {
                            val localRecipes = JsonAssetImporter.loadRecipesFromAsset(context, "recipes.json")
                            viewModel.forceReloadFromAssets(localRecipes)
                            showResetDialog = false
                        }
                    ) {
                        Text("IMPORTAR DATOS LIMPIOS (v3.0)", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = {
                            viewModel.restoreAllRecipes()
                            showResetDialog = false
                        }
                    ) {
                        Text("RESTAURAR OCULTAS")
                    }
                    TextButton(
                        onClick = { showResetDialog = false }
                    ) {
                        Text("CANCELAR", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            dismissButton = null
        )
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = { onQueryChange(it) },
        modifier = modifier,
        placeholder = { 
            Text(
                text = "BUSCAR RECETA O INGREDIENTE...",
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.bodySmall
            ) 
        },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { 
                    SoundUtil.playBeep()
                    onQueryChange("") 
                }) {
                    // Usamos Icons.Rounded.Clear en lugar de Close para mayor compatibilidad
                    Icon(
                        imageVector = Icons.Rounded.Close, 
                        contentDescription = "Borrar"
                    )
                }
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = MaterialTheme.shapes.medium,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = true
        )
    )
}

@Composable
fun RecipeItem(
    recipe: Recipe,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    // Limpiamos el título
    val displayTitle = remember(recipe.title) {
        val title = recipe.title.trim()
        if (title.contains(".") || title.contains(":") || title.length > 50) {
            val cleanCandidate = title.split(".", ":", ",", ";")[0].trim()
            if (cleanCandidate.length > 40) cleanCandidate.take(40).plus("...") else cleanCandidate
        } else {
            title
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {
                    SoundUtil.playBeep()
                    onClick()
                },
                onLongClick = {
                    SoundUtil.playBeep()
                    onLongClick()
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = recipe.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // Etiqueta MAGISTRAL / INTELIGENTE
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text(
                            text = "MAGISTRAL",
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    lineHeight = 20.sp
                )
                
                if (recipe.nutrition.perServing.kcal != null) {
                    Text(
                        text = "⚡ ${recipe.nutrition.perServing.kcal?.toInt()} kcal | 💪 ${recipe.nutrition.perServing.protein_g?.toInt()}g prot",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            
            IconButton(onClick = {
                SoundUtil.playBeep()
                onFavoriteToggle()
            }) {
                Icon(
                    imageVector = if (recipe.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (recipe.isFavorite) Color.Red else Color.LightGray
                )
            }
        }
    }
}
