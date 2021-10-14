package de.jepfa.yapm.ui.credential

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.autofill.CurrentCredentialHolder
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.label.LabelDialogOpener
import de.jepfa.yapm.usecase.ExportCredentialUseCase
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_ENABLE_COPY_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_ENABLE_OVERLAY_FEATURE
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_LABELS_IN_LIST
import de.jepfa.yapm.util.*
import java.util.*


class ListCredentialAdapter(val listCredentialsActivity: ListCredentialsActivity) :
        ListAdapter<EncCredential, ListCredentialAdapter.CredentialViewHolder>(CredentialsComparator()),
        Filterable {

    private lateinit var originList: List<EncCredential>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val holder = CredentialViewHolder.create(parent)

        if (Session.isDenied()) {
            return holder
        }
        val enableCopyPassword = PreferenceService.getAsBool(PREF_ENABLE_COPY_PASSWORD, listCredentialsActivity)
        if (!enableCopyPassword) {
            holder.hideCopyPasswordIcon()
        }

        val enableOverlayFeature = PreferenceService.getAsBool(PREF_ENABLE_OVERLAY_FEATURE, listCredentialsActivity)
        if (!enableOverlayFeature) {
            holder.hideDetachPasswordIcon()
        }

        holder.listenForShowCredential { pos, _ ->
            val current = getItem(pos)

            if (listCredentialsActivity.shouldPushBackAutoFill()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (current.isObfuscated) {
                        DeobfuscationDialog.openDeobfuscationDialog(listCredentialsActivity) { deobfuscationKey ->
                            listCredentialsActivity.pushBackAutofill(current, deobfuscationKey)
                        }
                    }
                    else {
                        listCredentialsActivity.pushBackAutofill(current, null)
                    }
                }
            }
            else {
                val intent = Intent(listCredentialsActivity, ShowCredentialActivity::class.java)

                intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, current.id)

                listCredentialsActivity.startActivity(intent)
            }
        }

        holder.listenForSetToAutofill { pos,  _ ->

            val current = getItem(pos)

            if (current.isObfuscated) {
                DeobfuscationDialog.openDeobfuscationDialog(listCredentialsActivity) { deobfuscationKey ->
                    if (listCredentialsActivity.shouldPushBackAutoFill()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            listCredentialsActivity.pushBackAutofill(current, deobfuscationKey)
                        }
                    }

                    toastText(listCredentialsActivity, R.string.credential_used_for_autofill)
                }
            }
            else {
                CurrentCredentialHolder.update(current, null)
                toastText(listCredentialsActivity, R.string.credential_used_for_autofill)
            }



            true
        }

        holder.listenForDetachPasswd { pos, _ ->

            val current = getItem(pos)
            DetachHelper.detachPassword(listCredentialsActivity, current.password, null, null)
        }

        holder.listenForCopyPasswd { pos, _ ->

            val current = getItem(pos)
            ClipboardUtil.copyEncPasswordWithCheck(current.password, null, listCredentialsActivity)
        }

        holder.listenForOpenMenu { position, _, view ->
            val popup = PopupMenu(listCredentialsActivity, view)
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    val current = getItem(position)
                    return when (item.itemId) {
                        R.id.menu_export_credential -> {
                            ExportCredentialUseCase.openStartExportDialog(current, null, listCredentialsActivity)
                            return true
                        }
                        R.id.menu_change_credential -> {
                            val intent = Intent(listCredentialsActivity, EditCredentialActivity::class.java)
                            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, current.id)

                            listCredentialsActivity.startActivityForResult(intent, listCredentialsActivity.newOrUpdateCredentialActivityRequestCode)
                            true
                        }
                        R.id.menu_delete_credential -> {
                            listCredentialsActivity.masterSecretKey?.let{ key ->
                                val decName = SecretService.decryptCommonString(key, current.name)
                                val name = enrichId(listCredentialsActivity, decName, current.id)

                                AlertDialog.Builder(listCredentialsActivity)
                                        .setTitle(R.string.title_delete_credential)
                                        .setMessage(listCredentialsActivity.getString(R.string.message_delete_credential, name))
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                                            listCredentialsActivity.deleteCredential(current)
                                        }
                                        .setNegativeButton(android.R.string.no, null)
                                        .show()
                            }
                            true
                        }
                        else -> false
                    }
                }
            })

            popup.inflate(R.menu.credential_list_menu)
            popup.show()
        }

        return holder
    }


    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        val current = getItem(position)
        val key = listCredentialsActivity.masterSecretKey
        holder.bind(key, current, listCredentialsActivity)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val key = listCredentialsActivity.masterSecretKey

                val filterResults = FilterResults()
                val charString = charSequence.toString()

                if (charString.isEmpty()) {
                    filterResults.values = filterByLabels(key, originList)
                } else {
                    val filteredList: MutableList<EncCredential> = ArrayList<EncCredential>()
                    for (credential in originList) {
                        var name: String
                        if (key != null) {
                            name = SecretService.decryptCommonString(key, credential.name)
                            name = enrichId(listCredentialsActivity, name, credential.id)

                            if (name.toLowerCase(Locale.ROOT).contains(charString.toLowerCase(Locale.ROOT))) {
                                filteredList.add(credential)
                            }
                        }

                    }
                    filterResults.values = filterByLabels(key, filteredList)
                }
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                val pubCredentials = filterResults.values as List<EncCredential?>
                submitList(pubCredentials)

                // refresh the list with filtered data
                notifyDataSetChanged()
            }


            private fun filterByLabels(key: SecretKeyHolder?, credentials: List<EncCredential>): List<EncCredential> {
                key ?: return credentials
                return credentials
                    .filter {
                        val labels = LabelService.getLabelsForCredential(key, it)
                        LabelFilter.isFilterFor(labels)
                    }
                    .toList()
            }
        }
    }

    fun submitOriginList(list: List<EncCredential>) {
        originList = list
        submitList(list)

    }

    class CredentialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val credentialContainerView: LinearLayout = itemView.findViewById(R.id.credential_container)
        private val credentialItemView: TextView = itemView.findViewById(R.id.credential_name)
        private val credentialDetachImageView: ImageView = itemView.findViewById(R.id.credential_detach)
        private val credentialCopyImageView: ImageView = itemView.findViewById(R.id.credential_copy)
        private val credentialMenuImageView: ImageView = itemView.findViewById(R.id.credential_menu_popup)
        private val credentialLabelContainerGroup: ChipGroup = itemView.findViewById(R.id.label_container)
        private val credentialToolbarContainerView: ConstraintLayout = itemView.findViewById(R.id.toolbar_container)

        fun hideCopyPasswordIcon() {
            credentialCopyImageView.visibility = View.GONE
            // TODO test if we can shrink the toolbar space on demand
            /*val contraintLayoutSet = ConstraintSet()
            contraintLayoutSet.clone(credentialToolbarContainerView)
            contraintLayoutSet.setHorizontalWeight(R.id.toolbar_container, 0.1f)*/
        }

        fun hideDetachPasswordIcon() {
            credentialDetachImageView.visibility = View.GONE
        }

        fun listenForShowCredential(event: (position: Int, type: Int) -> Unit) {
            credentialContainerView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForDetachPasswd(event: (position: Int, type: Int) -> Boolean) {
            credentialDetachImageView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForCopyPasswd(event: (position: Int, type: Int) -> Unit) {
            credentialCopyImageView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForOpenMenu(event: (position: Int, type: Int, view: View) -> Unit) {
            credentialMenuImageView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType, credentialMenuImageView)
            }
        }

        fun listenForSetToAutofill(event: (position: Int, type: Int) -> Boolean) {
            credentialContainerView.setOnLongClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun bind(key: SecretKeyHolder?, credential: EncCredential, activity: SecureActivity) {

            credentialLabelContainerGroup.removeAllViews()

            var name = itemView.context.getString(R.string.unknown_placeholder)
            if (key != null) {
                name = SecretService.decryptCommonString(key, credential.name)
                name = enrichId(activity, name, credential.id)

                val showLabels = PreferenceService.getAsBool(PREF_SHOW_LABELS_IN_LIST, itemView.context)
                if (showLabels) {
                    LabelService.getLabelsForCredential(key, credential).forEachIndexed { idx, label ->
                        val chip = createAndAddLabelChip(
                            label,
                            credentialLabelContainerGroup,
                            thinner = true,
                            itemView.context)
                        chip.setOnClickListener { _ ->
                            LabelDialogOpener.openLabelDialog(activity, label)
                        }
                    }
                }
            }
            credentialItemView.text = name
        }


        companion object {
            fun create(parent: ViewGroup): CredentialViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_credential, parent, false)
                return CredentialViewHolder(view)
            }
        }
    }

    class CredentialsComparator : DiffUtil.ItemCallback<EncCredential>() {
        override fun areItemsTheSame(oldItem: EncCredential, newItem: EncCredential): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: EncCredential, newItem: EncCredential): Boolean {
            return oldItem.id == newItem.id
        }
    }

}
