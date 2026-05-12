package com.gonzalez.trophychest.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserDefaultsTest {
    @Test
    fun generateFriendCode_returnsSixUppercaseAlphanumericCharacters() {
        val friendCode = UserDefaults.generateFriendCode(Random(123))

        assertEquals(UserDefaults.FRIEND_CODE_LENGTH, friendCode.length)
        assertTrue(Regex("^[A-Z0-9]{6}$").matches(friendCode))
    }

    @Test
    fun randomImageId_returnsValueBetweenOneAndFive() {
        repeat(100) {
            val imageId = UserDefaults.randomImageId(Random(it))

            assertTrue(imageId in UserDefaults.MIN_IMAGE_ID..UserDefaults.MAX_IMAGE_ID)
        }
    }

    @Test
    fun normalizeFriendCode_uppercasesAndRemovesSeparators() {
        assertEquals("AB12CD", UserDefaults.normalizeFriendCode(" ab-12 cd "))
    }

    @Test
    fun friendCodeError_requiresSixCharacters() {
        assertEquals(null, UserDefaults.friendCodeError("AB12CD"))
        assertTrue(UserDefaults.friendCodeError("ABC") != null)
    }
}
