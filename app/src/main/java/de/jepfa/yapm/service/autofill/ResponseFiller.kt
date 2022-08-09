package de.jepfa.yapm.service.autofill

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.service.autofill.Dataset
import android.service.autofill.FillResponse
import android.service.autofill.InlinePresentation
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.view.inputmethod.InlineSuggestionsRequest
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_EVERYWHERE
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_EXCLUSION_LIST
import de.jepfa.yapm.service.PreferenceService.PREF_AUTOFILL_INLINE_PRESENTATIONS
import de.jepfa.yapm.service.PreferenceService.STATE_PAUSE_AUTOFILL
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.credential.ListCredentialsActivity
import de.jepfa.yapm.util.DebugInfo
import de.jepfa.yapm.util.getAppNameFromPackage
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
object ResponseFiller {

    const val ACTION_OPEN_VAULT = "openVault"
    const val ACTION_CLOSE_VAULT = "closeVault"
    const val ACTION_EXCLUDE_FROM_AUTOFILL = "excludeFromAutofill"
    const val ACTION_PAUSE_AUTOFILL = "pauseAutofill"

    private const val VIEW_TO_IDENTIFY = "text"
    private val PASSWORD_INDICATORS = listOf("password", "passwd", "passphrase", "pin", "pass phrase", "keyword")
    private val USER_INDICATORS = listOf("user", "account", "email")

    private var inlinePresentationRequest: InlineSuggestionsRequest? = null
    private var inlinePresentationUsageCounter = HashMap<AutofillId, Int>()
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

        fun hasUserField(node: ViewNode) : Boolean
                = userFields.contains(node)

        fun hasPasswordField(node: ViewNode) : Boolean
                = passwordFields.contains(node)

        fun hasPotentialField(node: ViewNode) : Boolean
                = potentialFields.contains(node)

        fun hasFields() : Boolean
                = allFields.isNotEmpty()

