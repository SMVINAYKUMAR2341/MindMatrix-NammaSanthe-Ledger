package com.nammasanthe.ledger.ocr

/**
 * Multilingual grocery dictionary used for normalising OCR item names
 * to a canonical English name.
 *
 * Lookup priority:
 *   1. Exact match (lowercase)
 *   2. Prefix / substring match (≥ 3 chars)
 *   3. Levenshtein fuzzy match (distance ≤ 2, word length ≥ 4)
 *
 * Covers:
 *   • Kannada (Unicode ಕನ್ನಡ + transliterated)
 *   • Hindi (Devanagari + transliterated)
 *   • English variants, abbreviations, common OCR typos
 */
object GroceryDictionary {

    // ── canonical → aliases ──────────────────────────────────────────────────
    private val CATALOG: Map<String, List<String>> = mapOf(

        // ── Vegetables ──────────────────────────────────────────────────────
        "onion"         to listOf("ಈರುಳ್ಳಿ","eerulli","eerulli","ईरुळी","प्याज","pyaz","pyaaj","piaz",
                                   "onian","onoin","ulli","ullii","onyan"),
        "tomato"        to listOf("ಟೊಮಾಟೊ","tomato","टमाटर","tamatar","tomatoe","tomoto","tameta","t0mat0"),
        "potato"        to listOf("ಆಲುಗಡ್ಡೆ","ಆಲೂಗಡ್ಡೆ","alugadde","aloogadde","आलू","aloo","alu","aalu",
                                   "poteto","potat0","batata"),
        "brinjal"       to listOf("ಬದನೆಕಾಯಿ","ಬ್ರಿಂಜಾಲ್","badanekayi","बैंगन","baingan","baigan",
                                   "eggplant","brinjel","begun"),
        "carrot"        to listOf("ಕ್ಯಾರಟ್","kyarat","गाजर","gajar","gaajar","carat","carrt","gajor"),
        "beans"         to listOf("ಅವರೆಕಾಯಿ","avarekayi","बीन्स","beans","bens","sem","seim"),
        "spinach"       to listOf("ಪಾಲಕ","palak","पालक","spinch","palakku","saag"),
        "cabbage"       to listOf("ಎಲೆಕೋಸು","elekkosu","ಕ್ಯಾಬೇಜ್","पत्ता गोभी","patta gobhi",
                                   "kabbaj","kabbge","bandh gobhi"),
        "cauliflower"   to listOf("ಹೂಕೋಸು","hoo kosu","ಹೂ ಕೋಸು","फूल गोभी","phool gobhi",
                                   "cauliflwr","phulgobi","gobi"),
        "green chili"   to listOf("ಹಸಿಮೆಣಸು","ಪಚ್ಚೆಮೆಣಸು","hasi menasu","हरी मिर्च","hari mirch",
                                   "green chilli","greenchilli","hari mirchi","harimirch"),
        "bitter gourd"  to listOf("ಹಾಗಲಕಾಯಿ","hagalakayi","करेला","karela","karella","bitter gurd"),
        "ridge gourd"   to listOf("ಹೀರೆಕಾಯಿ","heerekayi","तोरी","tori","torai","toori"),
        "snake gourd"   to listOf("ಪಡವಲಕಾಯಿ","padavalkayi","चिचिंडा","padwal"),
        "drumstick"     to listOf("ಮುನಗ","munaga","ಮೊರಿಂಗ","मुनगा","munagakkai","drum stick"),
        "pumpkin"       to listOf("ಕುಂಬಳಕಾಯಿ","kumbalakayi","कद्दू","kaddu","kadu","kumbalkayi"),
        "cucumber"      to listOf("ಸೌತೆಕಾಯಿ","southekayi","खीरा","kheera","kakdi","cucmber","khira"),
        "ash gourd"     to listOf("ಬೂದುಗುಂಬಳ","boodugunbala","पेठा","petha","boodugumbala"),
        "lady finger"   to listOf("ಬೆಂಡೆಕಾಯಿ","bendekayi","भिंडी","bhindi","bhendi","okra"),
        "capsicum"      to listOf("ಸಿಮ್ಲಾ ಮೆಣಸು","shimla menasu","शिमला मिर्च","shimla mirch",
                                   "bell pepper","capsicm"),
        "raw banana"    to listOf("ಬಾಳೆಕಾಯಿ","balekaayi","कच्चा केला","kacha kela"),
        "raw mango"     to listOf("ಹಸಿ ಮಾವು","hasi mavu","कच्चा आम","kacha aam"),
        "cluster beans" to listOf("ಗೋರಿಕಾಯಿ","gorikayi","गवार","gavar","gawar"),
        "beetroot"      to listOf("ಬೀಟ್‍ರೂಟ್","beetroot","चुकंदर","chukandar","beet"),

        // ── Fruits ──────────────────────────────────────────────────────────
        "banana"        to listOf("ಬಾಳೆ","ಬಾಳೆಹಣ್ಣು","bale","baale","केला","kela","banan","bananna","kele"),
        "mango"         to listOf("ಮಾವಿನಹಣ್ಣು","maavina hannu","आम","aam","mang0","amm","mamidipandu"),
        "apple"         to listOf("ಸೇಬು","sebu","सेब","seb","appl","aple","apple"),
        "grapes"        to listOf("ದ್ರಾಕ್ಷಿ","ಅಂಗೂರು","anguru","अंगूर","angoor","graapes","drakshe"),
        "pomegranate"   to listOf("ದಾಳಿಂಬೆ","daalimbe","अनार","anar","dalimb","pomegranat"),
        "guava"         to listOf("ಸೀಬೆಕಾಯಿ","sibekayi","अमरूद","amrood","gava","peru"),
        "papaya"        to listOf("ಪಪ್ಪಾಯ","pappayi","पपीता","papita","papay","papita"),
        "orange"        to listOf("ಕಿತ್ತಳೆ","kittale","संतरा","santra","mosambi","orang"),
        "coconut"       to listOf("ತೆಂಗಿನಕಾಯಿ","tengina kayi","नारियल","nariyal","coconut","cocnut","narikel"),
        "watermelon"    to listOf("ಕಲ್ಲಂಗಡಿ","kallangadi","तरबूज","tarbuj","tarbooz"),
        "pineapple"     to listOf("ಅನಾನಸ","ananas","अनानास","ananaas","pineaple","ananas"),
        "jackfruit"     to listOf("ಹಲಸಿನ ಹಣ್ಣು","halasina hannu","कटहल","kathal","halasina"),

        // ── Dairy ────────────────────────────────────────────────────────────
        "milk"          to listOf("ಹಾಲು","haalu","दूध","doodh","dood","mlk","milik","haalu"),
        "curd"          to listOf("ಮೊಸರು","mosaru","दही","dahi","curd","curds","crd","mosru"),
        "butter"        to listOf("ಬೆಣ್ಣೆ","benne","मक्खन","makkhan","buter","bttr","benne"),
        "ghee"          to listOf("ತುಪ್ಪ","tuppa","घी","tuppa","ghi"),
        "paneer"        to listOf("ಪನೀರ್","paneer","पनीर","panir","chenna","chhena"),
        "cheese"        to listOf("ಚೀಸ್","chees","पनीर","cheese","chese"),

        // ── Oils & Fats ──────────────────────────────────────────────────────
        "oil"           to listOf("ಎಣ್ಣೆ","enne","तेल","tel","oill"),
        "coconut oil"   to listOf("ತೆಂಗಿನ ಎಣ್ಣೆ","tengina enne","नारियल तेल","nariyal tel","coconut ol"),
        "groundnut oil" to listOf("ಶೇಂಗಾ ಎಣ್ಣೆ","shenga enne","मूंगफली तेल","moongfali tel"),
        "mustard oil"   to listOf("ಸಾಸಿವೆ ಎಣ್ಣೆ","sasive enne","सरसों का तेल","sarson ka tel","sarson oil"),
        "sunflower oil" to listOf("ಸೂರ್ಯಕಾಂತಿ ಎಣ್ಣೆ","suryakanti enne","सूरजमुखी तेल","surajmukhi tel"),
        "palm oil"      to listOf("ತಾಳೆ ಎಣ್ಣೆ","taale enne","पाम तेल","palm tel"),
        "refined oil"   to listOf("ರಿಫೈಂಡ್ ಎಣ್ಣೆ","refined enne","रिफाइंड तेल","refined tel"),
        "vanaspati"     to listOf("ವನಸ್ಪತಿ","vanaspathi","वनस्पति","dalda"),

        // ── Grains & Flours ──────────────────────────────────────────────────
        "rice"          to listOf("ಅಕ್ಕಿ","akki","चावल","chawal","chaval","rce","ricc","biyam"),
        "wheat"         to listOf("ಗೋಧಿ","godhi","गेहूं","gehun","gehoon","weat","godhum"),
        "flour"         to listOf("ಹಿಟ್ಟು","hittu","आटा","atta","ata","floor","flor","maida"),
        "wheat flour"   to listOf("ಗೋಧಿ ಹಿಟ್ಟು","godhi hittu","गेहूं आटा","gehun atta","wheat flr","atta"),
        "rice flour"    to listOf("ಅಕ್ಕಿ ಹಿಟ್ಟು","akki hittu","चावल आटा","chawal atta","rice flr"),
        "semolina"      to listOf("ರವೆ","rave","سوجی","sooji","suji","rava","ravva","semolna"),
        "corn flour"    to listOf("ಜೋಳದ ಹಿಟ್ಟು","jolada hittu","मकई आटा","makkai atta","makki atta"),

        // ── Pulses / Dal ─────────────────────────────────────────────────────
        "dal"           to listOf("ಬೇಳೆ","bele","दाल","dal","dhal","daal","parippu"),
        "toor dal"      to listOf("ತೊಗರಿಬೇಳೆ","togari bele","तुअर दाल","tuvar dal","arhar dal","togri"),
        "chana dal"     to listOf("ಕಡಲೆಬೇಳೆ","kadle bele","चना दाल","chana dal","bengal gram"),
        "urad dal"      to listOf("ಉದ್ದಿನ ಬೇಳೆ","uddina bele","उड़द दाल","urad daal","black gram"),
        "moong dal"     to listOf("ಹೆಸರುಬೇಳೆ","hesaru bele","मूंग दाल","moong daal","mung dal","hesru"),
        "masoor dal"    to listOf("ಕೆಂಪು ಬೇಳೆ","kempu bele","मसूर दाल","masoor daal","red lentil"),
        "chickpea"      to listOf("ಕಡಲೆ","kadale","चना","chana","chole","kabuli chana"),
        "rajma"         to listOf("ರಾಜ್‍ಮಾ","rajma","राजमा","kidney beans","rajmah"),

        // ── Spices ───────────────────────────────────────────────────────────
        "turmeric"      to listOf("ಅರಿಶಿನ","arishina","हल्दी","haldi","turmric","haladi","haridra"),
        "chili powder"  to listOf("ಮೆಣಸಿನ ಪುಡಿ","menasina pudi","लाल मिर्च","lal mirch",
                                   "mirchi","red chili","chilli powder","mirch"),
        "coriander"     to listOf("ಕೊತ್ತಂಬರಿ","kottambari","धनिया","dhaniya","dhania","corinder",
                                   "dhaniya pudi","coriander seeds"),
        "cumin"         to listOf("ಜೀರಿಗೆ","jeerige","जीरा","jeera","jira","zeera","jeere"),
        "mustard"       to listOf("ಸಾಸಿವೆ","sasive","सरसों","sarson","rai","rye","sasve"),
        "pepper"        to listOf("ಮೆಣಸು","menasu","काली मिर्च","kali mirch","black pepper","kaaree menasu"),
        "fenugreek"     to listOf("ಮೆಂತ್ಯ","mentya","मेथी","methi","methee","menthi"),
        "cardamom"      to listOf("ಏಲಕ್ಕಿ","elakki","इलायची","elaichi","cardmom","elachi"),
        "cloves"        to listOf("ಲವಂಗ","lavanga","लौंग","laung","long","lavng"),
        "cinnamon"      to listOf("ದಾಲ್ಚಿನ್ನಿ","dalchini","दालचीनी","dalchini","cinnamon"),
        "ginger"        to listOf("ಶುಂಠಿ","shunthi","अदरक","adrak","adarak","adrk","shunti"),
        "garlic"        to listOf("ಬೆಳ್ಳುಳ್ಳಿ","bellulli","लहसुन","lahsun","lasun","garlik","bellulli"),
        "tamarind"      to listOf("ಹುಣಸೆ","hunase","इमली","imli","imlee","tamrind","puli","hunasina"),
        "jaggery"       to listOf("ಬೆಲ್ಲ","bella","गुड़","gur","gud","bela","jaggeri","vellam"),
        "asafoetida"    to listOf("ಇಂಗು","ingu","हींग","heeng","hing"),

        // ── Sweeteners & Sugar ───────────────────────────────────────────────
        "sugar"         to listOf("ಸಕ್ಕರೆ","sakkare","चीनी","cheeni","shakkar","sugr","suger","chini"),
        "brown sugar"   to listOf("ಕಂದು ಸಕ್ಕರೆ","kandu sakkare","ब्राउन शुगर","brown shugr"),
        "honey"         to listOf("ಜೇನುತುಪ್ಪ","jeenu tuppa","शहद","shahad","shahed","hunny"),

        // ── Salt ─────────────────────────────────────────────────────────────
        "salt"          to listOf("ಉಪ್ಪು","uppu","नमक","namak","saltt","upppu"),
        "rock salt"     to listOf("ಸೈಂಧವ ಉಪ್ಪು","saindhava uppu","सेंधा नमक","sendha namak"),

        // ── Beverages ────────────────────────────────────────────────────────
        "tea"           to listOf("ಚಹಾ","ಟೀ","chaha","टी","चाय","chai","cha","tee","ti"),
        "coffee"        to listOf("ಕಾಫಿ","kaafi","कॉफ़ी","coffee","cofee","cofe"),

        // ── Misc Grocery ─────────────────────────────────────────────────────
        "egg"           to listOf("ಮೊಟ್ಟೆ","motte","अंडा","anda","eggs","eg","motti"),
        "bread"         to listOf("ಬ್ರೆಡ್","bred","डबलरोटी","bread","braed"),
        "biscuit"       to listOf("ಬಿಸ್ಕಟ್","biskat","बिस्कुट","biskut","biscut","biskit"),
        "papad"         to listOf("ಪಾಪಡ","papada","पापड़","papar","papd","apalam"),
        "vermicelli"    to listOf("ಶ್ಯಾವಿಗೆ","shavige","सेवइयां","sevaiya","seviyan","shavge"),
        "noodles"       to listOf("ನೂಡಲ್ಸ್","noodals","नूडल्स","noodle","nudels"),
        "semiya"        to listOf("ಶ್ಯಾವಿಗೆ","shavige","सेवइयां","sevaiya","semya"),
        "groundnut"     to listOf("ಶೇಂಗಾ","shenga","मूंगफली","moongfali","kadlekayi","peanut","shennga"),

        // ── Household ────────────────────────────────────────────────────────
        "soap"          to listOf("ಸಾಬೂನು","saboonooo","साबुन","sabun","sop","sabu"),
        "shampoo"       to listOf("ಶ್ಯಾಂಪೂ","shyampu","शैम्पू","shampoo","sampu","champoo"),
        "detergent"     to listOf("ಡಿಟರ್ಜೆಂಟ್","dikali","डिटर्जेंट","detrgent","washing powder","washng pwdr"),
        "toothpaste"    to listOf("ಟೂತ್‌ಪೇಸ್ಟ್","toothpaste","टूथपेस्ट","toothpast","tootpest"),
    )

