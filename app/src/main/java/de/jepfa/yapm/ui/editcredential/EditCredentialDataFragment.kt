@file:Suppress("KotlinDeprecation")

package de.jepfa.yapm.ui.editcredential

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.net.HttpCredentialRequestHandler
import de.jepfa.yapm.service.net.RequestFlows
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.decryptCommonString
import de.jepfa.yapm.service.secret.SecretService.decryptLong
import de.jepfa.yapm.service.secret.SecretService.encryptCommonString
import de.jepfa.yapm.service.secret.SecretService.encryptLong
import de.jepfa.yapm.service.usernametemplate.UsernameTemplateService
import de.jepfa.yapm.ui.DropDownList
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.SecureFragment
import de.jepfa.yapm.ui.label.LabelEditViewExtender
import de.jepfa.yapm.usecase.vault.LockVaultUseCase
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.util.*


class EditCredentialDataFragment : SecureFragment() {

    private enum class ExpiryOptions(val representationId: Int) {
        EXPIRES_IN_EXACT(R.string.nothing_placeholder),
        EXPIRES_IN_A_MONTH(R.string.expires_in_1_month),
        EXPIRES_IN_3_MONTHS(R.string.expires_in_3_months),
        EXPIRES_IN_6_MONTHS(R.string.expires_in_6_months),
        EXPIRES_IN_12_MONTHS(R.string.expires_in_12_months),
        EXPIRES_ON_CUSTOM(R.string.expires_on),
    }

    private var plainUsernameTemplates: List<EncUsernameTemplate> = emptyList()
    private lateinit var editCredentialActivity: EditCredentialActivity
    private lateinit var labelEditViewExtender: LabelEditViewExtender

    private lateinit var editCredentialNameView: EditText
    private lateinit var editCredentialUserView: AutoCompleteTextView
    private lateinit var editCredentialWebsiteView: EditText
    private lateinit var editCredentialChooseExpiredAtImageView: ImageView
    private lateinit var editCredentialRemoveExpiredAtImageView: ImageView
    private lateinit var editCredentialExpiredAtSpinner: DropDownList
    private lateinit var editCredentialExpiredAtAdapter: ArrayAdapter<String>
    private var selectedExpiryDate: Date? = null

    private lateinit var editCredentialAdditionalInfoView: EditText
    private lateinit var expandAdditionalInfoImageView: ImageView

    init {
        enableBack = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (Session.isDenied()) {
            getSecureActivity()?.let { LockVaultUseCase.execute(it) }
            return null
        }
        return inflater.inflate(R.layout.fragment_edit_credential_data, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        editCredentialActivity = getBaseActivity() as EditCredentialActivity

        editCredentialNameView = view.findViewById(R.id.edit_credential_name)
        editCredentialUserView = view.findViewById(R.id.edit_credential_user)
        editCredentialWebsiteView = view.findViewById(R.id.edit_credential_website)
        editCredentialChooseExpiredAtImageView = view.findViewById(R.id.expired_at_imageview)
        editCredentialRemoveExpiredAtImageView = view.findViewById(R.id.remove_expired_at_imageview)
        editCredentialExpiredAtSpinner = view.findViewById(R.id.expired_at_spinner)
        editCredentialAdditionalInfoView = view.findViewById(R.id.edit_credential_additional_info)
        expandAdditionalInfoImageView = view.findViewById(R.id.imageview_expand_additional_info)

        editCredentialNameView.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                updateUsernameTemplateSuggestions()
            }
        }

        editCredentialActivity.usernameTemplateViewModel.allUsernameTemplates.observe(editCredentialActivity) { usernameTemplates ->
            plainUsernameTemplates = usernameTemplates
            updateUsernameTemplateSuggestions()
        }

        view.findViewById<ImageView>(R.id.icon_credential_user)?.let {
            it.setOnClickListener {
                updateUsernameTemplateSuggestions()
                editCredentialUserView.showDropDown()
            }
        }

