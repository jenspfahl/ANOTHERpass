package de.jepfa.yapm.ui

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.EncCredential
import de.jepfa.yapm.service.encrypt.SecretService
import de.jepfa.yapm.service.overlay.OverlayShowingService


class CredentialListAdapter(val mainActivity: MainActivity) : ListAdapter<EncCredential, CredentialListAdapter.CredentialViewHolder>(CredentialsComparator()) {

    val secretService = SecretService()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val holder = CredentialViewHolder.create(parent)
        holder.listenForOnClick { pos, type ->
            val current = getItem(pos)
            val key = secretService.getAndroidSecretKey("test-key")
            val decAdditionalInfo = secretService.decryptCommonString(key, current.additionalInfo)
            val password = secretService.decryptPassword(key, current.password)
            AlertDialog.Builder(holder.itemView.context)
                    .setTitle(decAdditionalInfo)
                    .setMessage(password.debugToString())
                    .show()

            password.clear()
        }

        holder.listenForOnLongClick { pos, type ->
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

        return holder
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        val current = getItem(position)
        val key = secretService.getAndroidSecretKey("test-key")
        val decName = secretService.decryptCommonString(key, current.name)
        holder.bind(decName)

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