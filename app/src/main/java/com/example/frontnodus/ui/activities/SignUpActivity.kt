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
import org.json.JSONObject

class SignUpActivity : AppCompatActivity() {
    private val authRepository: AuthRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Back button functionality
        val ivBack = findViewById<ImageView>(R.id.ivBack)
        ivBack.setOnClickListener {
            finish()
        }

        // Sign In link functionality
        val tvSignInLink = findViewById<TextView>(R.id.tvSignInLink)
        tvSignInLink.setOnClickListener {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Sign Up button functionality
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)

        btnSignUp.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Ingresa email y contraseña", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    // Build profile JSON from form fields (firstName/lastName from Full Name)
                    val profile = JSONObject()
                    try {
                            val firstField = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFirstName)
                            val lastField = findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etLastName)
                            val first = firstField?.text?.toString()?.trim()
                            val last = lastField?.text?.toString()?.trim()
                            if (!first.isNullOrBlank()) profile.put("firstName", first)
                            if (!last.isNullOrBlank()) profile.put("lastName", last)
                        // If you add a phone field later, include it only if present in the layout
                        val phoneFieldId = resources.getIdentifier("etPhone", "id", packageName)
                        val phoneField = if (phoneFieldId != 0) {
                            findViewById<com.google.android.material.textfield.TextInputEditText>(phoneFieldId)
                        } else null
                        val phone = phoneField?.text?.toString()?.trim()
                        if (!phone.isNullOrBlank()) profile.put("phone", phone)
                    } catch (_: Exception) { }

                    // If profile has no fields, send null so the backend receives no empty profile object
                    val profileToSend: JSONObject? = if (profile.length() > 0) profile else null
                    val result = authRepository.register(email, password, profileToSend)

                    val displayName = UserUtils.extractDisplayName(result.user)
                    Toast.makeText(this@SignUpActivity, "Registrado: $displayName", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@SignUpActivity, HomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(this@SignUpActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
