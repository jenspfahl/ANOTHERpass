package de.jepfa.yapm.service.otp

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import de.jepfa.yapm.model.encrypted.CipherAlgorithm
import de.jepfa.yapm.model.otp.TOTPConfig
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.ui.login.LoginActivity
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
@LargeTest
class OtpServiceTest {

    val TAG = LOG_PREFIX + "TOTP"

    val maxPins = 10 // 10_000_000
    var currentAttempt = 0
    val pin = Password("5")
    val masterPassword = Password("abcd")
    val cipherAlgorithm = CipherAlgorithm.AES_256

    lateinit var loginScenario: ActivityScenario<LoginActivity>


    @Before
    fun setup() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), LoginActivity::class.java)
        loginScenario = launchActivity(intent)
    }

    @Test
    fun testTOTP() {
        loginScenario.onActivity { loginActivity ->
            Log.i(TAG, "Creating test vault...")

            val uri = Uri.parse("otpauth://totp/ACME%20Co:john.doe@email.com?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ&issuer=ACME%20Co&algorithm=SHA1&digits=6&period=30")
            val totpConfig = TOTPConfig.fromUri(uri)

            println("TOTP Config: $totpConfig")

          /*  testTOTP(totpConfig, Date(0))
            testTOTP(totpConfig, Date(1))
            testTOTP(totpConfig, Date(29))
            testTOTP(totpConfig, Date(30))
            testTOTP(totpConfig, Date(31))
            testTOTP(totpConfig, Date(32))
            testTOTP(totpConfig, Date(59))
            testTOTP(totpConfig, Date(60))
            testTOTP(totpConfig, Date(61))*/
            testTOTP(totpConfig, Date())  // 320 382
        }
    }

    private fun testTOTP(totpConfig: TOTPConfig?, date: Date) {
        val totp = OtpService.createTOTP(totpConfig!!, date)
        println("TOTP for ${date.time}: $totp")
    }


}
