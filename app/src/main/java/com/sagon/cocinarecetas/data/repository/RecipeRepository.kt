package com.sagon.cocinarecetas.data.repository

import android.util.Log
import com.sagon.cocinarecetas.data.local.RecipeDao
import com.sagon.cocinarecetas.data.local.HealthRecordDao
import com.sagon.cocinarecetas.data.remote.FirestoreService
import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.data.model.HealthRecord
import kotlinx.coroutines.flow.Flow

class RecipeRepository(
    private val recipeDao: RecipeDao,
    private val healthRecordDao: HealthRecordDao,
    private val firestoreService: FirestoreService
) {
    val allRecipes: Flow<List<Recipe>> = recipeDao.getAllRecipes()
    val favoriteRecipes: Flow<List<Recipe>> = recipeDao.getFavoriteRecipes()
    val allHealthRecords: Flow<List<HealthRecord>> = healthRecordDao.getAllRecords()

    fun getHealthRecordsSince(since: Long): Flow<List<HealthRecord>> = healthRecordDao.getRecordsSince(since)

    suspend fun insertHealthRecord(record: HealthRecord) {
        healthRecordDao.insertRecord(record)
    }

    suspend fun getHealthRecordByDate(date: Long): HealthRecord? = healthRecordDao.getRecordByDate(date)

    suspend fun clearHealthData() {
        healthRecordDao.deleteAllRecords()
    }

    fun searchRecipes(query: String, category: String = ""): Flow<List<Recipe>> {
        return recipeDao.searchRecipes(query, category)
    }

    suspend fun getRecipeById(id: Int): Recipe? {
        return recipeDao.getRecipeById(id)
    }

    suspend fun getRandomRecipesSample(limit: Int, category: String = ""): List<Recipe> {
        return recipeDao.getRandomRecipesSample(limit, category)
    }

    suspend fun getRandomRecipesByCategories(categories: List<String>, limit: Int): List<Recipe> {
        return recipeDao.getRandomRecipesByCategories(categories, limit)
    }

    suspend fun insertRecipes(recipes: List<Recipe>) {
        recipeDao.insertAll(recipes)
    }

    suspend fun updateRecipe(recipe: Recipe) {
        recipeDao.updateRecipe(recipe)
    }

    suspend fun getRecipeCount(): Int {
        return recipeDao.getRecipeCount()
    }

    suspend fun clearAll() {
        recipeDao.deleteAllRecipes()
    }

    suspend fun restoreAllHidden() {
        recipeDao.restoreAllHiddenRecipes()
    }

    // --- Métodos de Nube con Lógica de Ahorro Inteligente ---

    /**
     * Sincronización Pro: Solo sube lo nuevo o lo modificado.
     */
    suspend fun smartUploadToCloud(recipes: List<Recipe>) {
        Log.d("Firestore", "Iniciando Sincronización Inteligente...")
        
        // 1. Obtener qué hay ya en la nube (1 sola lectura)
        val cloudManifest = firestoreService.getRecipeManifest()
        val newManifest = cloudManifest.toMutableMap()
        var uploadCount = 0

        // 2. Filtrar recetas que realmente necesitan subirse
        recipes.forEach { recipe ->
            val docId = recipe.title.uppercase().trim().hashCode().toString()
            val localHash = calculateRecipeHash(recipe)
            val remoteHash = cloudManifest[docId]

            if (localHash != remoteHash) {
                val success = firestoreService.uploadSingleRecipe(recipe)
                if (success) {
                    newManifest[docId] = localHash
                    uploadCount++
                }
            }
        }

        // 3. Si hubo cambios, actualizar el manifiesto global (1 sola escritura)
        if (uploadCount > 0) {
            firestoreService.updateManifest(newManifest)
            Log.d("Firestore", "Sincronización finalizada: $uploadCount recetas actualizadas.")
        } else {
            Log.d("Firestore", "Todo está al día. No se subió nada.")
        }
    }

    private fun calculateRecipeHash(recipe: Recipe): String {
        // Creamos una cadena con los datos críticos para detectar cambios
        val content = "${recipe.title}|${recipe.ingredients.joinToString()}|${recipe.instructions.joinToString()}|${recipe.notes}|${recipe.isFavorite}"
        return content.hashCode().toString()
    }

    suspend fun uploadToCloud(recipes: List<Recipe>) {
        smartUploadToCloud(recipes) // Redirigimos a la vía inteligente
    }

    suspend fun uploadOneToCloud(recipe: Recipe): Boolean {
        return firestoreService.uploadSingleRecipe(recipe)
    }

    suspend fun syncWithCloud(localVersion: Long): List<Recipe>? {
        return firestoreService.getCloudRecipesIfUpdated(localVersion)
    }
}