        editCredentialExpiredAtAdapter = ArrayAdapter(editCredentialActivity, android.R.layout.simple_spinner_dropdown_item, mutableListOf<String>())
        editCredentialExpiredAtSpinner.adapter = editCredentialExpiredAtAdapter
        updateExpiredAtAdapter(null, editCredentialActivity)

        editCredentialChooseExpiredAtImageView.setOnClickListener {
            selectExpiryDate()
        }
        editCredentialRemoveExpiredAtImageView.setOnClickListener {
            updateExpiredAtAdapter(null, editCredentialActivity)
        }
        editCredentialExpiredAtSpinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                Log.d(LOG_PREFIX + "EXP", "pos=$position id=$id selectedExpiryDate=$selectedExpiryDate")

                if (position == ExpiryOptions.EXPIRES_IN_EXACT.ordinal) {
                    // explicitly nothing!
                }
                else if (position == ExpiryOptions.EXPIRES_IN_A_MONTH.ordinal) {
                    updateExpiredAtAdapter(Date().addMonths(1), editCredentialActivity)
                }
                else if (position == ExpiryOptions.EXPIRES_IN_3_MONTHS.ordinal) {
                    updateExpiredAtAdapter(Date().addMonths(3), editCredentialActivity)
                }
                else if (position == ExpiryOptions.EXPIRES_IN_6_MONTHS.ordinal) {
                    updateExpiredAtAdapter(Date().addMonths(6), editCredentialActivity)
                }
                else if (position == ExpiryOptions.EXPIRES_IN_12_MONTHS.ordinal) {
                    updateExpiredAtAdapter(Date().addMonths(12), editCredentialActivity)
                }
                else if (position == ExpiryOptions.EXPIRES_ON_CUSTOM.ordinal) {
                    selectExpiryDate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        editCredentialAdditionalInfoView.addTextChangedListener {
            if (it == null) return@addTextChangedListener
            updateExpandAddInfoVisibility(expandAdditionalInfoImageView, it)
        }
        expandAdditionalInfoImageView.setOnClickListener {
            if (editCredentialAdditionalInfoView.maxLines == R.integer.max_credential_additional_info_length) {
                editCredentialAdditionalInfoView.maxLines = 3
                expandAdditionalInfoImageView.setImageDrawable(editCredentialActivity.getDrawable(R.drawable.ic_baseline_expand_more_24))
            }
            else {
                editCredentialAdditionalInfoView.maxLines = R.integer.max_credential_additional_info_length
                expandAdditionalInfoImageView.setImageDrawable(editCredentialActivity.getDrawable(R.drawable.ic_baseline_expand_less_24))
            }
        }

        val explanationView: TextView = view.findViewById(R.id.edit_credential_explanation)
        explanationView.setOnLongClickListener {
            DebugInfo.toggleDebug()
            toastText(
                getBaseActivity(),
                "Debug mode " + if (DebugInfo.isDebug) "ON" else "OFF"
            )
            true
        }

        labelEditViewExtender = LabelEditViewExtender(editCredentialActivity, view)

        //fill UI
        val current = editCredentialActivity.current

        if (current != null) {
            editCredentialActivity.hideKeyboard(editCredentialNameView)

            masterSecretKey?.let{ key ->
                fillUi(key, current)
            }
        }
        else if (editCredentialActivity.isUpdate()) {
            editCredentialActivity.hideKeyboard(editCredentialNameView)

            editCredentialActivity.load().observe(editCredentialActivity) { orig ->
                editCredentialActivity.original = orig
                masterSecretKey?.let { key ->
                    editCredentialActivity.updateTitle(orig)
                    fillUi(key, orig)
                }
            }
        }
        else {
            if (editCredentialActivity.suggestedCredentialName != null) {
                //TODO check if the name already exists and add (1) if so
                masterSecretKey?.let { key ->
                    editCredentialActivity.credentialViewModel.allCredentials.observeOnce(editCredentialActivity) { credentials ->

                        var suggestedName =
                            editCredentialActivity.suggestedCredentialName!!.capitalize()

                        var nameCounter = 0
                        var maxNumber = 0
                        // e.g. if given (1) and (2) and (1) is deleted, we don't want to get (2) again. Therefor i added maxNumber.
                        val regex = Regex("\\Q${suggestedName.lowercase()}\\E \\((\\d+)\\)")  // one capturing group to get the number in the brackets

                        credentials.forEach { credential ->
                            val name = decryptCommonString(key, credential.name).lowercase().trim()
                            if (name == suggestedName.lowercase() || regex.matches(name)) {
                                val groups = regex.find(name)?.groups
                                if (!groups.isNullOrEmpty()) {
                                    val value = groups[1]?.value?.toIntOrNull()
                                    if (value != null && value > maxNumber) {
                                        maxNumber = value + 1
                                    }
                                }
                                nameCounter++
                            }
                        }
                        val counter = Math.max(nameCounter, maxNumber)
                        if (counter > 0) {
                            suggestedName = "$suggestedName ($counter)"
                        }
                        editCredentialNameView.setText(suggestedName)
                    }
                }

            }
            else {
                editCredentialNameView.requestFocus()
            }

            editCredentialActivity.suggestedWebSite?.let {
                editCredentialWebsiteView.setText(it)
            }
            editCredentialActivity.suggestedUser?.let {
                editCredentialUserView.setText(it)
            }
        }

        val buttonNext: Button = view.findViewById(R.id.button_next)
        buttonNext.setOnClickListener {

            labelEditViewExtender.commitStaleInput()
            buttonNext.isActivated = false
            buttonNext.postDelayed( { // delayed is a hack to give the event loop time to recognise added chips
                val now = Date()
                if (selectedExpiryDate?.after(now) == false) {
                    toastText(editCredentialActivity, R.string.error_expired_in_the_past)
                    editCredentialExpiredAtSpinner.requestFocus()
                }
                else if (TextUtils.isEmpty(editCredentialNameView.text)) {
                    editCredentialNameView.error = getString(R.string.error_field_required)
                    editCredentialNameView.requestFocus()
                }
                else {
                    masterSecretKey?.let{ key ->
                        saveCurrentUiData(key)

                        findNavController().navigate(R.id.action_EditCredential_DataFragment_to_PasswordFragment)
                    }
                }
            }, 100L)
        }
    }

