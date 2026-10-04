package com.example.familysafety

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.familysafety.databinding.FragmentManagementBinding
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

/** STEP 6-2：Mock 管理入口；正式管理員權限仍需 Server 驗證。 */
class ManagementFragment : Fragment() {
    // 01. Binding 與 Dialog lifecycle
    private var _binding: FragmentManagementBinding? = null
    private val binding get() = _binding!!
    private val dialogs = mutableSetOf<Dialog>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val basePadding = binding.root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            root.updatePadding(top = basePadding + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        binding.buttonManagementMember.setOnClickListener { showMembers() }
        binding.buttonManagementTaskItem.setOnClickListener { showTaskItems() }
        binding.buttonManagementTask.setOnClickListener { showTasks() }
    }

    private fun show(dialog: Dialog) {
        if (_binding == null || !isAdded) return
        dialogs.add(dialog)
        dialog.setOnDismissListener { dialogs.remove(dialog) }
        dialog.show()
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun date(value: Long) = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
        DateFormat.SHORT, resources.configuration.locales[0]).format(Date(value))
    private fun label(key: Int, value: String) = getString(R.string.management_label_value, getString(key), value)
    private fun role(role: MemberRole) = getString(if (role == MemberRole.ADMIN)
        R.string.management_role_admin else R.string.management_role_member)
    private fun status(status: TaskStatus) = getString(when (status) {
        TaskStatus.PENDING -> R.string.task_status_pending
        TaskStatus.IN_PROGRESS -> R.string.task_status_in_progress
        TaskStatus.COMPLETED -> R.string.task_status_completed
    })

