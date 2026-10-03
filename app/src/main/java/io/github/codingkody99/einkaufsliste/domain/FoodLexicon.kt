package io.github.codingkody99.einkaufsliste.domain

import io.github.codingkody99.einkaufsliste.data.Category

/**
 * German grocery vocabulary mapped to supermarket sections.
 *
 * Keywords are written the way people type them and normalized on build, so
 * both "Käse" and "kaese" resolve. Common plurals are listed explicitly where
 * the suffix rules in [TextNormalizer] would not reach them.
 *
 * Entries win over the compound and substring heuristics in
 * [CategoryClassifier], so anything the heuristics would misfile belongs here
 * verbatim — "Erdnussbutter" is a pantry item, not dairy, despite ending in
 * "butter".
 */
object FoodLexicon {

    internal val declarations: List<Pair<Category, List<String>>> = listOf(
        Category.OBST_GEMUESE to listOf(
            // Obst
            "Obst", "Apfel", "Äpfel", "Birne", "Birnen", "Banane", "Bananen",
            "Orange", "Orangen", "Mandarine", "Mandarinen", "Clementine", "Clementinen",
            "Zitrone", "Zitronen", "Limette", "Limetten", "Grapefruit", "Pampelmuse",
            "Traube", "Trauben", "Weintrauben", "Erdbeere", "Erdbeeren",
            "Himbeere", "Himbeeren", "Blaubeere", "Blaubeeren", "Heidelbeere", "Heidelbeeren",
            "Brombeere", "Brombeeren", "Johannisbeere", "Johannisbeeren", "Stachelbeere",
            "Kirsche", "Kirschen", "Pflaume", "Pflaumen", "Zwetschge", "Zwetschgen",
            "Aprikose", "Aprikosen", "Pfirsich", "Pfirsiche", "Nektarine", "Nektarinen",
            "Melone", "Wassermelone", "Honigmelone", "Galiamelone", "Ananas",
            "Mango", "Papaya", "Kiwi", "Kiwis", "Avocado", "Avocados",
            "Feige", "Feigen", "Granatapfel", "Rhabarber", "Kaki", "Litschi",
            "Maracuja", "Passionsfrucht", "Beeren", "Beerenmischung",
            // Gemüse
            "Gemüse", "Tomate", "Tomaten", "Cherrytomaten", "Kirschtomaten", "Strauchtomaten",
            "Gurke", "Gurken", "Salatgurke", "Paprika", "Spitzpaprika",
            "Zucchini", "Aubergine", "Auberginen",
            "Karotte", "Karotten", "Möhre", "Möhren", "Wurzeln",
            "Kartoffel", "Kartoffeln", "Süßkartoffel", "Süßkartoffeln",
            "Zwiebel", "Zwiebeln", "Frühlingszwiebel", "Frühlingszwiebeln",
            "Schalotte", "Schalotten", "Knoblauch", "Lauch", "Porree",
            "Sellerie", "Staudensellerie", "Knollensellerie", "Fenchel", "Kohlrabi",
            "Brokkoli", "Broccoli", "Blumenkohl", "Romanesco", "Rosenkohl",
            "Weißkohl", "Rotkohl", "Spitzkohl", "Wirsing", "Grünkohl", "Chinakohl", "Pak Choi",
            "Spinat", "Mangold", "Salat", "Kopfsalat", "Eisbergsalat", "Eisberg",
            "Feldsalat", "Rucola", "Rauke", "Endivien", "Radicchio", "Romanasalat", "Batavia",
            "Radieschen", "Rettich", "Rote Bete", "Rote Beete", "Pastinake", "Pastinaken",
            "Petersilienwurzel", "Steckrübe", "Kürbis", "Hokkaido", "Butternut",
            "Mais", "Zuckermais", "Erbsen", "Zuckerschoten", "Bohnen", "Grüne Bohnen",
            "Brechbohnen", "Prinzessbohnen", "Spargel", "Artischocke", "Artischocken",
            "Champignon", "Champignons", "Pilz", "Pilze", "Kräuterseitling", "Austernpilze",
            "Shiitake", "Pfifferlinge", "Steinpilze",
            "Ingwer", "Kurkumawurzel", "Chili", "Chilischote", "Chilischoten", "Peperoni",
            "Kresse", "Sprossen", "Keimlinge", "Alfalfa", "Topinambur", "Zitronengras",
            // Frische Kräuter
            "Kräuter", "Petersilie", "Schnittlauch", "Basilikum", "Dill", "Koriander",
            "Minze", "Pfefferminze", "Rosmarin", "Thymian", "Oregano", "Salbei",
            "Estragon", "Kerbel", "Liebstöckel", "Bärlauch",
        ),

        Category.BACKWAREN to listOf(
            "Brot", "Brote", "Vollkornbrot", "Mischbrot", "Roggenbrot", "Dinkelbrot",
            "Sauerteigbrot", "Bauernbrot", "Toastbrot", "Toast", "Pumpernickel",
            "Brötchen", "Semmel", "Semmeln", "Schrippe", "Schrippen", "Kaiserbrötchen",
            "Mehrkornbrötchen", "Laugenbrötchen", "Baguette", "Ciabatta", "Focaccia",
            "Fladenbrot", "Pita", "Pitabrot", "Naan", "Tortilla", "Tortillas", "Wrap", "Wraps",
            "Burgerbrötchen", "Burger Buns", "Hotdogbrötchen", "Bagel", "Bagels",
            "Brezel", "Brezeln", "Laugenbrezel", "Laugenstange",
            "Croissant", "Croissants", "Hörnchen", "Franzbrötchen", "Zimtschnecke",
            "Kuchen", "Torte", "Blechkuchen", "Streuselkuchen", "Hefezopf",
            "Muffin", "Muffins", "Donut", "Donuts", "Berliner", "Krapfen",
            "Knäckebrot", "Zwieback", "Reiswaffeln",
        ),

        Category.MOLKEREI to listOf(
            "Milch", "Vollmilch", "Frischmilch", "Haltbare Milch", "H-Milch", "Halbfettmilch",
            "Hafermilch", "Haferdrink", "Sojamilch", "Sojadrink", "Mandelmilch", "Mandeldrink",
            "Sahne", "Schlagsahne", "Süße Sahne", "Saure Sahne", "Schmand",
            "Crème fraîche", "Creme fraiche", "Kochsahne",
            "Quark", "Magerquark", "Speisequark", "Kräuterquark",
            "Joghurt", "Jogurt", "Naturjoghurt", "Griechischer Joghurt", "Skyr",
            "Kefir", "Buttermilch", "Ayran",
            "Butter", "Margarine", "Streichfett",
            "Käse", "Gouda", "Edamer", "Emmentaler", "Bergkäse", "Cheddar",
            "Parmesan", "Pecorino", "Grana Padano", "Mozzarella", "Burrata",
            "Feta", "Hirtenkäse", "Schafskäse", "Ziegenkäse",
            "Camembert", "Brie", "Gorgonzola", "Blauschimmelkäse", "Roquefort",
            "Ricotta", "Mascarpone", "Hüttenkäse", "Frischkäse", "Streichkäse",
            "Schmelzkäse", "Raclettekäse", "Halloumi", "Reibekäse", "Käsescheiben",
            "Ei", "Eier", "Freilandeier", "Bioeier",
            "Pudding", "Grießpudding", "Milchreis Dessert", "Dessert", "Mousse",
            "Tofu", "Tempeh", "Seitan", "Räuchertofu",
            "Hummus", "Tzatziki", "Zaziki", "Kräuterbutter", "Halloumi Grillkäse",
            "Pizzateig", "Blätterteig", "Mürbeteig", "Hefeteig", "Filoteig", "Strudelteig",
            "Tortellini", "Frische Pasta", "Gnocchi frisch",
        ),

        Category.FLEISCH_FISCH to listOf(
            "Fleisch", "Hackfleisch", "Hack", "Rinderhack", "Rinderhackfleisch",
            "Gemischtes Hack", "Mett",
            "Rindfleisch", "Rind", "Steak", "Steaks", "Rumpsteak", "Entrecôte",
            "Filet", "Rinderfilet", "Gulasch", "Rouladen", "Tafelspitz",
            "Schnitzel", "Schweineschnitzel", "Putenschnitzel", "Kotelett", "Koteletts",
            "Schweinefleisch", "Schwein", "Kasseler", "Nackensteak", "Bauchspeck",
            "Speck", "Bacon", "Pancetta",
            "Schinken", "Kochschinken", "Rohschinken", "Serranoschinken", "Parmaschinken",
            "Salami", "Wurst", "Würstchen", "Bratwurst", "Bratwürste", "Wiener",
            "Frankfurter", "Bockwurst", "Currywurst", "Leberwurst", "Teewurst",
            "Mortadella", "Lyoner", "Fleischwurst", "Mettwurst", "Aufschnitt", "Wurstaufschnitt",
            "Geflügel", "Huhn", "Hähnchen", "Hühnchen", "Hähnchenbrust", "Hühnerbrust",
            "Hähnchenschenkel", "Hähnchenkeule", "Pute", "Putenbrust",
            "Ente", "Entenbrust", "Gans", "Lamm", "Lammkeule", "Lammfilet",
            "Kalb", "Kalbfleisch", "Hirsch", "Wildschwein",
            "Fisch", "Lachs", "Lachsfilet", "Räucherlachs", "Graved Lachs",
            "Thunfisch", "Forelle", "Kabeljau", "Seelachs", "Dorade", "Wolfsbarsch",
            "Zander", "Hering", "Matjes", "Makrele", "Sardinen", "Scholle",
            "Rotbarsch", "Pangasius", "Tilapia", "Garnelen", "Shrimps", "Scampi",
            "Muscheln", "Miesmuscheln", "Tintenfisch", "Calamari", "Krabben", "Surimi",
        ),

        Category.TIEFKUEHL to listOf(
            "Tiefkühlpizza", "Pizza", "Pizzen", "Flammkuchen",
            "Fischstäbchen", "Backfisch", "Chicken Nuggets", "Nuggets",
            "Pommes", "Pommes frites", "Kroketten", "Rösti", "Kartoffelpuffer",
            "Wedges", "Kartoffelecken",
            "Eis", "Speiseeis", "Eiscreme", "Eiswürfel", "Magnum", "Eis am Stiel",
            "Tiefkühlgemüse", "Wokgemüse", "Gemüsemischung", "Erbsen tiefgekühlt",
            "Rahmspinat", "Blattspinat", "Tiefkühlbeeren", "Tiefkühlhimbeeren",
            "Tiefkühlkräuter", "Blätterteig tiefgekühlt", "Aufbackbrötchen",
        ),

        Category.VORRAT to listOf(
            // Teigwaren, Reis, Getreide
            "Nudeln", "Nudel", "Pasta", "Spaghetti", "Penne", "Fusilli", "Farfalle",
            "Tagliatelle", "Linguine", "Makkaroni", "Lasagneplatten", "Lasagne",
            "Reis", "Basmatireis", "Jasminreis", "Risottoreis", "Milchreis", "Wildreis",
            "Reispapier", "Reisnudeln", "Glasnudeln", "Mienudeln", "Udonnudeln",
            "Couscous", "Bulgur", "Quinoa", "Hirse", "Polenta", "Grieß",
            "Haferflocken", "Müsli", "Cornflakes", "Cerealien", "Porridge", "Granola",
            "Spätzle", "Knödel", "Semmelknödel", "Kartoffelpüree", "Gnocchi",
            // Backen
            "Mehl", "Weizenmehl", "Dinkelmehl", "Vollkornmehl", "Roggenmehl",
            "Speisestärke", "Stärke", "Backpulver", "Hefe", "Trockenhefe", "Natron",
            "Hefeflocken", "Backmischung",
            "Zucker", "Puderzucker", "Brauner Zucker", "Vanillezucker", "Vanille",
            "Vanilleextrakt", "Vanilleschote", "Backkakao", "Kakao", "Kakaopulver",
            "Schokodrops", "Kuvertüre", "Gelatine", "Puddingpulver", "Tortenguss",
            "Zuckerstreusel", "Marzipan", "Backoblaten", "Paniermehl", "Semmelbrösel",
            // Gewürze
            "Salz", "Meersalz", "Pfeffer", "Paprikapulver", "Curry", "Currypulver",
            "Kreuzkümmel", "Kumin", "Zimt", "Muskat", "Lorbeerblätter", "Nelken",
            "Kardamom", "Anis", "Fenchelsamen", "Senfkörner", "Chiliflocken",
            "Gewürze", "Kräuter der Provence", "Italienische Kräuter", "Kurkuma",
            "Knoblauchpulver", "Zwiebelpulver", "Chilipulver",
            // Brühe, Öl, Essig, Saucen
            "Brühe", "Gemüsebrühe", "Hühnerbrühe", "Rinderbrühe", "Fond", "Bouillon",
            "Öl", "Olivenöl", "Sonnenblumenöl", "Rapsöl", "Sesamöl", "Kokosöl", "Distelöl",
            "Essig", "Balsamico", "Weißweinessig", "Apfelessig", "Aceto",
            "Senf", "Ketchup", "Mayonnaise", "Mayo", "Remoulade", "Aioli",
            "Sojasauce", "Sojasoße", "Worcestersauce", "Tabasco", "Sriracha", "Harissa",
            "Pesto", "Pizzasauce", "Salatdressing", "Dressing", "Grillsauce", "Bratensoße",
            // Konserven und Gläser
            "Tomatenmark", "Passierte Tomaten", "Tomatensauce", "Dosentomaten",
            "Gehackte Tomaten", "Passata", "Tomatenpassata",
            "Kokosmilch", "Kichererbsen", "Linsen", "Rote Linsen",
            "Kidneybohnen", "Weiße Bohnen", "Baked Beans", "Mais Dose",
            "Dosenthunfisch", "Thunfisch Dose", "Dosensuppe", "Suppe", "Fertiggericht",
            "Instantnudeln", "Oliven", "Kapern", "Gewürzgurken", "Essiggurken",
            "Silberzwiebeln", "Sauerkraut", "Apfelmus", "Rotkohl Glas",
            "Aufstrich", "Brotaufstrich", "Schokoaufstrich",
            "Marmelade", "Konfitüre", "Gelee", "Honig", "Ahornsirup", "Agavendicksaft",
            "Sirup", "Nussnougatcreme", "Nutella", "Erdnussbutter", "Mandelmus",
            "Nussmus", "Tahini",
            // Nüsse, Kerne, Trockenfrüchte
            "Erdnüsse", "Mandeln", "Walnüsse", "Haselnüsse", "Cashewkerne", "Cashews",
            "Pistazien", "Pinienkerne", "Sonnenblumenkerne", "Kürbiskerne", "Sesam",
            "Leinsamen", "Chiasamen", "Rosinen", "Trockenfrüchte", "Trockenobst",
            "Getrocknete Tomaten", "Datteln",
            // Heißgetränke
            "Kaffee", "Kaffeebohnen", "Filterkaffee", "Espresso", "Kaffeepads",
            "Kaffeekapseln", "Instantkaffee", "Tee", "Schwarztee", "Grüntee",
            "Kräutertee", "Früchtetee", "Kamillentee", "Pfefferminztee", "Rooibos",
        ),

        Category.SUESS_SNACKS to listOf(
            "Schokolade", "Schoko", "Vollmilchschokolade", "Zartbitterschokolade",
            "Nussschokolade", "Pralinen", "Schokoriegel", "Riegel", "Müsliriegel",
            "Kekse", "Plätzchen", "Butterkekse", "Waffeln", "Lebkuchen", "Spekulatius",
            "Bonbons", "Gummibärchen", "Fruchtgummi", "Weingummi", "Lakritz",
            "Lutscher", "Kaugummi", "Schokoküsse", "Marshmallows", "Eiskonfekt",
            "Chips", "Kartoffelchips", "Tortillachips", "Nachos", "Salzstangen",
            "Cracker", "Erdnussflips", "Flips", "Popcorn", "Studentenfutter",
            "Nussmischung", "Salzbrezeln", "Süßigkeiten", "Naschzeug",
        ),

        Category.GETRAENKE to listOf(
            "Wasser", "Mineralwasser", "Sprudel", "Sprudelwasser", "Stilles Wasser",
            "Saft", "Apfelsaft", "Orangensaft", "Multivitaminsaft", "Traubensaft",
            "Tomatensaft", "Kirschsaft", "Johannisbeersaft", "Direktsaft",
            "Schorle", "Apfelschorle", "Limonade", "Limo", "Cola", "Fanta", "Sprite",
            "Spezi", "Eistee", "Energydrink", "Tonic", "Tonic Water", "Bitter Lemon",
            "Ginger Ale", "Trinkschokolade", "Kakaogetränk", "Smoothie", "Kombucha",
            "Bier", "Pils", "Weizenbier", "Weizen", "Helles", "Radler", "Alkoholfreies Bier",
            "Wein", "Rotwein", "Weißwein", "Roséwein", "Rosé", "Sekt", "Prosecco",
            "Champagner", "Crémant", "Schnaps", "Wodka", "Vodka", "Gin", "Rum",
            "Whisky", "Likör", "Aperol", "Campari", "Martini", "Wermut", "Tequila",
            "Cidre", "Most", "Federweißer",
        ),

        Category.HAUSHALT to listOf(
            "Toilettenpapier", "Klopapier", "Küchenrolle", "Küchenpapier",
            "Taschentücher", "Papiertaschentücher", "Servietten",
            "Müllbeutel", "Mülltüten", "Müllsäcke", "Gefrierbeutel",
            "Frischhaltefolie", "Alufolie", "Backpapier", "Butterbrotpapier",
            "Spülmittel", "Geschirrspülmittel", "Spülmaschinentabs", "Spülmaschinensalz",
            "Klarspüler", "Waschmittel", "Weichspüler", "Fleckenentferner",
            "Allzweckreiniger", "Badreiniger", "Glasreiniger", "WC-Reiniger",
            "Entkalker", "Essigreiniger", "Scheuermilch", "Putzlappen", "Schwämme",
            "Spülschwamm", "Spülbürste", "Mikrofasertuch", "Staubtücher", "Gummihandschuhe",
            "Zahnpasta", "Zahnbürste", "Zahnseide", "Mundwasser", "Zahnreinigungstabletten",
            "Duschgel", "Shampoo", "Conditioner", "Haarspülung", "Seife", "Handseife",
            "Flüssigseife", "Deo", "Deodorant", "Rasierer", "Rasierschaum", "Rasierklingen",
            "Handcreme", "Bodylotion", "Sonnencreme", "Lippenpflege",
            "Wattestäbchen", "Wattepads", "Abschminktücher", "Damenbinden", "Tampons",
            "Windeln", "Feuchttücher", "Pflaster", "Desinfektionsmittel",
            "Batterien", "Glühbirne", "Kerzen", "Teelichter", "Streichhölzer", "Feuerzeug",
            "Blumenerde", "Katzenfutter", "Hundefutter", "Katzenstreu", "Tierfutter",
            "Strohhalme", "Zahnstocher", "Grillkohle", "Grillanzünder", "Geschenkpapier",
        ),
    )

    /** Normalized keyword to category. Built once; keys are unique by test. */
    val byKeyword: Map<String, Category> = buildMap {
        declarations.forEach { (category, keywords) ->
            keywords.forEach { put(TextNormalizer.normalize(it), category) }
        }
    }

    /** Keys ordered longest first, so compound matching prefers the specific one. */
    internal val keysByLengthDesc: List<String> =
        byKeyword.keys.sortedByDescending { it.length }
}
