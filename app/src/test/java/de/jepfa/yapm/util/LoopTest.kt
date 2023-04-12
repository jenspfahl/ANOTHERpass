package de.jepfa.obfusser.util.encrypt

import de.jepfa.yapm.util.Loop
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.DEFAULT_OBFUSCATIONABLE_SPECIAL_CHARS
import de.jepfa.yapm.service.secretgenerator.GeneratorBase.Companion.EXTENDED_SPECIAL_CHARS
import org.junit.Assert
import org.junit.Test
import java.util.*

class LoopTest {
    @Test
    fun loopApplies() {
        val loop = Loop(Arrays.asList(*arrayOf("one", "two", "three", "four")))
        Assert.assertFalse(loop.applies("zero"))
        Assert.assertTrue(loop.applies("one"))
    }

    @Test(expected = IllegalStateException::class)
    fun loopForwardUnknownFrom() {
        val loop = Loop(Arrays.asList(*arrayOf("one", "two", "three", "four")))
        Assert.assertEquals("two", loop.forwards("zero", 1))
    }

    @Test
    fun loopForward() {
        val loop = Loop(Arrays.asList(*arrayOf("one", "two", "three", "four")))
        Assert.assertEquals("two", loop.forwards("one", 1))
        Assert.assertEquals("four", loop.forwards("two", 2))
        Assert.assertEquals("three", loop.forwards("four", 3))
        Assert.assertEquals("three", loop.forwards("three", 8))
        Assert.assertEquals("two", loop.forwards("three", -1))
        Assert.assertEquals("four", loop.forwards("one", -1))
        Assert.assertEquals("three", loop.forwards("one", -10))
    }

    @Test(expected = IllegalStateException::class)
    fun loopBackwardUnknownFrom() {
        val loop = Loop(Arrays.asList(*arrayOf("one", "two", "three", "four")))
        Assert.assertEquals("two", loop.backwards("zero", 1))
    }

    @Test
    fun loopBackward() {
        val loop = Loop(Arrays.asList(*arrayOf("one", "two", "three", "four")))
        Assert.assertEquals("four", loop.backwards("one", 1))
        Assert.assertEquals("four", loop.backwards("two", 2))
        Assert.assertEquals("one", loop.backwards("four", 3))
        Assert.assertEquals("three", loop.backwards("three", 8))
        Assert.assertEquals("four", loop.backwards("three", -1))
        Assert.assertEquals("two", loop.backwards("one", -1))
        Assert.assertEquals("three", loop.backwards("one", -10))
    }

    @Test
    fun loopPassword() {
        val PASSWORD = "Abcd123?"
        val password = Password(PASSWORD)
        val key = Key(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
        Loop.loopPassword(password, key, forwards = true)
        Assert.assertEquals("Edgj680&", password.toString())
        Loop.loopPassword(password, key, forwards = false)
        Assert.assertEquals(PASSWORD, password.toString())
    }

    @Test
    fun loopLoopableSpecialChars() {
        val PASSWORD = DEFAULT_OBFUSCATIONABLE_SPECIAL_CHARS
        val password = Password(PASSWORD)
        val key = Key(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8))
        Loop.loopPassword(password, key, forwards = true)
        Assert.assertEquals("?,:$&#?,&#?,", password.toString())
        Loop.loopPassword(password, key, forwards = false)
        Assert.assertEquals(PASSWORD, password.toString())
    }

    @Test
    fun loopNotLoopableSpecialChars() {
        val PASSWORD = "_;+*()[]{}<>\"'=\\~|°"
        val password = Password(PASSWORD)
        val key = Key(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8, 1, 2, 3, 4, 5, 6, 7, 8))
        Loop.loopPassword(password, key, forwards = true)
        Assert.assertEquals(PASSWORD, password.toString())
        Loop.loopPassword(password, key, forwards = false)
        Assert.assertEquals(PASSWORD, password.toString())
    }
}