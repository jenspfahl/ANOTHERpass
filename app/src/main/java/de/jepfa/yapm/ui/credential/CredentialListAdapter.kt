package de.jepfa.yapm.ui.credential

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat.getColor
import androidx.core.view.setPadding
import androidx.core.view.size
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pchmn.materialchips.ChipView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.label.LabelFilter
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.ui.editcredential.EditCredentialActivity
import de.jepfa.yapm.util.ClipboardUtil
import de.jepfa.yapm.util.PreferenceUtil
import de.jepfa.yapm.util.PreferenceUtil.PREF_SHOW_LABELS_IN_LIST
import java.util.*
import javax.crypto.SecretKey


class CredentialListAdapter(val listCredentialsActivity: ListCredentialsActivity) :
        ListAdapter<EncCredential, CredentialListAdapter.CredentialViewHolder>(CredentialsComparator()),
        Filterable {

    private lateinit var originList: List<EncCredential>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val holder = CredentialViewHolder.create(parent)

        if (Session.isDenied()) {
            return holder
        }

        holder.listenForShowCredential { pos, _ ->
            val current = getItem(pos)

            val intent = Intent(listCredentialsActivity, ShowCredentialActivity::class.java)

            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, current.id)

            listCredentialsActivity.startActivity(intent)
        }

        holder.listenForDetachPasswd { pos, _ ->

            val current = getItem(pos)
            DetachHelper.detachPassword(listCredentialsActivity, current.password, null)
        }

        holder.listenForCopyPasswd { pos, _ ->

            val current = getItem(pos)
            ClipboardUtil.copyEncPasswordWithCheck(current.password, listCredentialsActivity)
        }

        holder.listenForOpenMenu { position, _, view ->
            val popup = PopupMenu(listCredentialsActivity, view)
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    val current = getItem(position)
                    return when (item.itemId) {
                        R.id.menu_change_credential -> {
                            val intent = Intent(listCredentialsActivity, EditCredentialActivity::class.java)
                            intent.putExtra(EncCredential.EXTRA_CREDENTIAL_ID, current.id)

                            listCredentialsActivity.startActivityForResult(intent, listCredentialsActivity.newOrUpdateCredentialActivityRequestCode)
                            true
                        }
                        R.id.menu_delete_credential -> {
                            val key = listCredentialsActivity.masterSecretKey
                            if (key != null) {
                                val decName = SecretService.decryptCommonString(key, current.name)

                                AlertDialog.Builder(listCredentialsActivity)
                                        .setTitle(R.string.title_delete_credential)
                                        .setMessage(listCredentialsActivity.getString(R.string.message_delete_credential, decName))
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
        holder.bind(key, current)
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
                            if (name.toLowerCase().contains(charString.toLowerCase())) {
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


            private fun filterByLabels(key: SecretKey?, credentials: List<EncCredential>): List<EncCredential> {
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
        private val credentialLabelContainerView: LinearLayout = itemView.findViewById(R.id.label_container)

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

        fun bind(key: SecretKey?, credential: EncCredential) {
            var name = "????"
            if (key != null) {
                name = SecretService.decryptCommonString(key, credential.name)

                credentialLabelContainerView.removeAllViews()

                val showLabels = PreferenceUtil.getAsBool(PREF_SHOW_LABELS_IN_LIST, true, itemView.context)
                if (showLabels) {
                    LabelService.getLabelsForCredential(key, credential).forEachIndexed { idx, it ->
                        val chipView = ChipView(itemView.context)
                        // doesnt work: chipView.setChip(it.labelChip)
                        chipView.label = it.labelChip.label
                        chipView.setChipBackgroundColor(it.labelChip.getColor(itemView.context))
                        chipView.setLabelColor(getColor(itemView.context, R.color.white))
                        chipView.setPadding(16, 0, 16, 0)
                        credentialLabelContainerView.addView(chipView, idx)
                    }
                }
            }
            credentialItemView.text = name
        }


        companion object {
            fun create(parent: ViewGroup): CredentialViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_item, parent, false)
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
