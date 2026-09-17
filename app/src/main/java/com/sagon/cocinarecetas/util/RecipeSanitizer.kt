package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe
import java.text.Normalizer

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
        if (categoryCleaned.isBlank() || categoryCleaned !in categoryMapRules.keys) {
            val titleNorm = titleCleaned.lowercase().replace(" ", "")
            categoryCleaned = categoryMapRules.entries.find { (_, words) ->
                words.any { titleNorm.contains(it) }
            }?.key ?: "APERITIVOS"
        }
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

    private val spanishCommonWords = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "y", "e", "o", "u", "pero", "mas", "sino",
        "de", "del", "a", "al", "con", "en", "por", "para", "se", "su", "sus", "mi", "tu", "tus",
        "que", "si", "no", "como", "donde", "cuando", "quien", "cual", "cuanto",
        "este", "esta", "estos", "estas", "ese", "esa", "esos", "esas", "aquel", "aquella", "aquellos", "aquellas",
        "yo", "tu", "nosotros", "vosotros", "ellos", "ellas", "me", "te", "nos", "os",
        "ser", "estar", "haber", "hacer", "ir", "ver", "dar", "decir", "poder", "querer", "saber", "poner", "parecer",
        "aceite", "agua", "sal", "pimienta", "cebolla", "ajo", "ajos", "tomate", "harina", "huevo", "huevos", "leche",
        "carne", "pollo", "pescado", "arroz", "pasta", "patatas", "patata", "verdura", "verduras", "fruta", "frutas",
        "sarten", "cazuela", "horno", "fuego", "minutos", "minuto", "hora", "horas", "cucharada", "cucharadas", "vaso",
        "bien", "muy", "poco", "mucho", "todo", "todos", "toda", "todas",
        "vez", "veces", "despues", "luego", "ahora", "antes", "mientras", "durante", "hasta", "desde",
        "picar", "cortar", "rehogar", "sofreir", "cocer", "hervir", "freir", "asar", "añadir", "mezclar", "servir", "limpiar", "pelar",
        "aliñar", "sazonar", "escurrir", "triturar", "batir", "rectificar", "adornar", "cubrir", "regar", "tapar", "dejar",
        "vinagre", "vino", "blanco", "tinto", "pimenton", "perejil", "laurel", "canela", "clavo",
        "picado", "picada", "molido", "entero", "entera", "rallado", "rallada", "queso", "jamon",
        "hecho", "hecha", "puesto", "puesta", "añadido", "mezclado", "formar", "forma",
        "secuenciacion", "nutrientes", "perdida", "peso", "glucosa", "insulina", "almidon", "resistente", "prioriza", "saciedad", "quemagrasas",
        "science", "ciencia", "pico", "picos", "orden", "fibra", "proteina", "grasa", "grasas", "carbohidratos", "absorcion", "digestion", "metabolismo", "energia", "saludable", "nutricional",
        "antelacion", "dejalo", "dejalos", "enfriar", "nevera", "comerlo", "puedes", "recalentarlo", "esto", "crea", "alimenta", "bacterias", "reduce", "impacto", "calorico",
        "vegetales", "despues", "final", "aplana", "curva", "azucar", "azucares", "evitando", "cansancio", "almacenamiento", "manzana", "mejora", "sensibilidad", "frena", "azucares",
        "caminar", "musculos", "musculo", "muscular", "absorban", "necesidad", "inyecciones", "extra", "añade", "especia", "semilla", "objetivo", "comer", "plantas", "distintas", "semana", "microbiota", "fuerte"
    )

    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace("ñ", "n")
    }

    private fun autoSplitJoinedWords(word: String): String {
        val normWord = normalize(word)
        if (word.length < 3 || normWord in spanishCommonWords) return word
        for (i in word.length - 1 downTo 1) {
            val prefix = normWord.substring(0, i)
            if (prefix in spanishCommonWords || (prefix.length == 1 && prefix in setOf("y", "a", "e", "o", "u"))) {
                val rest = word.substring(i)
                val restSplit = autoSplitJoinedWords(rest)
                val restSplitNorm = normalize(restSplit)
                if (restSplitNorm in spanishCommonWords || restSplit.contains(" ") || (restSplitNorm.length < 3 && restSplitNorm in spanishCommonWords)) {
                    return "${word.substring(0, i)} $restSplit"
                }
            }
        }
        return word
    }

    fun fixSpacedText(text: String): String {
        if (text.length < 3) return text
        var fixed = Normalizer.normalize(text, Normalizer.Form.NFC)
        
        // Corrección de Science 2025 phrases pegadas
        // Usamos regex muy flexible para capturar variaciones de acentos y caracteres
        val targets = listOf(
            """antelaci\S+n\S*y\S*d\S+jalos?""" to "antelación y déjalo",
            """reduceelpicodeglucosa""" to "reduce el pico de glucosa",
            """pastadeesta""" to "pasta de esta",
            """quemagrasasyreduce""" to "quemagrasas y reduce",
            """impactocal\S+rico""" to "impacto calórico",
            """antesdecomerlo""" to "antes de comerlo",
            """puedesrecalentarlo""" to "puedes recalentarlo",
            """mejoralasensibilidad""" to "mejora la sensibilidad",
            """frenalaabsorci\S+n""" to "frena la absorción",
            """curvadeaz\S+car""" to "curva de azúcar",
            """evitandoelcansancio""" to "evitando el cansancio",
            """almacenamientodegrasa""" to "almacenamiento de grasa",
            """siemprelosvegetales""" to "siempre los vegetales",
            """laprote\u00EDnaygrasas""" to "la proteína y grasas",
            """ydejaloscarbohidratos""" to "y deja los carbohidratos",
            """tusbacterias""" to "tus bacterias",
            """tusm\u00FAsculos""" to "tus músculos"
        )

        for ((pattern, replacement) in targets) {
            fixed = fixed.replace(Regex(pattern, RegexOption.IGNORE_CASE)) { match ->
                if (replacement == "antelación y déjalo" && match.value.lowercase().endsWith("s")) {
                    "antelación y déjalos"
                } else replacement
            }
        }

        // Split joined words
        fixed = fixed.split(" ").joinToString(" ") { autoSplitJoinedWords(it) }

        // OCR Fixes
        val singleLetterPattern = Regex("""(?<=\b\w)\s+(?=\w\b)""")
        repeat(3) { fixed = fixed.replace(singleLetterPattern, "") }
        
        return fixed.replace(Regex("""\s{2,}"""), " ").trim()
    }
}
