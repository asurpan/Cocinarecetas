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

    private val commonKeywords = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "a", "al", "con", "en", "por", "para", "se", "su", "sus", "que", "si", "no", "como", "y", "e", "o", "u",
        "hacer", "poner", "quitar", "anadir", "añadir", "mezclar", "batir", "cocer", "hervir", "freir", "fríe", "frie", "asar", "hornear", "sofreir", "rehogar", "picar", "cortar", "pelar", "limpiar", "lavar", "escurrir", "triturar", "incorporar", "verter", "echar", "retirar", "dejar", "deja", "reposar", "enfriar", "calentar", "precalentar", "dorar", "sellar", "sella", "sazonar", "alinar", "aliñar", "salpimentar", "rectificar", "servir", "decorar", "adornar", "cubrir", "tapar", "desmenuzar", "aplastar", "chafar", "moler", "rallar", "tamizar", "montar", "emulsionar", "reducir", "evaporar", "nacarar", "doran", "cuecen", "pican", "cortan", "limpian", "lavan", "anaden", "añaden", "mezclan", "frien", "hacen", "prepara", "preparar", "quede", "queden", "tomen", "tome", "colocar", "colocan", "quitarle", "echan", "sirven", "hierve", "quita", "saque", "saquen", "meta", "meten", "sofriendo", "batiendo", "mezclando", "anadiendo", "añadiendo", "rehogando", "picando", "cociendo", "hirviendo", "friendo", "asando", "horneando", "cortando", "limpiando", "lavando", "escurriendo", "triturando", "incorporando", "virtiendo", "echando", "poniendo", "quitando", "retirando", "dejando", "reposando", "enfriando", "calentando", "dorando", "sellando", "sazonando", "alinando", "aliñando", "sirviendo", "cubriendo", "tapando", "desmenuzando", "aplastando", "chafando", "moliendo", "rallando", "tamizando", "montando", "emulsionando", "reduciendo", "diluir", "diluye", "hara", "hará",
        "aceite", "agua", "sal", "pimienta", "cebolla", "ajo", "ajos", "tomate", "tomates", "harina", "huevo", "huevos", "leche", "carne", "pollo", "pescado", "arroz", "pasta", "patatas", "patata", "verdura", "verduras", "fruta", "frutas", "vino", "blanco", "tinto", "pimenton", "perejil", "laurel", "canela", "clavo", "queso", "jamon", "nata", "yogur", "azucar", "miel", "levadura", "mantequilla", "manteca", "pan", "migas", "caldo", "limon", "naranja", "vinagre", "mostaza", "mayonesa", "ketchup", "salsa", "salsas", "bacalao", "atun", "merluza", "salmon", "gambas", "gamba", "langostinos", "mejillones", "almejas", "calamares", "pulpo", "albondiga", "albondigas", "filete", "filetes", "lomo", "costilla", "ternera", "cerdo", "cordero", "pavo", "conejo", "garbanzos", "lentejas", "alubias", "frijoles", "frijol", "habas", "guisantes", "chicharos", "maiz", "maíz", "champinon", "champinones", "champiñón", "champiñones", "setas", "seta", "espinacas", "espinaca", "acelgas", "acelga", "calabaza", "calabacin", "berenjena", "zanahoria", "zanahorias", "pimiento", "pimientos", "pepino", "lechuga", "aguacate", "nueces", "almendras", "avellanas", "pinones", "piñones", "semillas", "sesamo", "sésamo", "trufa", "albahaca", "cilantro", "romero", "tomillo", "oregano", "orégano", "comino", "curry", "turmeric", "curcuma", "jengibre", "azafran", "azafrán", "vainilla", "canela", "anis", "menta", "espina", "espinas", "piel", "hueso", "huesos", "picada", "picado", "molida", "molido", "rallada", "rallado", "cortada", "cortado", "limpia", "limpio", "lavada", "lavado", "escurrida", "escurrida", "batida", "batido", "mezclada", "mezclado", "cocida", "cocido", "frita", "frito", "asada", "asado", "horneada", "horneado", "sofrida", "sofrito", "rehogada", "rehogado", "triturada", "triturado", "incorporada", "incorporado", "vertida", "vertido", "echada", "echado", "puesta", "puesto", "quitada", "quitado", "retirada", "retirado", "dejada", "dejado", "reposada", "reposado", "enfriada", "enfriado", "calentada", "calentado", "dorada", "dorado", "sellada", "sellado", "sazonada", "sazonado", "alinada", "alinado", "aliñada", "aliñado", "servida", "servido", "cubierta", "cubierto", "tapada", "tapado", "masa", "barro", "cazuela", "sarten", "sartén", "olla", "horno", "noche", "par", "minutos", "paso", "solo", "antes", "botellita", "chorrito", "tacita", "grano"
    )

    private val commonWordsNorm = commonKeywords.map { normalize(it) }.toSet()

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

        var instructionsCleaned = recipe.instructions.map { fixSpacedText(fixEncoding(it)) }
        val titleNorm = titleCleaned.lowercase()
        val allIngredientsNorm = recipe.ingredients.map { normalize(it) }
        
        fun hasIng(vararg keywords: String) = keywords.any { k -> 
            val nk = normalize(k)
            titleNorm.contains(nk) || allIngredientsNorm.any { it.contains(nk) } 
        }

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
            ingredients = recipe.ingredients.map { fixSpacedText(fixEncoding(it)) },
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

    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace("ñ", "n")
    }

    private fun autoSplitJoinedWords(word: String): String {
        val normWord = normalize(word)
        if (normWord.length < 3 || normWord in commonWordsNorm) return word
        
        for (i in normWord.length - 1 downTo 2) {
            val prefixNorm = normWord.substring(0, i)
            if (prefixNorm in commonWordsNorm) {
                val rest = word.substring(i)
                val restSplit = autoSplitJoinedWords(rest)
                return "${word.substring(0, i)} $restSplit"
            }
        }
        return word
    }

    fun fixSpacedText(text: String): String {
        if (text.length < 2) return text
        var fixed = Normalizer.normalize(text, Normalizer.Form.NFC)
        
        // --- 1. PATRONES CRITICOS DE UNION (Regex Flexible) ---
        val ocrFixes = mapOf(
            """A\s+T[ÚU]\s*N""" to "ATÚN",
            """A\s+JO""" to "AJO",
            """A\s+CEITE""" to "ACEITE",
            """A\s+RROZ""" to "ARROZ",
            """al\s+bahaca""" to "albahaca",
            """la\s+ngostinos?""" to "langostinos",
            """an\s+te\s+s""" to "antes",
            """de\s+ja""" to "deja",
            """se\s+l\s+la""" to "sella",
            """so\s+lo""" to "solo",
            """su\s+el\s+te\s*n?""" to "suelte",
            """gr\s+a\s+no""" to "grano",
            """al\s+as""" to "alas",
            """de\s+n""" to "den",
            """y\s*dejalo""" to "y dejalo",
            """moja\s*con""" to "moja con",
            """sal\s*gorda""" to "sal gorda",
            """tacita\s*con""" to "tacita con",
            """diluye\s*estas""" to "diluye estas",
            """esto\s*hara""" to "esto hara",
            """huevo\s*y\s*frie\s*el""" to "huevo y frie el"
        )
        
        for ((pattern, replacement) in ocrFixes) {
            fixed = fixed.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
        }

        // --- 2. UNIR LETRAS SUELTAS RESTANTES (S A L S A -> SALSA) ---
        fixed = fixed.replace(Regex("""(\b\w\b\s+)+(\b\w\b)""")) { match ->
            match.value.replace(" ", "")
        }

        // --- 3. REPARACION DE SALSA (OCR suele fallar aqui) ---
        fixed = fixed.replace(Regex("""unaS\s+AL\s+SA""", RegexOption.IGNORE_CASE), "unas salsa")
                     .replace(Regex("""una\s+S\s+AL\s+SA""", RegexOption.IGNORE_CASE), "una salsa")
                     .replace(Regex("""S\s+AL\s+SA""", RegexOption.IGNORE_CASE), "salsa")

        // --- 4. SPLIT AUTOMÁTICO POR DICCIONARIO ---
        fixed = fixed.split(" ").joinToString(" ") { autoSplitJoinedWords(it) }

        return fixed.replace(Regex("""\s{2,}"""), " ").trim()
    }
}