    // ── flattened alias → canonical (built once) ─────────────────────────────
    private val ALIAS_TO_CANONICAL: Map<String, String> by lazy {
        buildMap {
            for ((canonical, aliases) in CATALOG) {
                put(canonical.lowercase(), canonical)
                for (alias in aliases) put(alias.lowercase().trim(), canonical)
            }
        }
    }

    // ── sorted aliases longest-first for substring search ────────────────────
    private val SORTED_ALIASES: List<String> by lazy {
        ALIAS_TO_CANONICAL.keys.sortedByDescending { it.length }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Attempt to map [word] to a canonical English grocery name.
     * Returns null if no confident match is found.
     */
    fun normalize(word: String): String? {
        val key = word.lowercase().trim()
        if (key.length < 2) return null

        // 1 — exact
        ALIAS_TO_CANONICAL[key]?.let { return it }

        // 2 — prefix / contains (only for tokens ≥ 3 chars)
        if (key.length >= 3) {
            for (alias in SORTED_ALIASES) {
                if (alias.length < 3) continue
                if (alias.contains(key) || key.contains(alias)) {
                    return ALIAS_TO_CANONICAL[alias]
                }
            }
        }

        // 3 — Levenshtein fuzzy (word ≥ 4 chars, distance ≤ 2)
        if (key.length >= 4) {
            var bestDist = 3
            var bestCanonical: String? = null
            for (alias in SORTED_ALIASES) {
                if (alias.length < 3) continue
                if (kotlin.math.abs(alias.length - key.length) > 3) continue
                val d = levenshtein(key, alias)
                if (d < bestDist) { bestDist = d; bestCanonical = ALIAS_TO_CANONICAL[alias] }
            }
            bestCanonical?.let { return it }
        }

        return null
    }

    /**
     * Scan the whole [line] for any known item name (longest match wins).
     * Useful as a last-resort when word-level lookup fails.
     */
    fun findInLine(line: String): String? {
        val lower = line.lowercase()
        for (alias in SORTED_ALIASES) {
            if (alias.length < 3) continue
            if (lower.contains(alias)) return ALIAS_TO_CANONICAL[alias]
        }
        return null
    }

    // ── Levenshtein edit distance ─────────────────────────────────────────────
    private fun levenshtein(a: String, b: String): Int {
        if (a == b)      return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                curr[j] = if (a[i - 1] == b[j - 1]) prev[j - 1]
                           else 1 + minOf(prev[j], curr[j - 1], prev[j - 1])
            }
            prev.indices.forEach { prev[it] = curr[it] }
        }
        return curr[b.length]
    }
}
