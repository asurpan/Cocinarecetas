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

        // Corregir prefijos "AL" separados y palabras clave mal cortadas (Errores de OCR comunes)
        fixed = fixed.replace(Regex("""\bA\s+L\s+B[ÓO]NDIGAS\b""", RegexOption.IGNORE_CASE), "ALBÓNDIGAS")
                     .replace(Regex("""\bAL\s+B[ÓO]NDIGAS\b""", RegexOption.IGNORE_CASE), "ALBÓNDIGAS")
                     .replace(Regex("""\bPO\s*[_-]\s*LO\b""", RegexOption.IGNORE_CASE), "POLLO")
                     .replace(Regex("""\bSepican\b""", RegexOption.IGNORE_CASE), "Se pican")
                     .replace(Regex("""\bstrozosdepol\s+lo\b""", RegexOption.IGNORE_CASE), "trozos de pollo")
                     .replace(Regex("""\bA\s+las\b""", RegexOption.IGNORE_CASE), "Alas")
                     .replace(Regex("""\bA\s+lbóndigas\b""", RegexOption.IGNORE_CASE), "Albondigas")
                     .replace(Regex("""\bA\s+lcachofas\b""", RegexOption.IGNORE_CASE), "Alcachofas")
                     .replace(Regex("""\bA\s+lubias\b""", RegexOption.IGNORE_CASE), "Alubias")
                     .replace(Regex("""\bA\s+lmejas\b""", RegexOption.IGNORE_CASE), "Almejas")
                     .replace(Regex("""\bperasa\b""", RegexOption.IGNORE_CASE), "peras a")
                     .replace(Regex("""\bs\s+nunbol\b""", RegexOption.IGNORE_CASE), "en un bol")
                     .replace(Regex("""\be\s+panr\s+al\s+a\s+do\b""", RegexOption.IGNORE_CASE), "empanado")
                     .replace(Regex("""\bcimadel\b""", RegexOption.IGNORE_CASE), "encima del")
                     .replace(Regex("""\bHUEVOS\s*,\b""", RegexOption.IGNORE_CASE), "huevos,")
                     .replace(Regex("""\bSeamasa\b""", RegexOption.IGNORE_CASE), "Se amasa")

        // Reglas de pegado comunes detectadas en Escalfar Huevos y otras
        fixed = fixed.replace(Regex("""\baguacon\b""", RegexOption.IGNORE_CASE), "agua con")
                     .replace(Regex("""\bechalos\b""", RegexOption.IGNORE_CASE), "echa los")
                     .replace(Regex("""\bde\s+beránsermuy\b""", RegexOption.IGNORE_CASE), "deberán ser muy")
                     .replace(Regex("""\bdeberánsermuy\b""", RegexOption.IGNORE_CASE), "deberán ser muy")
                     .replace(Regex("""\bComomáximo\b""", RegexOption.IGNORE_CASE), "Como máximo")
                     .replace(Regex("""\btresa\b""", RegexOption.IGNORE_CASE), "tres a")
                     .replace(Regex("""\bhervira\b""", RegexOption.IGNORE_CASE), "hervir a")
                     .replace(Regex("""\bhuevoscon\b""", RegexOption.IGNORE_CASE), "huevos con")
                     .replace(Regex("""(\d)a(\d)"""), "$1 a $2") // "2a3" -> "2 a 3"
                     .replace(Regex("""\bde\s+l\b""", RegexOption.IGNORE_CASE), "del")
                     .replace(Regex("""\bde\s+be\b""", RegexOption.IGNORE_CASE), "debe")
                     
                     // Reglas Masivas de Pegado y OCR rotos agregadas dinámicamente
                     .replace(Regex("""\bantelaciónydéjalos\b""", RegexOption.IGNORE_CASE), "antelación y déjalos")
                     .replace(Regex("""\blasíntesis\b""", RegexOption.IGNORE_CASE), "la síntesis")
                     .replace(Regex("""\bcuandoesté\b""", RegexOption.IGNORE_CASE), "cuando esté")
                     .replace(Regex("""\bpocoa poco\b""", RegexOption.IGNORE_CASE), "poco a poco")
                     .replace(Regex("""\bde\s+rretida\b""", RegexOption.IGNORE_CASE), "derretida")
                     .replace(Regex("""\bde\s+jar\b""", RegexOption.IGNORE_CASE), "dejar")
                     .replace(Regex("""\bal\s+uminio\b""", RegexOption.IGNORE_CASE), "aluminio")
                     .replace(Regex("""\bcon\s+gelador\b""", RegexOption.IGNORE_CASE), "congelador")
                     .replace(Regex("""\bde\s+ntro\b""", RegexOption.IGNORE_CASE), "dentro")
                     .replace(Regex("""\bretíralodel\b""", RegexOption.IGNORE_CASE), "retíralo del")
                     .replace(Regex("""\brellénaloscon\b""", RegexOption.IGNORE_CASE), "rellénalos con")
                     .replace(Regex("""\bvinoblanco\b""", RegexOption.IGNORE_CASE), "vino blanco")
                     .replace(Regex("""\blasalmejas\b""", RegexOption.IGNORE_CASE), "las almejas")
                     .replace(Regex("""\bharinayfríel\s*as\b""", RegexOption.IGNORE_CASE), "harina y fríelas")
                     .replace(Regex("""\bsedejac\b""", RegexOption.IGNORE_CASE), "se deja")
                     .replace(Regex("""\bsedebenañadir\b""", RegexOption.IGNORE_CASE), "se deben añadir")
                     .replace(Regex("""\bcuandoestén\b""", RegexOption.IGNORE_CASE), "cuando estén")
                     .replace(Regex("""\bunasarténsesofríen\b""", RegexOption.IGNORE_CASE), "una sartén se sofríen")
                     .replace(Regex("""\bunasalbóndigas\b""", RegexOption.IGNORE_CASE), "unas albóndigas")
                     .replace(Regex("""\bhastaconseguir\b""", RegexOption.IGNORE_CASE), "hasta conseguir")
                     .replace(Regex("""\bpimentóny\b""", RegexOption.IGNORE_CASE), "pimentón y")
                     .replace(Regex("""\baguafríay\b""", RegexOption.IGNORE_CASE), "agua fría y")
                     .replace(Regex("""\bsequedensin\b""", RegexOption.IGNORE_CASE), "se queden sin")
                     .replace(Regex("""\byañadelas\b""", RegexOption.IGNORE_CASE), "y añade las")
                     .replace(Regex("""\bponlasjudias\b""", RegexOption.IGNORE_CASE), "pon las judías")
                     .replace(Regex("""\bunoauno\b""", RegexOption.IGNORE_CASE), "uno a uno")
                     .replace(Regex("""\bjuntocon\b""", RegexOption.IGNORE_CASE), "junto con")
                     .replace(Regex("""\bSehierven\b""", RegexOption.IGNORE_CASE), "Se hierven")
                     .replace(Regex("""\bSeescurreny\b""", RegexOption.IGNORE_CASE), "Se escurren y")
                     .replace(Regex("""\bsaly\s+sesirve\b""", RegexOption.IGNORE_CASE), "sal y se sirve")
                     .replace(Regex("""\bpuedesañadir\b""", RegexOption.IGNORE_CASE), "puedes añadir")
                     .replace(Regex("""\bacontinuación\b""", RegexOption.IGNORE_CASE), "a continuación")
                     .replace(Regex("""\bSefrien\b""", RegexOption.IGNORE_CASE), "Se fríen")
                     .replace(Regex("""\bSefrie\b""", RegexOption.IGNORE_CASE), "Se fríe")
                     .replace(Regex("""\bbañomaria\b""", RegexOption.IGNORE_CASE), "baño maría")
                     .replace(Regex("""\bSeleañade\b""", RegexOption.IGNORE_CASE), "Se le añade")
                     .replace(Regex("""\bsetapay\b""", RegexOption.IGNORE_CASE), "se tapa y")
                     .replace(Regex("""\bSetroceay\b""", RegexOption.IGNORE_CASE), "Se trocea y")
                     .replace(Regex("""\bEsrecomendable\b""", RegexOption.IGNORE_CASE), "Es recomendable")
                     
                     // Nuevas correcciones específicas detectadas en Albóndigas con Champiñones
                     .replace(Regex("""\bsalbóndigas\b""", RegexOption.IGNORE_CASE), "albóndigas")
                     .replace(Regex("""\bpásalaspor\b""", RegexOption.IGNORE_CASE), "pásalas por")
                     .replace(Regex("""\baceitefríe\b""", RegexOption.IGNORE_CASE), "aceite fríe")
                     .replace(Regex("""\bdespuésañade\b""", RegexOption.IGNORE_CASE), "después añade")
                     .replace(Regex("""\bsalydejalo\b""", RegexOption.IGNORE_CASE), "sal y déjalo")
                     .replace(Regex("""\bsalydejálo\b""", RegexOption.IGNORE_CASE), "sal y déjalo")
                     .replace(Regex("""\bsalydéjalo\b""", RegexOption.IGNORE_CASE), "sal y déjalo")
                     .replace(Regex("""\bPicadil\s+lo\b""", RegexOption.IGNORE_CASE), "Picadillo")
                     .replace(Regex("""\bpanremojada\b""", RegexOption.IGNORE_CASE), "pan remojada")
                     
                     // Nuevas correcciones específicas detectadas en Albóndigas con Queso
                     .replace(Regex("""\bal\s+bondigashaz\b""", RegexOption.IGNORE_CASE), "albóndigas haz")
                     .replace(Regex("""\bEn lasal\s+bóndigashaz\b""", RegexOption.IGNORE_CASE), "En las albóndigas haz")
                     .replace(Regex("""\bcerrándolosa\s+continuaciónPásalaspor\b""", RegexOption.IGNORE_CASE), "cerrándola a continuación. Pásalas por")
                     .replace(Regex("""\bcerrándolosa\s+continuaciónPásalas\b""", RegexOption.IGNORE_CASE), "cerrándola a continuación. Pásalas")
                     .replace(Regex("""\bcontinuaciónPásalaspor\b""", RegexOption.IGNORE_CASE), "continuación. Pásalas por")
                     .replace(Regex("""\bdon\s+de\s+meteráslasalbóndigas\b""", RegexOption.IGNORE_CASE), "donde meterás las albóndigas")
                     .replace(Regex("""\bmeteráslasalbóndigas\b""", RegexOption.IGNORE_CASE), "meterás las albóndigas")
                     .replace(Regex("""\bde\s+jándolas\b""", RegexOption.IGNORE_CASE), "dejándolas")
                     .replace(Regex("""\bhazbolitas\b""", RegexOption.IGNORE_CASE), "haz bolitas")
                     .replace(Regex("""\byañade\b""", RegexOption.IGNORE_CASE), "y añade")
                     .replace(Regex("""\bhazpuré\b""", RegexOption.IGNORE_CASE), "haz puré")
                     .replace(Regex("""\bsalmónymézclalo\b""", RegexOption.IGNORE_CASE), "salmón y mézclalo")
                     .replace(Regex("""\blasalmendras\b""", RegexOption.IGNORE_CASE), "las almendras")
                     .replace(Regex("""\bpanremojada\b""", RegexOption.IGNORE_CASE), "pan remojada")
                     .replace(Regex("""\bpanrallado\b""", RegexOption.IGNORE_CASE), "pan rallado")
                     .replace(Regex("""\bsarténpon\b""", RegexOption.IGNORE_CASE), "sartén pon")
                     .replace(Regex("""\bsácalasa\b""", RegexOption.IGNORE_CASE), "sácalas a")
                     // Corrección de Alcachofas de al Faro y OCR de texto roto
                     .replace("Sehierven la salc acho fasd uran te15 minutos", "Se hierven las alcachofas durante 15 minutos")
                     .replace("cambiándo lese la guaalm en os", "cambiándoles el agua al menos")
                     .replace("Sesazonan y Seescurren", "Se sazonan y se escurren")
                     .replace("En unasarténsesofríen e ndos cucharadasd eaceite", "En una sartén se sofríen en dos cucharadas de aceite")
                     .replace("cuatrod ientes", "cuatro dientes")
                     .replace("Sevierteels ofri tosobr el asal cach ofasyse", "Se vierte el sofrito sobre las alcachofas y se")
                     
                     // Correcciones críticas OCR para Albóndigas de Bacalao
                     .replace("Sedesal aelbac al aodu rant el anoche", "Se desala el bacalao durante la noche")
                     .replace("Sehierveunp ardeminutos", "Se hierve un par de minutos")
                     .replace("selequita la pi el y las espi nasysed esme nuza", "se le quita la piel y las espinas y se desmenuza")
                     .replace("En e la guadec ocerelb ac al aosecue cenl aspa tatasp el adas", "En el agua de cocer el bacalao se cuecen las patatas peladas")
                     .replace("seaplas tany", "se aplastan y")
                     .replace("j un toco nlos HUEVOS", "junto con los huevos")
                     .replace("e la joye lperejil", "el ajo y el perejil")
                     .replace("Se añadenalb ac al ao", "se añaden al bacalao")
                     .replace("Con est amasa se hacen las ALBÓNDIGAS", "Con esta masa se hacen las albóndigas")
                     .replace("Acontinuaciónse harinan", "A continuación se enharinan")
                     .replace("sefríen y sec olocan", "se fríen y se colocan")
                     .replace("una cazuela de b arro", "una cazuela de barro")
                     .replace("Seprepara un asal saso frie ndol acebolla picada", "Se prepara una salsa sofriendo la cebolla picada")
                     .replace("al aq ueSe añade harina", "a la que se añade harina")
                     .replace("el vinoye la gua", "el vino y el agua")
                     .replace("Seechatodos obre la salbóndi gasySe hierveu no spoc os minutos", "Se echa todo sobre las albóndigas y se hierve unos pocos minutos")
                     
                     // Nuevas correcciones de OCR y palabras pegadas o rotas solicitadas
                     .replace(Regex("""\bantelaciónydéjalos\b""", RegexOption.IGNORE_CASE), "antelación y déjalos")
                     .replace(Regex("""\blasíntesis\b""", RegexOption.IGNORE_CASE), "la síntesis")
                     .replace(Regex("""\bcuandoesté\b""", RegexOption.IGNORE_CASE), "cuando esté")
                     .replace(Regex("""\bsedoren\s+y\s+añade\b""", RegexOption.IGNORE_CASE), "se doren y añade")
                     .replace(Regex("""\bsedoreny\s+añade\b""", RegexOption.IGNORE_CASE), "se doren y añade")
                     .replace(Regex("""\bselasa\s+la\s+salsa\b""", RegexOption.IGNORE_CASE), "se las a la salsa")
                     .replace(Regex("""\bselasalasalsa\b""", RegexOption.IGNORE_CASE), "se las a la salsa")
                     .replace(Regex("""\bguisaloa\s+fuego\b""", RegexOption.IGNORE_CASE), "guísalo a fuego")
                     .replace(Regex("""\bguisaloafuego\b""", RegexOption.IGNORE_CASE), "guísalo a fuego")
                     .replace(Regex("""\btrozosmas\b""", RegexOption.IGNORE_CASE), "trozos más")
                     .replace(Regex("""\bincórporalasa\b""", RegexOption.IGNORE_CASE), "incorpóralas a")
                     .replace(Regex("""\bincorporalasa\b""", RegexOption.IGNORE_CASE), "incorpóralas a")
                     .replace(Regex("""\bhabitu\s+al:\b""", RegexOption.IGNORE_CASE), "habitual:")
                     .replace(Regex("""\bhabitu\s+al\b""", RegexOption.IGNORE_CASE), "habitual")
                     .replace(Regex("""\bespor\s+falta\b""", RegexOption.IGNORE_CASE), "es por falta")
                     .replace(Regex("""\besporfalta\b""", RegexOption.IGNORE_CASE), "es por falta")
                     .replace(Regex("""\bcon\s+trario\b""", RegexOption.IGNORE_CASE), "contrario")
                     .replace(Regex("""\bcontrario\b""", RegexOption.IGNORE_CASE), "contrario")
                     .replace(Regex("""\bsaldráncon\b""", RegexOption.IGNORE_CASE), "saldrán con")
                     .replace(Regex("""\bsaldrancon\b""", RegexOption.IGNORE_CASE), "saldrán con")
                     .replace(Regex("""\bmuchomás\b""", RegexOption.IGNORE_CASE), "mucho más")
                     .replace(Regex("""\bmuchomas\b""", RegexOption.IGNORE_CASE), "mucho más")
                     .replace(Regex("""\bsabora\b""", RegexOption.IGNORE_CASE), "sabor a")
                     .replace(Regex("""\bpocoa poco\b""", RegexOption.IGNORE_CASE), "poco a poco")
                     .replace(Regex("""\bde\s+rretida\b""", RegexOption.IGNORE_CASE), "derretida")
                     .replace(Regex("""\bde\s+jar\b""", RegexOption.IGNORE_CASE), "dejar")
                     .replace(Regex("""\bal\s+uminio\b""", RegexOption.IGNORE_CASE), "aluminio")
                     .replace(Regex("""\bcon\s+gelador\b""", RegexOption.IGNORE_CASE), "congelador")
                     
                     // Correcciones críticas OCR para Alubia Blanca de la Granja y comillas repetidas
                     .replace(Regex("\"{2,}", RegexOption.IGNORE_CASE), " ")
                     .replace("Se dejan la ALUBIAS en remojo d ddddurante todo la no che", "Se dejan las alubias en remojo durante toda la noche")
                     .replace("A los poco s minutos de c occiónsecam biae la aaaagua yd espuésselas", "A los pocos minutos de cocción se cambia el agua y después se las")
                     .replace("\"espanta\"", " \"espanta\" ")
                     .replace("unpar de vecescona guafría", "un par de veces con agua fría")
                     .replace("Sehaceuns ofri topo cohecho con", "Se hace un sofrito poco hecho con")
                     .replace("los ajos en tero squeSe añaden al asalubia s", "los ajos enteros que se añaden a las alubias")
                     .replace("Cuandoe sténcoc idasSe añade lasal ye el sofrito", "Cuando estén cocidas se añade la sal y el sofrito")
                     .replace(Regex("""\bDEL\s+ICIAS\b""", RegexOption.IGNORE_CASE), "DELICIAS")
                     .replace(Regex("""\blee\b""", RegexOption.IGNORE_CASE), "le")
                     .replace(Regex("""\bde\s+ntro\b""", RegexOption.IGNORE_CASE), "dentro")
                     .replace(Regex("""\bretíralodel\b""", RegexOption.IGNORE_CASE), "retíralo del")
                     .replace(Regex("""\brellénaloscon\b""", RegexOption.IGNORE_CASE), "rellénalos con")
                     .replace(Regex("""\bvinoblanco\b""", RegexOption.IGNORE_CASE), "vino blanco")
                     .replace(Regex("""\blasalmejas\b""", RegexOption.IGNORE_CASE), "las almejas")
                     .replace(Regex("""\bharinayfríel\s*as\b""", RegexOption.IGNORE_CASE), "harina y fríelas")
                     .replace(Regex("""\bsedejac\b""", RegexOption.IGNORE_CASE), "se deja")
                     .replace(Regex("""\bsedebenañadir\b""", RegexOption.IGNORE_CASE), "se deben añadir")

        // Unir letras sueltas y corregir espaciados dobles
        val singleLetterPattern = Regex("""(?<=\b\w)\s+(?=\w\b)""")
        repeat(3) { fixed = fixed.replace(singleLetterPattern, "") }
        
        return fixed.replace(Regex("""\s{2,}"""), " ").trim()
    }
}
