package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File
import java.text.Normalizer

@Serializable
data class RecipeWrapperV2(val recipes: List<Recipe>)

class DeepCleanTask {

    private val json = Json { 
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    @Test
    fun fixFirstRecipes() {
        val parts = listOf("recipes_1.json", "recipes_2.json", "recipes_3.json", "recipes_4.json", "recipes_5.json")
        
        for (part in parts) {
            val jsonFile = File("C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/$part")
            if (!jsonFile.exists()) continue
            
            println("Iniciando Limpieza en $part...")
            var content = jsonFile.readText(Charsets.UTF_8).removePrefix("\uFEFF")
            content = Normalizer.normalize(content, Normalizer.Form.NFC)
            
            // Limpieza de caracteres inválidos residuales
            content = content.replace("\",\uFFFD", "\",\"")
                             .replace("\uFFFD,", "\",")
                             .replace("[\uFFFD", "[\"")
                             .replace("\uFFFD]", "\"]")
            content = content.replace("\uFFFD", " ")
            
            // Procesamiento con el Sanitizer mejorado
            val wrapper = json.decodeFromString<RecipeWrapperV2>(content)
            val cleanedRecipes = wrapper.recipes.map { recipe ->
                RecipeSanitizer.sanitize(recipe)
            }
            
            var output = json.encodeToString(RecipeWrapperV2(cleanedRecipes))
            
            // REGLAS DE LIMPIEZA FINAL DE EMERGENCIA (Para asegurar que NADA se escape)
            val replacements = mapOf(
                "antelaci\u00F3nyd\u00E9jalo" to "antelaci\u00F3n y d\u00E9jalo",
                "antelaci\u00F3nyd\u00E9jalos" to "antelaci\u00F3n y d\u00E9jalos",
                "reduceelpicodeglucosa" to "reduce el pico de glucosa",
                "pastadeesta" to "pasta de esta",
                "quemagrasasyreduce" to "quemagrasas y reduce",
                "enfriarenlanevera" to "enfriar en la nevera"
            )
            
            for ((old, new) in replacements) {
                output = output.replace(old, new, ignoreCase = true)
            }
            
            // Verificación absoluta
            if (output.contains("antelaci\u00F3nyd\u00E9jalo", ignoreCase = true)) {
                println("ERROR CRITICO: No se pudo eliminar el t\u00E9rmino pegado en $part")
            }
            
            jsonFile.writeText(output, Charsets.UTF_8)
        }
        println("¡LIMPIEZA DEFINITIVA COMPLETADA!")
    }
}
