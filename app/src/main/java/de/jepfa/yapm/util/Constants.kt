package de.jepfa.yapm.util

import android.net.Uri
import java.text.DateFormat
import java.text.SimpleDateFormat

object Constants {
    val HOMEPAGE = Uri.parse("https://jepfa.de") // TODO add subdomain if online
    val BUG_REPORT_SITE = Uri.parse("https://bitbucket.org/jepfa/yapm") // TODO add bugtracker if online

    val MAX_LABELS_PER_CREDENTIAL = 5
    val MAX_LABEL_LENGTH = 20

    var SDF_DT_MEDIUM =
        SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.MEDIUM, SimpleDateFormat.MEDIUM)
    var SDF_D_INTERNATIONAL: DateFormat = SimpleDateFormat("yyyy-MM-dd")


}