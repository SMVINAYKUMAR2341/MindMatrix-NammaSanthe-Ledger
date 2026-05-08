package com.nammasanthe.ledger.translation

class KannadaOfflineTranslator {

    private val digitMap = mapOf(
        '೦' to '0', '೧' to '1', '೨' to '2', '೩' to '3', '೪' to '4',
        '೫' to '5', '೬' to '6', '೭' to '7', '೮' to '8', '೯' to '9'
    )

    private val unitReplacements = listOf(
        "ಕಿ.ಗ್" to "kg",
        "ಕಿಲೋ" to "kg",
        "ಲೀಟರ್" to "L",
        "ಮಿಲಿ" to "ml",
        "ಎಂಎಲ್" to "ml",
        "ರೂ" to "rs",
        "ರೂಪಾಯಿ" to "rs"
    )

    private val dictionary = listOf(
        "ಅಕ್ಕಿ" to "rice",
        "ಬಾಳೆ" to "banana",
        "ಬಾಳೆಹಣ್ಣು" to "banana",
        "ಬೆಣ್ಣೆ" to "butter",
        "ಬೇಳೆ" to "dal",
        "ಚಹಾ" to "tea",
        "ಟೀ" to "tea",
        "ಸಕ್ಕರೆ" to "sugar",
        "ಉಪ್ಪು" to "salt",
        "ಹಾಲು" to "milk",
        "ಮೊಸರು" to "curd",
        "ಹಿಟ್ಟು" to "flour",
        "ಗೋಧಿ" to "wheat",
        "ಅರಿಶಿನ" to "turmeric",
        "ಮೆಣಸು" to "pepper",
        "ಜೀರಿಗೆ" to "cumin",
        "ಎಣ್ಣೆ" to "oil",
        "ತೆಂಗಿನಎಣ್ಣೆ" to "coconut oil",
        "ಪಚ್ಚೆಮೆಣಸು" to "green chili",
        "ಈರುಳ್ಳಿ" to "onion",
        "ಟೊಮಾಟೊ" to "tomato",
        "ಆಲುಗಡ್ಡೆ" to "potato",
        "ಸೊಪ್ಪು" to "greens",
        "ಬ್ರಿಂಜಾಲ್" to "brinjal",
        "ಸಾಬೂನು" to "soap",
        "ಶ್ಯಾಂಪೂ" to "shampoo",
        "ಡಿಟರ್ಜೆಂಟ್" to "detergent",
        "ಪಾಪಡ" to "papad",
        "ಬಿಸ್ಕಟ್" to "biscuit",
        "ಬೆಲ್ಲ" to "jaggery",
        "ಸೇಬು" to "apple",
        "ದ್ರಾಕ್ಷಿ" to "grapes",
        "ಮಾವಿನಹಣ್ಣು" to "mango"
    )

    fun translate(text: String): String {
        if (text.isBlank()) return ""
        val normalized = normalizeDigits(text)
        return normalized.lines().joinToString("\n") { line ->
            var out = line
            unitReplacements.forEach { (k, v) -> out = out.replace(k, v) }
            dictionary.forEach { (k, v) -> out = out.replace(k, v) }
            out
        }
    }

    fun hasChanges(original: String, translated: String): Boolean = original != translated

    private fun normalizeDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (ch in text) {
            sb.append(digitMap[ch] ?: ch)
        }
        return sb.toString()
    }
}
