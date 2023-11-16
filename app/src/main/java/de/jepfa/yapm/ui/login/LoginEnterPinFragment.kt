package de.jepfa.yapm.ui.login

import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
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

        updateInfoText()

        // this is to perform next step out of the keyboard
        pinTextView.imeOptions = EditorInfo.IME_ACTION_DONE
        pinTextView.setOnEditorActionListener{ _, _, _ ->
            nextButton.performClick()
            true
        }

        pinTextView.requestFocus()

        nextButton.setOnLongClickListener{
            loginActivity.showRevokeQuickAccessDialog()
            true
        }

        nextButton.setOnClickListener {

            var keyForTemp = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, view.context)

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
                        } catch (e: UserNotAuthenticatedException) {
                            // this is a hack: since this exception might happen to user-authentication-free keys as well i try to recreate thme here
                            SecretService.removeAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT)
                            val keyForTemp = SecretService.getAndroidSecretKey(AndroidKey.ALIAS_KEY_TRANSPORT, view.context)

                            try {
                                moveToEnterMasterPasswordScreen(keyForTemp, userPin)
                            } catch (e: UserNotAuthenticatedException) {
                                // toastText(loginActivity, "User not authenticated, lock and unlock your phone")
                            }
                        }
                    }
                )
            }
        }
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