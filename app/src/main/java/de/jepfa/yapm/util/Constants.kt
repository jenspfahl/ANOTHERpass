package de.jepfa.yapm.util

import android.net.Uri
import java.text.DateFormat
import java.text.SimpleDateFormat

object Constants {
    val HOMEPAGE = Uri.parse("https://anotherpass.jepfa.de")
    val ONLINE_HELP = Uri.parse("https://anotherpass.jepfa.de/how/")
    val FEEDBACK = Uri.parse("https://jepfa.de/feedback/index.php/489541")
    val EXTENSION_HOMEPAGE = Uri.parse("https://anotherpass.jepfa.de/extension")
    val FOSS_SITE = "https://github.com/jenspfahl/anotherpass"
    val BUG_REPORT_SITE = FOSS_SITE + "/issues/new?title=%s&body=%s"

    val LOG_PREFIX = "ANOSS/"
    val MIN_PIN_LENGTH = 6
    val MAX_LABELS_PER_CREDENTIAL = 5
    val MAX_CREDENTIAL_PASSWD_LENGTH = 50

    val MASTER_KEY_BYTE_SIZE = 128

    val SDF_DT_MEDIUM =
        SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM)
    val SDF_D_INTERNATIONAL: DateFormat = SimpleDateFormat("yyyy-MM-dd")

    // Note: This is actually not a vault version rather than a Masterkey interpretation. So new vault versions should be handled differently !
    const val INITIAL_VAULT_VERSION = 1
    const val FAST_KEYGEN_VAULT_VERSION = 2
    const val CURRENT_VERSION = FAST_KEYGEN_VAULT_VERSION


    const val ACTION_DELIMITER = "$"
    const val ACTION_OPEN_VAULT_FOR_AUTOFILL = "openVault"
    const val ACTION_OPEN_VAULT_FOR_FILTERING = "openAndFilter"
    const val ACTION_CLOSE_VAULT = "closeVault"
    const val ACTION_EXCLUDE_FROM_AUTOFILL = "excludeFromAutofill"
    const val ACTION_PAUSE_AUTOFILL = "pauseAutofill"
    const val ACTION_PREFILLED_FROM_EXTENSION = "prefilledFromExtension"
    const val ACTION_OPEN_AUTOFILL_DIALOG = "openAutofillDialog"



}