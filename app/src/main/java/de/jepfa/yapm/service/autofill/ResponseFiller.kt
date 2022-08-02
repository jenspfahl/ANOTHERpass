package de.jepfa.yapm.service.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.os.Build
import android.service.autofill.*
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_EVERYWHERE
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_EXCLUSION_LIST
import de.jepfa.yapm.service.PreferenceService.STATE_PAUSE_AUTOFILL
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

@RequiresApi(Build.VERSION_CODES.O)
object ResponseFiller {

    const val ACTION_OPEN_VAULT = "openVault"
    const val ACTION_CLOSE_VAULT = "closeVault"
    const val ACTION_EXCLUDE_FROM_AUTOFILL = "excludeFromAutofill"

    private val VIEW_TO_IDENTIFY = "text"
    private val PASSWORD_INDICATORS = listOf("password", "passwd", "passphrase", "pin", "pass phrase", "keyword")
    private val USER_INDICATORS = listOf("user", "account", "email")

    private var isDeactivatedSinceWhen: Long? = null

    private class Fields {

        private val userFields: MutableSet<ViewNode> = HashSet()
        private val passwordFields: MutableSet<ViewNode> = HashSet()
        private val potentialFields: MutableSet<ViewNode> = HashSet()
        private val allFields: MutableSet<ViewNode> = HashSet()

        fun addUserField(node: ViewNode) {
            if (allFields.contains(node)) return

            userFields.add(node)
            allFields.add(node)
        }

        fun addPasswordField(node: ViewNode) {
            if (allFields.contains(node)) return

            passwordFields.add(node)
            allFields.add(node)
        }

        fun addPotentialField(node: ViewNode) {
            if (allFields.contains(node)) return

            potentialFields.add(node)
            allFields.add(node)
        }

        fun getUserFields(): Set<ViewNode> {
            return userFields
        }

        fun getPasswordFields(): Set<ViewNode> {
            return passwordFields
        }

        fun getPotentialFields(): Set<ViewNode> {
            return potentialFields
        }

        fun getAllFields(): Set<ViewNode> {
            return allFields
        }

        fun hasCredentialFields() : Boolean
                = !userFields.isNullOrEmpty() || !passwordFields.isNullOrEmpty()

        fun getAutofillIds(): Array<AutofillId> {
            return potentialFields.map { it.autofillId }
                .filterNotNull()
                .toTypedArray()
        }
    }


    fun createFillResponse(
        structure: AssistStructure,
        allowCreateAuthentication : Boolean,
        ignoreCurrentField: Boolean = false,
        context: Context
    ) : FillResponse? {
        if (structure.isHomeActivity) {
            Log.i("CFS", "home activity")
            return null
        }
        Log.i("CFS", isDeactivatedSinceWhen.toString())

        val pauseDurationInSec = PreferenceService.getAsString(PreferenceService.PREF_AUTOFILL_DEACTIVATION_DURATION, context)

        if (pauseDurationInSec != null && pauseDurationInSec.toInt() != 0) {

            if (isDeactivatedSinceWhen == null) {
                PreferenceService.getAsString(STATE_PAUSE_AUTOFILL, context)?.let { whenAsString ->
                    isDeactivatedSinceWhen = whenAsString.toLong()
                }
            }

            isDeactivatedSinceWhen?.let {
                val border = System.currentTimeMillis() - (pauseDurationInSec.toLong() * 1000)
                if (it > border) {
                    Log.i("CFS", "temporary deactivated")
                    return null
                } else {
                    resumeAutofill(context)
                }
            }
        }

        if (structure.activityComponent?.packageName.equals(context.packageName)) {
            Log.i("CFS", "myself")
            return null
        }

        if (getExcludedAppList(context).contains(structure.activityComponent?.packageName)) {
            Log.i("CFS", "excluded: " + structure.activityComponent?.packageName)
            return null
        }

        val vaultPresent =
            PreferenceService.isPresent(PreferenceService.DATA_ENCRYPTED_MASTER_KEY, context)
        if (!vaultPresent) {
            Log.i("CFS", "No vault created or imported")
            return null
        }

        val suggestEverywhere = PreferenceService.getAsBool(PREF_AUTOFILL_EVERYWHERE, context)
        val fields = identifyFields(structure, suggestEverywhere) ?: return null

        val key = Session.getMasterKeySK()
        val credential = AutofillCredentialHolder.currentCredential
        if (key == null || Session.isDenied() || credential == null) {
            if (allowCreateAuthentication) {
                if (suggestEverywhere || fields.hasCredentialFields()) {
                    return createAuthenticationFillResponse(fields, context)
                }
            }
            Log.i("CFS", "Not logged in or no credential selected")
            return null
        }

        val name = SecretService.decryptCommonString(key, credential.name)
        val user = SecretService.decryptCommonString(key, credential.user)
        val password = SecretService.decryptPassword(key, credential.password)
        AutofillCredentialHolder.obfuscationKey?.let {
            password.deobfuscate(it)
        }

        val dataSets = ArrayList<Dataset>()
        if (user.isNotBlank()) {
            createUserDataSets(fields.getUserFields(), name, user, context)
                .forEach { dataSets.add(it) }

            createUserDataSets(fields.getPotentialFields(), name, user, context)
                .forEach { dataSets.add(it) }
        }

        createPasswordDataSets(fields.getPasswordFields(), name, password, context)
            .forEach { dataSets.add(it) }
        createPasswordDataSets(fields.getPotentialFields(), name, password, context)
            .forEach { dataSets.add(it) }
        createAuthDataSets(fields.getPotentialFields(),
            R.drawable.ic_baseline_arrow_back_24_gray,
            context.getString(R.string.go_back_to_app),
            ACTION_OPEN_VAULT,
            context)
            .forEach { dataSets.add(it) }

        createAuthDataSets(fields.getPotentialFields(),
            R.drawable.ic_lock_outline_gray_24dp,
            context.getString(R.string.lock_items),
            ACTION_CLOSE_VAULT,
            context)
            .forEach { dataSets.add(it) }



        password.clear()

        if (dataSets.isEmpty()) return null

        val responseBuilder = FillResponse.Builder()
        addHeaderView(responseBuilder, context)

        dataSets.forEach { responseBuilder.addDataset(it) }

        if (ignoreCurrentField) {
            val ignoreIds = fields.getAllFields().filter { it.isFocused }.mapNotNull { it.autofillId }.toTypedArray()
            responseBuilder.setIgnoredIds(*ignoreIds)
        }

        return responseBuilder.build()

    }

