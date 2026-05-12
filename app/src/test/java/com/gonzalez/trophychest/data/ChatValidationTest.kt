package com.gonzalez.trophychest.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatValidationTest {
    @Test
    fun normalizeUsername_trimsAndLowercases() {
        assertEquals("elena_99", ChatValidation.normalizeUsername("  Elena_99  "))
    }

    @Test
    fun usernameError_rejectsInvalidNames() {
        assertNotNull(ChatValidation.usernameError("ab"))
        assertNotNull(ChatValidation.usernameError("nombre con espacios"))
        assertNotNull(ChatValidation.usernameError("nombre-malo"))
        assertNotNull(ChatValidation.usernameError("abcdefghijklmnopqrstu"))
    }

    @Test
    fun usernameError_acceptsLettersNumbersAndUnderscore() {
        assertEquals(null, ChatValidation.usernameError("player_123"))
    }

    @Test
    fun pairIdFor_sortsUidsDeterministically() {
        assertEquals("aaa_zzz", ChatValidation.pairIdFor("zzz", "aaa"))
        assertEquals("aaa_zzz", ChatValidation.pairIdFor("aaa", "zzz"))
    }

    @Test
    fun pairIdFor_rejectsSameUser() {
        assertThrows(IllegalArgumentException::class.java) {
            ChatValidation.pairIdFor("same", "same")
        }
    }

    @Test
    fun normalizeMessage_rejectsEmptyAndTooLongMessages() {
        assertThrows(IllegalArgumentException::class.java) {
            ChatValidation.normalizeMessage("   ")
        }

        val longMessage = "a".repeat(ChatValidation.MAX_MESSAGE_LENGTH + 1)
        assertThrows(IllegalArgumentException::class.java) {
            ChatValidation.normalizeMessage(longMessage)
        }
    }

    @Test
    fun normalizeMessage_trimsValidMessage() {
        assertEquals("hola", ChatValidation.normalizeMessage("  hola  "))
    }

    @Test
    fun hasUnread_detectsMessagesFromOtherUserAfterReadTime() {
        assertTrue(
            ChatValidation.hasUnread(
                lastMessageAtMillis = 200,
                lastSenderId = "friend",
                currentUid = "me",
                readAtMillis = 100
            )
        )
    }

    @Test
    fun hasUnread_ignoresOwnMessagesAndReadMessages() {
        assertFalse(
            ChatValidation.hasUnread(
                lastMessageAtMillis = 200,
                lastSenderId = "me",
                currentUid = "me",
                readAtMillis = null
            )
        )

        assertFalse(
            ChatValidation.hasUnread(
                lastMessageAtMillis = 100,
                lastSenderId = "friend",
                currentUid = "me",
                readAtMillis = 200
            )
        )
    }
}
