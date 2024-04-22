package de.jepfa.yapm.ui.login

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.LoginData
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_LAST_LOGIN_STATE
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_DENIED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_LOGIN_SUCCEEDED_AT
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_ATTEMPTS
import de.jepfa.yapm.service.PreferenceService.STATE_PREVIOUS_LOGIN_SUCCEEDED_AT
import de.jepfa.yapm.service.secret.AndroidKey
import de.jepfa.yapm.service.secret.MasterPasswordService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.UseCaseBackgroundLauncher
import de.jepfa.yapm.ui.createvault.CreateVaultActivity
import de.jepfa.yapm.usecase.session.LoginUseCase
import de.jepfa.yapm.util.dateTimeToNiceString
import de.jepfa.yapm.util.putEncrypted
import de.jepfa.yapm.util.toastText


class LoginEnterPinFragment : BaseFragment() {

    private var showNumberPad = false

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login_enter_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, null)

        val loginActivity = getBaseActivity() as LoginActivity
        loginActivity.showTagDetectedMessage = true

        val pinTextView: EditText = view.findViewById(R.id.edittext_enter_pin)
        val nextButton = view.findViewById<Button>(R.id.button_login_next)

        val changeImeiButton = view.findViewById<ImageView>(R.id.imageview_change_imei)
        val hideNumberPad = PreferenceService.getAsBool(PreferenceService.PREF_HIDE_NUMBER_PAD_FOR_PIN, activity)
        if (hideNumberPad) {
            changeImeiButton.visibility = View.GONE
        }
        else {
            changeImeiButton.setOnClickListener {
                showNumberPad = !showNumberPad
                updateShowNumberPad(pinTextView, changeImeiButton, loginActivity)
                PreferenceService.putBoolean(
                    PreferenceService.PREF_SHOW_NUMBER_PAD_FOR_PIN,
                    showNumberPad,
                    activity
                )
            }
        }

        updateInfoText()

        // this is to perform next step out of the keyboard
        pinTextView.imeOptions = EditorInfo.IME_ACTION_DONE
        pinTextView.setOnEditorActionListener{ _, _, _ ->
            nextButton.performClick()
            true
        }

        if (!hideNumberPad) {
            showNumberPad = PreferenceService.getAsBool(PreferenceService.PREF_SHOW_NUMBER_PAD_FOR_PIN, activity)
            updateShowNumberPad(pinTextView, changeImeiButton, loginActivity)
        }
        pinTextView.requestFocus()

        nextButton.setOnLongClickListener{
            loginActivity.showRevokeQuickAccessDialog()
            true
        }

        nextButton.setOnClickListener {

            val keyForTemp = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, view.context)

            val userPin = Password(pinTextView.text)
            if (userPin.isEmpty()) {
                pinTextView.error = getString(R.string.pin_required)
                pinTextView.requestFocus()

                return@setOnClickListener
            }

            pinTextView.text = null
            val scannedNdefTag = loginActivity.ndefTag?.data

            if (!Session.isLoggedOut()) {
                val encMasterPasswd = Session.getEncMasterPasswd()
                if (encMasterPasswd == null) {
                    toastText(context, R.string.something_went_wrong)
                    return@setOnClickListener
                }
                val masterPasswd = SecretService.decryptPassword(keyForTemp, encMasterPasswd)

                login(pinTextView, userPin, masterPasswd, loginActivity)
            }
            else if (scannedNdefTag != null) {
                loginActivity.readMasterPassword(scannedNdefTag, false)
                { masterPasswd ->
                    masterPasswd?.let {
                        login(pinTextView, userPin, masterPasswd, loginActivity)
                    }
                }
            }
            else {
                MasterPasswordService.getMasterPasswordFromStore(
                    loginActivity, { masterPasswd ->
                        login(pinTextView, userPin, masterPasswd, loginActivity)
                    }
                    , {
                        try {
                            moveToEnterMasterPasswordScreen(keyForTemp, userPin)
                        } catch (e: SecretService.KeyStoreNotReadyException) {
                            toastText(it.context, R.string.keystore_not_ready)
                        }
                    }
                )
            }
        }
    }

    private fun updateShowNumberPad(
        pinTextView: EditText,
        changeImeiButton: ImageView,
        loginActivity: LoginActivity
    ) {
        if (showNumberPad) {
            pinTextView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            changeImeiButton.setImageDrawable(loginActivity.getDrawable(R.drawable.baseline_abc_24))
        } else {
            pinTextView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            changeImeiButton.setImageDrawable(loginActivity.getDrawable(R.drawable.baseline_123_24))
        }
        pinTextView.typeface = Typeface.DEFAULT
        val imm = loginActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(pinTextView, 0)
    }

    private fun moveToEnterMasterPasswordScreen(
        keyForTemp: SecretKeyHolder,
        userPin: Password
    ) {
        val encUserPin = SecretService.encryptPassword(keyForTemp, userPin)
        val args = Bundle()
        args.putEncrypted(CreateVaultActivity.ARG_ENC_PIN, encUserPin)

        findNavController().navigate(
            R.id.action_Login_PinFragment_to_MasterPasswordFragment,
            args
        )
    }

    private fun updateInfoText() {
        view?.let { view ->
            val context = view.context
            val showPreviousLogins = PreferenceService.getAsInt(PREF_SHOW_LAST_LOGIN_STATE, context)

            val lastLoginDeniedAt = PreferenceService.getAsDate(STATE_LOGIN_DENIED_AT, context)
            val lastLoginSucceededAt =
                PreferenceService.getAsDate(STATE_LOGIN_SUCCEEDED_AT, context)
            val previousLoginSucceededAt =
                PreferenceService.getAsDate(STATE_PREVIOUS_LOGIN_SUCCEEDED_AT, context)
            if (showPreviousLogins > 0
                && lastLoginDeniedAt != null && lastLoginSucceededAt != null
                && lastLoginDeniedAt.after(lastLoginSucceededAt)) {
                val lastLoginDeniedAttempts =
                    PreferenceService.getAsInt(STATE_PREVIOUS_LOGIN_ATTEMPTS, context)

                val span = SpannableString(getString(R.string.last_succeeded_login,
                    dateTimeToNiceString(lastLoginDeniedAt, context), lastLoginDeniedAttempts))

                span.setSpan(ForegroundColorSpan(context.getColor(R.color.colorAltAccent)),
                    0, span.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                Snackbar.make(
                    view,
                    span,
                    1_200_000
                ).show()

            }
            else if (showPreviousLogins == 2 && previousLoginSucceededAt != null) {
                Snackbar.make(
                    view,

                    getString(R.string.last_denied_login_attempt, dateTimeToNiceString(previousLoginSucceededAt, context)),
                    7_000
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateInfoText()
    }

    private fun login(
        pinTextView: TextView,
        userPin: Password,
        masterPasswd: Password,
        loginActivity: LoginActivity
    ) {

        loginActivity.getProgressBar()?.let {

            UseCaseBackgroundLauncher(LoginUseCase)
                .launch(loginActivity, LoginData(userPin, masterPasswd))
                { output ->
                    if (!output.success) {
                        loginActivity.handleFailedLoginAttempt()
                        pinTextView.error = "${getString(R.string.password_wrong)} ${loginActivity.getLoginAttemptMessage()}"
                        pinTextView.requestFocus()
                    } else {
                        userPin.clear()
                        masterPasswd.clear()
                        pinTextView.text = ""

                        loginActivity.loginSuccessful()
                    }
                }
        }
    }
}