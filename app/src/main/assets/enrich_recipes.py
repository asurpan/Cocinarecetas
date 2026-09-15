import json
import re

def enrich_recipes():
    file_path = 'recipes.json'
    with open(file_path, 'r', encoding='utf-8') as f:
        recipes = json.load(f)

    # Text cleaning patterns
    replacements = {
        r'tambi\s+n': 'también',
        r'despu\s+s': 'después',
        r'A\s+ade': 'Añade',
        r'a\s+ade': 'añade',
        r'a\s+adiendo': 'añadiendo',
        r'salaz\s+n': 'salazón',
        r'ca\s+ada': 'cañada',
        r'habr\s+s': 'habrás',
        r'p\s+salo': 'pásalo',
        r't\s+rmi\s+no': 'término',
        r'me\s+dio': 'medio',
        r'm\s+s': 'más',
        r'utilizaci\s+n': 'utilización',
        r'cocci\s+n': 'cocción',
        r'el\s+aborar': 'elaborar',
        r'me\s+zcla': 'mezcla',
        r'de\s+ber\s+n': 'deberán',
        r'me\s+nor': 'menor',
        r'de\s+pendiendo': 'dependiendo',
        r'con\s+stante': 'constante',
        r'te\s+mperatura': 'temperatura',
        r'po\s+chado': 'pochado',
        r'me\s+rme\s+lada': 'mermelada',
        r'biz\s+cocho': 'bizcocho',
        r'de\s+rretida': 'derretida',
        r'de\s+rretirse': 'derretirse',
        r'de\s+rrite': 'derrite',
        r'se\s+rvir': 'servir',
        r'de\s+sear': 'desear',
        r'le\s+che': 'leche',
        r'becham\s+el': 'bechamel',
        r'volum\s+en': 'volumen',
        r'peque\s+o': 'pequeño',
        r'coraz\s+n': 'corazón',
        r'p\s+la\s+los': 'pélalos',
        r'p\s+rtelos': 'pártelos',
        r'lo\s+nchitas': 'lonchitas',
        r'en\s+saladil\s+la': 'ensaladilla',
        r'm\s+zcla\s+lo': 'mézclalo',
        r'continuaci\s+n': 'continuación',
        r'peque\s+a': 'pequeña',
        r'te\s+mplado': 'templado',
        r'espolvor\s+alos': 'espolvoréalos',
        r'espolvor\s+alo': 'espolvoréalo',
        r'r\s+mpelas': 'rómpelas',
        r't\s+ngas': 'tengas',
        r'de\s+smigado': 'desmigado',
        r'de\s+shuesar': 'deshuesar',
        r'de\s+scorazona': 'descorazona',
        r'A\s+continuación': 'A continuación',
        r'a\s+continuación': 'a continuación',
        r'le\s+nto': 'lento',
        r'chi\s+no': 'chino',
        r'pasapur\s+': 'pasapuré',
        r'velout\s+': 'velouté',
        r'txakol\s+': 'txakolí',
        r'lim\s+n': 'limón',
        r'az\s+car': 'azúcar',
        r'gratin\s+': 'gratiné',
        r'horn\s+e': 'hornee',
        r'h\s+galo': 'hágalo',
        r'd\s+jalo': 'déjalo'
    }

    def clean_text(text):
        if not isinstance(text, str):
            return text
        for pattern, repl in replacements.items():
            text = re.sub(pattern, repl, text, flags=re.IGNORECASE)
        # Fix spaces in words like "A l u b i a s"
        text = re.sub(r'(\b\w)\s+(\w\b)', r'\1\2', text) # Simple fix for single letters with spaces
        return text

    for r in recipes:
        # Clean fields
        r['title'] = clean_text(r['title'])
        r['ingredients'] = [clean_text(i) for i in r['ingredients']]
        r['instructions'] = [clean_text(ins) for i in r['instructions']]

        # Analyze tags
        tags = set()
        all_text = (r['title'] + ' ' + ' '.join(r['ingredients'])).lower()

        # Rule Sana
        if any(w in all_text for w in ['verdura', 'legumbre', 'pescado', 'pollo', 'pavo', 'vapor', 'plancha', 'hervido']):
            if 'aceite' in all_text and 'abundante' not in all_text:
                tags.add('Sana')
            elif 'aceite' not in all_text:
                tags.add('Sana')

        # Rule Músculo / Proteínas
        if any(w in all_text for w in ['pollo', 'pavo', 'ternera', 'vaca', 'huevo', 'pescado', 'atun', 'bonito', 'lomo', 'legumbre', 'garbanzo', 'lenteja', 'alubia']):
            tags.add('Músculo')
            tags.add('Proteínas')

        # Rule Diabéticos
        if not any(w in all_text for w in ['azúcar', 'miel', 'mermelada', 'dulce', 'caramelo']):
            # Check for excessive flour - simple heuristic
            if 'harina' not in all_text or 'cucharada' in all_text:
                tags.add('Diabéticos')

        # Rule Perder peso
        if any(w in all_text for w in ['ensalada', 'vapor', 'plancha', 'ligera', 'bajo en grasa']) or r['category'] in ['ENSALADAS', 'VERDURAS']:
            if not any(w in all_text for w in ['frito', 'rebozado', 'bechamel', 'nata', 'tocino', 'chorizo', 'azúcar']):
                tags.add('Perder peso')

        # Rule Carbohidratos
        if any(w in all_text for w in ['pasta', 'arroz', 'patata', 'macarron', 'tallarin', 'espagueti', 'fideo']) or r['category'] in ['PASTAS', 'ARROCES']:
            tags.add('Carbohidratos')

        # Rule Fibra
        if any(w in all_text for w in ['espinaca', 'acelga', 'lechuga', 'berza', 'coliflor', 'brocoli', 'judia verde', 'fruta', 'manzana', 'pera', 'naranja']):
            tags.add('Fibra')
        if r['category'] in ['VERDURAS', 'LEGUMBRES', 'ENSALADAS']:
            tags.add('Fibra')

        r['healthTags'] = sorted(list(tags))

    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(recipes, f, ensure_ascii=False, indent=4)

if __name__ == "__main__":
    enrich_recipes()