        fun getAutofillIds(): Array<AutofillId> {
            return potentialFields.map { it.autofillId }
                .filterNotNull()
                .toTypedArray()
        }
    }

    fun updateInlinePresentationRequest(req: InlineSuggestionsRequest?) {
        inlinePresentationRequest = req
        inlinePresentationUsageCounter.clear()

    }

    fun createFillResponse(
        structure: AssistStructure,
        allowCreateAuthentication : Boolean,
        ignoreCurrentApp: Boolean = false,
        context: Context
    ) : FillResponse? {
        if (structure.isHomeActivity) {
            Log.i("CFS", "home activity")
            return null
        }
        Log.i("CFS", "isDecativatedSince $isDeactivatedSinceWhen")

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
        if (fields.getAllFields().isEmpty()) {
            Log.i("CFS", "No fields to autofill found")
            return null
        }

        if (ignoreCurrentApp) {
            val currentApp = structure.activityComponent.packageName
            Log.i("CFS", "Ignore $currentApp")
            val excludedApps = PreferenceService.getAsStringSet(
                PREF_AUTOFILL_EXCLUSION_LIST, context) ?: emptySet()
            val newExcludedApps = HashSet(excludedApps)
            newExcludedApps.add(currentApp)
            PreferenceService.putStringSet(
                PREF_AUTOFILL_EXCLUSION_LIST, newExcludedApps, context)

            return null
        }

        val key = Session.getMasterKeySK()
        val credential = AutofillCredentialHolder.currentCredential
        if (key == null || Session.isDenied() || credential == null) {
            if (allowCreateAuthentication && fields.hasFields()) {
                return createAuthenticationFillResponse(fields, structure, suggestEverywhere, context)
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

        var firstUserField = fields.getUserFields().firstOrNull()
        var firstPasswordField = fields.getPasswordFields().firstOrNull()

        if (firstUserField != null && firstPasswordField != null) {
            var dataset = createUserAndPasswordDataSet(
                firstUserField,
                firstPasswordField,
                name,
                user,
                password,
                true,
                context
            )

            dataset?.let { dataSets.add(it) }

        }
        else {
            createUserDataSets(fields.getUserFields(), name, user, context)
                .forEach { dataSets.add(it) }
            createPasswordDataSets(fields.getPasswordFields(), name, password, context)
                .forEach { dataSets.add(it) }

            if (suggestEverywhere) {
                createUserDataSets(fields.getPotentialFields(), name, user, context)
                    .forEach { dataSets.add(it) }
                createPasswordDataSets(fields.getPotentialFields(), name, password, context)
                    .forEach { dataSets.add(it) }
            }
        }

        createAuthDataSets(fields.getAllFields(),
            R.drawable.ic_baseline_arrow_back_24_gray,
            context.getString(R.string.go_back_to_app),
            ACTION_OPEN_VAULT,
            true,
            context)
            .forEach { dataSets.add(it) }

        createAuthDataSets(fields.getAllFields(),
            R.drawable.ic_lock_outline_gray_24dp,
            context.getString(R.string.lock_items),
            ACTION_CLOSE_VAULT,
            true,
            context)
            .forEach { dataSets.add(it) }

        if (DebugInfo.isDebug) {
            fields.getAllFields().mapNotNull {
                createDebugDataSet(it, fields, context)
            }.forEach { dataSets.add(it) }
        }


        password.clear()

        if (dataSets.isEmpty()) return null

        val responseBuilder = FillResponse.Builder()
        addHeaderView(responseBuilder, context)

        dataSets.forEach { responseBuilder.addDataset(it) }

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

    private fun createAuthenticationFillResponse(
        fields: Fields,
        structure: AssistStructure,
        suggestEverywhere: Boolean,
        context: Context
    ): FillResponse {
        val responseBuilder = FillResponse.Builder()

        addHeaderView(responseBuilder, context)

        if (Session.isDenied()) {
            createAuthDataSets(fields.getAllFields(),
                R.drawable.ic_lock_open_gray_24dp,
                context.getString(R.string.login_required_first), ACTION_OPEN_VAULT, true, context)
                .forEach { responseBuilder.addDataset(it) }
        }
        else {
            createAuthDataSets(fields.getAllFields(),
                R.drawable.ic_baseline_list_gray_24,
                context.getString(R.string.select_credential_for_autofill), ACTION_OPEN_VAULT, true, context)
                .forEach { responseBuilder.addDataset(it) }
        }

        if (DebugInfo.isDebug) {
            fields.getAllFields().mapNotNull {
                createDebugDataSet(it, fields, context)
            }.forEach { responseBuilder.addDataset(it) }
        }

        createAuthDataSets(fields.getAllFields(), R.drawable.ic_baseline_not_interested_gray_24,
            context.getString(R.string.no_autofill_here,
                getAppNameFromPackage(structure.activityComponent.packageName, context)),
            ACTION_EXCLUDE_FROM_AUTOFILL,
            suggestEverywhere,
            context)
            .forEach { responseBuilder.addDataset(it) }

        val pauseDurationInSec = PreferenceService.getAsString(PreferenceService.PREF_AUTOFILL_DEACTIVATION_DURATION, context)

        if (pauseDurationInSec != null && pauseDurationInSec != "0" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            createAuthDataSets(
                fields.getAllFields(), R.drawable.ic_baseline_pause_gray_24,
                context.getString(R.string.temp_deact_autofill), ACTION_PAUSE_AUTOFILL, false, context
            ).forEach { responseBuilder.addDataset(it) }
        }

        return responseBuilder.build()
    }

    private fun createDebugDataSet(
        it: ViewNode,
        fields: Fields,
        context: Context
    ) = createDataSet(
        it,
        R.drawable.ic_baseline_bug_report_gray_24,
        "aId: ${it.autofillId}, webDomain: ${it.webDomain}, " +
                "aHints: ${Arrays.toString(it.autofillHints)}, hint: ${it.hint}, " +
                "text: ${it.text}, idEntry: ${it.idEntry}, htmlInfoTag: ${it.htmlInfo?.tag}, " +
                "htmlInfoAttr: ${it.htmlInfo?.attributes}, type: ${it.autofillType}, important: ${it.importantForAutofill}, " +
                "class: ${it.className}, isUserField: ${fields.hasUserField(it)}, " +
                "isPasswordField: ${fields.hasPasswordField(it)}, isPotentialField: ${fields.hasPotentialField(it)}",
        "debug", context, false
    )

    private fun createPendingIntent(context: Context, action: String): PendingIntent {
        val authIntent = Intent(context, ListCredentialsActivity::class.java)
        authIntent.putExtra(SecureActivity.SecretChecker.fromAutofill, true)
        authIntent.action = action

        return PendingIntent.getActivity(
            context,
            1001,
            authIntent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
    }

    private fun createAppIntentSender(context: Context, action: String): IntentSender {
        return createPendingIntent(context, action).intentSender
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

        if (structure.windowNodeCount == 0) {
            return null
        }
        for (i in 0..structure.windowNodeCount) {
            val windowNode = structure.getWindowNodeAt(0)
            val viewNode = windowNode?.rootViewNode
            if (viewNode != null) {
                identifyFields(viewNode, fields, suggestEverywhere)
            }
        }

        return fields
    }


    private fun identifyFields(node: ViewNode, fields: Fields, suggestEverywhere: Boolean) {

        if (!suggestEverywhere && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            if (node.importantForAutofill == View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS) {
                // don't traverse anything
                Log.i("CFS", "No autofill for current node and children")
                return
            }
        }

        if (suggestEverywhere || (node.autofillType == View.AUTOFILL_TYPE_TEXT
            && node.importantForAutofill != View.IMPORTANT_FOR_AUTOFILL_NO)) {

            inspectNodeAttributes(node, fields)
            if (suggestEverywhere) {
                fields.addPotentialField(node)
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

    private fun inspectNodeAttributes(
        node: ViewNode,
        fields: Fields
    ) {
        node.text?.let { identifyField(it.toString(), node, fields) }
        node.hint?.let { identifyField(it, node, fields) }
        node.htmlInfo?.let { identifyField(it.tag, node, fields) }
        node.autofillHints?.map { identifyField(it, node, fields) }
        node.htmlInfo?.attributes?.mapNotNull {
            if (it.second != null) {
                identifyField(it.second, node, fields) // this is the value
            }
        }
    }

    private fun identifyField(
        attribute: String,
        node: ViewNode,
        fields: Fields
    ) {

        val attrib = attribute.lowercase()
        if (contains(attrib, USER_INDICATORS)) {
            fields.addUserField(node)
        } else if (contains(attrib, PASSWORD_INDICATORS)) {
            fields.addPasswordField(node)
        }
    }

    private fun contains(s: String, contents: Collection<String>) : Boolean {
        return contents.any { s.contains(it) }
    }

    private fun createUserDataSets(fields : Set<ViewNode>, name: String, user: String, context: Context): List<Dataset> {
        var message = context.getString(R.string.paste_user_for_autofill, name)
        var withInlinePresentation = true
        if (user.isBlank()) {
            message = context.getString(R.string.no_user_for_autofill, name)
            withInlinePresentation = false
        }
        return createDataSets(fields,
            R.drawable.ic_baseline_person_24_gray,
            message,
            user,
            withInlinePresentation,
            context)
    }

    private fun createPasswordDataSets(fields : Set<ViewNode>, name: String, password: Password, context: Context): List<Dataset> {
        return createDataSets(fields,
            R.drawable.ic_baseline_vpn_key_24_gray,
            context.getString(R.string.paste_passwd_for_autofill, name),
            password.toRawFormattedPassword(),
            true,
            context)
    }


    private fun createDataSets(fields : Set<ViewNode>, iconId: Int, text: String, content: CharSequence, withInlinePresentation: Boolean, context: Context): List<Dataset> {
        return fields.mapNotNull { createDataSet(it, iconId, text, content, context, withInlinePresentation) }
    }

    private fun createAuthDataSets(fields : Set<ViewNode>, iconId: Int, text: String, action: String, withInlinePresentation: Boolean, context: Context): List<Dataset> {
        return fields.mapNotNull { createAuthDataSet(it, iconId, text, action, context, withInlinePresentation) }
    }

    private fun createDataSet(
        field: ViewNode,
        iconId : Int,
        text: String,
        content: CharSequence,
        context: Context,
        withInlinePresentation: Boolean
    ): Dataset? {

        val autofillId = field.autofillId ?: return null
        val remoteView = createRemoteView(iconId, text, context)
        val builder = Dataset.Builder(remoteView)

        if (withInlinePresentation) {
            buildInlinePresentation(autofillId, iconId, text, content, "", context, builder)
        }

        return builder
            .setValue(
                autofillId,
                AutofillValue.forText(content))
            .build()
    }

    private fun buildInlinePresentation(
        autofillId: AutofillId,
        iconId: Int,
        text: String,
        content: CharSequence,
        action: String,
        context: Context,
        builder: Dataset.Builder
    ) {
        val showInlinePresentation = PreferenceService.getAsBool(PREF_AUTOFILL_INLINE_PRESENTATIONS, context)

        if (showInlinePresentation && isInlinePresentationSupported()) {
            val inlinePresentation = createInlinePresentation(autofillId, iconId, text, content, action, context)
            if (inlinePresentation != null) {
                builder
                    .setInlinePresentation(inlinePresentation)

            }
        }
    }

    fun isInlinePresentationSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    private fun createInlinePresentation(
        autofillId: AutofillId,
        iconId: Int,
        text: String,
        content: CharSequence,
        action: String,
        context: Context
    ): InlinePresentation? {

        val specs = inlinePresentationRequest?.inlinePresentationSpecs ?: return null

        val lastValue = inlinePresentationUsageCounter.getOrDefault(autofillId, 0)
        if (specs.count() <= lastValue) {
            return null
        }
        val spec = specs[lastValue]
        val slice = SliceCreator.createSlice(spec, text, "",
            Icon.createWithResource(context, iconId),
            Icon.createWithResource(context, R.mipmap.ic_launcher_round),
            "ANOTHERpass",
            createPendingIntent(context, action)

        ) ?: return null
        inlinePresentationUsageCounter.put(autofillId, lastValue + 1)

        return InlinePresentation(slice, spec, false)
    }





    private fun createUserAndPasswordDataSet(
        userField: ViewNode,
        passwordField: ViewNode,
        credentialName: String,
        user: CharSequence,
        password: CharSequence,
        withInlinePresentation: Boolean,
        context: Context
    ): Dataset? {

        val userAutofillId = userField.autofillId ?: return null
        val passwordAutofillId = passwordField.autofillId ?: return null
        val text = context.getString(R.string.paste_credential_for_autofill, credentialName)
        val remoteView = createUserPasswordRemoteView(
            text,
            context)
        val builder = Dataset.Builder(remoteView)
        if (withInlinePresentation) {
            buildInlinePresentation(userAutofillId, R.drawable.ic_baseline_person_24_gray, text, user, "", context, builder)
            buildInlinePresentation(passwordAutofillId, R.drawable.ic_baseline_vpn_key_24_gray, text, password, "", context, builder)
        }

        return builder
            .setValue(
                userAutofillId,
                AutofillValue.forText(user)
            ).setValue(
                passwordAutofillId,
                AutofillValue.forText(password)
            ).build()
    }

    private fun createAuthDataSet(
        field: ViewNode,
        iconId : Int,
        text: String,
        action: String,
        context: Context,
        withInlinePresentation: Boolean
    ): Dataset? {
        val autofillId = field.autofillId ?: return null

        val remoteView = createRemoteView(iconId, text, context)
        val intentSender = createAppIntentSender(context, action)
        val builder = Dataset.Builder(remoteView)

        if (withInlinePresentation) {
            buildInlinePresentation(autofillId, iconId, text, "", action, context, builder)
        }

        return builder
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

    private fun createUserPasswordRemoteView(text: String, context: Context): RemoteViews {
        val remoteView = RemoteViews(
            context.packageName,
            R.layout.content_autofill_user_password_view
        )

        remoteView.setTextViewText(R.id.autofill_item, text)
        return remoteView
    }


    private fun getExcludedAppList(context: Context): List<String> {
        val apps = PreferenceService.getAsStringSet(PREF_AUTOFILL_EXCLUSION_LIST, context)
        return apps?.toList() ?: emptyList()
    }

}