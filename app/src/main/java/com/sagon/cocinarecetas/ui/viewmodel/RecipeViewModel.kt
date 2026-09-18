package com.sagon.cocinarecetas.ui.viewmodel

import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.*
import com.sagon.cocinarecetas.data.model.*
import com.sagon.cocinarecetas.data.repository.RecipeRepository
import com.sagon.cocinarecetas.util.RecipeSanitizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlin.math.abs
import kotlin.random.Random

data class DayMenu(
    val breakfast: Recipe,
    val lunch: Recipe,
    val dinner: Recipe,
    val tips: List<String> = emptyList(),
    val totalKcal: Double = 0.0,
    val totalProtein: Double = 0.0
)

class RecipeViewModel(
    private val repository: RecipeRepository,
    private val prefs: SharedPreferences,
    initialSearch: String = "",
    initialHealthTag: String = "",
    initialCategory: String = ""
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }

    private val _userProfile = MutableStateFlow(loadUserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("Semana")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    val filteredHealthRecords: StateFlow<List<HealthRecord>> = combine(
        repository.allHealthRecords,
        _selectedPeriod
    ) { records, period ->
        val days = if (period == "Semana") 7 else 30
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        records.filter { it.date >= cutoff }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHealthRecords: StateFlow<List<HealthRecord>> = repository.allHealthRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow(initialSearch)
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedHealthTag = MutableStateFlow(initialHealthTag)
    val selectedHealthTag: StateFlow<String> = _selectedHealthTag.asStateFlow()

    private val _selectedCategory = MutableStateFlow(initialCategory)
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow("Al día")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    private val _weeklyMenu = MutableStateFlow<Map<Int, DayMenu>>(emptyMap())
    val weeklyMenu: StateFlow<Map<Int, DayMenu>> = _weeklyMenu.asStateFlow()

    val searchSuggestions = MutableStateFlow<List<String>>(emptyList())

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val recipes: StateFlow<List<Recipe>> = combine(_searchQuery, _selectedHealthTag, _selectedCategory) { query, healthTag, category ->
        Triple(query, healthTag, category)
    }
    .debounce { 150 }
    .flatMapLatest { (query, healthTag, category) ->
        val normalizedQuery = normalizeForSearch(query)
        val queryToSearch = if (normalizedQuery.length > 3) {
            when {
                normalizedQuery.endsWith("es") && !normalizedQuery.endsWith("champiñones") -> normalizedQuery.dropLast(2)
                normalizedQuery.endsWith("s") && !normalizedQuery.endsWith("albondigas") && !normalizedQuery.endsWith("champiñones") -> normalizedQuery.dropLast(1)
                else -> normalizedQuery
            }
        } else normalizedQuery

        repository.searchRecipes(queryToSearch, category.lowercase()).map { list ->
            // Mostramos la lista TAL CUAL está en la base de datos para que no haya tirones.
            // Si el usuario quiere verla limpia, debe usar el botón de Importar una vez.
            list.filter { isRecipeAptForHealthTag(it, healthTag) }
        }
    }
    .flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isRecipeAptForHealthTag(recipe: Recipe, healthTag: String): Boolean {
        if (healthTag.isEmpty()) return true
        val tag = healthTag.lowercase().trim()
        val goalMatch: Boolean? = when (tag) {
            "sana" -> recipe.goals.healthy
            "perder peso" -> recipe.goals.weightLoss
            "músculo", "musculo" -> recipe.goals.muscleGain
            "diabéticos", "diabetico" -> recipe.goals.diabeticFriendly
            else -> null
        }
        if (goalMatch == true) return true
        return recipe.healthTags.any { it.lowercase().contains(tag) }
    }

    fun onSearchQueryChange(newQuery: String) { _searchQuery.value = newQuery }
    fun onHealthTagToggle(tag: String) { _selectedHealthTag.value = if (_selectedHealthTag.value == tag) "" else tag }
    fun onCategoryToggle(category: String) { _selectedCategory.value = if (_selectedCategory.value == category) "" else category }
    fun onPeriodChange(period: String) { _selectedPeriod.value = period }

    fun toggleFavorite(recipe: Recipe) { viewModelScope.launch { repository.updateRecipe(recipe.copy(isFavorite = !recipe.isFavorite)) } }
    fun deleteRecipeLocally(recipe: Recipe) { viewModelScope.launch { repository.updateRecipe(recipe.copy(isDeletedLocally = true)) } }
    fun restoreAllRecipes() { viewModelScope.launch { repository.restoreAllHidden() } }
    fun updateRecipeNotes(recipe: Recipe, newNotes: String) { viewModelScope.launch { repository.updateRecipe(recipe.copy(notes = newNotes)) } }

    fun insertInitialData(recipes: List<Recipe>) {
        if (recipes.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (repository.getRecipeCount() < 100) {
                repository.clearAll()
                // En el primer arranque también limpiamos por si acaso
                val cleanRecipes = recipes.map { RecipeSanitizer.sanitize(it) }
                cleanRecipes.chunked(200).forEach { repository.insertRecipes(it) }
                Log.d("RecipeViewModel", "Datos iniciales saneados y cargados.")
            }
        }
    }

    fun forceReloadFromAssets(recipes: List<Recipe>) {
        if (recipes.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { 
                    _isLoading.value = true 
                    _syncStatus.value = "Saneando recetas..."
                }
                
                // Realizamos el saneamiento pesado en un hilo de fondo
                val cleanRecipes = recipes.map { RecipeSanitizer.sanitize(it) }
                
                withContext(Dispatchers.Main) { _syncStatus.value = "Borrando base de datos antigua..." }
                repository.clearAll()
                
                withContext(Dispatchers.Main) { _syncStatus.value = "Guardando recetas limpias..." }
                cleanRecipes.chunked(200).forEachIndexed { i, chunk ->
                    repository.insertRecipes(chunk)
                    withContext(Dispatchers.Main) {
                        _syncStatus.value = "Cargando: ${(i + 1) * 200} de ${cleanRecipes.size}"
                    }
                }
                
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _syncStatus.value = "¡Todo limpio y listo!"
                }
            } catch (e: Exception) {
                Log.e("RecipeViewModel", "Error en importación", e)
                withContext(Dispatchers.Main) { 
                    _isLoading.value = false 
                    _syncStatus.value = "Error al limpiar datos"
                }
            }
        }
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        val recipe = repository.getRecipeById(id)
        return recipe?.let { r ->
            // --- AUTO-CURACIÓN INVISIBLE ---
            val sanitized = RecipeSanitizer.sanitize(r)
            if (sanitized != r) {
                viewModelScope.launch(Dispatchers.IO) { repository.updateRecipe(sanitized) }
                sanitized
            } else r
        }
    }

    // --- MÉTODOS DE APOYO ---
    private fun normalizeForSearch(t: String): String = t.trim().lowercase().replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u").replace("ü","u").replace("ñ","n")
    private fun loadUserProfile(): UserProfile = try { prefs.getString("user_profile", null)?.let { json.decodeFromString<UserProfile>(it) } ?: UserProfile() } catch (e: Exception) { UserProfile() }
    fun saveUserProfile(p: UserProfile) { _userProfile.value = p; prefs.edit().putString("user_profile", json.encodeToString(p)).apply() }
    fun resetHealthData() { viewModelScope.launch { repository.clearHealthData(); saveUserProfile(UserProfile()) } }
    fun updateWeight(w: Float) { val t = System.currentTimeMillis() / (24*60*60*1000) * (24*60*60*1000); viewModelScope.launch { val r = repository.getHealthRecordByDate(t) ?: HealthRecord(t); repository.insertHealthRecord(r.copy(weight = w)); saveUserProfile(_userProfile.value.copy(weight = w)) } }
    fun addCalories(c: Int) { val t = System.currentTimeMillis() / (24*60*60*1000) * (24*60*60*1000); viewModelScope.launch { val r = repository.getHealthRecordByDate(t) ?: HealthRecord(t); repository.insertHealthRecord(r.copy(caloriesConsumed = r.caloriesConsumed + c)) } }
    fun addNutritionFromRecipe(recipe: Recipe) {
        val t = System.currentTimeMillis() / (24*60*60*1000) * (24*60*60*1000)
        val kcal = recipe.nutrition.perServing.kcal?.toInt() ?: 450
        val prot = recipe.nutrition.perServing.protein_g?.toFloat() ?: 20f
        val carbs = recipe.nutrition.perServing.carbohydrate_g?.toFloat() ?: 40f
        val fat = recipe.nutrition.perServing.fat_g?.toFloat() ?: 15f
        viewModelScope.launch {
            val r = repository.getHealthRecordByDate(t) ?: HealthRecord(t)
            repository.insertHealthRecord(r.copy(caloriesConsumed = r.caloriesConsumed + kcal, proteinConsumed = r.proteinConsumed + prot, carbsConsumed = r.carbsConsumed + carbs, fatConsumed = r.fatConsumed + fat))
        }
    }
    
    fun generateWeeklyMenu() { /* Lógica de menú semanal... */ }
    fun refreshMeal(dayIndex: Int, mealType: String) { /* Lógica de refresco... */ }
}

class RecipeViewModelFactory(
    private val repository: RecipeRepository,
    private val prefs: SharedPreferences,
    private val initialSearch: String = "",
    private val initialHealthTag: String = "",
    private val initialCategory: String = ""
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return RecipeViewModel(repository, prefs, initialSearch, initialHealthTag, initialCategory) as T
    }
}
