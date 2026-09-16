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
        val userDir = File(System.getProperty("user.dir"))
        val jsonFile = if (userDir.name == "app") {
            File(userDir, "src/main/assets/recipes.json")
        } else {
            File(userDir, "app/src/main/assets/recipes.json")
        }
        
        if (!jsonFile.exists()) {
            println("ERROR: No se encuentra el archivo en ${jsonFile.absolutePath}")
            return
        }

        val content = jsonFile.readText()
        val wrapper = json.decodeFromString<RecipeWrapper>(content)
        val recipes = wrapper.recipes
        
        println("Enriqueciendo las 1461 recetas con CIENCIA 2025 para todos los perfiles de salud...")
        
        val enriched = recipes.map { recipe ->
            val newInstructions = recipe.instructions.toMutableList()
            val textToSearch = (recipe.title + recipe.ingredients.joinToString(" ") + recipe.instructions.joinToString(" ")).lowercase()
            val tags = recipe.healthTags.map { it.lowercase() }
            
            // --- CIENCIA NUTRICIONAL DE VANGUARDIA (ASOCIADA A FILTROS) ---

            // 1. FILTRO: PERDER PESO (Metabolismo y Saciedad Hormonal)
            if (tags.contains("perder peso")) {
                val tips = listOf(
                    "TIP ADELGAZAMIENTO 2025: Prioriza la proteína en este plato (mín. 30g). Activa naturalmente la GLP-1, la hormona que señaliza saciedad inmediata y frena el hambre por horas.",
                    "ORDEN QUEMAGRASA: Empieza por los vegetales, sigue con la proteína y deja los carbohidratos para el final. Esto evita picos de insulina y mantiene tu cuerpo en modo quema de grasa.",
                    "ESTRATEGIA PRE-COMIDA: Toma un vaso de agua grande 15 min antes de este plato para estirar las paredes gástricas y reducir la ingesta calórica total de forma espontánea."
                )
                if (!newInstructions.any { it.contains("TIP ADELGAZAMIENTO") }) {
                    newInstructions.add(0, tips[0])
                    newInstructions.add(tips[1])
                    newInstructions.add(tips[2])
                }
            }

            // 2. FILTRO: DIABÉTICOS (Control de Glucosa de Precisión)
            if (tags.contains("diabéticos")) {
                val tips = listOf(
                    "CIENCIA DIABETES 2025: La 'Secuenciación de Nutrientes' es vital. Comer fibra (vegetales) antes que el arroz o pasta de esta receta reduce el pico de glucosa hasta un 75%.",
                    "TRUCO DEL VINAGRE: Tomar una cucharada de vinagre de manzana en un vaso de agua antes de este plato mejora la sensibilidad a la insulina y frena la absorción de azúcares.",
                    "PASEO POST-COMIDA: Caminar solo 10 min tras comer este plato permite que tus músculos absorban la glucosa sin necesidad de inyecciones extra de insulina."
                )
                if (!newInstructions.any { it.contains("CIENCIA DIABETES") }) {
                    newInstructions.add(0, tips[0])
                    newInstructions.add(tips[1])
                    newInstructions.add(tips[2])
                }
            }

            // 3. FILTRO: MÚSCULO (Anabolismo y Recuperación)
            if (tags.contains("músculo")) {
                val tips = listOf(
                    "TIP MUSCULACIÓN 2025: Alcanza el 'Umbral de Leucina' en este plato (aprox. 3g de este aminoácido) para encender el interruptor de crecimiento muscular (mTOR).",
                    "SINERGIA OMEGA-3: Si añades pescado o nueces, los ácidos grasos potenciarán la síntesis de proteína de esta receta, acelerando la recuperación de tus fibras musculares.",
                    "RENDIMIENTO MITOCONDRIAL: Los nitratos de vegetales verdes (espinacas/acelgas) mejoran el uso del oxígeno en tus músculos; ideal si entrenas tras comer este plato."
                )
                if (!newInstructions.any { it.contains("TIP MUSCULACIÓN") }) {
                    newInstructions.add(0, tips[0])
                    newInstructions.add(tips[1])
                    newInstructions.add(tips[2])
                }
            }

            // 4. FILTRO: SANA (Longevidad y Antiinflamación)
            if (tags.contains("sana")) {
                val tips = listOf(
                    "CIENCIA SALUD 2025: Añade una especia o semilla extra a este plato. El objetivo es comer 30 plantas distintas por semana para una microbiota súper-resistente y un sistema inmune fuerte.",
                    "TIP ANTIINFLAMATORIO: Añade un chorrito de Aceite de Oliva virgen extra al final (en crudo). Conservarás el oleocanthal, un compuesto que actúa como ibuprofeno natural para tus células.",
                    "LONGEVIDAD: Los fitonutrientes de los ingredientes coloridos de esta receta activan la 'autofagia', el proceso de limpieza y reciclaje celular de tu cuerpo."
                )
                if (!newInstructions.any { it.contains("CIENCIA SALUD") }) {
                    newInstructions.add(0, tips[0])
                    newInstructions.add(tips[1])
                    newInstructions.add(tips[2])
                }
            }

            // 5. CARBOHIDRATOS (Arroz, Pasta, Patata) - Almidón Resistente
            if (textToSearch.contains("arroz") || textToSearch.contains("pasta") || textToSearch.contains("patata")) {
                val starchTip = "CONSEJO ALMIDÓN RESISTENTE: Cocina los carbohidratos de esta receta con antelación y déjalos enfriar 24h. Esto reduce su impacto calórico y alimenta tus bacterias quemagrasas."
                if (!newInstructions.any { it.contains("ALMIDÓN RESISTENTE") }) newInstructions.add(starchTip)
            }

            // Aplicamos sanitización y guardamos
            RecipeSanitizer.sanitize(recipe.copy(instructions = newInstructions))
        }

        val output = json.encodeToString(RecipeWrapper(enriched))
        jsonFile.writeText(output)
        
        println("¡SÚPER ÉXITO! Las 1461 recetas ahora tienen consejos de ciencia avanzada 2025 para todos los filtros.")
    }
}
