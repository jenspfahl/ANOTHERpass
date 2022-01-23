package de.jepfa.yapm.util

import android.net.Uri
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object Constants {
    val HOMEPAGE = Uri.parse("https://anotherpass.jepfa.de")
    val FOSS_SITE = "https://github.com/jenspfahl/anotherpass"
    val BUG_REPORT_SITE = FOSS_SITE + "/issues/new?title=%s&body=%s"

    val MIN_PIN_LENGTH = 6
    val MAX_LABELS_PER_CREDENTIAL = 5
    val MAX_CREDENTIAL_PASSWD_LENGTH = 50

    val MASTER_KEY_BYTE_SIZE = 128

    val SDF_DT_MEDIUM =
        SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM)
    val SDF_D_INTERNATIONAL: DateFormat = SimpleDateFormat("yyyy-MM-dd")

    val UNKNOWN_VAULT_VERSION = 1
    val FAST_KEYGEN_VAULT_VERSION = 2

}