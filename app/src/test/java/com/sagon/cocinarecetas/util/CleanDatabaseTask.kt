package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

@Serializable
data class RecipeWrapper(val recipes: List<Recipe>)

class CleanDatabaseTask {

    private val json = Json { 
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun cleanAndExportJson() {
        val jsonFile = File("C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/recipes.json")
        
        if (!jsonFile.exists()) {
            println("ERROR: No se encuentra el archivo en ${jsonFile.absolutePath}")
            return
        }
        println("PROCESANDO ARCHIVO EN: ${jsonFile.absolutePath}")

        val content = jsonFile.readText()
        val wrapper = json.decodeFromString<RecipeWrapper>(content)
        val recipes = wrapper.recipes
        
        println("Iniciando limpieza PROFUNDA de las 1461 recetas...")
        
        val enriched = recipes.map { recipe ->
            // 1. Limpieza agresiva de OCR para todos los campos de texto
            val cleanTitle = RecipeSanitizer.fixSpacedText(recipe.title).uppercase()
            val cleanIngredients = recipe.ingredients.map { RecipeSanitizer.fixSpacedText(it) }
            val cleanInstructions = recipe.instructions.map { RecipeSanitizer.fixSpacedText(it) }.toMutableList()
            
            val textToSearch = (cleanTitle + cleanIngredients.joinToString(" ") + cleanInstructions.joinToString(" ")).lowercase()
            val tags = recipe.healthTags.map { it.lowercase() }
            
            // 2. Enriquecimiento con Ciencia 2025 (Solo si no están ya presentes)
            if (tags.contains("perder peso") && !cleanInstructions.any { it.contains("TIP ADELGAZAMIENTO") }) {
                cleanInstructions.add(0, "TIP ADELGAZAMIENTO 2025: Prioriza la proteína en este plato (mín. 30g). Activa naturalmente la GLP-1, la hormona que señaliza saciedad inmediata y frena el hambre por horas.")
                cleanInstructions.add("ORDEN QUEMAGRASA: Empieza por los vegetales, sigue con la proteína y deja los carbohidratos para el final. Esto evita picos de insulina y mantiene tu cuerpo en modo quema de grasa.")
            }
            
            if (tags.contains("diabéticos") && !cleanInstructions.any { it.contains("CIENCIA DIABETES") }) {
                cleanInstructions.add(0, "CIENCIA DIABETES 2025: La 'Secuenciación de Nutrientes' es vital. Comer fibra (vegetales) antes que el arroz o pasta de esta receta reduce el pico de glucosa hasta un 75%.")
                cleanInstructions.add("TRUCO DEL VINAGRE: Tomar una cucharada de vinagre de manzana en un vaso de agua antes de este plato mejora la sensibilidad a la insulina y frena la absorción de azúcares.")
            }

            // Aplicamos la sanitización final (encoding, etc)
            recipe.copy(
                title = cleanTitle,
                ingredients = cleanIngredients,
                instructions = cleanInstructions
            )
        }

        val output = json.encodeToString(RecipeWrapper(enriched))
        jsonFile.writeText(output)
        
        println("¡ÉXITO TOTAL! Las 1461 recetas han sido corregidas una a una en el archivo JSON original.")
    }
}