    private fun updateUsernameTemplateSuggestions() {
        masterSecretKey?.let { key ->
            val list = UsernameTemplateService.getUsernamesWithGeneratedAliases(
                key,
                plainUsernameTemplates,
                editCredentialNameView.text.toString()
            )
            editCredentialUserView.setAdapter(
                ArrayAdapter(
                    editCredentialActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    list
                )
            )
        }
    }

    private fun updateExpiredAtAdapter(expiryDate: Date?, context: Context) {
        editCredentialExpiredAtAdapter.clear()
        editCredentialExpiredAtAdapter.addAll(
            context.getString(ExpiryOptions.EXPIRES_IN_A_MONTH.representationId),
            context.getString(ExpiryOptions.EXPIRES_IN_3_MONTHS.representationId),
            context.getString(ExpiryOptions.EXPIRES_IN_6_MONTHS.representationId),
            context.getString(ExpiryOptions.EXPIRES_IN_12_MONTHS.representationId),
            context.getString(ExpiryOptions.EXPIRES_ON_CUSTOM.representationId),
        )

        val color: Int?
        val text: String
        if (expiryDate == null) {
            color = null
            text = context.getString(R.string.no_expiration)
        }
        else {
            val now = Date()
            if (expiryDate.after(now)) {
                // good
                text = "${getString(R.string.expires)}: ${dateToNiceString(expiryDate, editCredentialActivity)}"
                color = null
            }
            else {
                // expired
                text = "${getString(R.string.expired_since)}: ${dateToNiceString(expiryDate, editCredentialActivity, withPreposition = false)} "
                color = editCredentialActivity.getColor(R.color.Red)

            }

        }

        editCredentialExpiredAtAdapter.insert(text, 0)
        if (color == null) {
            editCredentialChooseExpiredAtImageView.colorFilter = null
        }
        else {
            editCredentialChooseExpiredAtImageView.setColorFilter(color)
        }

        selectedExpiryDate = expiryDate?.removeTime()
        editCredentialExpiredAtSpinner.setSelection(0)

        editCredentialRemoveExpiredAtImageView.visibility =
            if (selectedExpiryDate != null) View.VISIBLE else View.GONE

        editCredentialExpiredAtAdapter.notifyDataSetChanged()
    }

