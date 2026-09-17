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
    fun insertNewRecipe() {
        val file = File("C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/recipes.json")
        val tempFile = File("C:/Users/Jose/AndroidStudioProjects/CocinaREcetas/app/src/main/assets/recipes_temp.json")
        
        file.bufferedReader().use { reader ->
            tempFile.printWriter().use { writer ->
                var line = reader.readLine()
                while (line != null) {
                    writer.println(line)
                    if (line.contains("\"recipes\": [")) {
                        writer.println("""        {
            "id": "receta_alubias_baked_beans",
            "title": "ALUBIAS EN SALSA DE TOMATE TIPO HEINZ (BAKED BEANS)",
            "ingredients": [
                "1 bote grande de alubias blancas cocidas (400-500 g)",
                "400 g de tomate triturado de lata",
                "1 cebolla mediana picada muy fina",
                "2 o 3 dientes de ajo picados",
                "2 cucharadas de kétchup",
                "1 o 2 cucharadas de tomate concentrado",
                "1 a 2 cucharadas de azúcar moreno o miel",
                "1 cucharada de vinagre",
                "1 cucharada de salsa Worcestershire (salsa Perrins)",
                "1 cucharadita de pimentón dulce",
                "Una pizca de ajo en polvo, cebolla en polvo y sal al gusto",
                "Aceite de oliva",
                "Opcional: una pizca de cayena o tabasco si te gusta con picante"
            ],
            "instructions": [
                "CIENCIA SALUD 2025: Las alubias blancas son ricas en fibra soluble. Al cocinarlas en esta salsa casera, evitas los conservantes industriales de las latas comerciales y mantienes el control sobre el índice glucémico.",
                "Sofreír la base: Calienta un chorro de aceite de oliva en una sartén a fuego medio. Añade la cebolla y los ajos picados. Cocina hasta que la cebolla esté tierna y transparente.",
                "Añadir especias y sabor: Agrega el pimentón dulce, el tomate concentrado y remueve rápido durante unos segundos para que no se queme.",
                "Hacer la salsa: Incorpora el tomate triturado, el kétchup, el azúcar moreno, el vinagre, la salsa Worcestershire y la sal. Cocina a fuego medio-bajo durante unos 20 minutos para que la salsa reduzca y pierda la acidez.",
                "Triturar (opcional): Si prefieres una textura más fina y uniforme similar a la de bote comercial, pasa la salsa por la batidora.",
                "Juntar con las alubias: Añade las alubias blancas (bien lavadas y escurridas si son de bote) a la salsa.",
                "Cocinar el conjunto: Deja que todo junto hierva a fuego suave durante 10 minutos más para que las alubias absorban todo el sabor de la salsa.",
                "TIP SALUD: Para reducir el pico de glucosa del azúcar moreno, acompaña estas alubias con una ración de vegetales verdes o espinacas como primer plato."
            ],
            "category": "LEGUMBRES",
            "healthTags": ["Sana", "Fibra"],
            "notes": "Versión casera de las famosas Baked Beans con el toque exacto de salsa Worcestershire.",
            "cookingTime": "40 min",
            "servings": 4,
            "isMainDish": false,
            "goals": {
                "healthy": true,
                "weightLoss": false,
                "muscleGain": true,
                "diabeticFriendly": false,
                "highProtein": false,
                "lowCarb": false
            }
        },""")
                    }
                    line = reader.readLine()
                }
            }
        }
        file.delete()
        tempFile.renameTo(file)
        println("Recipe inserted via streaming successfully!")
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
