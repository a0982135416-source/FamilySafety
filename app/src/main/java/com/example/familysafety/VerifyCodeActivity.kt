package com.example.familysafety

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.widget.doAfterTextChanged
import com.example.familysafety.databinding.ActivityVerifyCodeBinding

/** STEP 7：沿用六個 OTP 欄位，驗證成功才授權重設指定 Email。 */
class VerifyCodeActivity : AppCompatActivity() {
    private var errorDialog: AlertDialog? = null
    private lateinit var binding: ActivityVerifyCodeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra(AuthNavigation.EXTRA_EMAIL).orEmpty()
        if (!MockAuthDataSource.emailExists(email)) { AuthNavigation.returnToLogin(this); return }
        binding = ActivityVerifyCodeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAuthInsets(binding.root)
        binding.textViewVerifyMaskedEmail.text = email
        binding.textViewVerifyBackArrow.setOnClickListener { finish() }
        val fields = listOf(binding.editTextVerifyOtp1, binding.editTextVerifyOtp2, binding.editTextVerifyOtp3,
            binding.editTextVerifyOtp4, binding.editTextVerifyOtp5, binding.editTextVerifyOtp6)
        fields.forEachIndexed { index, field ->
            field.contentDescription = getString(R.string.auth_code_digit, index + 1)
            field.doAfterTextChanged {
                if (it?.length == 1 && field.hasFocus()) fields.getOrNull(index + 1)?.requestFocus()
            }
        }
        binding.buttonVerifySubmit.setOnClickListener {
            fields.forEach { it.error = null }
            val code = fields.joinToString("") { it.text.toString() }
            when {
                !code.matches(Regex("[0-9]{6}")) -> fields.first().error = getString(R.string.auth_code_invalid)
                !MockAuthDataSource.verifyResetCode(email, code) -> {
                    if (errorDialog?.isShowing == true) return@setOnClickListener
                    val dialog = MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.auth_code_error_title).setMessage(R.string.auth_code_error_message)
                        .setCancelable(false).setPositiveButton(R.string.auth_ok) { _, _ ->
                            fields.forEach { it.text?.clear(); it.error = null }
                            fields.first().requestFocus()
                        }.create()
                    errorDialog = dialog
                    dialog.setOnDismissListener { if (errorDialog === dialog) errorDialog = null }
                    dialog.show()
                }
                else -> startActivity(Intent(this, ResetPasswordActivity::class.java).putExtra(AuthNavigation.EXTRA_EMAIL, email))
            }
        }
    }
    override fun onDestroy() {
        errorDialog?.dismiss()
        errorDialog = null
        super.onDestroy()
    }
}
