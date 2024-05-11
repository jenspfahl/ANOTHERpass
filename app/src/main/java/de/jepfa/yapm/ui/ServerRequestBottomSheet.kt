package de.jepfa.yapm.ui

import android.content.Context
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomsheet.BottomSheetDialog
import de.jepfa.yapm.R

class ServerRequestBottomSheet(
    context: Context,
    webClientTitle: CharSequence,
    webClientId: CharSequence,
    webRequestDetails: CharSequence,
    fingerprint: CharSequence,
    denyHandler: () -> Unit,
    acceptHandler: () -> Unit,
): BottomSheetDialog(context) {

    init {
        setCanceledOnTouchOutside(false)
        setCancelable(false)

        val bottomSheet = layoutInflater.inflate(R.layout.server_bottom_sheet, null)

        bottomSheet.findViewById<TextView>(R.id.text_webclient_title).text = webClientTitle
        bottomSheet.findViewById<TextView>(R.id.text_webclient_id).text = webClientId
        bottomSheet.findViewById<TextView>(R.id.text_web_request).text = webRequestDetails
        bottomSheet.findViewById<TextView>(R.id.text_fingerprint).text = fingerprint

        bottomSheet.findViewById<Button>(R.id.button_server_call_deny).setOnClickListener {
            denyHandler()
            dismiss()
        }
        bottomSheet.findViewById<Button>(R.id.button_server_call_accept).setOnClickListener {
            acceptHandler()
            dismiss()
        }

        bottomSheet.findViewById<ImageView>(R.id.imageview_fingerprint_help).setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Fingerprint")
                .setMessage("Ensure the fingerprint is equal with the fingerprint shown in the browser before accept.")
                .show()
        }

        setContentView(bottomSheet)
    }

}