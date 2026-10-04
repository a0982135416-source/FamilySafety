package com.example.familysafety

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.appcompat.app.AppCompatActivity
import com.example.familysafety.databinding.ActivityForgotPasswordBinding

/** STEP 7：驗證現有成員 Email，僅進入 Mock 流程，不寄送 Email。 */
class ForgotPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPasswordBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupAuthInsets(binding.root)
        binding.textViewForgotBack.setOnClickListener { finish() }
        binding.textViewForgotBackLogin.setOnClickListener { finish() }
        binding.buttonForgotSendCode.setOnClickListener {
            if (!requiredAuthFields(binding.editTextForgotEmail)) return@setOnClickListener
            val email = binding.editTextForgotEmail.text.toString().trim()
            when {
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> binding.editTextForgotEmail.error = getString(R.string.auth_email_invalid)
                !MockAuthDataSource.beginPasswordReset(email) -> binding.editTextForgotEmail.error = getString(R.string.auth_email_unknown)
                else -> startActivity(Intent(this, VerifyCodeActivity::class.java).putExtra(AuthNavigation.EXTRA_EMAIL, email))
            }
        }
    }
}
