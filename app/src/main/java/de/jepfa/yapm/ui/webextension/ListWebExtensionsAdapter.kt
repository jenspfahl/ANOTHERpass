package de.jepfa.yapm.ui.webextension

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService


class ListWebExtensionsAdapter(private val listWebExtensionsActivity: ListWebExtensionsActivity) :
        ListAdapter<EncWebExtension, ListWebExtensionsAdapter.WebExtensionViewHolder>(WebExtensionsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WebExtensionViewHolder {
        val holder = WebExtensionViewHolder.create(parent)

        if (Session.isDenied()) {
            return holder
        }

        holder.listenForEditWebExtension { pos, _ ->
            val current = getItem(pos)

            val intent = Intent(listWebExtensionsActivity, EditWebExtensionActivity::class.java)
            intent.putExtra(EncWebExtension.EXTRA_WEB_EXTENSION_ID, current.id)
            listWebExtensionsActivity.startActivity(intent)

        }

        holder.listenForDeleteWebExtension { pos, _ ->

            if (!Session.isDenied()) {
                val current = getItem(pos)
                WebExtensionDialogs.openDeleteWebExtension(current, listWebExtensionsActivity)
            }
        }


        return holder
    }


    override fun onBindViewHolder(holder: WebExtensionViewHolder, position: Int) {
        val current = getItem(position)
        val key = listWebExtensionsActivity.masterSecretKey
        holder.bind(key, current, listWebExtensionsActivity)
    }

    class WebExtensionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val webExtensionTitleTextView: TextView = itemView.findViewById(R.id.web_extension_title)
        private val webExtensionClientIdTextView: TextView = itemView.findViewById(R.id.web_extension_client_id)
        private val webExtensionDeleteImageView: ImageView = itemView.findViewById(R.id.web_extension_delete)

        fun listenForEditWebExtension(event: (position: Int, type: Int) -> Unit) {
            webExtensionTitleTextView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
            webExtensionClientIdTextView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }


        fun listenForDeleteWebExtension(event: (position: Int, type: Int) -> Unit) {
            webExtensionDeleteImageView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun bind(
            key: SecretKeyHolder?,
            webExtension: EncWebExtension,
            context: Context
        ) {
            var name = itemView.context.getString(R.string.unknown_placeholder)
            var clientId = itemView.context.getString(R.string.unknown_placeholder)
            if (key != null) {
                clientId = SecretService.decryptCommonString(key, webExtension.webClientId)
                if (webExtension.title != null) {
                    name = SecretService.decryptCommonString(key, webExtension.title!!)
                }
                else {
                    name = "Untitled"
                }
            }
            webExtensionTitleTextView.text = name
            webExtensionClientIdTextView.text = clientId
        }


        companion object {
            fun create(parent: ViewGroup): WebExtensionViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_web_extension, parent, false)
                return WebExtensionViewHolder(view)
            }
        }
    }

    class WebExtensionsComparator : DiffUtil.ItemCallback<EncWebExtension>() {
        override fun areItemsTheSame(oldItem: EncWebExtension, newItem: EncWebExtension): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: EncWebExtension, newItem: EncWebExtension): Boolean {
            return oldItem.id == newItem.id
        }
    }
}