    fun resumeAutofill(context: Context) {
        PreferenceService.delete(STATE_PAUSE_AUTOFILL, context)
        isDeactivatedSinceWhen = null
    }

    fun isAutofillPaused(context: Context): Boolean {
        return isDeactivatedSinceWhen != null || PreferenceService.isPresent(STATE_PAUSE_AUTOFILL, context)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    fun createAutofillPauseResponse(context: Context, pauseDurationInSec: Long): FillResponse? {
        if (pauseDurationInSec == 0L) {
            resumeAutofill(context)
            return null;
        }
        isDeactivatedSinceWhen = System.currentTimeMillis()
        PreferenceService.putString(STATE_PAUSE_AUTOFILL, isDeactivatedSinceWhen.toString(), context)

        return FillResponse.Builder()
                .disableAutofill( 1) // disabling autofill is managed by the app to be able to resume it earlier
                .build()
    }

    private fun createAuthenticationFillResponse(fields: Fields, context: Context): FillResponse {
        val responseBuilder = FillResponse.Builder()

        addHeaderView(responseBuilder, context)

        if (Session.isDenied()) {
            createAuthDataSets(fields.getPotentialFields(),
                R.drawable.ic_lock_open_gray_24dp,
                context.getString(R.string.login_required_first), ACTION_OPEN_VAULT, context)
                .forEach { responseBuilder.addDataset(it) }
        }
        else {
            createAuthDataSets(fields.getPotentialFields(),
                R.drawable.ic_baseline_list_gray_24,
                context.getString(R.string.select_credential_for_autofill), ACTION_OPEN_VAULT, context)
                .forEach { responseBuilder.addDataset(it) }
        }


        createAuthDataSets(fields.getPotentialFields(), R.drawable.ic_baseline_not_interested_gray_24,
            context.getString(R.string.no_autofill_here), ACTION_EXCLUDE_FROM_AUTOFILL, context)
            .forEach { responseBuilder.addDataset(it) }


        return responseBuilder.build()
    }

    private fun createAppIntentSender(context: Context, action: String): IntentSender {
        val authIntent = Intent(context, ListCredentialsActivity::class.java)
        authIntent.putExtra(SecureActivity.SecretChecker.fromAutofill, true)
        authIntent.action = action

        return PendingIntent.getActivity(
            context,
            1001,
            authIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT
        ).intentSender
    }

    private fun addHeaderView(responseBuilder: FillResponse.Builder, context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            responseBuilder.setHeader(createHeaderView(context))
        }
    }

    private fun createHeaderView(context: Context): RemoteViews {
        val headerView =
            createRemoteView(R.mipmap.ic_launcher_round, context.getString(R.string.app_name), context)
        headerView.setTextViewTextSize(R.id.autofill_item, TypedValue.COMPLEX_UNIT_SP, 24f)
        headerView.setTextColor(R.id.autofill_item, Color.GRAY)
        headerView.setViewPadding(R.id.autofill_item, 0, 32, 0, 0)
        return headerView
    }

    private fun identifyFields(structure: AssistStructure, suggestEverywhere: Boolean): Fields? {

        val fields = Fields()

        val windowNode = structure.getWindowNodeAt(0) ?: return null
        val viewNode = windowNode.rootViewNode ?: return null

        identifyFields(viewNode, fields, suggestEverywhere)

        return fields

    }


