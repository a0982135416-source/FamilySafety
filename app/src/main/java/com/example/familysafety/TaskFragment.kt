package com.example.familysafety

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.familysafety.databinding.FragmentTaskBinding

/**
 * ========================================================================
 * 任務中心頁面
 * Task Center Fragment
 * ========================================================================
 *
 * 此 Fragment 負責顯示家庭任務。
 * This Fragment displays family tasks.
 */
class TaskFragment : Fragment() {

    // ================================================================
    // 01. ViewBinding
    // ViewBinding
    // ================================================================

    private var _binding: FragmentTaskBinding? = null

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

        _binding = FragmentTaskBinding.inflate(
            inflater,
            container,
            false
        )

        return binding.root
    }


    // ================================================================
    // 03. 銷毀 ViewBinding
    // Destroy ViewBinding
    // ================================================================

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}