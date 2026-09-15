package com.sagon.cocinarecetas.data.local

import android.content.Context
import android.util.Log
import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Serializable
private data class RecipeJsonRoot(
    val recipes: List<Recipe> = emptyList()
)

object JsonAssetImporter {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        isLenient = true
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun loadRecipesFromAsset(context: Context, fileName: String): List<Recipe> {
        val allRecipes = mutableListOf<Recipe>()
        
        // 1. Carga de la Base Principal con Blindaje
        try {
            context.assets.open(fileName).use { stream ->
                val root = json.decodeFromStream<RecipeJsonRoot>(stream)
                if (root.recipes.isNotEmpty()) {
                    allRecipes.addAll(root.recipes)
                    Log.d("JsonImporter", "Cargadas ${root.recipes.size} recetas principales.")
                }
            }
        } catch (e: Exception) {
            Log.e("JsonImporter", "Aviso: Error en base principal (posible archivo corrupto). Cargando alternativas...", e)
        }

        return allRecipes
    }
}
