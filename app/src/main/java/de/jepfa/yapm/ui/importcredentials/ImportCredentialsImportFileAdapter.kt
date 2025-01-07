package de.jepfa.yapm.ui.importcredentials

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.jepfa.yapm.R
import de.jepfa.yapm.model.encrypted.EncCredential
import de.jepfa.yapm.service.io.CredentialFileRecord
import de.jepfa.yapm.service.label.LabelService
import de.jepfa.yapm.ui.SecureActivity
import de.jepfa.yapm.ui.ViewRecyclerViewAdapter
import de.jepfa.yapm.ui.credential.ShowCredentialActivity
import java.util.*


class ImportCredentialsImportFileAdapter(
    private val selectNoneAll: CheckBox,
    private val activity: SecureActivity,
    private val dataMap: List<CredentialFileRecord>,
    val checkedChildren: MutableSet<CredentialFileRecord>
): BaseExpandableListAdapter() {


    private val checkBoxes = ArrayList<CheckBox>()

    init {
        updateSelectNoneAll()
    }

    override fun getGroupCount(): Int {
        return 1
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return dataMap.size
    }

    override fun getGroup(groupPosition: Int): String {
        return "group"
    }

    override fun getChild(groupPosition: Int, childPosition: Int): CredentialFileRecord {
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
        val childrenCount = getChildrenCount(groupPosition)
        val checkedChildrenCount = checkedChildren.count()
        val textView = view.findViewById<TextView>(R.id.expandable_text_view_1)
        textView.setTypeface(null, Typeface.BOLD)
        textView.text = activity.getString(R.string.credentials_to_be_imported, checkedChildrenCount, childrenCount)
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val child = getChild(groupPosition, childPosition)

        val view = getInflater().inflate(R.layout.expandable_list_item, null)
        var checkBox = view.findViewById<CheckBox>(R.id.expandable_check_box)
        if (!checkBox.hasOnClickListeners()) {
            checkBox.setOnClickListener {
                if (checkBox?.isChecked?:false) {
                    checkedChildren.add(child)
                } else {
                    checkedChildren.remove(child)
                }
                updateSelectNoneAll()
                notifyDataSetChanged()
            }
            checkBox.isChecked = checkedChildren.contains(child)
        }

        val namedContainer = view.findViewById<RecyclerView>(R.id.expandable_container)
        val layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
        namedContainer.layoutManager = layoutManager

        activity.masterSecretKey?.let { key ->

            val views = ArrayList<View>()

            createAndAddCredentialNameTextView(activity as ImportCredentialsActivity, child, views, isExternal = true)

            namedContainer.adapter = ViewRecyclerViewAdapter(activity, views)

        }

        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun selectNoneAllClicked() {
        val nonSelected = checkedChildren.isEmpty()
        checkBoxes.forEach {
            it.isChecked = nonSelected
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
        activity: ImportCredentialsActivity,
        record: CredentialFileRecord,
        views: ArrayList<View>,
        isExternal: Boolean
    ) {
        val textView = TextView(this.activity)
        textView.text = record.name
        textView.textSize = 16f
        textView.setTextAppearance(R.style.credential_title)
        textView.setOnClickListener {
            this.activity.masterSecretKey?.let { key ->
                val credential = activity.createCredentialFromRecord(key, record, emptyList(), LabelService.externalHolder)
                startShowCredentialActivity(credential, isExternal)
            }
        }

        views.add(textView)
    }


    private fun fillCheckedChildrenWithAllNamed() {
        dataMap.forEach { elem ->
                checkedChildren.add(elem)
        }
    }


}