package de.jepfa.yapm.service.autofill

import android.app.assist.AssistStructure
import android.app.assist.AssistStructure.ViewNode
import android.os.CancellationSignal
import android.service.autofill.*
import android.util.Log
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Password
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.secret.SecretService


class CredentialFillService: AutofillService() {

    private val VIEW_TO_IDENTIFY = "EditText"
    private val PASSWORD_INDICATORS = listOf("password", "passwd", "passphrase", "pin", "pass")
    private val USER_INDICATORS = listOf("user", "account", "email")

    private data class Fields(
        val userFields: MutableList<ViewNode>,
        val passwordFields: MutableList<ViewNode>,
        val potentialFields: MutableList<ViewNode>
    ) {

        fun hasCredentialFields() : Boolean
                = !userFields.isNullOrEmpty() || !passwordFields.isNullOrEmpty()
    }

    override fun onFillRequest(
        fillRequest: FillRequest,
        cancellationSignal: CancellationSignal,
        fillCallback: FillCallback
    ) {

        val contexts = fillRequest.fillContexts
        val structure = contexts.get(contexts.size - 1).getStructure()

        if (structure.isHomeActivity) {
            Log.i("CFS", "home activity")
            return
        }

        if (structure.activityComponent?.packageName.equals(packageName)) {
            Log.i("CFS", "myself")
            return
        }

        val fields = identifyFields(structure, cancellationSignal) ?: return

        val key = Session.getMasterKeySK()
        if (key == null || Session.isDenied()) {
            if (fields.hasCredentialFields()) {
                fillCallback.onFailure("${getString(R.string.app_name)}: You need to login to your vault first.")
            }
            return
        }

        val credential = CurrentCredentialHolder.currentCredential
        if (credential == null) {
            if (fields.hasCredentialFields()) {
                fillCallback.onFailure("${getString(R.string.app_name)}: Open the credential to use first.")
            }
            return
        }

        val name = SecretService.decryptCommonString(key, credential.name)
        val user = SecretService.decryptCommonString(key, credential.user)
        val password = SecretService.decryptPassword(key, credential.password)


        val dataSets = ArrayList<Dataset>()
        if (user.isNotBlank()) {
            createUserDataSets(fields.userFields, name, user)
                .forEach { dataSets.add(it) }

            createUserDataSets(fields.potentialFields, name, user)
                .forEach { dataSets.add(it) }
        }

        createPasswordDataSets(fields.passwordFields, name, password)
            .forEach { dataSets.add(it) }
        createPasswordDataSets(fields.potentialFields, name, password)
            .forEach { dataSets.add(it) }


        if (dataSets.isNotEmpty()) {
            val responseBuilder = FillResponse.Builder()

            dataSets.forEach { responseBuilder.addDataset(it) }

            fillCallback.onSuccess(responseBuilder.build())
        }

        password.clear()

    }

    override fun onSaveRequest(saveRequest: SaveRequest, saveCallback: SaveCallback) {
    }


    private fun identifyFields(structure: AssistStructure, cancellationSignal: CancellationSignal): Fields? {

        val userFields = ArrayList<ViewNode>()
        val passwordFields = ArrayList<ViewNode>()
        val potentialFields = ArrayList<ViewNode>()

        val fields = Fields(userFields, passwordFields, potentialFields)

        val windowNode = structure.getWindowNodeAt(0) ?: return null
        val viewNode = windowNode.rootViewNode ?: return null

        identifyFields(viewNode, fields, cancellationSignal)

        return fields

    }


    private fun identifyFields(node: ViewNode, fields: Fields, cancellationSignal: CancellationSignal) {
        if (cancellationSignal.isCanceled) {
            return
        }
        if (node.className != null) {
            val viewId = node.idEntry?.toLowerCase()
            if (viewId != null &&  node.className.contains(VIEW_TO_IDENTIFY)) {
                if (contains(viewId, USER_INDICATORS)) {
                    fields.userFields.add(node)
                }
                else if (contains(viewId, PASSWORD_INDICATORS)) {
                    fields.passwordFields.add(node)
                }
            }
        }

        fields.potentialFields.add(node)


        // go deeper in the tree
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child != null) {
                identifyFields(child, fields, cancellationSignal)
            }
        }
    }

    private fun contains(s: String, contents: Collection<String>) : Boolean {
        return contents.filter { s.contains(it) }.any()
    }

    private fun createUserDataSets(fields : List<ViewNode>, name: String, user: String): List<Dataset> {
        return createDataSets(fields, R.drawable.ic_baseline_person_24,"Paste user for '$name'", user)
    }

    private fun createPasswordDataSets(fields : List<ViewNode>, name: String, password: Password): List<Dataset> {
        return createDataSets(fields, R.drawable.ic_baseline_vpn_key_24, "Paste password for '$name'", password.toString())
    }

    private fun createDataSets(fields : List<ViewNode>, iconId: Int, text: String, content: String): List<Dataset> {
        return fields
            .map { createDataSet(it, iconId, text, content) }
            .filterNotNull()
    }

    private fun createDataSet(
        field: ViewNode,
        iconId : Int,
        text: String,
        content: String
    ): Dataset? {

        val autofillId = field.autofillId ?: return null
        val remoteView = RemoteViews(
            packageName,
            R.layout.content_autofill_view
        )

        remoteView.setImageViewResource(R.id.autofill_image, iconId)
        remoteView.setTextViewText(R.id.autofill_item, text)

        return Dataset.Builder(remoteView)
            .setValue(
                autofillId,
                AutofillValue.forText(content)
            ).build()
    }

}