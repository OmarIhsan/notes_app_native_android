package com.mal5odha.core.ink.models

import com.google.gson.Gson
import com.mal5odha.app.ui.components.StickyCardPalette
import com.mal5odha.core.data.models.Ml5TextAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StickyCardAnnotationTest {

    @Test
    fun testStickyCardAnnotation_defaultValues() {
        val card = StickyCardAnnotation(
            pageId = "page-1",
            xNorm = 0.2f,
            yNorm = 0.3f
        )

        assertEquals("page-1", card.pageId)
        assertEquals(0.2f, card.xNorm, 0.001f)
        assertEquals(0.3f, card.yNorm, 0.001f)
        assertEquals(0.32f, card.widthNorm, 0.001f)
        assertEquals("", card.title)
        assertEquals("", card.body)
        assertEquals(0xFFFFF9C4L, card.cardColorHex)
        assertFalse(card.isPinned)
    }

    @Test
    fun testStickyCardAnnotation_toTextAnnotationMapping() {
        val card = StickyCardAnnotation(
            id = "test-card-1",
            pageId = "page-10",
            xNorm = 0.15f,
            yNorm = 0.25f,
            widthNorm = 0.35f,
            title = "Meeting Notes",
            body = "1. Discuss Q3 deliverables\n2. Review budget",
            cardColorHex = StickyCardPalette.PEACH,
            isPinned = true
        )

        val textAnnotation = card.toTextAnnotation()

        assertEquals("test-card-1", textAnnotation.id)
        assertEquals("page-10", textAnnotation.pageId)
        assertEquals(0.15f, textAnnotation.xNorm, 0.001f)
        assertEquals(0.25f, textAnnotation.yNorm, 0.001f)
        assertEquals(0.35f, textAnnotation.widthNorm, 0.001f)
        assertEquals("Meeting Notes", textAnnotation.title)
        assertEquals("1. Discuss Q3 deliverables\n2. Review budget", textAnnotation.content)
        assertEquals(StickyCardPalette.PEACH.toInt(), textAnnotation.backgroundColor)
        assertTrue(textAnnotation.isCard)
        assertTrue(textAnnotation.isPinned)
    }

    @Test
    fun testTextAnnotation_toStickyCardAnnotationMapping() {
        val textAnnotation = TextAnnotation.createStickyCard(
            pageId = "page-20",
            xNorm = 0.05f,
            yNorm = 0.10f,
            widthNorm = 0.30f,
            title = "Ideas",
            body = "Implement minimal sticky cards",
            cardColorHex = StickyCardPalette.GREEN,
            isPinned = false
        )

        val card = textAnnotation.toStickyCardAnnotation()

        assertEquals(textAnnotation.id, card.id)
        assertEquals("page-20", card.pageId)
        assertEquals(0.05f, card.xNorm, 0.001f)
        assertEquals(0.10f, card.yNorm, 0.001f)
        assertEquals(0.30f, card.widthNorm, 0.001f)
        assertEquals("Ideas", card.title)
        assertEquals("Implement minimal sticky cards", card.body)
        assertEquals(StickyCardPalette.GREEN, card.cardColorHex)
        assertFalse(card.isPinned)
    }

    @Test
    fun testMl5Serialization_roundTripPreservesCardFields() {
        val ml5 = Ml5TextAnnotation(
            id = "ann-1",
            text = "Body text",
            x = 0.1f,
            y = 0.2f,
            width = 0.32f,
            height = 0.15f,
            fontSize = 14f,
            color = 0xFF1C1B1F.toInt(),
            backgroundColor = StickyCardPalette.BLUE.toInt(),
            isCard = true,
            title = "Important",
            isPinned = true
        )

        val gson = Gson()
        val json = gson.toJson(ml5)
        val deserialized = gson.fromJson(json, Ml5TextAnnotation::class.java)

        assertEquals("ann-1", deserialized.id)
        assertEquals("Important", deserialized.title)
        assertEquals("Body text", deserialized.text)
        assertTrue(deserialized.isCard)
        assertTrue(deserialized.isPinned)
        assertEquals(StickyCardPalette.BLUE.toInt(), deserialized.backgroundColor)
    }

    @Test
    fun testInkTool_stickyCardProperties() {
        val tool = InkTool.STICKY_CARD
        assertTrue(tool.isStickyCard)
        assertTrue(tool.isPlacementTool)

        val noteTool = InkTool.STICKY_NOTE
        assertTrue(noteTool.isStickyCard)
        assertTrue(noteTool.isPlacementTool)

        val textTool = InkTool.TEXT
        assertFalse(textTool.isStickyCard)
        assertTrue(textTool.isPlacementTool)

        val penTool = InkTool.PEN
        assertFalse(penTool.isStickyCard)
        assertFalse(penTool.isPlacementTool)
    }

    @Test
    fun testStickyCardAnnotation_alignmentMappingAndCycling() {
        // 1. Default alignment is START
        val defaultCard = StickyCardAnnotation(pageId = "page-1", xNorm = 0.1f, yNorm = 0.1f)
        assertEquals("START", defaultCard.alignment)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Start, defaultCard.toTextAnnotation().textAlign)

        // 2. Custom alignment: CENTER
        val centerCard = defaultCard.copy(alignment = "CENTER")
        val centerText = centerCard.toTextAnnotation()
        assertEquals(androidx.compose.ui.text.style.TextAlign.Center, centerText.textAlign)
        assertEquals("CENTER", centerText.toStickyCardAnnotation().alignment)

        // 3. Custom alignment: END
        val endCard = defaultCard.copy(alignment = "END")
        val endText = endCard.toTextAnnotation()
        assertEquals(androidx.compose.ui.text.style.TextAlign.End, endText.textAlign)
        assertEquals("END", endText.toStickyCardAnnotation().alignment)

        // 4. Cycle logic verification (Start -> Center -> End -> Start)
        fun cycleAlign(current: androidx.compose.ui.text.style.TextAlign): androidx.compose.ui.text.style.TextAlign {
            return when (current) {
                androidx.compose.ui.text.style.TextAlign.Start, androidx.compose.ui.text.style.TextAlign.Left -> androidx.compose.ui.text.style.TextAlign.Center
                androidx.compose.ui.text.style.TextAlign.Center -> androidx.compose.ui.text.style.TextAlign.End
                else -> androidx.compose.ui.text.style.TextAlign.Start
            }
        }

        var align = androidx.compose.ui.text.style.TextAlign.Start
        align = cycleAlign(align)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Center, align)
        align = cycleAlign(align)
        assertEquals(androidx.compose.ui.text.style.TextAlign.End, align)
        align = cycleAlign(align)
        assertEquals(androidx.compose.ui.text.style.TextAlign.Start, align)
    }

    @Test
    fun testInkTool_channelIsolation() {
        val stickyTool = InkTool.STICKY_CARD
        assertTrue(stickyTool.isStickyCard)
        assertFalse(stickyTool.isText)
        assertTrue(stickyTool.matchesAnnotation(isCard = true))
        assertFalse(stickyTool.matchesAnnotation(isCard = false))

        val textTool = InkTool.TEXT
        assertFalse(textTool.isStickyCard)
        assertTrue(textTool.isText)
        assertFalse(textTool.matchesAnnotation(isCard = true))
        assertTrue(textTool.matchesAnnotation(isCard = false))

        val penTool = InkTool.PEN
        assertFalse(penTool.isStickyCard)
        assertFalse(penTool.isText)
        assertFalse(penTool.matchesAnnotation(isCard = true))
        assertFalse(penTool.matchesAnnotation(isCard = false))
    }
}
