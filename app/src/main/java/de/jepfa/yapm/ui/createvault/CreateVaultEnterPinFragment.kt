package de.jepfa.yapm.ui.createvault

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_PIN
import de.jepfa.yapm.util.Constants

class CreateVaultEnterPinFragment : BaseFragment() {


    init {
        enableBack = true
        backToPreviousFragment = true
    }

    private var showNumberPad = false

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_enter_pin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val createVaultActivity = getBaseActivity() as CreateVaultActivity


        setTitle(R.string.create_vault_enter_pin_fragment_label)

        val pin1TextView: EditText = view.findViewById(R.id.first_pin)
        val pin2TextView: EditText = view.findViewById(R.id.second_pin)

        val changeImeiButton = view.findViewById<ImageView>(R.id.imageview_change_imei)
        changeImeiButton.setOnClickListener {
            showNumberPad = !showNumberPad
            if (showNumberPad) {
                pin1TextView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                pin2TextView.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                changeImeiButton.setImageDrawable(createVaultActivity.getDrawable(R.drawable.baseline_abc_24))
            } else {
                pin1TextView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                pin2TextView.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                changeImeiButton.setImageDrawable(createVaultActivity.getDrawable(R.drawable.baseline_123_24))
            }
            pin1TextView.typeface = Typeface.DEFAULT
            pin2TextView.typeface = Typeface.DEFAULT

            pin1TextView.requestFocus()

            val imm = createVaultActivity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(pin1TextView, 0)
            imm.showSoftInput(pin2TextView, 0)

            PreferenceService.putBoolean(
                PreferenceService.PREF_SHOW_NUMBER_PAD_FOR_PIN,
                showNumberPad,
                createVaultActivity
            )
        }

        view.findViewById<Button>(R.id.button_next).setOnClickListener {

            val pin1 = Password(pin1TextView.text)
            val pin2 = Password(pin2TextView.text)

            if (pin1.isEmpty()) {
                pin1TextView.error = getString(R.string.pin_required)
                pin1TextView.requestFocus()
            }
            else if (pin1.length < Constants.MIN_PIN_LENGTH) {
                pin1TextView.error = getString(R.string.pin_too_short)
                pin1TextView.requestFocus()
            }
            else if (! pin1.isEqual(pin2)) {
                pin2TextView.error = getString(R.string.pin_not_equal)
                pin2TextView.requestFocus()
            }
            else {
                val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT, view.context)
                val encPin = encryptPassword(transSK, pin1)
                val encMasterPasswd = arguments?.getString(ARG_ENC_MASTER_PASSWD)
                val args = Bundle()
                args.putString(ARG_ENC_MASTER_PASSWD, encMasterPasswd)
                args.putString(ARG_ENC_PIN, encPin.toBase64String())

                findNavController().navigate(R.id.action_Create_Vault_SecondFragment_to_ThirdFragment, args)
            }

            pin1.clear()
            pin2.clear()
        }
    }

}