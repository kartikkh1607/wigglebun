package com.kartikkhandelwal.wigglebun.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The name field (CLAUDE.md §6.2): at most MAX_PET_NAME_LENGTH characters. */
class PetNameRulesTest {

    @Test
    fun aNameUpToTheLimitIsKept() {
        assertEquals("Wigglebottom", limitPetNameInput("Wigglebottom"))
    }

    @Test
    fun pastingSomethingLongerKeepsTheStart() {
        assertEquals("Wigglebottom", limitPetNameInput("Wigglebottomley"))
    }

    @Test
    fun aPastedNewlineIsDropped() {
        assertEquals("BunBun", limitPetNameInput("Bun\nBun"))
    }

    @Test
    fun anEmojiAtTheLimitIsNotCutInHalf() {
        val eleven = "A".repeat(MAX_PET_NAME_LENGTH - 1)
        assertEquals(eleven, limitPetNameInput(eleven + "🐰"))
    }

    @Test
    fun spacesAroundTheNameAreNotSaved() {
        assertEquals("Mr Bun", cleanPetName("  Mr Bun "))
    }

    @Test
    fun onlySpacesMeansNoName() {
        assertNull(cleanPetName("   "))
        assertNull(cleanPetName(""))
    }
}
