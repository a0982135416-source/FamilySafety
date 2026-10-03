package com.example.familysafety

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.familysafety.databinding.FragmentManagementBinding
import com.google.android.material.snackbar.Snackbar

/**
 * ========================================================================
 * 管理中心頁面
 * Management Center Fragment
 * ========================================================================
 *
 * 此 Fragment 負責管理家庭成員與任務。
 * This Fragment manages family members and tasks.
 *
 * 注意：
 * Note:
 *
 * 正式版本除了 Android UI 權限控制之外，
 * Server API 也必須再次驗證管理員權限。
 *
 * In the final version, administrator permissions must also be
 * validated by the Server API, not only by the Android UI.
 */
class ManagementFragment : Fragment() {

    // ================================================================
    // 01. ViewBinding
    // ViewBinding
    // ================================================================

    private var _binding: FragmentManagementBinding? = null

    private val binding
        get() = _binding!!


    // ================================================================
    // 02. 建立 Fragment View
    // Create Fragment View
    // ================================================================

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentManagementBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    // ================================================================
    // 03. Dashboard：安全區域與暫時入口，不執行 CRUD
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val basePadding = binding.root.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { root, insets ->
            root.updatePadding(top = basePadding + insets.getInsets(WindowInsetsCompat.Type.statusBars()).top)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
        listOf(binding.buttonManagementMember, binding.buttonManagementTask).forEach { button ->
            button.setOnClickListener {
                Snackbar.make(binding.root, R.string.management_in_development, Snackbar.LENGTH_SHORT)
                    .setAnchorView(R.id.bottomNavigationView_main_navigation)
                    .show()
            }
        }
    }

    // 04. 銷毀 ViewBinding
    // Destroy ViewBinding
    // ================================================================

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
