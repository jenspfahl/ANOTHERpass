package de.jepfa.yapm.ui.usernametemplate

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncUsernameTemplate
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.util.*


class ListUsernameTemplatesAdapter(private val listUsernameTemplatesActivity: ListUsernameTemplatesActivity) :
        ListAdapter<EncUsernameTemplate, ListUsernameTemplatesAdapter.UsernameTemplateViewHolder>(UsernameTemplatesComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsernameTemplateViewHolder {
        val holder = UsernameTemplateViewHolder.create(parent)

        if (Session.isDenied()) {
            return holder
        }

        holder.listenForEditUsernameTemplate { pos, _ ->
            val current = getItem(pos)

            val intent = Intent(listUsernameTemplatesActivity, EditUsernameTemplateActivity::class.java)
            intent.putExtra(EncUsernameTemplate.EXTRA_USERNAME_TEMPLATE_ID, current.id)
            listUsernameTemplatesActivity.startActivity(intent)

        }

        holder.listenForDeleteUsernameTemplate { pos, _ ->

            if (!Session.isDenied()) {
                val current = getItem(pos)
                UsernameTemplateDialogs.openDeleteUsernameTemplate(current, listUsernameTemplatesActivity)
            }
        }


        return holder
    }


    override fun onBindViewHolder(holder: UsernameTemplateViewHolder, position: Int) {
        val current = getItem(position)
        val key = listUsernameTemplatesActivity.masterSecretKey
        holder.bind(key, current)
    }

    class UsernameTemplateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTemplateUsernameTextView: TextView = itemView.findViewById(R.id.username_template_username)
        private val usernameTemplateGeneratorTypeTextView: TextView = itemView.findViewById(R.id.username_template_type)
        private val usernameTemplateDeleteImageView: ImageView = itemView.findViewById(R.id.username_template_delete)

        fun listenForEditUsernameTemplate(event: (position: Int, type: Int) -> Unit) {
            usernameTemplateUsernameTextView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
            usernameTemplateGeneratorTypeTextView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }


        fun listenForDeleteUsernameTemplate(event: (position: Int, type: Int) -> Unit) {
            usernameTemplateDeleteImageView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun bind(key: SecretKeyHolder?, usernameTemplate: EncUsernameTemplate) {
            var name = itemView.context.getString(R.string.unknown_placeholder)
            var type = itemView.context.getString(R.string.unknown_placeholder)
            if (key != null) {
                name = SecretService.decryptCommonString(key, usernameTemplate.username)
                val typeAsInt = SecretService.decryptLong(key, usernameTemplate.generatorType) ?: 0
                type = translateType(EncUsernameTemplate.GeneratorType.values()[typeAsInt.toInt()])
            }
            usernameTemplateUsernameTextView.text = name
            usernameTemplateGeneratorTypeTextView.text = type
        }

        private fun translateType(generatorType: EncUsernameTemplate.GeneratorType): String {
            return when (generatorType) {
                EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_CREDENTIAL_NAME_BASED -> "email extension"
                EncUsernameTemplate.GeneratorType.EMAIL_EXTENSION_CREDENTIAL_RANDOM_BASED -> "email extension"
                else -> "email extension" // "" //TODO mockup
            }
        }


        companion object {
            fun create(parent: ViewGroup): UsernameTemplateViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_username_template, parent, false)
                return UsernameTemplateViewHolder(view)
            }
        }
    }

    class UsernameTemplatesComparator : DiffUtil.ItemCallback<EncUsernameTemplate>() {
        override fun areItemsTheSame(oldItem: EncUsernameTemplate, newItem: EncUsernameTemplate): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: EncUsernameTemplate, newItem: EncUsernameTemplate): Boolean {
            return oldItem.id == newItem.id
        }
    }
}
