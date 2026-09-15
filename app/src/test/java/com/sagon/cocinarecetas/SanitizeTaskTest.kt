package com.sagon.cocinarecetas

import com.sagon.cocinarecetas.data.model.Recipe
import com.sagon.cocinarecetas.util.RecipeSanitizer
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

    @Test
    fun performSanitization() {
        val paths = listOf(
            "C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/recipes.json"
            // Add other paths here if discovered later
        )

        for (path in paths) {
            val file = File(path)
            if (!file.exists()) {
                println("File not found: $path")
                continue
            }

            println("Processing $path...")
            val content = file.readText()
            val recipes = try {
                json.decodeFromString<List<Recipe>>(content)
            } catch (e: Exception) {
                println("Error decoding $path: ${e.message}")
                continue
            }

            val seenTitles = mutableSetOf<String>()
            val cleaned = recipes.map { RecipeSanitizer.sanitize(it) }
                .filter { 
                    if (seenTitles.contains(it.title)) {
                        false
                    } else {
                        seenTitles.add(it.title)
                        true
                    }
                }

            val output = json.encodeToString(cleaned)
            file.writeText(output)
            println("Finished $path. Total: ${recipes.size} -> ${cleaned.size}")
        }
    }
}
