package com.aramasaati.app

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import java.util.UUID

class RuleEditActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ID = "rule_id"
    }

    private lateinit var store: RuleStore
    private var ruleId: String? = null
    private val windows = mutableListOf<TimeWindow>()
    private val dayBoxes = mutableMapOf<Int, CheckBox>()

    private lateinit var nameEdit: EditText
    private lateinit var numbersEdit: EditText
    private lateinit var modeGroup: RadioGroup
    private lateinit var radioAllow: RadioButton
    private lateinit var radioBlock: RadioButton
    private lateinit var modeHint: TextView
    private lateinit var windowsContainer: LinearLayout
    private lateinit var enabledSwitch: SwitchCompat

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            if (uri != null) loadContact(uri)
        }

    private val contactsPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) pickContact.launch(null)
            else Toast.makeText(this, "Rehber izni olmadan numarayı elle yazabilirsin.", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rule_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        store = RuleStore(this)
        nameEdit = findViewById(R.id.nameEdit)
        numbersEdit = findViewById(R.id.numbersEdit)
        modeGroup = findViewById(R.id.modeGroup)
        radioAllow = findViewById(R.id.radioAllow)
        radioBlock = findViewById(R.id.radioBlock)
        modeHint = findViewById(R.id.modeHint)
        windowsContainer = findViewById(R.id.windowsContainer)
        enabledSwitch = findViewById(R.id.enabledSwitch)

        buildDayBoxes(findViewById(R.id.daysContainer))

        findViewById<Button>(R.id.pickContactButton).setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                pickContact.launch(null)
            } else {
                contactsPerm.launch(Manifest.permission.READ_CONTACTS)
            }
        }
        findViewById<Button>(R.id.addWindowButton).setOnClickListener { addWindow() }
        findViewById<Button>(R.id.saveButton).setOnClickListener { save() }
        modeGroup.setOnCheckedChangeListener { _, _ -> updateHint() }

        val deleteButton = findViewById<Button>(R.id.deleteButton)
        deleteButton.setOnClickListener { confirmDelete() }

        ruleId = intent.getStringExtra(EXTRA_ID)
        val existing = ruleId?.let { store.get(it) }
        if (existing != null) {
            fill(existing)
            deleteButton.visibility = View.VISIBLE
        } else {
            // Yeni kural için varsayılan: her gün, sadece 08:00–18:00
            radioAllow.isChecked = true
            windows.add(TimeWindow(8 * 60, 18 * 60))
            dayBoxes.values.forEach { it.isChecked = true }
            enabledSwitch.isChecked = true
            deleteButton.visibility = View.GONE
        }
        renderWindows()
        updateHint()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun buildDayBoxes(container: LinearLayout) {
        Days.ORDER.forEach { day ->
            val cb = CheckBox(this)
            cb.text = Days.name(day)
            container.addView(cb)
            dayBoxes[day] = cb
        }
    }

    private fun fill(r: Rule) {
        nameEdit.setText(r.name)
        numbersEdit.setText(r.numbers.joinToString(", "))
        if (r.mode == RuleMode.ALLOW_ONLY) radioAllow.isChecked = true else radioBlock.isChecked = true
        windows.clear()
        windows.addAll(r.windows)
        dayBoxes.forEach { (day, cb) -> cb.isChecked = day in r.days }
        enabledSwitch.isChecked = r.enabled
    }

    private fun updateHint() {
        modeHint.text = if (radioBlock.isChecked) {
            "Seçili günlerde aşağıdaki saatlerde arayamaz (ör. öğle arası). Diğer zamanlarda arayabilir. Mesaj her zaman gelir."
        } else {
            "Seçili günlerde yalnızca aşağıdaki saatlerde arayabilir. Bu saatlerin dışında ve seçilmeyen günlerde arayamaz. Mesaj her zaman gelir."
        }
    }

    private fun loadContact(uri: Uri) {
        try {
            var contactId: String? = null
            contentResolver.query(
                uri,
                arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
                null, null, null
            )?.use { c ->
                if (c.moveToFirst()) {
                    contactId = c.getString(0)
                    val name = c.getString(1)
                    if (!name.isNullOrBlank()) nameEdit.setText(name)
                }
            }
            val id = contactId ?: return
            val phones = mutableListOf<String>()
            contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(id), null
            )?.use { c ->
                while (c.moveToNext()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank() && phones.none { PhoneUtil.normalize(it) == PhoneUtil.normalize(n) }) {
                        phones.add(n)
                    }
                }
            }
            if (phones.isEmpty()) {
                Toast.makeText(this, "Bu kişide telefon numarası yok.", Toast.LENGTH_SHORT).show()
            } else {
                numbersEdit.setText(phones.joinToString(", "))
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Rehber izni gerekli.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addWindow() {
        TimePickerDialog(this, { _, h1, m1 ->
            val start = h1 * 60 + m1
            TimePickerDialog(this, { _, h2, m2 ->
                windows.add(TimeWindow(start, h2 * 60 + m2))
                renderWindows()
            }, (h1 + 1) % 24, m1, true).apply {
                setTitle("Bitiş saati")
                show()
            }
        }, 12, 0, true).apply {
            setTitle("Başlangıç saati")
            show()
        }
    }

    private fun renderWindows() {
        windowsContainer.removeAllViews()
        if (windows.isEmpty()) {
            val tv = TextView(this)
            tv.text = "Henüz saat aralığı yok."
            windowsContainer.addView(tv)
            return
        }
        windows.forEachIndexed { index, w ->
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = Gravity.CENTER_VERTICAL

            val tv = TextView(this)
            tv.text = w.label()
            tv.textSize = 18f
            tv.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

            val del = Button(this)
            del.text = "Sil"
            del.setOnClickListener {
                windows.removeAt(index)
                renderWindows()
            }

            row.addView(tv)
            row.addView(del)
            windowsContainer.addView(row)
        }
    }

    private fun save() {
        val numbers = numbersEdit.text.toString()
            .split(",", ";", "\n")
            .map { it.trim() }
            .filter { PhoneUtil.normalize(it).isNotEmpty() }
        val days = dayBoxes.filter { it.value.isChecked }.keys.toSet()

        when {
            numbers.isEmpty() -> { toast("En az bir telefon numarası gir."); return }
            windows.isEmpty() -> { toast("En az bir saat aralığı ekle."); return }
            days.isEmpty() -> { toast("En az bir gün seç."); return }
        }

        val name = nameEdit.text.toString().trim().ifEmpty { numbers.first() }
        val rule = Rule(
            id = ruleId ?: UUID.randomUUID().toString(),
            name = name,
            numbers = numbers,
            mode = if (radioBlock.isChecked) RuleMode.BLOCK_DURING else RuleMode.ALLOW_ONLY,
            windows = windows.toList(),
            days = days,
            enabled = enabledSwitch.isChecked
        )
        store.upsert(rule)
        toast("Kaydedildi")
        finish()
    }

    private fun confirmDelete() {
        val id = ruleId ?: return
        AlertDialog.Builder(this)
            .setMessage("Bu kural silinsin mi?")
            .setPositiveButton("Sil") { _, _ ->
                store.delete(id)
                finish()
            }
            .setNegativeButton("Vazgeç", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
