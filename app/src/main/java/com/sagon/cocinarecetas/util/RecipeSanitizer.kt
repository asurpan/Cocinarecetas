package com.sagon.cocinarecetas.util

import com.sagon.cocinarecetas.data.model.Recipe

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

    private val salsasPriorityKeywords = listOf("SALSA", "ALIÑO", "VINAGRETA", "MAYONESA", "MOJO", "BECHAMEL", "ROQUEFORT", "AJOARRIERO")

    private val mainDishFalseKeywords = listOf(
        "MACERAR", "MARINAR", "DESALAR", "TRUCO", "TÉCNICA", "RELLENO",
        "MASA", "SOFRITO", "LIMPIEZA", "PREPARACIÓN", "DECORACIÓN", "CÓMO HACER", "CALDO", "BASE"
    )

    fun sanitize(recipe: Recipe): Recipe {
        val titleCleaned = fixSpacedText(fixEncoding(recipe.title)).uppercase().trim()
        
        var categoryCleaned = fixEncoding(recipe.category).trim()
        val catNormalized = categoryCleaned.replace(" ", "").lowercase()
        
        var mappedCat: String? = null
        for ((key, words) in categoryMapRules) {
            if (words.any { catNormalized.contains(it) }) {
                mappedCat = key
                break
            }
        }
        
        if (mappedCat == null) {
            val titleNorm = titleCleaned.lowercase().replace(" ", "")
            for ((key, words) in categoryMapRules) {
                if (words.any { titleNorm.contains(it) }) {
                    mappedCat = key
                    break
                }
            }
        }
        
        categoryCleaned = mappedCat ?: "APERITIVOS"

        for (kw in salsasPriorityKeywords) {
            if (titleCleaned.contains(kw)) {
                categoryCleaned = "SALSAS"
                break
            }
        }

        var isMain = true
        if (categoryCleaned == "SALSAS") {
            isMain = false
        } else {
            for (kw in mainDishFalseKeywords) {
                if (titleCleaned.contains(kw)) {
                    isMain = false
                    break
                }
            }
        }

        return recipe.copy(
            title = titleCleaned,
            category = categoryCleaned,
            ingredients = sanitizeIngredients(recipe.ingredients.map { fixEncoding(it) }),
            instructions = recipe.instructions.map { fixSpacedText(fixEncoding(it)) },
            isMainDish = isMain
        )
    }

    private fun fixEncoding(text: String): String {
        var fixed = text
        val replacements = mapOf(
            "Ã¡" to "á", "Ã©" to "é", "Ã\u00AD" to "í", "Ã³" to "ó", "Ãº" to "ú",
            "Ã±" to "ñ", "Ã‘" to "Ñ", "Ã“" to "Ó", "Ã\u00A0" to "Á", "Ã\u0089" to "É",
            "Â½" to "½", "Âº" to "º", "Ã" to "í", "%%" to "É"
        )
        for ((old, new) in replacements) {
            fixed = fixed.replace(old, new)
        }
        
        fixed = fixed.replace("FRÃDO", "FRÍO")
            .replace("PURÃ", "PURÉ")
            .replace("CARIÃ", "CARIÑOSA")
            .replace("ALIÃ‘O", "ALIÑO")
            .replace("TIÃ“RTARA", "TÁRTARA")
            .replace("TIORTARA", "TÁRTARA")
            
        return fixed
    }

    fun sanitizeIngredients(ingredients: List<String>): List<String> {
        if (ingredients.isEmpty()) return emptyList()

        val cleaned = ingredients.map { fixSpacedText(it) }.filter { it.isNotBlank() }
        val result = mutableListOf<String>()
        
        for (item in cleaned) {
            var formatted = item
            formatted = formatted.replace(Regex("""(\d+)\s*(gr|g|ml|Kg|kg|cl|l|unidades|unidad|dientes|diente|porciones|porcion|cucharadas|cucharada|vaso|taza|pizca|gramos|litro)\s*(\d+)""", RegexOption.IGNORE_CASE)) { match ->
                "${match.groupValues[1]} ${match.groupValues[2]} ${match.groupValues[3]}"
            }
            formatted = formatted.replace(Regex("""([a-zA-ZñÑáéíóúÁÉÍÓÚ])(\d)"""), "$1 $2")
            formatted = formatted.replace(Regex("""(\d)([a-zA-ZñÑáéíóúÁÉÍÓÚ])"""), "$1 $2")
            result.add(formatted.trim().replace(Regex("""\s{2,}"""), " "))
        }
        
        return result
    }

    fun fixSpacedText(text: String): String {
        if (text.length < 3) return text
        var fixed = text

        fixed = fixed.replace(Regex("""\bdoc\s+[a-zA-Z0-9]+\b""", RegexOption.IGNORE_CASE), "")
        fixed = fixed.replace(Regex("""\bpage\s+\d+\b""", RegexOption.IGNORE_CASE), "")
        fixed = fixed.replace("---", "")

        val heavyFixes = mapOf(
            "car doco ngua ntes" to "cardo con guantes",
            "dur ante un ah orae nagu acon" to "durante una hora en agua con",
            "has taqu eque de \"\"al dente\"\"" to "hasta que quede \"al dente\"",
            "has taqu equede \"ald en te\"" to "hasta que quede \"al dente\"",
            "has taqu eset rans pare nte" to "hasta que esté transparente",
            "Sis ecue ce eno llae xpre ssse rásuf icie ntec on" to "Si se cuece en olla express será suficiente con ",
            "Una vez coc idos edej aesc urri r" to "Una vez cocidos se deja escurrir",
            "sof ríenl os" to "sofríen los",
            "los4" to "los 4",
            "de a jo" to "de ajo",
            "Seaña de" to "Se añade",
            "lah arin ayl astr escu char adas dea ceit e" to "la harina y las tres cucharadas de aceite",
            "dea lm en drar al la da" to "de almendra rallada",
            "se vierteels ofri toso bree lcar do" to "se vierte el sofrito sobre el cardo",
            "sem ezcl atod o" to "se mezcla todo",
            "fin al me nte" to "finalmente",
            "sel eaña de lal eche" to "se le añade la leche",
            "af uego muy l en toha star educ irsu vol um en al am itad ysed ejae nfri ar" to "a fuego muy lento hasta reducir su volumen a la mitad y se deja enfriar",
            "Seasanlospimientosenelhornoyunavezenfriadossepelanysequitannlassemillas" to "Se asan los pimientos en el horno y una vez enfriados se pelan y se quitan las semillas",
            "Sep ic al aceb ol la fina ysed ora" to "Se pica la cebolla fina y se dora",
            "Set ritu rana mbas c la sesd epim ient osyl aceb ol la" to "Se trituran ambas clases de pimientos y la cebolla",
            "Seaña de l al eche hast aobt en er el es peso rdes eado" to "Se añade la leche hasta obtener el espesor deseado",
            "Ses al pi ment aalg usto yses irve mu yfría" to "Se salpimenta al gusto y se sirve muy fría",
            "cam adee ns al adac onl echu gaco rtad aen juliana" to "cama de ensalada con lechuga cortada en juliana",
            "esc arol atro cead a,ber rosy hoj as de rob le(ora dicc ioo 10110r osso)segúnloq ue seencue ntre en e lmer cado" to "escarola troceada, berros y hojas de roble (o radiccio o lollo rosso) según lo que se encuentre en el mercado",
            "Se forma una cama de ensalada con lechuga cortada en juliana, escarola troceada, berros y hojas de roble (o radiccio o lollo rosso) según lo que se encuentre en el mercado." to "Se forma una cama de ensalada con lechuga cortada en juliana, escarola troceada, berros y hojas de roble (o radiccio o lollo rosso) según lo que se encuentre en el mercado.",
            "me nt aent iras finasque conseguimos al cortarlas hojas que han sidoenrolladase nsentido vertical,ysel aañadi mo sal aens al ad a.Sec oloc al ar odaj adeq ueso sobre elpan dem ol de,queha sido cortadoco nunv asoi nver tido,se espolvoreaelq ueso con pimienta negra ysem etea lhor no ag rati nar.Seh aceu nav inag reta, batiendocon unt en ed or la mermelada de frambuesa ente(deo liva)y el vinagre de mes ahas taco nseg uiru nam ezcl ahom ogéneay con ella se rocía la ensalada.Se saca el quesog rati nado de lh or no ysec oloc aene lc en trod el ac am adee ns al ada,sec oloc an la shoj as dem en ta alr eded or de lque soys obre la ensaladaco mod ecor ación.Seaña de tom il lo,rom eroy orégan oalg usto." to "menta en tiras finas que conseguimos al cortar las hojas que han sido enrolladas en sentido vertical, y se la añadimos a la ensalada. Se coloca la rodaja de queso sobre el pan de molde, que ha sido cortado con un vaso invertido, se espolvorea el queso con pimienta negra y se mete al horno a gratinar. Se hace una vinagreta, batiendo con un tenedor la mermelada de frambuesa con aceite de oliva y el vinagre de mesa hasta conseguir una mezcla homogénea y con ella se rocía la ensalada. Se saca el queso gratinado del horno y se coloca en el centro de la cama de ensalada, se colocan las hojas de menta alrededor del queso y sobre la ensalada como decoración. Se añade tomillo, romero y orégano al gusto.",
            "cam adee ns al adac onl echu gaco rtad aenj ulia na" to "cama de ensalada con lechuga cortada en juliana",
            "tom il lo" to "tomillo",
            "rom eroy" to "romero y"
        )

        for ((old, new) in heavyFixes) {
            fixed = fixed.replace(old, new, ignoreCase = true)
        }

        val replacements = mapOf(
            "Sep el las l" to "Se pela el ",
            "Sep el las" to "Se pela las",
            "Sep el an" to "Se pelan",
            "Sep el a" to "Se pela",
            "Sec orta n" to "Se cortan",
            "Sec orta" to "Se corta",
            "Sef orma" to "Se forma",
            "Sec uece n" to "Se cuecen",
            "Sec uece" to "Se cuece",
            "Sep on ee n" to "Se pone en",
            "Sep on een" to "Se pone en",
            "Sep on en" to "Se ponen",
            "Sep on e" to "Se pone",
            "Ses al pi ment a" to "Se salpimenta",
            "Ses al pi mentan" to "Se salpimentan",
            "Ses al" to "Se sal",
            "Sev iert ee n" to "Se vierte en",
            "Sev iert e" to "Se vierte",
            "See scur re n" to "Se escurren",
            "See scur ren" to "Se escurren",
            "See scur re" to "Se escurre",
            "Ser ehog an" to "Se rehogan",
            "Ser ehog a" to "Se rehoga",
            "Seh ac en" to "Se hacen",
            "Seh ac e" to "Se hace",
            "Sel impi an" to "Se limpian",
            "Sel impi a" to "Se limpia",
            "Set roce an" to "Se trocean",
            "Set roce a" to "Se trocea",
            "Ses irv en" to "Se sirven",
            "Ses irv e" to "Se sirve",
            "Seba te n" to "Se baten",
            "Seba te" to "Se bate",
            "Seanad en" to "Se añaden",
            "Seanad e" to "Se añade",
            "Seañad en" to "Se añaden",
            "Seañad e" to "Se añade",
            "Se añade n" to "Se añaden",
            "Se añade" to "Se añade",
            "Se me zc la" to "Se mezcla",
            "Se de jan" to "Se dejan",
            "Se de ja" to "Se deja",
            "Se r o cí a" to "Se rocía",
            "Se r o ci a" to "Se rocía",
            "Seh ierv en" to "Se hierven",
            "Seh ierv e" to "Se hierve",
            "Enl asar tén" to "En la sartén",
            "En la sar tén" to "En la sartén",
            "af uego" to "a fuego",
            "afue go" to "a fuego",
            "m inut os" to "minutos",
            "min utos" to "minutos",
            "t acos" to "tacos",
            "t rozo s" to "trozos",
            "ent rozo s" to "en trozos",
            "entir as" to "en tiras",
            "en j ulia na" to "en juliana",
            "enj ulia na" to "en juliana",
            "alac oc ina" to "a la cocina",
            "con s al" to "con sal",
            "cons al" to "con sal",
            "unp oc o" to "un poco",
            "unp oc ode" to "un poco de"
        )

        for ((old, new) in replacements) {
            fixed = fixed.replace(old, new, ignoreCase = true)
        }

        val wordFixes = mapOf(
            "cal abacín" to "calabacín",
            "cal abacin" to "calabacín",
            "z anah oria" to "zanahoria",
            "zanah oria" to "zanahoria",
            "cebo lla" to "cebolla",
            "puer ro" to "puerro",
            "pimi en to" to "pimiento",
            "toma te" to "tomate",
            "pata ta" to "patata",
            "ace ite" to "aceite",
            "har ina" to "harina",
            "die ntes" to "dientes",
            "cuc hara da" to "cucharada",
            "basta nte" to "bastante",
            "f uerz a" to "fuerza",
            "fina s" to "finas",
            "segu iros" to "seguros",
            "con segu imos" to "conseguimos",
            "al c orta r" to "al cortar",
            "alc orta r" to "al cortar",
            "hoja sque" to "hojas que",
            "han sid o" to "han sido",
            "enr ol la das" to "enrolladas",
            "s en tido" to "sentido",
            "se n tido" to "sentido",
            "vert ic al" to "vertical",
            "sob ree l" to "sobre el",
            "sob re la" to "sobre la",
            "pan de m ol de" to "pan de molde",
            "has idoc orta do" to "ha sido cortado",
            "see spol vore a" to "se espolvorea",
            "pim ient aneg ra" to "pimienta negra",
            "bat iend o" to "batiendo",
            "lam erme la da" to "la mermelada",
            "def ramb aame sa" to "de frambuesa",
            "yelv inag re" to "y el vinagre",
            "unam ezcl ahom ogénea" to "una mezcla homogénea",
            "ycon ell aser ocía" to "y con ella se rocía",
            "la en sa la da" to "la ensalada",
            "Ses acae lque so" to "Se saca el queso"
        )

        for ((old, new) in wordFixes) {
            fixed = fixed.replace(old, new, ignoreCase = true)
        }

        fixed = fixed.replace(Regex("""\bonch\s+as\b""", RegexOption.IGNORE_CASE), "lonchas")
        fixed = fixed.replace(Regex("""\bfi\s+nas\b""", RegexOption.IGNORE_CASE), "finas")
        fixed = fixed.replace(Regex("""\bme\s+nt\s+a\b""", RegexOption.IGNORE_CASE), "menta")
        fixed = fixed.replace(Regex("""\bal\s+me\s+ndra\s+s\b""", RegexOption.IGNORE_CASE), "almendras")
        fixed = fixed.replace(Regex("""\bal\s+me\s+ndra\b""", RegexOption.IGNORE_CASE), "almendra")
        fixed = fixed.replace(Regex("""\bch\s+am\s+pi\s+ñone\s+s\b""", RegexOption.IGNORE_CASE), "champiñones")
        fixed = fixed.replace(Regex("""\bguis\s+ante\s+s\b""", RegexOption.IGNORE_CASE), "guisantes")
        fixed = fixed.replace(Regex("""\bguis\s+ante\b""", RegexOption.IGNORE_CASE), "guisante")
        fixed = fixed.replace(Regex("""\bsemi\s+llas\b""", RegexOption.IGNORE_CASE), "semillas")
        fixed = fixed.replace(Regex("""\blech\s+uga\b""", RegexOption.IGNORE_CASE), "lechuga")
        fixed = fixed.replace(Regex("""\besc\s+arol\s+a\b""", RegexOption.IGNORE_CASE), "escarola")
        fixed = fixed.replace(Regex("""\bserra\s+no\b""", RegexOption.IGNORE_CASE), "serrano")

        fixed = fixed.replace(Regex("""\b([b-df-hj-np-tv-z]{1,2}[aeiou]{1,2})\s+([b-df-hj-np-tv-z]{1,2}[aeiou]{0,2})\b""", RegexOption.IGNORE_CASE)) { match ->
            val w1 = match.groupValues[1].lowercase()
            val w2 = match.groupValues[2].lowercase()
            val stopWords = setOf("se", "el", "la", "un", "en", "de", "al", "su", "no", "si", "lo", "le", "me", "te", "va", "ve")
            if (stopWords.contains(w1) || stopWords.contains(w2)) {
                match.value
            } else {
                match.groupValues[1] + match.groupValues[2]
            }
        }

        val singleLetterPattern = Regex("""(?<=\b\w)\s+(?=\w\b)""")
        repeat(5) {
            val next = fixed.replace(singleLetterPattern, "")
            if (next == fixed) return@repeat
            fixed = next
        }

        fixed = fixed.replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
        fixed = fixed.replace(Regex("""\s+([,.:;])"""), "$1")
        fixed = fixed.replace(Regex("""\s{2,}"""), " ")

        fixed = correctSpellingAndAccents(fixed)

        return fixed.trim()
    }

    fun correctSpellingAndAccents(text: String): String {
        var fixed = text
        val spellingMap = mapOf(
            "sarten" to "sartén",
            "Sarten" to "Sartén",
            "anade" to "añade",
            "anaden" to "añaden",
            "Anade" to "Añade",
            "Anaden" to "Añaden",
            "balsamico" to "balsámico",
            "Balsamico" to "Balsámico",
            "homogenea" to "homogénea",
            "decoracion" to "decoración",
            "Decoracion" to "Decoración",
            "oregano" to "orégano",
            "Oregano" to "Orégano",
            "calabacin" to "calabacín",
            "Calabacin" to "Calabacín",
            "pimenton" to "pimentón",
            "Pimenton" to "Pimentón",
            "cazon" to "cazón",
            "Cazon" to "Cazón",
            "pure" to "puré",
            "Pure" to "Puré",
            "frio" to "frío",
            "Frio" to "Frío",
            "tartara" to "tártara",
            "Tartara" to "Tártara",
            "aldente" to "\"al dente\"",
            "ald en te" to "\"al dente\"",
            "al de nte" to "\"al dente\""
        )
        for ((old, new) in spellingMap) {
            fixed = fixed.replace(Regex("""\b$old\b""", RegexOption.IGNORE_CASE)) { match ->
                if (match.value.first().isUpperCase()) new.replaceFirstChar { it.uppercase() } else new
            }
        }
        return fixed
    }
}
