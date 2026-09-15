package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

class CleanDatabaseTask {

    private val json = Json { 
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun cleanAndExportJson() {
        val projectRoot = File("..").canonicalFile // Sube desde app/ hacia la raíz
        val jsonFile = File(projectRoot, "app/src/main/assets/recipes.json")
        
        if (!jsonFile.exists()) {
            println("ERROR: No se encuentra el archivo en ${jsonFile.absolutePath}")
            return
        }

        println("Cargando recetas desde: ${jsonFile.absolutePath}")
        val content = jsonFile.readText()
        val recipes = json.decodeFromString<List<Recipe>>(content)
        
        println("Procesando ${recipes.size} recetas...")
        
        // Aplicamos la misma lógica de RecipeSanitizer
        val sanitized = recipes
            .filter { it.title.trim().uppercase() != "SALMOREJO" } // Eliminamos la versión incorrecta
            .map { RecipeSanitizer.sanitize(it) }
            .map { recipe ->
                // Normalización extra de categorías para la Web
                val title = recipe.title.uppercase()
                var cat = recipe.category.uppercase()
                
                if (title.contains("SALSA") || title.contains("ALIÑO")) cat = "SALSAS"
                
                recipe.copy(category = cat)
            }

        val output = json.encodeToString(sanitized)
        jsonFile.writeText(output)
        
        println("¡ÉXITO! Base de datos limpia y guardada.")
        println("Recetas finales: ${sanitized.size}")
    }
}
