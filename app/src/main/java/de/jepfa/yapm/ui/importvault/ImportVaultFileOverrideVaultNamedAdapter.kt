package de.jepfa.yapm.ui.importvault

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.model.encrypted.EncLabel
import de.jepfa.yapm.model.encrypted.EncNamed
import de.jepfa.yapm.service.secret.SecretService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.util.enrichId
import kotlin.collections.HashMap
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.HorizontalScrollableViewAdapter
import de.jepfa.yapm.ui.credential.ShowCredentialActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.LabelDialogs
import de.jepfa.yapm.util.createLabelChip


class ImportVaultFileOverrideVaultNamedAdapter(
    private val selectNoneAll: CheckBox,
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
    private val checkBoxes = HashMap<CheckBox, GroupType>()

    private val MAX_NAMED_LENGTH = 15

    init {
        fillCheckedChildrenWithAllNamed()
        selectNoneAll.isChecked = true

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
                selectNoneAll.isChecked = checkedChildren.isNotEmpty()
                notifyDataSetChanged()
            }
            checkBox.isChecked = checkedChildren.containsKey(child)
        }
        checkBoxes[checkBox] = group

        val namedContainer = view.findViewById<RecyclerView>(R.id.expandable_container)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        namedContainer.layoutManager = layoutManager

        activity.masterSecretKey?.let { key ->

            val origName = if (child.origNamed != null) SecretService.decryptCommonString(key, child.origNamed!!.name) else null
            val newName = SecretService.decryptCommonString(key, child.newNamed.name)
            val views = ArrayList<View>()

            if (child.newNamed is EncCredential) {
                if (origName == null) {
                    createAndAddCredentialNameTextView(newName, child.newNamed, views)
                }
                else if (child.origNamed is EncCredential) {
                    createAndAddCredentialNameTextView(origName, child.origNamed, views)
                    createAndAddSeparator(views)
                    createAndAddCredentialNameTextView(newName, child.newNamed, views)
                }
            }
            else if (child.newNamed is EncLabel) {
                val newEncLabel = child.newNamed as EncLabel
                val origEncLabel = child.origNamed as? EncLabel
                if (origEncLabel == null) {
                    val encLabel = child.newNamed
                    val label = LabelService.defaultHolder.createLabel(key, encLabel)
                    views += createLabel(label)
                }
                else {
                    val origLabel = LabelService.defaultHolder.createLabel(key, origEncLabel)
                    views += createLabel(origLabel)

                    createAndAddSeparator(views)

                    val newLabel = LabelService.defaultHolder.createLabel(key, newEncLabel)
                    views += createLabel(newLabel)
                }
            }

            namedContainer.adapter = HorizontalScrollableViewAdapter(activity, views)

        }

        return view
    }

    private fun createLabel(label: Label): Chip {
        val chip = createLabelChip(label, thinner = true, activity)
        chip.setOnClickListener {
            LabelDialogs.openLabelOverviewDialog(activity, label, showChangeLabelButton = false)
        }
        return chip
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun getCheckedChildren(): Set<ChildType> = checkedChildren.keys

    fun selectNoneAllClicked() {
        val nonSelected = checkedChildren.isEmpty()
        checkBoxes.forEach {
            it.key.isChecked = nonSelected
        }
        if (nonSelected) {
            fillCheckedChildrenWithAllNamed()
        }
        else {
            checkedChildren.clear()
        }
        selectNoneAll.isChecked = checkedChildren.isNotEmpty()
        notifyDataSetChanged()
    }

    private fun startShowCredentialActivity(credential: EncCredential) {
        val intent = Intent(activity, ShowCredentialActivity::class.java)
        credential.applyExtras(intent)
        intent.putExtra(ShowCredentialActivity.EXTRA_MODE, ShowCredentialActivity.EXTRA_MODE_SHOW_EXTERNAL_FROM_VAULT_FILE)
        activity.startActivity(intent)
    }

    private fun getInflater() = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater


    private fun createAndAddSeparator(views: ArrayList<View>) {
        val separatorView = ImageView(activity)
        separatorView.setImageDrawable(activity.getDrawable(R.drawable.ic_baseline_arrow_forward_12))
        views.add(separatorView)
    }

    private fun createAndAddCredentialNameTextView(
        name: String,
        credential: EncCredential,
        views: ArrayList<View>
    ) {
        val textView = TextView(activity)
        textView.text = enrichId(activity, name, credential.id)
        textView.setTypeface(null, Typeface.BOLD)
        textView.setOnClickListener {
            startShowCredentialActivity(credential)
        }

        views.add(textView)
    }

    private fun getCheckedChildrenCount(group: GroupType): Int {
        return checkedChildren.filter { it.value == group }.count()
    }

    private fun fillCheckedChildrenWithAllNamed() {
        dataMap.forEach { group, elems ->
            elems.forEach { elem ->
                checkedChildren[elem] = group
            }
        }
    }


}