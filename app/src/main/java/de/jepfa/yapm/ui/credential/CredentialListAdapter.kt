package de.jepfa.yapm.ui.credential

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.model.Secret
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.overlay.DetachHelper
import de.jepfa.yapm.service.secretgenerator.PasswordGenerator
import de.jepfa.yapm.service.secretgenerator.PasswordGeneratorSpec
import de.jepfa.yapm.service.secretgenerator.PasswordStrength
import java.util.*


class CredentialListAdapter(val listCredentialsActivity: ListCredentialsActivity) :
        ListAdapter<EncCredential, CredentialListAdapter.CredentialViewHolder>(CredentialsComparator()),
        Filterable {

    private lateinit var originList: List<EncCredential>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val holder = CredentialViewHolder.create(parent)

        if (Secret.isDenied()) {
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
            DetachHelper.detachPassword(listCredentialsActivity, current.password)
        }

        holder.listenForOpenMenu { position, type, view ->
            val popup = PopupMenu(listCredentialsActivity, view)
            popup.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
                override fun onMenuItemClick(item: MenuItem): Boolean {
                    val current = getItem(position)
                    return when (item.itemId) {
                        R.id.menu_change_credential -> {
                            val intent = Intent(listCredentialsActivity, NewOrChangeCredentialActivity::class.java)
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
        var name = "????"
        if (key != null) {
            name = SecretService.decryptCommonString(key, current.name)
        }
        holder.bind(name)

    }

    override fun getFilter(): Filter? {
        return object : Filter() {
            override fun performFiltering(charSequence: CharSequence): FilterResults {
                val filterResults = FilterResults()
                val charString = charSequence.toString()
                if (charString.isEmpty()) {
                    filterResults.values = originList
                } else {
                    val filteredList: MutableList<EncCredential> = ArrayList<EncCredential>()
                    val key = listCredentialsActivity.masterSecretKey
                    for (credential in originList) {
                        var name = ""
                        if (key != null) {
                            name = SecretService.decryptCommonString(key, credential.name)
                            if (name.toLowerCase().contains(charString.toLowerCase())) {
                                filteredList.add(credential)
                            }
                        }

                    }
                    filterResults.values = filteredList
                }
                return filterResults
            }

            override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
                val pubCredentials = filterResults.values as MutableList<EncCredential?>
                submitList(pubCredentials)

                // refresh the list with filtered data
                notifyDataSetChanged()
            }
        }
    }

    fun submitOriginList(list: List<EncCredential>) {
        originList = list
        submitList(list)

    }

    class CredentialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val credentialItemView: TextView = itemView.findViewById(R.id.credential_name)
        private val credentialDetachImageView: ImageView = itemView.findViewById(R.id.credential_detach)
        private val credentialMenuImageView: ImageView = itemView.findViewById(R.id.credential_menu_popup)

        fun listenForShowCredential(event: (position: Int, type: Int) -> Unit) {
            credentialItemView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }
        fun listenForDetachPasswd(event: (position: Int, type: Int) -> Boolean) {
            credentialDetachImageView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForOpenMenu(event: (position: Int, type: Int, view: View) -> Unit) {
            credentialMenuImageView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType, credentialMenuImageView)
            }
        }

        fun bind(text: CharSequence?) {
            credentialItemView.text = text
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
