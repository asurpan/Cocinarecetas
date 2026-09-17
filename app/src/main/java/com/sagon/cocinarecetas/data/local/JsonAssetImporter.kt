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
        
        val assetFiles = if (fileName == "recipes.json") {
            listOf("recipes_1.json", "recipes_2.json", "recipes_3.json", "recipes_4.json", "recipes_5.json")
        } else {
            listOf(fileName)
        }

        for (file in assetFiles) {
            try {
                context.assets.open(file).use { stream ->
                    val root = json.decodeFromStream<RecipeJsonRoot>(stream)
                    if (root.recipes.isNotEmpty()) {
                        allRecipes.addAll(root.recipes)
                        Log.d("JsonImporter", "Cargadas ${root.recipes.size} recetas desde $file.")
                    }
                }
            } catch (e: Exception) {
                Log.e("JsonImporter", "Error cargando $file: ${e.message}")
            }
        }

        return allRecipes
    }
}
