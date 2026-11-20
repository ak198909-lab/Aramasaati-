package com.aramasaati.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

/** Kuralları ve engellenen arama kaydını telefonda (SharedPreferences) saklar. */
class RuleStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("arama_saati", Context.MODE_PRIVATE)

    // ---------- Kurallar ----------

    fun loadRules(): List<Rule> {
        val raw = prefs.getString(KEY_RULES, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveRules(rules: List<Rule>) {
        val arr = JSONArray()
        rules.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY_RULES, arr.toString()).apply()
    }

    fun get(id: String): Rule? = loadRules().firstOrNull { it.id == id }

    fun upsert(rule: Rule) {
        val list = loadRules().toMutableList()
        val i = list.indexOfFirst { it.id == rule.id }
        if (i >= 0) list[i] = rule else list.add(rule)
        saveRules(list)
    }

    fun delete(id: String) {
        saveRules(loadRules().filterNot { it.id == id })
    }

    /** Bu numara şu an engellenmeli mi? Engelleyen kuralı döner, yoksa null. */
    fun findBlockingRule(number: String, now: Calendar): Rule? =
        loadRules().firstOrNull { it.matches(number) && it.isBlockedAt(now) }

    // ---------- Engellenen arama kaydı ----------

    fun addLog(line: String) {
        val list = loadLog().toMutableList()
        list.add(0, line)
        val arr = JSONArray()
        list.take(MAX_LOG).forEach { arr.put(it) }
        prefs.edit().putString(KEY_LOG, arr.toString()).apply()
    }

    fun loadLog(): List<String> {
        val raw = prefs.getString(KEY_LOG, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearLog() {
        prefs.edit().remove(KEY_LOG).apply()
    }

    // ---------- JSON ----------

    private fun toJson(r: Rule): JSONObject = JSONObject().apply {
        put("id", r.id)
        put("name", r.name)
        put("numbers", JSONArray(r.numbers))
        put("mode", r.mode.name)
        put("days", JSONArray(r.days.toList()))
        put("windows", JSONArray().also { arr ->
            r.windows.forEach { w -> arr.put(JSONObject().put("s", w.start).put("e", w.end)) }
        })
        put("enabled", r.enabled)
    }

    private fun fromJson(o: JSONObject): Rule {
        val nums = o.getJSONArray("numbers")
        val days = o.getJSONArray("days")
        val wins = o.getJSONArray("windows")
        return Rule(
            id = o.getString("id"),
            name = o.optString("name"),
            numbers = (0 until nums.length()).map { nums.getString(it) },
            mode = try { RuleMode.valueOf(o.getString("mode")) } catch (e: Exception) { RuleMode.ALLOW_ONLY },
            windows = (0 until wins.length()).map {
                val w = wins.getJSONObject(it)
                TimeWindow(w.getInt("s"), w.getInt("e"))
            },
            days = (0 until days.length()).map { days.getInt(it) }.toSet(),
            enabled = o.optBoolean("enabled", true)
        )
    }

    companion object {
        private const val KEY_RULES = "rules"
        private const val KEY_LOG = "log"
        private const val MAX_LOG = 50
    }
}
