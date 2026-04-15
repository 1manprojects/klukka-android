package de.onemanprojects.klukka

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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

        val etServerUrl = findViewById<EditText>(R.id.et_server_url)
        val etApiToken = findViewById<EditText>(R.id.et_api_token)
        val btnLogin = findViewById<Button>(R.id.btn_login)

        etServerUrl.setText(viewModel.getSavedServerUrl())
        etApiToken.setText(viewModel.getSavedApiToken())

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
            viewModel.saveCredentials(serverUrl, apiToken)
        }
    }
}
