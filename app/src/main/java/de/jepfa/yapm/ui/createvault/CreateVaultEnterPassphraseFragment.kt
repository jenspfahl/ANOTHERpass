package de.jepfa.yapm.ui.createvault

import android.app.Activity.RESULT_OK
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import androidx.navigation.fragment.findNavController
import de.jepfa.yapm.R
import de.jepfa.yapm.model.secret.Key
import de.jepfa.yapm.model.secret.Password
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.secret.AndroidKey.ALIAS_KEY_TRANSPORT
import de.jepfa.yapm.service.secret.SaltService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.secret.SecretService.encryptPassword
import de.jepfa.yapm.service.secret.SecretService.getAndroidSecretKey
import de.jepfa.yapm.ui.BaseFragment
import de.jepfa.yapm.ui.createvault.CreateVaultActivity.Companion.ARG_ENC_MASTER_PASSWD
import de.jepfa.yapm.usecase.secret.GenerateMasterPasswordUseCase
import de.jepfa.yapm.util.*
import java.nio.ByteBuffer


class CreateVaultEnterPassphraseFragment : BaseFragment() {

    private val REQUEST_IMAGE_CAPTURE = 18353

    private var manuallySeedView: TextView? = null
    private var generatedPassword: Password = Password.empty()

    init {
        enableBack = true
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_create_vault_enter_passphrase, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setTitle(R.string.create_vault_enter_passphrase_fragment_label)

        val pseudoPhraseSwitch: SwitchCompat = view.findViewById(R.id.switch_use_pseudo_phrase)
        val generatedPasswdView: TextView = view.findViewById(R.id.generated_passwd)

        manuallySeedView = view.findViewById(R.id.button_seed_manually)
        val hasCamera = requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val hasCameraPermission = PermissionChecker.hasCameraPermission(requireContext())
        if (!hasCamera || !hasCameraPermission) {
            manuallySeedView?.visibility = View.GONE
        }
        else {
            manuallySeedView?.setOnClickListener {

                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.title_add_user_seed)
                    .setMessage(R.string.message_add_user_seed)
                    .setPositiveButton(android.R.string.ok) { _, _ ->

                        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        try {
                            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                        } catch (e: ActivityNotFoundException) {
                            toastText(requireContext(), R.string.error_add_user_seed)
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()


            }
        }

        val buttonGeneratePasswd: Button = view.findViewById(R.id.button_generate_passwd)
        if (DebugInfo.isDebug) {
            buttonGeneratePasswd.setOnLongClickListener {

                val input = EditText(it.context)
                input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                input.setText(generatedPassword.toRawFormattedPassword(), TextView.BufferType.EDITABLE)

                val filters = arrayOf<InputFilter>(InputFilter.LengthFilter(Constants.MAX_CREDENTIAL_PASSWD_LENGTH))
                input.setFilters(filters)

                AlertDialog.Builder(it.context)
                    .setTitle(R.string.edit_password)
                    .setMessage(R.string.edit_password_message)
                    .setView(input)
                    .setPositiveButton(android.R.string.ok) { dialog, which ->
                        generatedPassword = Password(input.text)
                        var spannedString = PasswordColorizer.spannableString(generatedPassword, it.context)
                        generatedPasswdView.text = spannedString
                    }
                    .setNegativeButton(android.R.string.cancel) { dialog, which ->
                        dialog.cancel()
                    }
                    .show()

                true
            }
        }

        buttonGeneratePasswd.setOnClickListener {
           getBaseActivity()?.let { baseActivity ->
               generatedPassword = GenerateMasterPasswordUseCase.execute(pseudoPhraseSwitch.isChecked, baseActivity).data
               var spannedString = PasswordColorizer.spannableString(generatedPassword, getBaseActivity())
               generatedPasswdView.text = spannedString
           }
        }

        val button = view.findViewById<Button>(R.id.button_next)
        button.setOnClickListener {
            if (generatedPassword.data.isEmpty()) {
                toastText(it.context, R.string.generate_password_first)
            }
            else {
                val transSK = getAndroidSecretKey(ALIAS_KEY_TRANSPORT, view.context)
                val encPassword = encryptPassword(transSK, generatedPassword)
                generatedPassword.clear()

                PreferenceService.putCurrentDate(PreferenceService.DATA_MP_MODIFIED_AT, button.context)

                val args = Bundle()
                args.putEncrypted(ARG_ENC_MASTER_PASSWD, encPassword)
                findNavController().navigate(R.id.action_Create_Vault_FirstFragment_to_SecondFragment, args)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val seedFromThumbnailImage = extractSeedFromImage(data)
            SecretService.setUserSeed(seedFromThumbnailImage)
            manuallySeedView?.text = getString(
                R.string.used_seed,
                seedFromThumbnailImage!!.data.copyOf(8).toHex())
        }
    }

    private fun extractSeedFromImage(data: Intent?): Key? {
        val imageBitmap = data?.extras?.get("data") as Bitmap
        Log.i("SEED byte count", imageBitmap.byteCount.toString())
        Log.i("SEED x", imageBitmap.width.toString())
        Log.i("SEED y", imageBitmap.height.toString())

        if (imageBitmap.width < 32 || imageBitmap.height < 32) {
            Log.i("SEED", "thumbnail image too small to extract a random seed")
            toastText(requireContext(), R.string.error_picture_too_small_for_user_seed)
            return null
        }


        val size: Int = imageBitmap.rowBytes * imageBitmap.height
        val byteBuffer = ByteBuffer.allocate(size)
        imageBitmap.copyPixelsToBuffer(byteBuffer)
        val byteArray = byteBuffer.array()
        imageBitmap.recycle()

        return SecretService.fastHash(byteArray, SaltService.getSalt(requireContext()))
    }


}
