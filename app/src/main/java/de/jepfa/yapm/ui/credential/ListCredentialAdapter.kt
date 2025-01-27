package de.jepfa.yapm.ui.credential

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
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
import de.jepfa.yapm.service.net.HttpCredentialRequestHandler
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.LabelDialogs
import de.jepfa.yapm.usecase.credential.ExportCredentialUseCase
import de.jepfa.yapm.util.*
import de.jepfa.yapm.util.Constants.LOG_PREFIX
import java.util.*


class ListCredentialAdapter(val listCredentialsActivity: ListCredentialsActivity, val multipleSelectionCallback: (Set<EncCredential>) -> Unit) :
        ListAdapter<EncCredential, ListCredentialAdapter.CredentialViewHolder>(CredentialsComparator()),
        Filterable {

    private var originList: List<EncCredential> = emptyList()
    private var selectionMode = false
    private var selected = HashSet<EncCredential>() 

    fun getSelectedCredentials() = HashSet(selected)
    

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

            if (listCredentialsActivity.shouldPushBackAutoFill() || HttpCredentialRequestHandler.isProgressing()) {
                    if (current.passwordData.isObfuscated) {
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

        holder.listenForToggleSelection { pos, _ ->
            val current = getItem(pos)

            if (!selected.contains(current)) {
                selected.add(current)
            }
            else {
                selected.remove(current)
            }
            notifyItemChanged(pos)
            multipleSelectionCallback(selected)
        }

        holder.listenForLongClick { pos, _ ->


            if (selectionMode) {
                stopSelectionMode()
            }
            else {
                startSelectionMode()


                val current = getItem(pos)
                selected.add(current)

                multipleSelectionCallback(selected)
            }

            true
        }

        holder.listenForDetachPasswd { pos, _ ->

            val current = getItem(pos)
            DetachHelper.detachPassword(listCredentialsActivity, current.user, current.passwordData.password, null, null)
        }

        holder.listenForCopyPasswd { pos, _ ->

            val current = getItem(pos)
            ClipboardUtil.copyEncPasswordWithCheck(current.passwordData.password, null, listCredentialsActivity)
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

    fun startSelectionMode() {
        selectionMode = true
        multipleSelectionCallback(selected)
        notifyDataSetChanged()
    }


    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        val current = getItem(position)
        val key = listCredentialsActivity.masterSecretKey
        if (selectionMode) {
            holder.credentialSelectionContainerView.visibility = View.VISIBLE
            if (selected.contains(current)) {
                holder.credentialSelectedView.setImageDrawable(listCredentialsActivity.getDrawable(R.drawable.outline_check_circle_24))
            }
            else {
                holder.credentialSelectedView.setImageDrawable(listCredentialsActivity.getDrawable(R.drawable.outline_circle_24))
            }
        }
        else {
            holder.credentialSelectionContainerView.visibility = View.GONE
        }
        holder.bind(key, current, listCredentialsActivity)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {

                val key = listCredentialsActivity.masterSecretKey
                val filterResults = FilterResults()

                if (charSequence.isEmpty()) {
                    // no search query entered
                    filterResults.values = filterByLabels(key, originList)
                } else {
                    val filteredList: MutableList<EncCredential> = ArrayList<EncCredential>()

                    val exactMatch = charSequence.endsWith(SEARCH_COMMAND_END)
                    val filterLatest = SearchCommand.SEARCH_COMMAND_SHOW_LATEST.applies(charSequence)

                    var latestModifyTimestamp: Long ? = null
                    if (filterLatest) {
                        latestModifyTimestamp = originList.mapNotNull { it.timeData.modifyTimestamp }.maxByOrNull { it }
                    }

                    for (credential in originList) {
                        if (key != null) {
                            var name = SecretService.decryptCommonString(key, credential.name)
                            name = enrichId(listCredentialsActivity, name, credential.id)
                            val website = SecretService.decryptCommonString(key, credential.website)
                            val user = SecretService.decryptCommonString(key, credential.user)
                            val addInfo = SecretService.decryptCommonString(key, credential.additionalInfo)
                            val expiredAt = SecretService.decryptLong(key, credential.timeData.expiresAt)
                            val uid = credential.uid?.toReadableString()
                            val id = credential.id?.toString()

                            if (SearchCommand.SEARCH_COMMAND_SHOW_EXPIRED.applies(charSequence)) {
                                if (credential.isExpired(key)) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SHOW_EXPIRES.applies(charSequence)) {
                                if ((expiredAt != null) && (expiredAt > 0)) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (filterLatest) {
                                if (credential.timeData.modifyTimestamp == latestModifyTimestamp) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SHOW_VEILED.applies(charSequence)) {
                                if (credential.passwordData.isObfuscated) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SHOW_OTP.applies(charSequence)) {
                                if (credential.otpData != null) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SEARCH_ID.applies(charSequence)) {
                                val arg = SearchCommand.SEARCH_COMMAND_SEARCH_ID.extractArg(charSequence)
                                if (exactMatch) {
                                    if (id != null && id == arg) {
                                        filteredList.add(credential)
                                    }
                                }
                                else {
                                    if (id != null && containsValue(id, arg)) {
                                        filteredList.add(credential)
                                    }
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SEARCH_UID.applies(charSequence)) {
                                val searchUid = SearchCommand.SEARCH_COMMAND_SEARCH_UID.extractArg(charSequence)
                                if (exactMatch) {
                                    if (uid != null && uid == searchUid) {
                                        filteredList.add(credential)
                                    }
                                }
                                else {
                                    if (uid != null && containsValue(uid, searchUid)) {
                                        filteredList.add(credential)
                                    }
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SEARCH_USER.applies(charSequence)) {
                                val searchUser = SearchCommand.SEARCH_COMMAND_SEARCH_USER.extractArg(charSequence)
                                if (exactMatch) {
                                    if (user == searchUser) {
                                        filteredList.add(credential)
                                    }
                                }
                                else {
                                    if (containsValue(user, searchUser)) {
                                        filteredList.add(credential)
                                    }
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SEARCH_WEBSITE.applies(charSequence)) {
                                val searchWebsite = SearchCommand.SEARCH_COMMAND_SEARCH_WEBSITE.extractArg(charSequence)
                                if (exactMatch) {
                                    if (website == searchWebsite) {
                                        filteredList.add(credential)
                                    }
                                }
                                else {
                                    if (containsValue(website, searchWebsite)) {
                                        filteredList.add(credential)
                                    }
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SEARCH_LABEL.applies(charSequence)) {
                                val searchLabel = SearchCommand.SEARCH_COMMAND_SEARCH_LABEL.extractArg(charSequence)
                                val labels =
                                    LabelService.defaultHolder.decryptLabelsForCredential(
                                        key,
                                        credential
                                    )
                                if (exactMatch) {
                                    labels
                                        .filter { it.name.lowercase() == searchLabel }
                                        .take(1)
                                        .forEach { filteredList.add(credential)
                                        }
                                }
                                else {
                                    labels
                                        .filter { containsValue(it.name, searchLabel) }
                                        .take(1)
                                        .forEach { filteredList.add(credential)
                                        }
                                }
                            }
                            else if (SearchCommand.SEARCH_COMMAND_SEARCH_IN_ALL.applies(charSequence)) {
                                var searchString = SearchCommand.SEARCH_COMMAND_SEARCH_IN_ALL.extractArg(charSequence)
                                if (containsValue(name, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (containsValue(website, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (containsValue(user, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (containsValue(addInfo, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (uid != null && containsValue(uid, searchString)) {
                                    filteredList.add(credential)
                                }
                            }
                            else if (charSequence.startsWith(SEARCH_COMMAND_START)) {
                                // filter in all fields
                                var searchString = charSequence.removePrefix(SEARCH_COMMAND_START).toString()
                                if (containsValue(name, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (containsValue(website, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (containsValue(user, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (containsValue(addInfo, searchString)) {
                                    filteredList.add(credential)
                                }
                                else if (uid != null && containsValue(uid, searchString)) {
                                    filteredList.add(credential)
                                }
                            }
                            else {
                                // filter only credential name
                                if (containsValue(name, charSequence.toString())) {
                                    filteredList.add(credential)
                                }
                            }
                        }

                    }


                    filterResults.values = filteredList
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
                    Log.i(LOG_PREFIX + "LST", "Null in pop search result")
                    //listCredentialsActivity.recreate() this seems useless in most cases
                }
            }

            private fun containsValue(value: String, searchString: String): Boolean {
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

    fun stopSelectionMode(withRefresh: Boolean = true) {
        selectionMode = false
        selected.clear()
        multipleSelectionCallback(selected)
        if (withRefresh) {
            notifyDataSetChanged()
        }
    }

    class CredentialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val credentialContainerView: LinearLayout = itemView.findViewById(R.id.credential_container)
        private val credentialItemView: TextView = itemView.findViewById(R.id.credential_name)
        private val credentialDetachImageView: ImageView = itemView.findViewById(R.id.credential_detach)
        private val credentialCopyImageView: ImageView = itemView.findViewById(R.id.credential_copy)
        private val credentialMenuImageView: ImageView = itemView.findViewById(R.id.credential_menu_popup)
        private val credentialLabelContainerGroup: ChipGroup = itemView.findViewById(R.id.label_container)
        val credentialSelectionContainerView: LinearLayout = itemView.findViewById(R.id.selection_container)
        val credentialSelectedView: ImageView = itemView.findViewById(R.id.selected)

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

        fun listenForToggleSelection(event: (position: Int, type: Int) -> Unit) {
            credentialSelectedView.setOnClickListener {
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
