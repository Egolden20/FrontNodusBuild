package com.example.frontnodus.ui.activities

import com.example.frontnodus.R

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import com.example.frontnodus.data.repository.AuthRepository
import com.example.frontnodus.utils.UserUtils

class SignInActivity : AppCompatActivity() {
    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        // Back button functionality
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }

        // Sign Up link functionality
        val tvSignUpLink = findViewById<TextView>(R.id.tvSignUpLink)
        tvSignUpLink.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Sign In button functionality
        val btnSignIn = findViewById<Button>(R.id.btnSignIn)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        btnSignIn.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call backend via AuthRepository
            lifecycleScope.launch {
                try {
                    val result = authRepository.login(email, password)

                    val displayName = UserUtils.extractDisplayName(result.user)
                    Toast.makeText(this@SignInActivity, "Bienvenido: $displayName", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SignInActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@SignInActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