    // 02. Member / Task 列表：簡單 RecyclerView，不重做 Task Center Card
    private fun showList(title: Int, add: Int, id: Int, rows: List<String>, onAdd: () -> Unit) {
        if (_binding == null || !isAdded || dialogs.any { it.isShowing }) return
        val list = RecyclerView(requireContext()).apply {
            this.id = id
            layoutManager = LinearLayoutManager(context)
            setPadding(dp(24), dp(8), dp(24), dp(8))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                minOf(dp(320), resources.displayMetrics.heightPixels / 2))
            adapter = ManagementRowsAdapter(rows)
        }
        show(MaterialAlertDialogBuilder(requireContext()).setTitle(title).setView(list)
            .setNegativeButton(R.string.task_detail_close, null)
            .setPositiveButton(add) { dialog, _ ->
                dialog.dismiss()
                if (_binding != null) onAdd()
            }.create())
    }

    private fun showMembers() = showList(R.string.management_member_title, R.string.management_add_member,
        R.id.recyclerView_management_member, MockMemberDataSource.getMembers().map {
            listOf(it.name, label(R.string.management_account, it.account),
                label(R.string.management_email, it.email), label(R.string.management_role, role(it.role)))
                .joinToString("\n")
        }, ::addMember)

    private fun showTasks() = showList(R.string.management_task_title, R.string.management_add_task,
        R.id.recyclerView_management_task, MockTaskDataSource.getTasks { getString(it) }.map {
            listOf(it.title, getString(R.string.task_assignee, it.assignee),
                getString(R.string.task_due_date, date(it.dueDate)),
                getString(R.string.task_detail_status, status(it.status))).joinToString("\n")
        }, ::addTask)

    private fun showTaskItems() = showList(R.string.management_task_item_title, R.string.management_add_task_item,
        R.id.recyclerView_management_task_item, MockTaskItemDataSource.getItems { getString(it) }.map {
            listOf(it.title, it.description).joinToString("\n")
        }, ::addTaskItem)

    // 03. 表單共用元件：資源文字、必填驗證及可捲動內容
    private fun form() = LinearLayout(requireContext()).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(8), dp(24), dp(8))
    }
    private fun LinearLayout.field(id: Int, hint: Int, type: Int = InputType.TYPE_CLASS_TEXT): EditText =
        EditText(context).apply {
            this.id = id
            setHint(hint)
            inputType = type
            setTextColor(ContextCompat.getColor(context, R.color.title_blue))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            this@field.addView(this)
        }
    private fun LinearLayout.selection(id: Int, label: Int, values: List<String>, selected: Int = 0): Spinner {
        addView(TextView(context).apply { setText(label); setPadding(0, dp(12), 0, dp(4)) })
        return Spinner(context).apply {
            this.id = id
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, values).also {
                it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }
            setSelection(selected)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
            this@selection.addView(this)
        }
    }
    private fun required(vararg fields: EditText): Boolean {
        var valid = true
        fields.forEach {
            it.error = if (it.text.isNullOrBlank()) getString(R.string.management_required).also { valid = false } else null
        }
        return valid
    }
    private fun formDialog(title: Int, content: LinearLayout): AlertDialog =
        MaterialAlertDialogBuilder(requireContext()).setTitle(title)
            .setView(ScrollView(requireContext()).apply { addView(content) })
            .setNegativeButton(R.string.alert_resolve_dialog_cancel, null)
            .setPositiveButton(title, null).create()

    // 04. 新增 Member：不設定密碼、不做 Edit / Delete
    private fun addMember() {
        val content = form()
        val name = content.field(R.id.editText_management_member_name, R.string.management_name)
        val account = content.field(R.id.editText_management_member_account, R.string.management_account)
        val email = content.field(R.id.editText_management_member_email, R.string.management_email,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val roles = MemberRole.entries
        val selection = content.selection(R.id.spinner_management_member_role, R.string.management_role,
            roles.map(::role), roles.indexOf(MemberRole.MEMBER))
        val dialog = formDialog(R.string.management_add_member, content)
        show(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (_binding == null || !required(name, account, email)) return@setOnClickListener
            val member = MockMemberDataSource.addMember(name.text.toString(), account.text.toString(),
                email.text.toString(), roles[selection.selectedItemPosition])
            if (member != null) { dialog.dismiss(); showMembers() }
        }
    }

    // 05. 工作項目：標題必填，說明可空白，不做 Edit / Delete
    private fun addTaskItem() {
        val content = form()
        val title = content.field(R.id.editText_management_task_item_title, R.string.management_task_name)
        val description = content.field(R.id.editText_management_task_item_description,
            R.string.management_task_description_label,
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE).apply { minLines = 2 }
        val dialog = formDialog(R.string.management_add_task_item, content)
        show(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (_binding == null || !required(title)) return@setOnClickListener
            val item = MockTaskItemDataSource.addItem(title.text.toString(), description.text.toString()) { getString(it) }
            if (item != null) { dialog.dismiss(); showTaskItems() }
        }
    }

    // 06. 新增 Task：選擇既有工作項目與成員，未來日期，唯讀 PENDING
    private fun addTask() {
        val content = form()
        val items = MockTaskItemDataSource.getItems { getString(it) }
        val taskItem = content.selection(R.id.spinner_management_task_item, R.string.management_task_item,
            items.map { it.title })
        val members = MockMemberDataSource.getMembers()
        val assignee = content.selection(R.id.spinner_management_task_assignee, R.string.management_assignee, members.map { it.name })
        var dueDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val due = MaterialButton(requireContext()).apply {
            id = R.id.button_management_task_due
            text = getString(R.string.task_due_date, date(dueDate))
            contentDescription = getString(R.string.management_due_date)
            content.addView(this)
        }
        due.setOnClickListener {
            val selected = Calendar.getInstance().apply { timeInMillis = dueDate }
            val picker = DatePickerDialog(requireContext(), { _, year, month, day ->
                if (_binding != null) {
                    dueDate = Calendar.getInstance().apply {
                        set(year, month, day, 23, 59, 59); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    due.text = getString(R.string.task_due_date, date(dueDate))
                    due.error = null
                }
            }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH))
            picker.datePicker.minDate = System.currentTimeMillis()
            show(picker)
        }
        content.addView(TextView(requireContext()).apply {
            id = R.id.textView_management_task_status
            text = getString(R.string.task_detail_status, getString(R.string.task_status_pending))
            setPadding(0, dp(12), 0, dp(12))
            setTextColor(ContextCompat.getColor(context, R.color.title_blue))
        })
        val dialog = formDialog(R.string.management_add_task, content)
        show(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            if (_binding == null) return@setOnClickListener
            if (dueDate <= System.currentTimeMillis()) {
                due.error = getString(R.string.management_future_due); return@setOnClickListener
            }
            val member = members.getOrNull(assignee.selectedItemPosition) ?: return@setOnClickListener
            val item = items.getOrNull(taskItem.selectedItemPosition) ?: return@setOnClickListener
            val task = MockTaskDataSource.addTask(item.id, member.id,
                dueDate, { getString(it) })
            if (task != null) { dialog.dismiss(); showTasks() }
        }
    }

    override fun onDestroyView() {
        dialogs.toList().forEach { it.setOnDismissListener(null); it.dismiss() }
        dialogs.clear()
        _binding = null
        super.onDestroyView()
    }
}

/** 只顯示管理用文字清單，不操作資料來源或導航。 */
private class ManagementRowsAdapter(private val rows: List<String>) : RecyclerView.Adapter<ManagementRowsAdapter.Holder>() {
    class Holder(val text: TextView) : RecyclerView.ViewHolder(text)
    override fun getItemCount() = rows.size
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(TextView(parent.context).apply {
        setTextColor(ContextCompat.getColor(context, R.color.title_blue))
        textSize = 16f
        val padding = (12 * resources.displayMetrics.density).toInt()
        setPadding(0, padding, 0, padding)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    })
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.text.text = rows[position] }
}
