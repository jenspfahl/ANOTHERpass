package de.jepfa.yapm.ui.importvault

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
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
import de.jepfa.yapm.ui.ViewRecyclerViewAdapter
import de.jepfa.yapm.ui.credential.ShowCredentialActivity
import de.jepfa.yapm.ui.label.Label
import de.jepfa.yapm.ui.label.LabelDialogs
import de.jepfa.yapm.util.createLabelChip


class ImportVaultFileOverrideVaultNamedAdapter(
    private val selectNoneAll: CheckBox,
    private val activity: SecureActivity,
    private val groupType: GroupType,
    private val dataMap: List<ChildType>,
    val checkedChildren: MutableSet<ChildType>
): BaseExpandableListAdapter() {

    enum class GroupType(val titleId: Int) {
        CREDENTIALS_TO_BE_INSERTED(R.string.credentials_to_be_inserted),
        CREDENTIALS_TO_BE_UPDATED(R.string.credentials_to_be_updated),
        LABELS_TO_BE_INSERTED(R.string.labels_to_be_inserted),
        LABELS_TO_BE_UPDATED(R.string.labels_to_be_updated),
    }

    data class ChildType(val id: Int, val origNamed: EncNamed?, val newNamed: EncNamed)

    private val checkBoxes = HashMap<CheckBox, GroupType>()

    init {
        updateSelectNoneAll()
    }

    override fun getGroupCount(): Int {
        return 1
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return dataMap.size
    }

    override fun getGroup(groupPosition: Int): GroupType {
        return groupType
    }

    override fun getChild(groupPosition: Int, childPosition: Int): ChildType {
        return dataMap[childPosition]
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
        val checkedChildrenCount = checkedChildren.count()
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
                    checkedChildren.add(child)
                } else {
                    checkedChildren.remove(child)
                }
                updateSelectNoneAll()
                notifyDataSetChanged()
            }
            checkBox.isChecked = checkedChildren.contains(child)
        }
        checkBoxes[checkBox] = group

        val namedContainer = view.findViewById<RecyclerView>(R.id.expandable_container)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        namedContainer.layoutManager = layoutManager

        activity.masterSecretKey?.let { key ->

            val origName = if (child.origNamed != null) SecretService.decryptCommonString(key, child.origNamed.name) else null
            val newName = SecretService.decryptCommonString(key, child.newNamed.name)
            val views = ArrayList<View>()

            if (child.newNamed is EncCredential) {
                if (origName == null) {
                    createAndAddCredentialNameTextView(newName, child.newNamed, views, isExternal = true)
                }
                else if (child.origNamed is EncCredential) {
                    createAndAddCredentialNameTextView(origName, child.origNamed, views, isExternal = false)
                    createAndAddSeparator(views)
                    createAndAddCredentialNameTextView(newName, child.newNamed, views, isExternal = true)
                }
            }
            else if (child.newNamed is EncLabel) {
                val newEncLabel = child.newNamed
                val origEncLabel = child.origNamed as? EncLabel
                if (origEncLabel == null) {
                    val encLabel = child.newNamed
                    val label = LabelService.createLabel(key, encLabel)
                    views += createLabel(label)
                }
                else {
                    val origLabel = LabelService.createLabel(key, origEncLabel)
                    views += createLabel(origLabel)

                    createAndAddSeparator(views)

                    val newLabel = LabelService.createLabel(key, newEncLabel)
                    views += createLabel(newLabel)
                }
            }

            namedContainer.adapter = ViewRecyclerViewAdapter(activity, views)

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
        updateSelectNoneAll()
        notifyDataSetChanged()
    }

    private fun startShowCredentialActivity(credential: EncCredential, isExternal: Boolean) {
        val intent = Intent(activity, ShowCredentialActivity::class.java)
        credential.applyExtras(intent)
        if (isExternal) {
            intent.putExtra(
                ShowCredentialActivity.EXTRA_MODE,
                ShowCredentialActivity.EXTRA_MODE_SHOW_EXTERNAL_FROM_FILE
            )
        }
        else {
            intent.putExtra(
                ShowCredentialActivity.EXTRA_MODE,
                ShowCredentialActivity.EXTRA_MODE_SHOW_NORMAL_READONLY
            )
        }
        activity.startActivity(intent)
    }

    private fun getInflater() = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private fun updateSelectNoneAll() {
        selectNoneAll.isChecked = checkedChildren.isNotEmpty()
    }

    private fun createAndAddSeparator(views: ArrayList<View>) {
        val separatorView = ImageView(activity)
        separatorView.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_baseline_arrow_forward_12))
        separatorView.setPadding(10, 0, 10, 0)
        views.add(separatorView)
    }

    private fun createAndAddCredentialNameTextView(
        name: String,
        credential: EncCredential,
        views: ArrayList<View>,
        isExternal: Boolean
    ) {
        val textView = TextView(activity)
        textView.text = enrichId(activity, name, credential.id)
        textView.textSize = 16f
        textView.setTextAppearance(R.style.credential_title)
        textView.setOnClickListener {
            startShowCredentialActivity(credential, isExternal)
        }

        views.add(textView)
    }

    private fun fillCheckedChildrenWithAllNamed() {
        dataMap.forEach { elem ->
                checkedChildren.add(elem)
        }
    }


}