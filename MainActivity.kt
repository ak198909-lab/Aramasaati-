package com.aramasaati.app

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var store: RuleStore
    private lateinit var statusText: TextView
    private lateinit var roleButton: Button
    private lateinit var listView: ListView
    private lateinit var emptyText: TextView
    private lateinit var logText: TextView
    private var rules: List<Rule> = emptyList()

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateStatus() }

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        store = RuleStore(this)
        statusText = findViewById(R.id.statusText)
        roleButton = findViewById(R.id.roleButton)
        listView = findViewById(R.id.ruleList)
        emptyText = findViewById(R.id.emptyText)
        logText = findViewById(R.id.logText)

        roleButton.setOnClickListener { requestRole() }

        findViewById<Button>(R.id.addButton).setOnClickListener {
            startActivity(Intent(this, RuleEditActivity::class.java))
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            startActivity(
                Intent(this, RuleEditActivity::class.java)
                    .putExtra(RuleEditActivity.EXTRA_ID, rules[position].id)
            )
        }

        findViewById<Button>(R.id.clearLogButton).setOnClickListener {
            store.clearLog()
            refresh()
        }

        askPermissions()
        if (!hasRole()) requestRole()
    }

    override fun onResume() {
        super.onResume()
        refresh()
        updateStatus()
    }

    private fun askPermissions() {
        val wanted = mutableListOf(Manifest.permission.READ_CONTACTS)
        if (Build.VERSION.SDK_INT >= 33) wanted.add(Manifest.permission.POST_NOTIFICATIONS)
        val missing = wanted.filter {
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permLauncher.launch(missing.toTypedArray())
    }

    private fun roleManager(): RoleManager? = getSystemService(RoleManager::class.java)

    private fun hasRole(): Boolean =
        roleManager()?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true

    private fun requestRole() {
        val rm = roleManager()
        if (rm == null || !rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            Toast.makeText(this, "Bu telefon arama filtrelemeyi desteklemiyor.", Toast.LENGTH_LONG).show()
            return
        }
        roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }

    private fun updateStatus() {
        if (hasRole()) {
            statusText.text = "✅ Arama filtreleme aktif"
            roleButton.visibility = View.GONE
        } else {
            statusText.text = "⚠️ Uygulama henüz aramaları filtreleyemiyor. " +
                    "Aşağıdaki düğmeye basıp “Arama Saati”ni arayan kimliği / spam uygulaması olarak seç."
            roleButton.visibility = View.VISIBLE
        }
    }

    private fun refresh() {
        rules = store.loadRules()
        val data = rules.map {
            mapOf(
                "title" to if (it.enabled) it.name else "${it.name}  (kapalı)",
                "sub" to it.describe()
            )
        }
        listView.adapter = SimpleAdapter(
            this, data, android.R.layout.simple_list_item_2,
            arrayOf("title", "sub"), intArrayOf(android.R.id.text1, android.R.id.text2)
        )
        emptyText.visibility = if (rules.isEmpty()) View.VISIBLE else View.GONE

        val log = store.loadLog()
        logText.text = if (log.isEmpty()) "Henüz engellenen arama yok." else log.joinToString("\n")
    }
}
