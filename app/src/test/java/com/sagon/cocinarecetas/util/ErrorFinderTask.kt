package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Test
import java.io.File

@Serializable
data class RecipeWrapperV3(val recipes: List<Recipe>)

class ErrorFinderTask {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun findRemainingErrors() {
        val parts = listOf("recipes_1.json", "recipes_2.json", "recipes_3.json", "recipes_4.json", "recipes_5.json")
        val errors = mutableSetOf<String>()

        for (part in parts) {
            val file = File("C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/$part")
            if (!file.exists()) continue

            val content = file.readText()
            val wrapper = json.decodeFromString<RecipeWrapperV3>(content)

            for (recipe in wrapper.recipes) {
                val allText = (recipe.instructions + recipe.ingredients + recipe.title).joinToString(" ")
                val words = allText.split(Regex("""\s+"""))
                
                for (word in words) {
                    val cleanWord = word.replace(Regex("""[^a-zA-ZáéíóúÁÉÍÓÚñÑ]"""), "").lowercase()
                    if (cleanWord.length > 12) {
                        // Si la palabra es muy larga y no está en nuestro diccionario, es sospechosa
                        if (!isKnown(cleanWord)) {
                            errors.add(cleanWord)
                        }
                    }
                }
            }
        }

        println("=== PALABRAS SOSPECHOSAS ENCONTRADAS (${errors.size}) ===")
        errors.sorted().forEach { println(it) }
    }

    private fun isKnown(word: String): Boolean {
        // Aquí deberíamos tener el mismo set de spanishCommonWords de RecipeSanitizer
        // Pero para este test, buscaremos si contiene sub-palabras comunes
        val common = listOf("aceite", "cebolla", "patata", "huevo", "salsa", "durante", "minutos", "despues", "luego", "anadir", "mezclar", "cocer")
        return common.any { word == it }
    }
}
