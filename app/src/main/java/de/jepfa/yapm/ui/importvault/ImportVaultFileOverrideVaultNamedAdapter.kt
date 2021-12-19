package de.jepfa.yapm.ui.importvault

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.EncNamed
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.createAndAddLabelChip
import de.jepfa.yapm.util.enrichId
import kotlin.collections.HashMap

class ImportVaultFileOverrideVaultNamedAdapter(
    private val activity: SecureActivity,
    private val dataMap: HashMap<GroupType, List<ChildType>>
): BaseExpandableListAdapter() {

    enum class GroupType(val titleId: Int) {
        CREDENTIALS_TO_BE_INSERTED(R.string.credentials_to_be_inserted),
        CREDENTIALS_TO_BE_UPDATED(R.string.credentials_to_be_updated),
        LABELS_TO_BE_INSERTED(R.string.labels_to_be_inserted),
        LABELS_TO_BE_UPDATED(R.string.labels_to_be_updated),
    }

    data class ChildType(val id: Int, val origNamed: EncNamed?, val newNamed: EncNamed)

    private var titleList: List<GroupType>
    private val checkedChildren = HashMap<ChildType, GroupType>()

    init {
        dataMap.forEach {group, elems ->
            elems.forEach { elem ->
                checkedChildren[elem] = group
            }
        }
        titleList = GroupType.values().filter { dataMap.keys.contains(it) }.toList()

    }

    override fun getGroupCount(): Int {
        return titleList.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        val group = getGroup(groupPosition)
        return dataMap[group]?.size ?: 0
    }

    override fun getGroup(groupPosition: Int): GroupType {
        return titleList[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): ChildType {
        val group = getGroup(groupPosition)
        return dataMap[group]!![childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return getChild(groupPosition, childPosition).id.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = getInflater().inflate(R.layout.expandable_group_item, null)
        val group = getGroup(groupPosition)
        val childrenCount = getChildrenCount(groupPosition)
        val checkedChildrenCount = getCheckedChildrenCount(group)
        val textView = view.findViewById<TextView>(R.id.expandable_text_view_1)
        textView.setTypeface(null, Typeface.BOLD)
        textView.text = activity.getString(group.titleId, checkedChildrenCount, childrenCount)
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val group = getGroup(groupPosition)
        val child = getChild(groupPosition, childPosition)

        val view = getInflater().inflate(R.layout.expandable_list_item, null)
        val checkBox = view.findViewById<CheckBox>(R.id.expandable_check_box)
        if (!checkBox.hasOnClickListeners()) {
            checkBox.setOnClickListener {
                if (checkBox.isChecked) {
                    checkedChildren[child] = group
                } else {
                    checkedChildren.remove(child)
                }
                notifyDataSetChanged()
            }
            checkBox.isChecked = checkedChildren.containsKey(child)
        }

        val linearLayout = view.findViewById<LinearLayout>(R.id.expandable_linear_layout)

        activity.masterSecretKey?.let { key ->

            val origName = if (child.origNamed != null) SecretService.decryptCommonString(key, child.origNamed!!.name) else null
            val newName = SecretService.decryptCommonString(key, child.newNamed.name)

            if (child.newNamed is EncCredential) {
                if (origName == null || origName == newName) {
                    createAndAddCredentialNameTextView(newName, child.newNamed, linearLayout)
                }
                else {
                    createAndAddCredentialNameTextView(origName, child.newNamed, linearLayout)
                    createAndAddSeparator(linearLayout)
                    createAndAddCredentialNameTextView(newName, child.newNamed, linearLayout)
                }
            }
            else if (child.newNamed is EncLabel) {
                val newEncLabel = child.newNamed as EncLabel
                val origEncLabel = child.origNamed as? EncLabel
                if (origEncLabel == null || (origName == newName && origEncLabel.color == newEncLabel.color)) {
                    val encLabel = child.newNamed
                    val label = LabelService.createLabel(key, encLabel)
                    createAndAddLabelChip(label, linearLayout, true, activity)
                }
                else {
                    val origLabel = LabelService.createLabel(key, origEncLabel)
                    createAndAddLabelChip(origLabel, linearLayout, true, activity)

                    createAndAddSeparator(linearLayout)

                    val newLabel = LabelService.createLabel(key, newEncLabel)
                    createAndAddLabelChip(newLabel, linearLayout, true, activity)
                }
            }
            
        }

        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun getCheckedChildren(): Set<ChildType> = checkedChildren.keys

    private fun getInflater() = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


    private fun createAndAddSeparator(linearLayout: LinearLayout) {
        val separatorView = ImageView(activity)
        separatorView.setImageDrawable(activity.getDrawable(R.drawable.ic_baseline_arrow_forward_12))
        separatorView.setPadding(0, 10, 0, 0)
        linearLayout.addView(separatorView)
    }

    private fun createAndAddCredentialNameTextView(
        name: String,
        credential: EncCredential,
        linearLayout: LinearLayout
    ) {
        val newTextView = TextView(activity)
        newTextView.text = enrichId(activity, name, credential.id)
        newTextView.setTypeface(null, Typeface.BOLD)
        linearLayout.addView(newTextView)
    }

    private fun getCheckedChildrenCount(group: GroupType): Int {
        return checkedChildren.filter { it.value == group }.count()
    }

}