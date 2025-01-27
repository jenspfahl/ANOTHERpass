package de.jepfa.yapm.service.otp

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import de.jepfa.yapm.model.otp.OtpConfig
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Date

@RunWith(AndroidJUnit4::class)
@LargeTest
class OtpServiceTest {

    val TAG = LOG_PREFIX + "OTP"

    lateinit var anyScenario: ActivityScenario<LoginActivity>


    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
        anyScenario = launchActivity(intent)
    }

    @Test
    fun testHOTP() {
        anyScenario.onActivity { _ ->
            Log.i(TAG, "Creating test vault...")

            val hotpUri = Uri.parse("otpauth://hotp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&counter=0")
            val otpConfig = OtpConfig.fromUri(hotpUri)

            if (otpConfig == null) {
                Assert.fail()
                return@onActivity
            }

            Assert.assertEquals(hotpUri, otpConfig.toUri())

            println("HOTP Config: $otpConfig")

            doTestHOTP(otpConfig, "818800")
            doTestHOTP(otpConfig.incCounter(), "320382")
            doTestHOTP(otpConfig.incCounter(), "404533")
        }
    }

    @Test
    fun testTOTP() {
        anyScenario.onActivity { _ ->
            Log.i(TAG, "Creating test vault...")

            val totpUri = Uri.parse("otpauth://totp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30")
            val otpConfig = OtpConfig.fromUri(totpUri)

            if (otpConfig == null) {
                Assert.fail()
                return@onActivity
            }

            Assert.assertEquals(totpUri, otpConfig.toUri())

            println("TOTP Config: $otpConfig")

            doTestTOTP(otpConfig, Date(0), "818800")
            doTestTOTP(otpConfig, Date(1000), "818800")
            doTestTOTP(otpConfig, Date(29000), "818800")
            doTestTOTP(otpConfig, Date(29999), "818800")
            doTestTOTP(otpConfig, Date(30000), "320382")
            doTestTOTP(otpConfig, Date(31000), "320382")
            doTestTOTP(otpConfig, Date(59999), "320382")
            doTestTOTP(otpConfig, Date(60000), "404533")
            doTestTOTP(otpConfig,
                Date.from(LocalDateTime.of(2025, 1, 18, 19, 18, 14).toInstant(ZoneOffset.UTC)),
                "697112")

        }
    }

    private fun doTestHOTP(otpConfig: OtpConfig, expectedToken: String) {
        val hotp = OtpService.generateOTP(otpConfig, Date())
        println("HOTP for counter ${otpConfig.counter}: $hotp")
        Assert.assertEquals(expectedToken, hotp.toString())
    }

    private fun doTestTOTP(otpConfig: OtpConfig, date: Date, expectedToken: String) {
        val totp = OtpService.generateOTP(otpConfig, date)
        println("TOTP for timestamp ${date.time}: $totp")
        Assert.assertEquals(expectedToken, totp.toString())

    }


}
