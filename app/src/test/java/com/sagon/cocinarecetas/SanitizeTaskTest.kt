package com.sagon.cocinarecetas

import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.util.RecipeSanitizer
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class SanitizeTaskTest {
    
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }

    @Serializable
    data class RecipeJsonRoot(
        val recipes: List<Recipe> = emptyList(),
        val schemaVersion: String = "3.0",
        val recipeCount: Int = 0
    )

    @Test
    fun performSanitization() {
        val paths = listOf(
            "C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/recipes.json"
        )

        for (path in paths) {
            val file = File(path)
            if (!file.exists()) {
                println("File not found: $path")
                continue
            }

            println("Processing $path...")
            val content = file.readText()
            
            // Intentar cargar como root u objeto directo
            var recipesToClean: List<Recipe> = emptyList()
            var metaVersion = "3.0"
            
            try {
                val root = json.decodeFromString<RecipeJsonRoot>(content)
                recipesToClean = root.recipes
                metaVersion = root.schemaVersion
            } catch (e: Exception) {
                println("Falling back to list decoding for $path...")
                try {
                    val list = json.decodeFromString<List<Recipe>>(content)
                    recipesToClean = list
                } catch (e2: Exception) {
                    println("Error decoding $path: ${e2.message}")
                    continue
                }
            }

            println("Found ${recipesToClean.size} recipes to clean.")

            val seenTitles = mutableSetOf<String>()
            val cleaned = recipesToClean.map { RecipeSanitizer.sanitize(it) }
                .filter { 
                    val normTitle = it.title.trim().uppercase()
                    if (seenTitles.contains(normTitle)) {
                        false
                    } else {
                        seenTitles.add(normTitle)
                        true
                    }
                }

            val finalRoot = RecipeJsonRoot(
                recipes = cleaned,
                schemaVersion = metaVersion,
                recipeCount = cleaned.size
            )

            val output = json.encodeToString(finalRoot)
            file.writeText(output)
            println("Finished $path. Total: ${recipesToClean.size} -> ${cleaned.size}")
        }
    }
}
