package de.jepfa.yapm.ui.label

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pchmn.materialchips.ChipView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.Session
import de.jepfa.yapm.service.label.LabelService
import javax.crypto.SecretKey


class ListLabelsAdapter(val listLabelsActivity: ListLabelsActivity) :
        ListAdapter<LabelService.Label, ListLabelsAdapter.LabelViewHolder>(LabelsComparator()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelViewHolder {
        val holder = LabelViewHolder.create(parent)

        if (Session.isDenied()) {
            return holder
        }

        holder.listenForEditLabel { pos, _ ->
            val current = getItem(pos)

            val intent = Intent(listLabelsActivity, EditLabelActivity::class.java)
            intent.putExtra(EncLabel.EXTRA_LABEL_ID, current.encLabel.id)
            listLabelsActivity.startActivity(intent)

        }

        holder.listenForDeleteLabel { pos, _ ->

            if (!Session.isDenied()) {
                val current = getItem(pos)

                AlertDialog.Builder(listLabelsActivity)
                        .setTitle(R.string.title_delete_label)
                        .setMessage(listLabelsActivity.getString(R.string.message_delete_label, current.labelChip.label))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes) { dialog, whichButton ->
                            listLabelsActivity.deleteLabel(current)
                        }
                        .setNegativeButton(android.R.string.no, null)
                        .show()
            }

        }

        return holder
    }


    override fun onBindViewHolder(holder: LabelViewHolder, position: Int) {
        val current = getItem(position)
        val key = listLabelsActivity.masterSecretKey
        holder.bind(key, current)
    }

    class LabelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val labelChipView: ChipView = itemView.findViewById(R.id.label_chip)
        private val labelUsageTextView: TextView = itemView.findViewById(R.id.label_usage)
        private val labelDeleteImageView: ImageView = itemView.findViewById(R.id.label_delete)

        fun listenForEditLabel(event: (position: Int, type: Int) -> Unit) {
            labelChipView.setOnChipClicked {
                event.invoke(adapterPosition, itemViewType)
            }
            labelUsageTextView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun listenForDeleteLabel(event: (position: Int, type: Int) -> Unit) {
            labelDeleteImageView.setOnClickListener {
                event.invoke(adapterPosition, itemViewType)
            }
        }

        fun bind(key: SecretKey?, label: LabelService.Label) {
            var name = "????"
            if (key != null) {
                name = label.labelChip.label

            }
            labelChipView.label = name
            labelChipView.setChipBackgroundColor(label.labelChip.getColor(itemView.context))
            labelChipView.setLabelColor(ContextCompat.getColor(itemView.context, R.color.white))
            val labelId = label.encLabel.id
            if (labelId != null) {
                val usageCount = LabelService.getCredentialIdsForLabelId(labelId)?.size
                if (usageCount == null || usageCount == 0) {
                    labelUsageTextView.text = "Never used"
                }
                else if (usageCount == 1) {
                    labelUsageTextView.text = "Used $usageCount time"
                }
                else if (usageCount > 1) {
                    labelUsageTextView.text = "Used $usageCount times"

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

    class LabelsComparator : DiffUtil.ItemCallback<LabelService.Label>() {
        override fun areItemsTheSame(oldItem: LabelService.Label, newItem: LabelService.Label): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: LabelService.Label, newItem: LabelService.Label): Boolean {
            return oldItem.encLabel.id == newItem.encLabel.id
        }
    }
}
