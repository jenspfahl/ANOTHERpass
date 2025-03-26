package de.jepfa.yapm.util

import android.content.Context
import de.jepfa.yapm.R

const val SEARCH_COMMAND_START = "!"
const val SEARCH_COMMAND_COMMAND_END = ":"
const val SEARCH_COMMAND_END = ";"

enum class SearchCommand(
    private val cmd: String,
    private val descriptionId: Int,
    private val withArg: Boolean) {

    SEARCH_COMMAND_SEARCH_IN_ALL("all", R.string.command_all, true),
    SEARCH_COMMAND_SEARCH_ID ("id", R.string.command_id, true),
    SEARCH_COMMAND_SEARCH_UID ("uid", R.string.command_uid, true),
    SEARCH_COMMAND_SEARCH_LABEL("tag", R.string.command_tag, true),
    SEARCH_COMMAND_SEARCH_USER("user", R.string.command_user, true),
    SEARCH_COMMAND_SEARCH_WEBSITE("web", R.string.command_web, true),
    SEARCH_COMMAND_SHOW_MARKED("marked", R.string.command_marked, false),
    SEARCH_COMMAND_SHOW_EXPIRED("expired", R.string.command_expired, false),
    SEARCH_COMMAND_SHOW_EXPIRES("expires", R.string.command_expires, false),
    SEARCH_COMMAND_SHOW_LATEST("latest", R.string.command_latest, false),
    SEARCH_COMMAND_SHOW_VEILED("veiled", R.string.command_veiled, false),
    SEARCH_COMMAND_SHOW_OTP("onetime", R.string.command_otp, false),
    ;

    fun getCmd(): String {
        return if (withArg) {
            SEARCH_COMMAND_START + cmd + SEARCH_COMMAND_COMMAND_END
        } else {
            SEARCH_COMMAND_START + cmd
        }
    }
    fun getDescription(context: Context) = context.getString(descriptionId)

    fun applies(q: CharSequence) = q.startsWith(getCmd(), ignoreCase = true)

    fun extractArg(q: CharSequence): String {
        return if (withArg) {
            q.substring(getCmd().length).lowercase().trimStart().removeSuffix(SEARCH_COMMAND_END)
        } else {
            ""
        }
    }

}
