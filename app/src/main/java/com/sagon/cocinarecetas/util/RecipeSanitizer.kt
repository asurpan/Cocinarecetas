package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe

/**
 * Sanitizer simplificado. La mayoría de la limpieza de OCR y enriquecimiento de datos
 * se ha realizado permanentemente sobre el archivo recipes.json.
 */
object RecipeSanitizer {

    private val categoryMapRules = mapOf(
        "APERITIVOS" to listOf("aperitivo", "aperitivos", "tapa", "tapas"),
        "ENSALADAS" to listOf("ensalada", "ensaladas"),
        "LEGUMBRES" to listOf("legumbre", "legumbres", "garbazos", "lentejas", "alubias", "garbanzos"),
        "SOPAS" to listOf("sopa", "sopas", "crema", "cremas", "caldo", "caldos"),
        "ARROCES" to listOf("arroz", "arroces"),
        "PASTAS" to listOf("pasta", "pastas", "macarrones", "espaguetis"),
        "VERDURAS" to listOf("verdura", "verduras", "hortalizas", "cardo", "coliflor", "celga"),
        "PESCADOS" to listOf("pescado", "pescados", "bacalao", "anchoas"),
        "CARNES" to listOf("carne", "carnes", "pollo", "ternera", "cerdo", "pavo"),
        "POSTRES" to listOf("postre", "postres", "dulces", "tarta", "tartas", "fresas", "fresones"),
        "SALSAS" to listOf("salsa", "salsas", "aliño", "aliños", "vinagreta", "vinagretas", "mayonesa")
    )

    fun sanitize(recipe: Recipe): Recipe {
        val titleCleaned = fixSpacedText(fixEncoding(recipe.title)).uppercase().trim()
        
        var categoryCleaned = fixEncoding(recipe.category).trim().uppercase()
        
        // Validación de categoría por si viene vacía o mal escrita en nuevas entradas
        if (categoryCleaned.isBlank() || categoryCleaned !in categoryMapRules.keys) {
            val titleNorm = titleCleaned.lowercase().replace(" ", "")
            categoryCleaned = categoryMapRules.entries.find { (_, words) ->
                words.any { titleNorm.contains(it) }
            }?.key ?: "APERITIVOS"
        }

        // Lógica de Plato Principal basada en categoría
        val isMain = categoryCleaned !in setOf("POSTRES", "SALSAS")

        return recipe.copy(
            title = titleCleaned,
            category = categoryCleaned,
            ingredients = sanitizeIngredients(recipe.ingredients.map { fixEncoding(it) }),
            instructions = recipe.instructions.map { fixSpacedText(fixEncoding(it)) },
            isMainDish = isMain
        )
    }

    private fun fixEncoding(text: String): String {
        val replacements = mapOf(
            "Ã¡" to "á", "Ã©" to "é", "Ã\u00AD" to "í", "Ã³" to "ó", "Ãº" to "ú",
            "Ã±" to "ñ", "Ã‘" to "Ñ", "Ã“" to "Ó", "Ã\u00A0" to "Á", "Ã\u0089" to "É",
            "Â½" to "½", "Âº" to "º", "Ã" to "í", "%%" to "É", "Â" to ""
        )
        var fixed = text
        for ((old, new) in replacements) {
            fixed = fixed.replace(old, new)
        }
        return fixed
    }

    private fun isQuantityOnly(text: String): Boolean {
        val t = text.trim().lowercase()
        return t.matches(Regex("""^(\d+|½|¼|¾|1/|1 /|2 /|3 /|4 /|l|L|I|z|[1-9]/[1-9])$""")) ||
               t.matches(Regex("""^\d+\s*(gr|g|kg|ml|cl|l|cuch|cdta|cucharada|vaso|taza|pizca|gramos|litro|unidad)$""")) ||
               t in setOf("gr", "g", "kg", "ml", "cl", "l", "cuch", "cucharada", "gramos", "litro", "unidades")
    }

    fun sanitizeIngredients(ingredients: List<String>): List<String> {
        if (ingredients.isEmpty()) return emptyList()

        val cleaned = ingredients.map { fixSpacedText(it) }.filter { it.isNotBlank() }
        val result = mutableListOf<String>()
        
        var i = 0
        while (i < cleaned.size) {
            var current = cleaned[i].trim()
            if (i < cleaned.size - 1) {
                val next = cleaned[i + 1].trim()
                if (isQuantityOnly(current) && !isQuantityOnly(next)) {
                    current = "$current $next"
                    i++
                } else if (!isQuantityOnly(current) && isQuantityOnly(next)) {
                    current = "$next $current"
                    i++
                }
            }

            val formatted = current.replace(Regex("""(\d+)\s*/\s*(\d+)"""), "$1/$2")
                .replace(Regex("""(\d+)\s*(gr|g|ml|Kg|kg|cl|l|unidades|unidad|dientes|diente|porciones|porcion|cucharadas|cucharada|vaso|taza|pizca|gramos|litro)""", RegexOption.IGNORE_CASE)) { "${it.groupValues[1]} ${it.groupValues[2]}" }
                .replace(Regex("""([a-zA-ZñÑáéíóúÁÉÍÓÚ])(\d)"""), "$1 $2")
                .replace(Regex("""^[-*•.]\s*"""), "")

            result.add(formatted.trim().replace(Regex("""\s{2,}"""), " "))
            i++
        }
        return result
    }

    fun fixSpacedText(text: String): String {
        if (text.length < 3) return text
        var fixed = text

        // Limpieza básica de espacios sobrantes y fragmentos de OCR comunes
        fixed = fixed.replace(Regex("""\bdoc\s+[a-zA-Z0-9]+\b""", RegexOption.IGNORE_CASE), "")
                     .replace(Regex("""\bpage\s+\d+\b""", RegexOption.IGNORE_CASE), "")
                     .replace("---", "")

        // Unir letras sueltas y corregir espaciados dobles
        val singleLetterPattern = Regex("""(?<=\b\w)\s+(?=\w\b)""")
        repeat(3) { fixed = fixed.replace(singleLetterPattern, "") }
        
        return fixed.replace(Regex("""\s{2,}"""), " ").trim()
    }
}
