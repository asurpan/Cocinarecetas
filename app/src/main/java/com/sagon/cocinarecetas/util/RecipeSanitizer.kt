package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe
import java.text.Normalizer

object RecipeSanitizer {

    private val commonKeywords = setOf(
        "el", "la", "los", "las", "un", "una", "unos", "unas", "de", "del", "a", "al", "con", "en", "por", "para", "se", "su", "sus", "que", "si", "no", "como", "y", "e", "o", "u", "s", "n",
        "hacer", "hacen", "poner", "ponen", "quitar", "anadir", "añadir", "anade", "añade", "mezclar", "mezclan", "batir", "baten", "cocer", "cuecen", "hervir", "hierve", "freir", "frien", "fríen", "asar", "asan", "hornear", "hornean", "sofreir", "sofríen", "rehogar", "rehogan", "picar", "pican", "cortar", "cortan", "pelar", "pelan", "limpiar", "limpian", "lavar", "lavan", "escurrir", "escurren", "triturar", "trituran", "incorporar", "incorporan", "verter", "vierten", "echar", "echan", "retirar", "retiran", "dejar", "dejan", "deja", "reposar", "reposan", "enfriar", "enfrian", "calentar", "calientan", "dorar", "doran", "sellar", "sellan", "sella", "sazonar", "alinar", "aliñar", "salpimentar", "rectificar", "servir", "sirven", "decorar", "adornar", "cubrir", "cubren", "tapar", "tapan", "desmenuzar", "aplastar", "chafar", "moler", "rallar", "tamizar", "montar", "emulsionar", "reducir", "evaporar", "nacarar", "prepara", "preparar", "quede", "queden", "tomen", "tome", "colocar", "colocan", "quitarle", "saque", "meta", "meten", "sofriendo", "batiendo", "mezclando", "rehogando", "picando", "cociendo", "hirviendo", "friendo", "asando", "horneando", "cortando", "limpiando", "lavando", "escurriendo", "triturando", "incorporando", "virtiendo", "echando", "poniendo", "quitando", "retirando", "dejando", "reposando", "enfriando", "calentando", "dorando", "sellando", "sazonando", "alinando", "sirviendo", "cubriendo", "tapando", "desmenuzando", "aplastando", "chafando", "moliendo", "rallando", "tamizando", "montando", "emulsionando", "reduciendo", "diluir", "diluye", "hara", "hará", "amasar", "amasa", "rebozar", "rebozan",
        "aceite", "agua", "sal", "pimienta", "cebolla", "ajo", "ajos", "tomate", "tomates", "harina", "huevo", "huevos", "leche", "carne", "pollo", "pescado", "arroz", "pasta", "patatas", "patata", "verdura", "verduras", "fruta", "frutas", "vino", "blanco", "tinto", "pimenton", "perejil", "laurel", "canela", "clavo", "queso", "jamon", "nata", "yogur", "azucar", "miel", "levadura", "mantequilla", "manteca", "pan", "migas", "caldo", "limon", "naranja", "vinagre", "mostaza", "mayonesa", "ketchup", "salsa", "salsas", "bacalao", "atun", "merluza", "salmon", "gambas", "langostinos", "mejillones", "almejas", "calamares", "pulpo", "albondiga", "albondigas", "albóndigas", "filete", "lomo", "costilla", "ternera", "cerdo", "cordero", "pavo", "conejo", "garbanzos", "lentejas", "alubias", "frijoles", "frijol", "habas", "guisantes", "chicharos", "maiz", "maíz", "champinon", "setas", "seta", "espinacas", "acelgas", "calabaza", "calabacin", "berenjena", "zanahoria", "pimiento", "pepino", "lechuga", "aguacate", "nueces", "almendras", "avellanas", "pinones", "piñones", "semillas", "sesamo", "trufa", "albahaca", "cilantro", "romero", "tomillo", "oregano", "comino", "curry", "turmeric", "curcuma", "jengibre", "azafran", "vainilla", "canela", "anis", "menta", "espina", "espinas", "piel", "hueso", "huesos", "picada", "picado", "molida", "molido", "rallada", "rallado", "cortada", "cortado", "limpia", "limpio", "lavada", "lavado", "escurrida", "escurrido", "batida", "batido", "mezclada", "mezclado", "cocida", "cocido", "frita", "frito", "asada", "asado", "horneada", "horneado", "sofrida", "sofrito", "rehogada", "rehogado", "triturada", "triturado", "masa", "barro", "cazuela", "sarten", "sartén", "olla", "horno", "fuego", "nevera", "minutos", "hora", "cuarto", "par", "solo", "antes"
    )

    private val commonWordsNorm = commonKeywords.map { normalize(it) }.toSet()

    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .lowercase()
            .replace("ñ", "n")
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun autoSplit(word: String): String {
        val normWord = normalize(word)
        if (normWord.length < 3 || normWord in commonWordsNorm) return word
        
        for (i in normWord.length - 1 downTo 2) {
            val prefixNorm = normWord.substring(0, i)
            if (prefixNorm in commonWordsNorm) {
                val rest = word.substring(i)
                val restSplit = autoSplit(rest)
                return "${word.substring(0, i)} $restSplit"
            }
        }
        return word
    }

    fun sanitize(recipe: Recipe): Recipe {
        return recipe.copy(
            title = fixSpacedText(recipe.title).uppercase(),
            ingredients = recipe.ingredients.map { fixSpacedText(it) },
            instructions = recipe.instructions.map { fixSpacedText(it) }
        )
    }

    fun fixSpacedText(text: String): String {
        if (text.length < 2) return text
        var fixed = Normalizer.normalize(text, Normalizer.Form.NFC)
        
        // --- 1. ARREGLO DE ERRORES OCR CRITICOS (De tu captura) ---
        val ocrFixes = mapOf(
            """s\s+in\b""" to "sin",
            """cos\b""" to "dos",
            """os\s+ajos""" to "los ajos",
            """unaS\s+AL\s+SA""" to "una salsa",
            """formade""" to "forma de",
            """rebozanen""" to "rebozan en",
            """\bhuevo\s*y\s*frie\s*el""" to "huevo y frie el",
            """y\s*se\s*deja""" to "y se deja"
        )
        for ((pattern, replacement) in ocrFixes) {
            fixed = fixed.replace(Regex(pattern, RegexOption.IGNORE_CASE), replacement)
        }

        // --- 2. UNIR LETRAS SUELTAS (S A L S A -> SALSA) ---
        fixed = fixed.replace(Regex("""(\b\w\b\s+)+(\b\w\b)""")) { it.value.replace(" ", "") }

        // --- 3. SEPARAR PALABRAS PEGADAS POR DICCIONARIO ---
        fixed = fixed.split(" ").joinToString(" ") { autoSplit(it) }

        return fixed.replace(Regex("""\s{2,}"""), " ").trim()
    }

    private fun fixEncoding(text: String): String {
        val replacements = mapOf("Ã¡" to "á", "Ã©" to "é", "Ã­" to "í", "Ã³" to "ó", "Ãº" to "ú", "Ã±" to "ñ", "Â½" to "½")
        var res = text
        replacements.forEach { (old, new) -> res = res.replace(old, new) }
        return res
    }
}
