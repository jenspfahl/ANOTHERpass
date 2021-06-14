package de.jepfa.yapm.util

import android.net.Uri
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat

object Constants {
    val HOMEPAGE = Uri.parse("https://anotherpass.jepfa.de")
    val BUG_REPORT_SITE = Uri.parse("https://github.com/jenspfahl/anotherpass/issues")

    val MIN_PIN_LENGTH = 6
    val MAX_LABELS_PER_CREDENTIAL = 5
    val MAX_LABEL_LENGTH = 20

    var SDF_DT_MEDIUM =
        SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM)
    var SDF_D_INTERNATIONAL: DateFormat = SimpleDateFormat("yyyy-MM-dd")

    var NF: NumberFormat = NumberFormat.getNumberInstance()
    var DF: DecimalFormat = NF as DecimalFormat

}