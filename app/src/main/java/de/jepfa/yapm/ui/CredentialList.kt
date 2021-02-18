package de.jepfa.yapm.ui

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.core.content.PermissionChecker.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.overlay.OverlayShowingService
import java.util.*


class CredentialListAdapter(val mainActivity: MainActivity) :
        ListAdapter<EncCredential, CredentialListAdapter.CredentialViewHolder>(CredentialsComparator()),
        Filterable {

    private lateinit var originList: List<EncCredential>

    val secretService = SecretService()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val holder = CredentialViewHolder.create(parent)
        holder.listenForOnClick { pos, type ->
            val current = getItem(pos)
            val key = secretService.getAndroidSecretKey("test-key")
            val decName = secretService.decryptCommonString(key, current.name)
            val decAdditionalInfo = secretService.decryptCommonString(key, current.additionalInfo)
            val password = secretService.decryptPassword(key, current.password)
            AlertDialog.Builder(holder.itemView.context)
                    .setTitle(decName)
                    .setMessage(decAdditionalInfo + System.lineSeparator() + password.debugToString())
                    .show()

            password.clear()
        }

        holder.listenForOnLongClick { pos, type ->

            if (!Settings.canDrawOverlays(parent.context)) {
                                // this check returns false true on my phone although i have granted that permission!!!!
                AlertDialog.Builder(holder.itemView.context)
                        .setTitle("Missing permission")
                        .setMessage("App cannot draw over other apps. Enable permission and try again.")
                        .setPositiveButton("Open permission",
                                DialogInterface.OnClickListener { dialogInterface, i ->
                                    val intent = Intent()
                                    intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                                    intent.data = Uri.parse("package:" + mainActivity.applicationInfo.packageName)
                                    mainActivity.startActivity(intent)
                                })
                        .setNegativeButton("Close",
                                DialogInterface.OnClickListener { dialogInterface, i -> dialogInterface.cancel() })
                        .show()
                false
            }
            else {

                val current = getItem(pos)
                val key = secretService.getAndroidSecretKey("test-key")
                val password = secretService.decryptPassword(key, current.password)

                val intent = Intent(mainActivity, OverlayShowingService::class.java)
                intent.putExtra("password", password.data)
                mainActivity.startService(intent)
                // minimize app: mainActivity.
                mainActivity.moveTaskToBack(true)
                password.clear()
                true
            }
        }

        return holder
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        val current = getItem(position)
        val key = secretService.getAndroidSecretKey("test-key")
        val decName = secretService.decryptCommonString(key, current.name)
        holder.bind(decName)

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
                    val key = secretService.getAndroidSecretKey("test-key")
                    for (credential in originList) {
                        val decName = secretService.decryptCommonString(key, credential.name)
                        if (decName.toLowerCase().contains(charString.toLowerCase())) {
                            filteredList.add(credential)
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
        private val credentialItemView: TextView = itemView.findViewById(R.id.textView)

        fun listenForOnClick(event: (position: Int, type: Int) -> Unit) {
            credentialItemView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }
        fun listenForOnLongClick(event: (position: Int, type: Int) -> Boolean) {
            credentialItemView.setOnLongClickListener {
                event.invoke(adapterPosition, itemViewType)
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