    private fun selectExpiryDate() {

        val c = Calendar.getInstance()
        selectedExpiryDate?.let { c.time = it }
        val mYear = c.get(Calendar.YEAR)
        val mMonth = c.get(Calendar.MONTH)
        val mDay = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            editCredentialActivity,
            { _, year, monthOfYear, dayOfMonth ->
                val c = Calendar.getInstance()
                c.set(year, monthOfYear, dayOfMonth)
                updateExpiredAtAdapter(c.time, editCredentialActivity)

            }, mYear, mMonth, mDay
        )
        datePickerDialog.show()
    }

    private fun updateExpandAddInfoVisibility(
        expandAdditionalInfoImageView: ImageView,
        charSequence: CharSequence
    ) {
        expandAdditionalInfoImageView.visibility =
            if (charSequence.lines().count() > 3) View.VISIBLE else View.INVISIBLE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (isVisible) {
            masterSecretKey?.let { key ->
                saveCurrentUiData(key)
            }
        }
    }

    private fun fillUi(
        key: SecretKeyHolder,
        current: EncCredential
    ) {
        val name = decryptCommonString(key, current.name)
        val user = decryptCommonString(key, current.user)
        val website = decryptCommonString(key, current.website)
        val expiresAtAsLong = decryptLong(key, current.expiresAt)
        val additionalInfo = decryptCommonString(
            key,
            current.additionalInfo
        )


        editCredentialNameView.setText(name)
        editCredentialUserView.setText(user)
        editCredentialWebsiteView.setText(website)

        val expiresAt = if (expiresAtAsLong != null && expiresAtAsLong > 0) Date(expiresAtAsLong) else null
        updateExpiredAtAdapter(expiresAt, editCredentialActivity)

        editCredentialAdditionalInfoView.setText(additionalInfo)
        updateExpandAddInfoVisibility(expandAdditionalInfoImageView, additionalInfo)

        val allLabelsForCredential = LabelService.defaultHolder.decryptLabelsForCredential(key, current)

        labelEditViewExtender.addPersistedLabels(allLabelsForCredential)


    }

    private fun saveCurrentUiData(
        key: SecretKeyHolder
    ) {
        val name = editCredentialNameView.text.toString().trim()
        val additionalInfo = editCredentialAdditionalInfoView.text.toString()
        val user = editCredentialUserView.text.toString().trim()
        val website = editCredentialWebsiteView.text.toString().trim()
        val expiresAt = selectedExpiryDate?.time

        val encName = encryptCommonString(key, name)
        val encAdditionalInfo = encryptCommonString(key, additionalInfo)
        val encUser = encryptCommonString(key, user)
        val encPassword = editCredentialActivity.current?.password
            ?: editCredentialActivity.original?.password
            ?: SecretService.encryptPassword(key, Password.empty()
        )
        val encWebsite = encryptCommonString(key, website)
        val encExpiresAt = encryptLong(key, expiresAt ?: 0L)
        val encLabels = LabelService.defaultHolder.encryptLabelIds(
            key,
            labelEditViewExtender.getCommittedLabelNames()
        )

        // we create the new credential out of a former current if present or else out of the original if present
        val credentialToSave = EncCredential(
            editCredentialActivity.currentId,
            editCredentialActivity.original?.uid,
            encName,
            encAdditionalInfo,
            encUser,
            encPassword,
            editCredentialActivity.original?.lastPassword,
            encWebsite,
            encLabels,
            encExpiresAt,
            editCredentialActivity.current?.isObfuscated
                ?: editCredentialActivity.original?.isObfuscated
                ?: false,
            editCredentialActivity.original?.isLastPasswordObfuscated
                ?: false,
            null
        )
        editCredentialActivity.current = credentialToSave
    }


}