    private fun identifyFields(node: ViewNode, fields: Fields, suggestEverywhere: Boolean) {
        node.idEntry?.let { identifyField(it, node, fields) }
        node.text?.let { identifyField(it.toString(), node, fields) }
        node.hint?.let { identifyField(it, node, fields) }
        node.htmlInfo?.let { identifyField(it.tag, node, fields) }
        node.autofillHints?.map { identifyField(it, node, fields) }

        if (!suggestEverywhere && node.autofillType != View.AUTOFILL_TYPE_TEXT) {
            Log.i("CFS", "No text autofill type")
            return
        }

        if (!suggestEverywhere && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (node.importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS) {
                // don't traverse anything
                Log.i("CFS", "No autofill for current node and children")
                return
            }
        }

        if (suggestEverywhere && node.className != null) {
            val viewId = node.idEntry?.toLowerCase(Locale.ROOT)
            val className = node.className
            if (viewId != null && className != null && className.contains(VIEW_TO_IDENTIFY)) {
                if (!suggestEverywhere && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    if (node.importantForAutofill != View.IMPORTANT_FOR_AUTOFILL_NO) {
                        // only consider current node if AUTO and YES*
                        fields.addPotentialField(node)
                    }
                    else {
                        Log.i("CFS", "No autofill for current node but its children")
                    }
                }
                else {
                    fields.addPotentialField(node)
                }
            }
        }

        if (!suggestEverywhere && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (node.importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS) {
                // don't traverse children
                Log.i("CFS", "No autofill for current nodes children only")
                return
            }
        }

        // go deeper in the tree
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child != null) {
                identifyFields(child, fields, suggestEverywhere)
            }
        }
    }

    private fun identifyField(
        attribute: String,
        node: ViewNode,
        fields: Fields
    ) {

        val attrib = attribute.toLowerCase(Locale.ROOT)
        if (contains(attrib, USER_INDICATORS)) {
            fields.addUserField(node)
        } else if (contains(attrib, PASSWORD_INDICATORS)) {
            fields.addPasswordField(node)
        }
        else {
            fields.addPotentialField(node)
        }
    }

    private fun contains(s: String, contents: Collection<String>) : Boolean {
        return contents.filter { s.contains(it) }.any()
    }

    private fun createUserDataSets(fields : Set<ViewNode>, name: String, user: String, context: Context): List<Dataset> {
        return createDataSets(fields,
            R.drawable.ic_baseline_person_24_gray,
            context.getString(R.string.paste_user_for_autofill, name),
            user,
            context)
    }

    private fun createPasswordDataSets(fields : Set<ViewNode>, name: String, password: Password, context: Context): List<Dataset> {
        return createDataSets(fields,
            R.drawable.ic_baseline_vpn_key_24_gray,
            context.getString(R.string.paste_passwd_for_autofill, name),
            password.toRawFormattedPassword(),
            context)
    }


    private fun createDataSets(fields : Set<ViewNode>, iconId: Int, text: String, content: CharSequence, context: Context): List<Dataset> {
        return fields.mapNotNull { createDataSet(it, iconId, text, content, context) }
    }

    private fun createAuthDataSets(fields : Set<ViewNode>, iconId: Int, text: String, action: String, context: Context): List<Dataset> {
        return fields.mapNotNull { createAuthDataSet(it, iconId, text, action, context) }
    }

    private fun createDataSet(
        field: ViewNode,
        iconId : Int,
        text: String,
        content: CharSequence,
        context: Context
    ): Dataset? {

        val autofillId = field.autofillId ?: return null
        val remoteView = createRemoteView(iconId, text, context)

        return Dataset.Builder(remoteView)
            .setValue(
                autofillId,
                AutofillValue.forText(content)
            ).build()
    }

    private fun createAuthDataSet(
        field: ViewNode,
        iconId : Int,
        text: String,
        action: String,
        context: Context
    ): Dataset? {
        val autofillId = field.autofillId ?: return null

        val remoteView = createRemoteView(iconId, text, context)
        val intentSender = createAppIntentSender(context, action)

        return Dataset.Builder(remoteView)
            .setValue(autofillId, null)
            .setAuthentication(intentSender)
            .build()
    }

    private fun createRemoteView(iconId: Int, text: String, context: Context): RemoteViews {
        val remoteView = RemoteViews(
            context.packageName,
            R.layout.content_autofill_view
        )

        remoteView.setImageViewResource(R.id.autofill_image, iconId)
        remoteView.setTextViewText(R.id.autofill_item, text)
        return remoteView
    }


    private fun getExcludedAppList(context: Context): List<String> {
        val apps = PreferenceService.getAsStringSet(PREF_AUTOFILL_EXCLUSION_LIST, context)
        return apps?.toList() ?: emptyList()
    }

}