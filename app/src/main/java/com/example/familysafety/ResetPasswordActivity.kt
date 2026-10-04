package com.example.familysafety

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.familysafety.databinding.ActivityResetPasswordBinding

/** STEP 7：成功重設後消耗驗證授權，清除整個重設返回堆疊。 */
class ResetPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResetPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val email = intent.getStringExtra(AuthNavigation.EXTRA_EMAIL).orEmpty()
        if (!MockAuthDataSource.isResetVerified(email)) { AuthNavigation.returnToLogin(this); return }
        binding = ActivityResetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAuthInsets(binding.root)
        binding.textViewResetBackArrow.setOnClickListener { finish() }
        binding.buttonResetSubmit.setOnClickListener {
            if (!requiredAuthFields(binding.editTextResetNewPassword, binding.editTextResetConfirmPassword)) return@setOnClickListener
            val password = binding.editTextResetNewPassword.text.toString()
            val confirmation = binding.editTextResetConfirmPassword.text.toString()
            when (validatePassword(password, confirmation)) {
                PasswordValidation.REQUIRED -> return@setOnClickListener
                PasswordValidation.RULE -> binding.editTextResetNewPassword.setAuthError(getString(R.string.auth_password_rules))
                PasswordValidation.MISMATCH -> binding.editTextResetConfirmPassword.setAuthError(getString(R.string.auth_password_mismatch))
                null -> when {
                    MockAuthDataSource.resetPassword(email, password) -> AuthNavigation.returnToLogin(this, success = true)
                    else -> AuthNavigation.returnToLogin(this)
                }
            }
        }
    }
}
