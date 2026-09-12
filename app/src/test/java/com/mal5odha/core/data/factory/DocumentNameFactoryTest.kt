package com.mal5odha.core.data.factory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentNameFactoryTest {

    @Test
    fun defaultNotebookTitle_matchesTimestampFormat() {
        val title = DocumentNameFactory.defaultNotebookTitle()
        assertTrue("Title should start with 'Note '", title.startsWith("Note "))
        val datePart = title.removePrefix("Note ")
        assertTrue(
            "Timestamp part should match yyyy-MM-dd HH:mm pattern",
            datePart.matches(Regex("^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}$"))
        )
    }

    @Test
    fun fromImportedUri_stripsExtensionAndSeparators() {
        assertEquals("Lecture 01 Introduction", DocumentNameFactory.fromImportedUri("Lecture_01-Introduction.pdf"))
        assertEquals("Chapter 3 Notes", DocumentNameFactory.fromImportedUri("Chapter__3--Notes.docx"))
        assertEquals("My Slides", DocumentNameFactory.fromImportedUri("My_Slides.pptx"))
        assertEquals("SingleWord", DocumentNameFactory.fromImportedUri("SingleWord.pdf"))
    }

    @Test
    fun fromImportedUri_blankFallback() {
        assertEquals("Imported Document", DocumentNameFactory.fromImportedUri(".pdf"))
        assertEquals("Imported Document", DocumentNameFactory.fromImportedUri("___---.pdf"))
        assertEquals("Imported Document", DocumentNameFactory.fromImportedUri(""))
    }

    @Test
    fun forDuplicate_firstCopy() {
        val existing = listOf("Physics 101", "Chemistry Notes")
        val duplicate = DocumentNameFactory.forDuplicate("Physics 101", existing)
        assertEquals("Physics 101 (Copy)", duplicate)
    }

    @Test
    fun forDuplicate_incrementsWhenCopyExists() {
        val existing = listOf("Physics 101", "Physics 101 (Copy)")
        val duplicate = DocumentNameFactory.forDuplicate("Physics 101", existing)
        assertEquals("Physics 101 (Copy 2)", duplicate)
    }

    @Test
    fun forDuplicate_incrementsSequentially() {
        val existing = listOf(
            "Physics 101",
            "Physics 101 (Copy)",
            "Physics 101 (Copy 2)",
            "Physics 101 (Copy 3)"
        )
        val duplicate = DocumentNameFactory.forDuplicate("Physics 101", existing)
        assertEquals("Physics 101 (Copy 4)", duplicate)
    }

    @Test
    fun forDuplicate_whenDuplicatingExistingCopy() {
        val existing = listOf(
            "Physics 101",
            "Physics 101 (Copy)",
            "Physics 101 (Copy 2)"
        )
        // Duplicating "Physics 101 (Copy)" directly should detect base title and assign "(Copy 3)"
        val duplicate = DocumentNameFactory.forDuplicate("Physics 101 (Copy)", existing)
        assertEquals("Physics 101 (Copy 3)", duplicate)
    }

    @Test
    fun sanitizeForExport_stripsIllegalFilesystemCharacters() {
        val dirty = "Lecture: Intro/Summary? *Draft* <v1> | \"final\""
        val sanitized = DocumentNameFactory.sanitizeForExport(dirty)
        assertEquals("Lecture_ Intro_Summary_ _Draft_ _v1_ _ _final_", sanitized)
    }

    @Test
    fun sanitizeForExport_clampsTo128Characters() {
        val longTitle = "A".repeat(200)
        val sanitized = DocumentNameFactory.sanitizeForExport(longTitle)
        assertEquals(128, sanitized.length)
        assertEquals("A".repeat(128), sanitized)
    }

    @Test
    fun sanitizeForExport_fallbackToUntitledNote() {
        assertEquals("Untitled_Note", DocumentNameFactory.sanitizeForExport(""))
        assertEquals("Untitled_Note", DocumentNameFactory.sanitizeForExport("   "))
        assertEquals("_________", DocumentNameFactory.sanitizeForExport("///:::***"))
    }
}
