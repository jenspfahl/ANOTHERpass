package de.jepfa.yapm.service.biometrix

import android.content.Context
import android.widget.Button
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.jepfa.yapm.R

class BiometricDialogV23(context: Context, private val biometricCallback: BiometricCallback)
    : BottomSheetDialog(context) {

    private lateinit var itemDescription: TextView
    private lateinit var itemStatus: TextView

    init {
        setDialogView()
    }

    private fun setDialogView() {
        val bottomSheetView = layoutInflater.inflate(R.layout.view_biometrix_bottom_sheet, null)
        setContentView(bottomSheetView)

        findViewById<Button>(R.id.btn_cancel)?.setOnClickListener{
            dismiss()
            biometricCallback.onAuthenticationCancelled()
        }

        itemStatus = findViewById(R.id.item_status)!!
        itemDescription = findViewById(R.id.item_description)!!
    }

    fun updateStatus(status: String) {
        itemStatus.text = status
    }

    fun setDescription(description: String) {
        itemDescription.text = description
    }

}