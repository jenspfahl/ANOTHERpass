package de.jepfa.yapm.ui.webextension

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncWebExtension
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.*
import org.json.JSONObject
import java.math.BigInteger


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

        holder.listenForDetailsWebExtension { pos, _ ->
            val current = getItem(pos)
            WebExtensionDialogs.openWebExtensionDetails(current, listWebExtensionsActivity)
        }

        holder.listenForUnlinkWebExtension { pos, _ ->

            if (!Session.isDenied()) {
                val current = getItem(pos)
                listWebExtensionsActivity.masterSecretKey?.let { key ->
                    val webClientId = SecretService.decryptCommonString(key, current.webClientId)
                    current.enabled = !current.enabled
                    listWebExtensionsActivity.webExtensionViewModel.save(current,listWebExtensionsActivity)
                    if (current.enabled) {
                        toastText(listWebExtensionsActivity,
                            listWebExtensionsActivity.getString(R.string.device_xx_linked, webClientId))
                    }
                    else {
                        toastText(listWebExtensionsActivity,
                            listWebExtensionsActivity.getString(R.string.device_xx_unlinked, webClientId))
                    }
                }
            }
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
        private val webExtensionUnlinkImageView: ImageView = itemView.findViewById(R.id.web_extension_unlink)
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

        fun listenForDetailsWebExtension(event: (position: Int, type: Int) -> Unit) {
            webExtensionTitleTextView.setOnLongClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener true
                }
                event.invoke(adapterPosition, itemViewType)
                true
            }
            webExtensionClientIdTextView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }


        fun listenForUnlinkWebExtension(event: (position: Int, type: Int) -> Unit) {
            webExtensionUnlinkImageView.setOnClickListener {
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
                name = SecretService.decryptCommonString(key, webExtension.title)

            }
            webExtensionTitleTextView.text = name
            if (!webExtension.enabled) {
                webExtensionUnlinkImageView.setImageDrawable(context.getDrawable(R.drawable.baseline_phonelink_off_24))
                webExtensionClientIdTextView.paintFlags =
                    webExtensionClientIdTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            }
            else {
                webExtensionUnlinkImageView.setImageDrawable(context.getDrawable(R.drawable.baseline_phonelink_24))
            }
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
