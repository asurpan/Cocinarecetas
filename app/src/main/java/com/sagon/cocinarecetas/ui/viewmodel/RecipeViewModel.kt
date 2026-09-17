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

    private val _selectedPeriod = MutableStateFlow("Semana") // "Semana", "Mes"
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

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _weeklyMenu = MutableStateFlow<Map<Int, DayMenu>>(emptyMap())
    val weeklyMenu: StateFlow<Map<Int, DayMenu>> = _weeklyMenu.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _syncStatus = MutableStateFlow("Al día")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val recipes: StateFlow<List<Recipe>> = combine(_searchQuery, _selectedHealthTag, _selectedCategory) { query, healthTag, category ->
        Triple(query, healthTag, category)
    }
        .debounce { (query, healthTag, category) ->
            if (query.isEmpty() && healthTag.isEmpty() && category.isEmpty()) 0 else 150
        }
        .flatMapLatest { (query, healthTag, category) ->
            _isLoading.value = true
            val normalizedQuery = normalizeForSearch(query)
            val queryToSearch = if (normalizedQuery.length > 3) {
                when {
                    normalizedQuery.endsWith("es") && !normalizedQuery.endsWith("champiñones") -> normalizedQuery.dropLast(2)
                    normalizedQuery.endsWith("s") && !normalizedQuery.endsWith("albondigas") && !normalizedQuery.endsWith("champiñones") -> normalizedQuery.dropLast(1)
                    else -> normalizedQuery
                }
            } else normalizedQuery

            repository.searchRecipes(queryToSearch, category.lowercase()).flatMapLatest { list ->
                flowOf(list.filter { recipe ->
                    val normTitle = normalizeForSearch(recipe.title)
                    val normIngs = recipe.ingredients.map { normalizeForSearch(it) }
                    val match = if (queryToSearch.isEmpty()) true 
                               else normTitle.contains(queryToSearch) || normIngs.any { it.contains(queryToSearch) }
                    match && isRecipeAptForHealthTag(recipe, healthTag)
                })
            }
        }
        .onEach { _isLoading.value = false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun isRecipeAptForHealthTag(recipe: Recipe, healthTag: String): Boolean {
        if (healthTag.isEmpty()) return true
        val tag = healthTag.lowercase().trim()
        
        // 1. Verificación por Objetivos Estructurados (Solo si el campo NO es nulo)
        val goalMatch: Boolean? = when (tag) {
            "sana" -> recipe.goals.healthy
            "perder peso" -> recipe.goals.weightLoss
            "músculo", "musculo" -> recipe.goals.muscleGain
            "diabéticos", "diabetico" -> recipe.goals.diabeticFriendly
            "alto en proteina" -> recipe.goals.highProtein
            "bajo en carbohidratos" -> recipe.goals.lowCarb
            else -> null
        }
        
        // Si el objetivo está marcado explícitamente como TRUE, aceptamos
        if (goalMatch == true) return true
        
        // 2. Respaldo: Verificación por Etiquetas Clásicas (Para las 1.453 recetas originales)
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
        if (recipes.isEmpty()) {
            Log.e("RecipeViewModel", "insertInitialData: ¡La lista de recetas está VACÍA!")
            return
        }
        viewModelScope.launch {
            val currentCount = repository.getRecipeCount()
            Log.d("RecipeViewModel", "insertInitialData: Count actual = $currentCount. Intentando insertar ${recipes.size} recetas.")
            if (currentCount < 100) {
                repository.clearAll()
                recipes.chunked(100).forEach { repository.insertRecipes(it) }
                Log.d("RecipeViewModel", "insertInitialData: Inserción completada con éxito.")
            }
        }
    }

    fun forceReloadFromAssets(recipes: List<Recipe>) {
        if (recipes.isEmpty()) {
            _syncStatus.value = "Error: Sin recetas."
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                _syncStatus.value = "Limpiando..."
                repository.clearAll()
                
                _syncStatus.value = "Importando..."
                recipes.chunked(100).forEach { chunk ->
                    repository.insertRecipes(chunk)
                }
                
                _syncStatus.value = "¡Completado!"
                _isLoading.value = false
                Log.d("RecipeViewModel", "Carga local finalizada.")
            } catch (e: Exception) {
                _isLoading.value = false
                _syncStatus.value = "Error"
            }
        }
    }
    }

    private val breakfastWildcards = listOf(
        Recipe(title = "Tostadas integrales con AOVE y tomate", category = "DESAYUNO", nutrition = Nutrition(perServing = NutritionValues(kcal = 250.0, protein_g = 8.0)), mealSuitability = MealSuitability(breakfast = true)),
        Recipe(title = "Tortilla francesa (2 huevos) con pavo", category = "DESAYUNO", nutrition = Nutrition(perServing = NutritionValues(kcal = 220.0, protein_g = 18.0)), mealSuitability = MealSuitability(breakfast = true)),
        Recipe(title = "Yogur natural con nueces y fruta", category = "DESAYUNO", nutrition = Nutrition(perServing = NutritionValues(kcal = 180.0, protein_g = 10.0)), mealSuitability = MealSuitability(breakfast = true)),
        Recipe(title = "Avena con leche y canela", category = "DESAYUNO", nutrition = Nutrition(perServing = NutritionValues(kcal = 300.0, protein_g = 12.0)), mealSuitability = MealSuitability(breakfast = true)),
        Recipe(title = "Lonchas de pavo extra con queso fresco", category = "DESAYUNO", nutrition = Nutrition(perServing = NutritionValues(kcal = 150.0, protein_g = 22.0)), mealSuitability = MealSuitability(breakfast = true)),
        Recipe(title = "Tostada de pan de centeno con aguacate", category = "DESAYUNO", nutrition = Nutrition(perServing = NutritionValues(kcal = 280.0, protein_g = 6.0)), mealSuitability = MealSuitability(breakfast = true))
    )

    fun generateWeeklyMenu() {
        viewModelScope.launch {
            val healthTag = _selectedHealthTag.value
            val profile = _userProfile.value
            val targetKcal = profile.dailyCalorieTarget
            var pool = emptyList<Recipe>()
            repeat(3) {
                pool = repository.getRandomRecipesSample(800).filter { isRecipeAptForHealthTag(it, healthTag) }
                if (pool.size >= 50) return@repeat
                delay(400)
            }
            if (pool.isEmpty()) return@launch
            
            val usedIds = mutableSetOf<Int>()
            val menu = mutableMapOf<Int, DayMenu>()
            val weeklyIngredients = mutableSetOf<String>()
            val commonBlacklist = listOf("sal", "aceite", "agua", "pimienta", "azucar", "ajo", "cebolla")

            val breakfastPool = if (Random.nextFloat() < 0.8f) breakfastWildcards 
                               else pool.filter { it.mealSuitability.breakfast && !it.isMainDish }.let { if (it.isEmpty()) breakfastWildcards else it }
            
            val lunchPool = pool.filter { it.mealSuitability.lunch && !it.menuConstraints.mustNotBeUsedAsStandaloneMeal }
            val dinnerPool = pool.filter { it.mealSuitability.dinner && !it.menuConstraints.mustNotBeUsedAsStandaloneMeal }
            val weeklyCategories = mutableMapOf<String, Int>()

            for (day in 0 until 7) {
                val idealB = targetKcal * 0.20
                val breakfast = breakfastPool.filter { it.id !in usedIds }.minByOrNull { abs((it.nutrition.perServing.kcal ?: 350.0) - idealB) } ?: breakfastWildcards.random()
                usedIds.add(breakfast.id)

                val idealL = targetKcal * 0.45
                val mustPick = when {
                    (weeklyCategories["PESCADOS"] ?: 0) < 2 && day > 3 -> "PESCADOS"
                    (weeklyCategories["LEGUMBRES"] ?: 0) < 2 && day > 3 -> "LEGUMBRES"
                    else -> null
                }
                val lunchOptions = lunchPool.filter { it.id !in usedIds && (mustPick == null || it.category.uppercase() == mustPick) }
                val lunch = lunchOptions.minByOrNull { r ->
                    val bonus = r.ingredients.count { ing -> val clean = ing.lowercase(); weeklyIngredients.any { it in clean } && !commonBlacklist.any { it in clean } } * 50.0
                    abs((r.nutrition.perServing.kcal ?: 650.0) - idealL) - bonus
                } ?: lunchPool.filter { it.id !in usedIds }.randomOrNull() ?: pool.random()
                usedIds.add(lunch.id)
                lunch.ingredients.forEach { if(!commonBlacklist.any { b -> b in it.lowercase() }) weeklyIngredients.add(it.lowercase()) }
                weeklyCategories[lunch.category.uppercase()] = (weeklyCategories[lunch.category.uppercase()] ?: 0) + 1

                val idealD = targetKcal * 0.35
                val dinner = dinnerPool.filter { it.id !in usedIds && it.category.uppercase() != lunch.category.uppercase() }.minByOrNull { r ->
                    val bonus = r.ingredients.count { ing -> val clean = ing.lowercase(); weeklyIngredients.any { it in clean } && !commonBlacklist.any { it in clean } } * 30.0
                    abs((r.nutrition.perServing.kcal ?: 500.0) - idealD) - bonus
                } ?: dinnerPool.filter { it.id !in usedIds }.randomOrNull() ?: pool.random()
                usedIds.add(dinner.id)

                val totalKcal = (breakfast.nutrition.perServing.kcal ?: 0.0) + (lunch.nutrition.perServing.kcal ?: 0.0) + (dinner.nutrition.perServing.kcal ?: 0.0)
                val totalProt = (breakfast.nutrition.perServing.protein_g ?: 0.0) + (lunch.nutrition.perServing.protein_g ?: 0.0) + (dinner.nutrition.perServing.protein_g ?: 0.0)
                val tips = mutableListOf<String>()
                if (lunch.category.uppercase() == "LEGUMBRES") tips.add("💡 Truco: Aliña con limón para absorber el hierro.")
                if (totalProt < profile.dailyProteinTarget * 0.8) tips.add("⚠️ Tip: Añade un puñado de frutos secos para completar tu proteína.")
                menu[day] = DayMenu(breakfast, lunch, dinner, tips, totalKcal, totalProt)
            }
            _weeklyMenu.value = menu
        }
    }

    fun refreshMeal(dayIndex: Int, mealType: String) {
        viewModelScope.launch {
            val healthTag = _selectedHealthTag.value
            val profile = _userProfile.value
            val targetKcal = profile.dailyCalorieTarget
            val currentMenu = _weeklyMenu.value.toMutableMap()
            val dayMenu = currentMenu[dayIndex] ?: return@launch
            val usedIds = _weeklyMenu.value.values.flatMap { listOf(it.breakfast.id, it.lunch.id, it.dinner.id) }.toSet()
            val idealKcal = when(mealType) { "BREAKFAST" -> targetKcal * 0.20; "LUNCH" -> targetKcal * 0.45; "DINNER" -> targetKcal * 0.35; else -> 500.0 }
            val sampleRecipes = repository.getRandomRecipesSample(400).filter { r ->
                isRecipeAptForHealthTag(r, healthTag) && when (mealType) { "BREAKFAST" -> r.mealSuitability.breakfast; "LUNCH" -> r.mealSuitability.lunch; "DINNER" -> r.mealSuitability.dinner; else -> false } && !r.menuConstraints.mustNotBeUsedAsStandaloneMeal
            }.let { if (it.isEmpty()) repository.getRandomRecipesByCategories(when(mealType){"BREAKFAST"->listOf("POSTRES");"LUNCH"->listOf("ARROCES","PASTAS","LEGUMBRES","CARNES","PESCADOS");else->listOf("VERDURAS","ENSALADAS","SOPAS")}, 100).filter { r -> isRecipeAptForHealthTag(r, healthTag) } else it }.let { if(mealType=="BREAKFAST") it + breakfastWildcards else it }
            if (sampleRecipes.isEmpty()) return@launch
            val newRecipe = sampleRecipes.filter { it.id !in usedIds }.minByOrNull { abs((it.nutrition.perServing.kcal ?: idealKcal) - idealKcal) } ?: sampleRecipes.random()
            val b = if (mealType == "BREAKFAST") newRecipe else dayMenu.breakfast
            val l = if (mealType == "LUNCH") newRecipe else dayMenu.lunch
            val d = if (mealType == "DINNER") newRecipe else dayMenu.dinner
            val totalKcal = (b.nutrition.perServing.kcal ?: 0.0) + (l.nutrition.perServing.kcal ?: 0.0) + (d.nutrition.perServing.kcal ?: 0.0)
            val totalProt = (b.nutrition.perServing.protein_g ?: 0.0) + (l.nutrition.perServing.protein_g ?: 0.0) + (d.nutrition.perServing.protein_g ?: 0.0)
            currentMenu[dayIndex] = dayMenu.copy(breakfast = b, lunch = l, dinner = d, totalKcal = totalKcal, totalProtein = totalProt)
            _weeklyMenu.value = currentMenu
        }
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        val recipe = repository.getRecipeById(id)
        if (recipe != null) {
            // --- SELF-HEALING AUTOMÁTICO AL ABRIR ---
            val sanitized = RecipeSanitizer.sanitize(recipe)
            if (sanitized != recipe) {
                Log.d("SelfHealing", "Corrigiendo receta '${recipe.title}' automáticamente.")
                repository.updateRecipe(sanitized)
                return sanitized
            }
        }
        return recipe
    }
    
    // --- NOTA IMPORTANTE: FIREBASE DESACTIVADO PERMANENTEMENTE PARA PRIVILEGIAR ASSETS LOCALES ---
    fun syncWithCloud(localVersion: Long) { Log.d("Firebase", "Sincronización Cloud Desactivada.") }
    fun wipeAndUploadAll(recipes: List<Recipe>) { Log.d("Firebase", "Subida Cloud Desactivada.") }
    fun uploadInitialDataToCloud(recipes: List<Recipe>) { Log.d("Firebase", "Subida Inicial Cloud Desactivada.") }
    fun uploadRecipeToCloud(recipe: Recipe): Boolean = false

    private fun normalizeForSearch(t: String): String = t.trim().lowercase().replace("á","a").replace("é","e").replace("í","i").replace("ó","o").replace("ú","u").replace("ü","u").replace("ñ","n")
    private fun loadUserProfile(): UserProfile {
        return try { prefs.getString("user_profile", null)?.let { json.decodeFromString<UserProfile>(it) } ?: UserProfile() } catch (e: Exception) { UserProfile() }
    }
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
    private fun fuzzyMatch(q: String, t: String): Boolean {
        if (q.isEmpty()) return true; if (t.isEmpty()) return false
        var qi = 0; var ti = 0
        while (qi < q.length && ti < t.length) { if (q[qi] == t[ti]) qi++; ti++ }
        return qi == q.length
    }
}

class RecipeViewModelFactory(
    private val repository: RecipeRepository,
    private val prefs: SharedPreferences,
    private val initialSearch: String = "",
    private val initialHealthTag: String = "",
    private val initialCategory: String = ""
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecipeViewModel(repository, prefs, initialSearch, initialHealthTag, initialCategory) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
