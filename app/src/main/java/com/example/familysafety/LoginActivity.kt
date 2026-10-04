package com.example.familysafety

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.familysafety.databinding.ActivityLoginBinding
import com.google.android.material.snackbar.Snackbar

/** STEP 7：每次正常啟動均先登入，沒有記住登入或自動登入。 */
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAuthInsets(binding.root)
        showResetSuccess()
        binding.buttonLoginSignIn.setOnClickListener {
            if (!requiredAuthFields(binding.editTextLoginAccount, binding.editTextLoginPassword)) return@setOnClickListener
            val member = MockAuthDataSource.login(binding.editTextLoginAccount.text.toString(), binding.editTextLoginPassword.text.toString())
            if (member == null) {
                binding.editTextLoginPassword.setAuthError(getString(R.string.auth_login_failed))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        binding.textViewLoginForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        binding.editTextLoginPassword.text?.clear()
        binding.editTextLoginPassword.setAuthError(null)
        showResetSuccess()
    }

    private fun showResetSuccess() {
        if (intent.getBooleanExtra(AuthNavigation.EXTRA_RESET_SUCCESS, false)) {
            intent.removeExtra(AuthNavigation.EXTRA_RESET_SUCCESS)
            Snackbar.make(binding.root, R.string.auth_reset_success, Snackbar.LENGTH_LONG).show()
        }
    }
}
