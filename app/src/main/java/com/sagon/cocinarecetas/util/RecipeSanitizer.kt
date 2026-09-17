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

        // Coletilla de legumbres de bote para gente joven
        var instructionsCleaned = recipe.instructions.map { fixSpacedText(fixEncoding(it)) }
        val titleNorm = titleCleaned.lowercase()
        val allIngredientsNorm = recipe.ingredients.map { it.lowercase() }
        
        fun hasIng(vararg keywords: String) = keywords.any { k -> titleNorm.contains(k) || allIngredientsNorm.any { it.contains(k) } }

        if (hasIng("garbanzo", "lenteja", "alubia", "frijol", "frejol") && instructionsCleaned.none { it.contains("LEGUMBRE DE BOTE", ignoreCase = true) }) {
            instructionsCleaned = instructionsCleaned + "NOTA PARA LEGUMBRE DE BOTE: Si usas garbanzos, lentejas, alubias o frijoles de bote (ya cocidos), haz todo el proceso de sofrito/caldo igual, lava muy bien las legumbres bajo el grifo para quitar el liquido preservante, añadelas en los ultimos 10-15 minutos de coccion para que tomen el sabor de la receta sin deshacerse."
        }
        if (hasIng("guisante", "chicharo", "maiz") && instructionsCleaned.none { it.contains("CONSERVA DE VERDURA", ignoreCase = true) }) {
            instructionsCleaned = instructionsCleaned + "NOTA PARA CONSERVA DE VERDURA: Si usas guisantes o maiz de lata, añadelos solo en los ultimos 2-3 minutos de la receta para que mantengan su color brillante y su textura crujiente."
        }
        if (hasIng("champiñon", "seta") && instructionsCleaned.none { it.contains("SETAS EN CONSERVA", ignoreCase = true) }) {
            instructionsCleaned = instructionsCleaned + "NOTA PARA SETAS EN CONSERVA: Si usas champiñones o setas de bote, escurelos muy bien y añadelos al final del sofrito o de la coccion; si se cocinan demasiado pueden volverse gomosos."
        }
        if (hasIng("patata") && instructionsCleaned.none { it.contains("PATATAS DE BOTE", ignoreCase = true) }) {
            instructionsCleaned = instructionsCleaned + "NOTA PARA PATATAS DE BOTE: Si usas patatas cocidas de frasco, añadelas solo en los ultimos 5-10 minutos del guiso o estofado para que absorban el sabor del caldo sin llegar a deshacerse."
        }
        if (hasIng("espinaca", "acelga") && instructionsCleaned.none { it.contains("HOJAS EN CONSERVA", ignoreCase = true) }) {
            instructionsCleaned = instructionsCleaned + "NOTA PARA HOJAS EN CONSERVA: Si usas espinacas o acelgas de bote, es muy importante escurrirlas y exprimirlas bien con la mano antes de añadirlas al final de la receta; asi evitaras que el exceso de agua agüe el plato."
        }
        if (hasIng("bizcocho", "magdalena", "tarta", "cake", "muffin") && instructionsCleaned.none { it.contains("TIPS DE EXITO REPOSTERIA", ignoreCase = true) }) {
            instructionsCleaned = instructionsCleaned + "TIPS DE EXITO REPOSTERIA: 1) Tipo de Harina: Usa siempre harina comun (floja), NUNCA harina de fuerza, para que el bizcocho quede esponjoso y tierno. 2) Reducir Azucar: Puedes sustituir el azucar por eritritol (misma cantidad) o pure de platano maduro/manzana asada (reduce el liquido de la receta). 3) Reducir Aceite: Puedes cambiar la mitad del aceite por yogur natural o pure de manzana para hacerlo mas ligero sin perder humedad. 4) Horneado: Hornea siempre SIN AIRE (calor arriba y abajo) para que no se reseque por fuera y suba de forma uniforme."
        }

        return recipe.copy(
            title = titleCleaned,
            category = categoryCleaned,
            ingredients = sanitizeIngredients(recipe.ingredients.map { fixEncoding(it) }),
            instructions = instructionsCleaned,
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
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "a", "al", "con", "en", "por", "para", "se", "su", "sus", "que", "si", "no", "como", "y", "e", "o", "u", "s", "n",
        "yo", "tu", "me", "te", "nos", "os", "lo", "le", "les", "este", "esta", "esto", "ese", "esa", "eso", "mismo", "misma", "cada", "todo", "toda", "todos", "todas", "otro", "otra", "otros", "otras",
        "hacer", "poner", "quitar", "anadir", "mezclar", "batir", "cocer", "hervir", "freir", "asar", "hornear", "sofreir", "rehogar", "picar", "cortar", "pelar", "limpiar", "lavar", "escurrir", "triturar", "incorporar", "verter", "echar", "retirar", "dejar", "reposar", "enfriar", "calentar", "precalentar", "dorar", "sellar", "sazonar", "alinar", "salpimentar", "rectificar", "servir", "decorar", "adornar", "cubrir", "tapar", "desmenuzar", "aplastar", "chafar", "moler", "rallar", "tamizar", "montar", "emulsionar", "reducir", "evaporar", "nacarar", "doran", "cuecen", "pican", "cortan", "limpian", "lavan", "anaden", "mezclan", "frien", "hacen", "prepara", "preparar", "quede", "queden", "tomen", "tome", "colocar", "colocan", "quitarle", "echan", "sirven", "hierve", "quita", "saque", "saquen", "meta", "meten", "sofriendo", "batiendo", "mezclando", "anadiendo", "rehogando", "picando", "cociendo", "hirviendo", "friendo", "asando", "horneando", "cortando", "limpiando", "lavando", "escurriendo", "triturando", "incorporando", "virtiendo", "echando", "poniendo", "quitando", "retirando", "dejando", "reposando", "enfriando", "calentando", "dorando", "sellando", "sazonando", "alinando", "sirviendo", "cubriendo", "tapando", "desmenuzando", "aplastando", "chafando", "moliendo", "rallando", "tamizando", "montando", "emulsionando", "reduciendo",
        "aceite", "agua", "sal", "pimienta", "cebolla", "ajo", "ajos", "tomate", "tomates", "harina", "huevo", "huevos", "leche", "carne", "pollo", "pescado", "arroz", "pasta", "patatas", "patata", "verdura", "verduras", "fruta", "frutas", "vino", "blanco", "tinto", "pimenton", "perejil", "laurel", "canela", "clavo", "queso", "jamon", "nata", "yogur", "azucar", "miel", "levadura", "mantequilla", "manteca", "pan", "migas", "caldo", "limon", "naranja", "vinagre", "mostaza", "mayonesa", "ketchup", "salsa", "salsas", "bacalao", "atun", "merluza", "salmon", "gambas", "gamba", "langostinos", "mejillones", "almejas", "calamares", "pulpo", "albondiga", "albondigas", "filete", "filetes", "lomo", "costilla", "ternera", "cerdo", "cordero", "pavo", "conejo", "garbanzos", "lentejas", "alubias", "frijoles", "frijol", "habas", "guisantes", "chicharos", "maiz", "champinon", "champinones", "setas", "seta", "espinacas", "espinaca", "acelgas", "acelga", "calabaza", "calabacin", "berenjena", "zanahoria", "zanahorias", "pimiento", "pimientos", "pepino", "lechuga", "aguacate", "nueces", "almendras", "avellanas", "pinones", "semillas", "sesamo", "trufa", "albahaca", "cilantro", "perejil", "romero", "tomillo", "oregano", "comino", "curry", "turmeric", "curcuma", "jengibre", "azafran", "vainilla", "canela", "anis", "menta", "espina", "espinas", "piel", "hueso", "huesos", "picada", "picado", "molida", "molido", "rallada", "rallado", "cortada", "cortado", "limpia", "limpio", "lavada", "lavado", "escurrida", "escurrido", "batida", "batido", "mezclada", "mezclado", "cocida", "cocido", "frita", "frito", "asada", "asado", "horneada", "horneado", "sofrida", "sofrito", "rehogada", "rehogado", "triturada", "triturado", "incorporada", "incorporado", "vertida", "vertido", "echada", "echado", "puesta", "puesto", "quitada", "quitado", "retirada", "retirado", "dejada", "dejado", "reposada", "reposado", "enfriada", "enfriado", "calentada", "calentado", "dorada", "dorado", "sellada", "sellado", "sazonada", "sazonado", "alinada", "alinado", "servida", "servido", "cubierta", "cubierto", "tapada", "tapado", "masa", "barro", "cazuela", "sarten", "olla", "horno", "noche", "par", "minutos", "paso", "anade", "anaden", "cebolla"
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
            if (prefix in spanishCommonWords || (prefix.length == 1 && prefix in setOf("y", "a", "e", "o", "u", "s", "n"))) {
                val rest = word.substring(i)
                val restSplit = autoSplitJoinedWords(rest)
                val restSplitFirst = normalize(restSplit.split(" ")[0])
                
                if (restSplitFirst in spanishCommonWords || restSplit.contains(" ") || (restSplitFirst.length < 2 && restSplitFirst in spanishCommonWords)) {
                    return "${word.substring(0, i)} $restSplit"
                }
            }
        }
        return word
    }

    fun fixSpacedText(text: String): String {
        if (text.length < 3) return text
        var fixed = Normalizer.normalize(text, Normalizer.Form.NFC)
        
        // Reconstrucción OCR "Salsa"
        fixed = fixed.replace(Regex("""unaS\s+AL\s+SA""", RegexOption.IGNORE_CASE), "unas salsa")
                     .replace(Regex("""una\s+S\s+AL\s+SA""", RegexOption.IGNORE_CASE), "una salsa")
                     .replace(Regex("""S\s+AL\s+SA""", RegexOption.IGNORE_CASE), "salsa")
                     .replace(Regex("""S\s+A\s+l\s+a""", RegexOption.IGNORE_CASE), "salsa")

        // Unir letras sueltas
        fixed = fixed.replace(Regex("""(\b\w\b\s+)+(\b\w\b)""")) { match ->
            match.value.replace(" ", "")
        }
        
        val commonSplits = mapOf(
            "mas a" to "masa",
            "en harina n" to "enharinan",
            "en harina r" to "enharinar",
            "de sal a" to "desala"
        )
        for ((old, new) in commonSplits) {
            fixed = fixed.replace(Regex(old, RegexOption.IGNORE_CASE), new)
        }

        val targets = listOf(
            """antelaci\S+n\S*y\S*d\S+jalos?""" to "antelación y déjalo",
            """reduceelpicodeglucosa""" to "reduce el pico de glucosa",
            "pastadeesta" to "pasta de esta",
            "quemagrasasyreduce" to "quemagrasas y reduce"
        )
        for ((pattern, replacement) in targets) {
            fixed = fixed.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
        }

        fixed = fixed.split(" ").joinToString(" ") { autoSplitJoinedWords(it) }

        return fixed.replace(Regex("""\s{2,}"""), " ").trim()
    }
}
