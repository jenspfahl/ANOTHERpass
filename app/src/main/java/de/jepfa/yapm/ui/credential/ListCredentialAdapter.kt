package de.jepfa.yapm.ui.credential

import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.ChipGroup
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.PreferenceService
import de.jepfa.yapm.service.PreferenceService.PREF_ENABLE_COPY_PASSWORD
import de.jepfa.yapm.service.PreferenceService.PREF_ENABLE_OVERLAY_FEATURE
import de.jepfa.yapm.service.PreferenceService.PREF_SHOW_LABELS_IN_LIST
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.LabelDialogs
import de.jepfa.yapm.usecase.credential.ExportCredentialUseCase
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
        val inAutofillMode = listCredentialsActivity.shouldPushBackAutoFill()
        if (inAutofillMode || !enableOverlayFeature) {
            holder.hideDetachPasswordIcon()
        }

        holder.listenForShowCredential { pos, _ ->
            val current = getItem(pos)

            if (listCredentialsActivity.shouldPushBackAutoFill()) {
                    if (current.isObfuscated) {
                    DeobfuscationDialog.openDeobfuscationDialogForCredentials(listCredentialsActivity) { deobfuscationKey ->
                        if (deobfuscationKey == null) {
                            return@openDeobfuscationDialogForCredentials
                        }
                        listCredentialsActivity.pushBackAutofill(current, deobfuscationKey)
                    }
                }
                else {
                    listCredentialsActivity.pushBackAutofill(current, null)
                }
            }
            else {
                val intent = Intent(listCredentialsActivity, ShowCredentialActivity::class.java)

                intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, current.id)

                listCredentialsActivity.startActivity(intent)
            }
        }

        holder.listenForLongClick { pos, _ ->

            val current = getItem(pos)

            val sb = StringBuilder()

            current.id?.let { sb.addFormattedLine(listCredentialsActivity.getString(R.string.identifier), it)}
            current.uid?.let {
                sb.addFormattedLine(
                    listCredentialsActivity.getString(R.string.universal_identifier),
                    shortenBase64String(it.toBase64String()))
            }

            listCredentialsActivity.masterSecretKey?.let { key ->
                val name = SecretService.decryptCommonString(key, current.name)
                sb.addFormattedLine(listCredentialsActivity.getString(R.string.name), name)
            }
            current.modifyTimestamp?.let{
                if (it > 1000) // modifyTimestamp is the credential Id after running db migration, assume ids are lower than 1000
                    sb.addFormattedLine(listCredentialsActivity.getString(R.string.last_modified), dateTimeToNiceString(it.toDate(), listCredentialsActivity))
            }

            AlertDialog.Builder(listCredentialsActivity)
                .setTitle(R.string.title_credential_details)
                .setMessage(sb.toString())
                .show()

            true
        }

        holder.listenForDetachPasswd { pos, _ ->

            val current = getItem(pos)
            DetachHelper.detachPassword(listCredentialsActivity, current.user, current.password, null, null)
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
                        R.id.menu_duplicate_credential -> {
                            listCredentialsActivity.masterSecretKey?.let{ key ->
                                listCredentialsActivity.duplicateCredential(current, key)
                            }
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
            popup.inflate(R.menu.menu_credential_list)
            popup.setForceShowIcon(true)
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
                val filterId = charSequence.startsWith("!!:")
                val filterExpired = charSequence.startsWith("!!exp")
                val filterLabel = charSequence.startsWith("!:")
                val filterAll = charSequence.startsWith("!")
                var charString =
                    if (filterLabel) charSequence.substring(2).lowercase().trimStart()
                    else if (filterId) charSequence.substring(3).lowercase().trimStart()
                    else if (filterAll) charSequence.substring(1).lowercase().trimStart()
                    else charSequence.toString().lowercase()

                if (charString.isEmpty()) {
                    filterResults.values = filterByLabels(key, originList)
                } else {
                    val filteredList: MutableList<EncCredential> = ArrayList<EncCredential>()
                    for (credential in originList) {
                        if (key != null) {
                            var name = SecretService.decryptCommonString(key, credential.name)
                            name = enrichId(listCredentialsActivity, name, credential.id)
                            val website = SecretService.decryptCommonString(key, credential.website)
                            val user = SecretService.decryptCommonString(key, credential.user)
                            val addInfo = SecretService.decryptCommonString(key, credential.additionalInfo)
                            val uid = credential.uid?.toBase64String()
                            val id = credential.id?.toString()

                            if (filterExpired) {
                                if (credential.isExpired(key)) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (filterId) {
                                val exactMatch = charString.endsWith(":")
                                if (exactMatch) {
                                    val searchId = charString.removeSuffix(":")
                                    if (id != null && id == searchId) {
                                        filteredList.add(credential)
                                    }
                                }
                                else {
                                    if (id != null && isFilterValue(id, charString)) {
                                        filteredList.add(credential)
                                    }
                                }
                            }
                            else if (filterLabel) {
                                val labels =
                                    LabelService.defaultHolder.decryptLabelsForCredential(
                                        key,
                                        credential
                                    )
                                val exactMatch = charString.endsWith(":")
                                if (exactMatch) {
                                    val searchLabel = charString.removeSuffix(":")
                                    labels.forEach { label ->
                                        if (label.name.lowercase() == searchLabel) {
                                            filteredList.add(credential)
                                        }
                                    }
                                }
                                else {
                                    labels.forEach { label ->
                                        if (isFilterValue(label.name, charString)) {
                                            filteredList.add(credential)
                                        }
                                    }
                                }
                            }
                            else if (filterAll) {
                                if (isFilterValue(name, charString)) {
                                    filteredList.add(credential)
                                }
                                else if (isFilterValue(website, charString)) {
                                    filteredList.add(credential)
                                }
                                else if (isFilterValue(user, charString)) {
                                    filteredList.add(credential)
                                }
                                else if (isFilterValue(addInfo, charString)) {
                                    filteredList.add(credential)
                                }
                                else if (uid != null && isFilterValue(uid, charString)) {
                                    filteredList.add(credential)
                                }
                            }
                            else {
                                // filter only credential name
                                if (isFilterValue(name, charString)) {
                                    filteredList.add(credential)
                                }
                            }
                        }

                    }
                    if (filterAll) {
                        filterResults.values = filteredList

                    }
                    else {
                        filterResults.values = filterByLabels(key, filteredList)
                    }
                }
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                val list = filterResults.values
                if (list is List<*>) {
                    submitList(list as List<EncCredential>?)
                }
                else {
                    // in some cases the filter result is null in Android 13, recreate it
                    listCredentialsActivity.recreate()
                }
            }

            private fun isFilterValue(value: String, searchString: String): Boolean {
                return value.lowercase().contains(searchString)
            }
        }
    }

    fun submitOriginList(list: List<EncCredential>) {
        originList = list
        val key = listCredentialsActivity.masterSecretKey
        val filteredList = filterByLabels(key, list)
        submitList(filteredList)

    }

    private fun filterByLabels(key: SecretKeyHolder?, credentials: List<EncCredential>): List<EncCredential> {
        key ?: return credentials
        return credentials
            .filter {
                val labels = LabelService.defaultHolder.decryptLabelsForCredential(key, it)
                LabelFilter.isFilterFor(labels)
            }
            .toList()
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
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForDetachPasswd(event: (position: Int, type: Int) -> Boolean) {
            credentialDetachImageView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForCopyPasswd(event: (position: Int, type: Int) -> Unit) {
            credentialCopyImageView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForOpenMenu(event: (position: Int, type: Int, view: View) -> Unit) {
            credentialMenuImageView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType, credentialMenuImageView)
            }
        }

        fun listenForLongClick(event: (position: Int, type: Int) -> Boolean) {
            credentialContainerView.setOnLongClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener false
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun bind(key: SecretKeyHolder?, credential: EncCredential, activity: SecureActivity) {

            credentialLabelContainerGroup.removeAllViews()

            var name = itemView.context.getString(R.string.unknown_placeholder)
            if (key != null) {
                name = SecretService.decryptCommonString(key, credential.name)
                name = enrichId(activity, name, credential.id)

                if (credential.isExpired(key)) { // expired
                    createAndAddLabelChip(
                        Label(itemView.context.getString(R.string.expired), activity.getColor(R.color.Red), R.drawable.baseline_lock_clock_24),
                        credentialLabelContainerGroup,
                        thinner = true,
                        itemView.context,
                        outlined = true,
                    )
                }

                val showLabels = PreferenceService.getAsBool(PREF_SHOW_LABELS_IN_LIST, itemView.context)
                if (showLabels) {
                    LabelService.defaultHolder.decryptLabelsForCredential(key, credential).forEachIndexed { idx, label ->
                        val chip = createAndAddLabelChip(
                            label,
                            credentialLabelContainerGroup,
                            thinner = true,
                            itemView.context)
                        chip.setOnClickListener {
                            LabelDialogs.openLabelOverviewDialog(activity, label)
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
