package de.jepfa.obfusser.util.encrypt

import de.jepfa.yapm.model.secret.Password
import org.junit.Assert
import org.junit.Test
import java.nio.charset.Charset
import java.util.*

class PasswordTest {

    @Test
    fun testAscii() {
        val passwd = Password("abc")
        System.out.println("bytes: " + Arrays.toString(passwd.toByteArray()))
        Assert.assertEquals("abc", passwd.toRawFormattedPassword().toString())
        Assert.assertEquals("abc", passwd.toString())
        Assert.assertArrayEquals("abc".toCharArray(), passwd.toEncodedCharArray())
    }

    @Test
    fun testNonAscii() {
        val passwd = Password("abä")
        System.out.println("bytes: " + Arrays.toString(passwd.toByteArray()))
        System.out.println("chars: " + Arrays.toString(passwd.toEncodedCharArray()))
        System.out.println("chars of ${Charset.defaultCharset()}: " + Arrays.toString(passwd.decodeToCharArray()))
        System.out.println("byte: " + 'ä'.toByte())
        Assert.assertEquals("abä", passwd.toRawFormattedPassword().toString())
        Assert.assertEquals("abä", passwd.toString())
        Assert.assertArrayEquals("abä".toCharArray(), passwd.decodeToCharArray())
    }

}