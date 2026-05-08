package com.nammasanthe.ledger.ocr

import com.nammasanthe.ledger.translation.KannadaOfflineTranslator
import org.junit.Assert.assertEquals
import org.junit.Test

class BillParserTest {

    private val parser = BillParser(KannadaOfflineTranslator())

    @Test
    fun parsesMixedLanguageMarketBill() {
        val items = parser.parse(
            """
            Onion 1kg 50
            ಬಾಳೆಹಣ್ಣು 1kg 40
            ಆಲೂಗಡ್ಡೆ 30
            Tomato 2kg 60
            प्याज 500g 25
            """.trimIndent(),
            OcrLanguage.KANNADA
        )

        assertEquals(
            listOf(
                Triple("Onion", "1kg", 50),
                Triple("Banana", "1kg", 40),
                Triple("Potato", "unknown", 30),
                Triple("Tomato", "2kg", 60),
                Triple("Onion", "500g", 25)
            ),
            items.map { Triple(it.item, it.quantity, it.amount) }
        )
    }

    @Test
    fun parsesScriptTextGluedToPrice() {
        val items = parser.parseTranslated(
            """
            ಬಾಳೆಹಣ್ಣು40
            प्याज50
            Tomato2kg60
            """.trimIndent()
        )

        assertEquals(
            listOf(
                Triple("Banana", "unknown", 40),
                Triple("Onion", "unknown", 50),
                Triple("Tomato", "2kg", 60)
            ),
            items.map { Triple(it.item, it.quantity, it.amount) }
        )
    }

    @Test
    fun emitsStrictJsonShape() {
        val items = parser.parseTranslated("Onion 1 kg 50\nPotato 30")

        assertEquals(
            """
            [
              {"item":"Onion","quantity":"1kg","amount":50},
              {"item":"Potato","quantity":"unknown","amount":30}
            ]
            """.trimIndent(),
            parser.toJsonString(items)
        )
    }
}
