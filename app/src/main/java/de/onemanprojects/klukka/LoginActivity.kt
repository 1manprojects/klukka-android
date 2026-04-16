package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // If credentials already stored, go directly to projects
        val secureStorage = SecureStorage(this)
        if (secureStorage.hasCredentials()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.login_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tilServerUrl = findViewById<TextInputLayout>(R.id.til_server_url)
        val etServerUrl = findViewById<TextInputEditText>(R.id.et_server_url)
        val etApiToken = findViewById<TextInputEditText>(R.id.et_api_token)
        val btnLogin = findViewById<MaterialButton>(R.id.btn_login)

        etServerUrl.setText(viewModel.getSavedServerUrl())
        etApiToken.setText(viewModel.getSavedApiToken())

        // Inline error on the URL field for invalid URLs
        viewModel.urlError.observe(this) { error ->
            tilServerUrl.error = error
        }

        // AlertDialog when the URL scheme is HTTP (not HTTPS)
        viewModel.insecureUrlWarning.observe(this) { url ->
            if (url != null) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.warning_insecure_title))
                    .setMessage(getString(R.string.warning_insecure_message))
                    .setPositiveButton(getString(R.string.btn_proceed_anyway)) { _, _ ->
                        viewModel.confirmInsecureUrl()
                    }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        viewModel.dismissInsecureWarning()
                    }
                    .setCancelable(false)
                    .show()
            }
        }

        viewModel.saveResult.observe(this) { success ->
            if (success) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        btnLogin.setOnClickListener {
            val serverUrl = etServerUrl.text.toString().trim()
            val apiToken = etApiToken.text.toString().trim()
            if (serverUrl.isEmpty() || apiToken.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.validateAndSave(serverUrl, apiToken)
        }
    }
}
