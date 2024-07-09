package de.jepfa.yapm.ui.label

import android.content.Intent
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.secret.SecretKeyHolder
import de.jepfa.yapm.model.session.Session
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.util.*


class ListLabelsAdapter(private val listLabelsActivity: ListLabelsActivity) :
        ListAdapter<Label, ListLabelsAdapter.LabelViewHolder>(LabelsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val holder = LabelViewHolder.create(parent)

        if (Session.isDenied()) {
            return holder
        }

        holder.listenForEditLabel { pos, _ ->
            val current = getItem(pos)

            val intent = Intent(listLabelsActivity, EditLabelActivity::class.java)
            intent.putExtra(EncLabel.EXTRA_LABEL_ID, current.labelId)
            listLabelsActivity.startActivity(intent)

        }

        holder.listenForDeleteLabel { pos, _ ->

            if (!Session.isDenied()) {
                val current = getItem(pos)
                LabelDialogs.openDeleteLabel(current, listLabelsActivity)
            }
        }

        holder.listenForLongClick { pos, _ ->
            val current = getItem(pos)
            current.labelId?.let { id ->
                listLabelsActivity.labelViewModel.getById(id).observeOnce(listLabelsActivity) { encLabel ->
                    val sb = StringBuilder()

                    encLabel.id?.let { sb.addFormattedLine(listLabelsActivity.getString(R.string.identifier), it)}
                    encLabel.uid?.let {
                        sb.addFormattedLine(
                            listLabelsActivity.getString(R.string.universal_identifier),
                            it.toReadableString())
                    }
                    sb.addFormattedLine(listLabelsActivity.getString(R.string.name), current.name)
                    AlertDialog.Builder(listLabelsActivity)
                        .setTitle(R.string.title_label_details)
                        .setMessage(sb.toString())
                        .show()
                }

            }
            true

        }

        return holder
    }


    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val current = getItem(position)
        val key = listLabelsActivity.masterSecretKey
        holder.bind(key, current)
    }

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelChip: Chip = itemView.findViewById(R.id.label_chip)
        private val labelUsageTextView: TextView = itemView.findViewById(R.id.label_usage)
        private val labelDeleteImageView: ImageView = itemView.findViewById(R.id.label_delete)

        fun listenForEditLabel(event: (position: Int, type: Int) -> Unit) {
            labelChip.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
            labelUsageTextView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForLongClick(event: (position: Int, type: Int) -> Unit) {
            labelChip.setOnLongClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnLongClickListener false
                }
                event.invoke(adapterPosition, itemViewType)
                true
            }
        }

        fun listenForDeleteLabel(event: (position: Int, type: Int) -> Unit) {
            labelDeleteImageView.setOnClickListener {
                if (adapterPosition == RecyclerView.NO_POSITION) {
                    return@setOnClickListener
                }
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun bind(key: SecretKeyHolder?, label: Label) {
            var name = itemView.context.getString(R.string.unknown_placeholder)
            if (key != null) {
                name = label.name

            }
            labelChip.text = name
            labelChip.chipBackgroundColor = ColorStateList.valueOf(label.getColor(itemView.context))
            val labelId = label.labelId
            if (labelId != null) {
                val usageCount = LabelService.defaultHolder.getCredentialIdsForLabelId(labelId)?.size
                if (usageCount == null || usageCount == 0) {
                    labelUsageTextView.text = itemView.context.getString(R.string.label_never_used)
                }
                else if (usageCount == 1) {
                    labelUsageTextView.text = itemView.context.getString(R.string.label_used_once, usageCount)
                }
                else if (usageCount > 1) {
                    labelUsageTextView.text = itemView.context.getString(R.string.label_used_many, usageCount)

                }
            }
        }


        companion object {
            fun create(parent: ViewGroup): LabelViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                        .inflate(R.layout.recyclerview_label, parent, false)
                return LabelViewHolder(view)
            }
        }
    }

    class LabelsComparator : DiffUtil.ItemCallback<Label>() {
        override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean {
            return oldItem.labelId == newItem.labelId
        }
    }
}
