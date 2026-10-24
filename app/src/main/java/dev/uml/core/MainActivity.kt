package dev.uml.core

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val pickModRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Universal Mod Loader"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(16, 28, 16, 12)
        }
        status = TextView(this).apply {
            textSize = 16f
            text = "UML Core v0.1\n\nГотов к проверке пакета.\nИсполнение кода модов пока не реализовано."
            setPadding(16, 20, 16, 20)
        }
        val importButton = Button(this).apply {
            text = "Выбрать mod.json"
            setOnClickListener { chooseManifest() }
        }
        val sampleButton = Button(this).apply {
            text = "Проверить встроенный пример"
            setOnClickListener { validateManifest(sampleManifest()) }
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 12, 20, 20)
            addView(title)
            addView(importButton)
            addView(sampleButton)
            addView(status)
        }
        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun chooseManifest() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, pickModRequest)
    }

    @Deprecated("Uses Activity result API for a compact MVP; migrate later.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != pickModRequest || resultCode != RESULT_OK) return
        val uri: Uri = data?.data ?: return
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                ?: error("Не удалось прочитать выбранный файл")
            validateManifest(text)
        } catch (e: Exception) {
            showResult(false, "Ошибка чтения: ${e.message ?: "неизвестная ошибка"}")
        }
    }

    private fun validateManifest(raw: String) {
        try {
            val json = JSONObject(raw)
            val requiredStrings = listOf("id", "name", "version", "runtime", "entrypoint", "apiVersion")
            val missing = requiredStrings.filter { json.optString(it).isBlank() }
            if (missing.isNotEmpty()) {
                showResult(false, "Отсутствуют обязательные поля: ${missing.joinToString()}")
                return
            }
            val id = json.getString("id")
            if (!id.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{2,127}"))) {
                showResult(false, "Некорректный id мода: $id")
                return
            }
            val runtime = json.getString("runtime")
            if (runtime != "managed") {
                showResult(false, "Runtime '$runtime' пока не поддерживается. Допустимо: managed")
                return
            }
            val targets = json.optJSONObject("targets")
            if (targets == null) {
                showResult(false, "Поле targets должно быть JSON-объектом")
                return
            }
            val games = targets.optJSONArray("games")
            if (games == null || games.length() == 0) {
                showResult(false, "targets.games должен содержать хотя бы один идентификатор игры")
                return
            }
            showResult(true, "Манифест корректен\n\nИмя: ${json.getString("name")}\nID: $id\nВерсия: ${json.getString("version")}\nRuntime: $runtime\n\nЭто только проверка метаданных. Код мода не загружается и не исполняется.")
        } catch (e: Exception) {
            showResult(false, "Некорректный JSON: ${e.message ?: "ошибка разбора"}")
        }
    }

    private fun showResult(ok: Boolean, message: String) {
        status.text = (if (ok) "ПРОВЕРКА ПРОЙДЕНА" else "ПРОВЕРКА НЕ ПРОЙДЕНА") + "\n\n$message"
    }

    private fun sampleManifest(): String = listOf(
        "{",
        """  "schemaVersion": 1,""",
        """  "id": "example.gameplay",""",
        """  "name": "Example Gameplay Mod",""",
        """  "version": "1.0.0",""",
        """  "runtime": "managed",""",
        """  "entrypoint": "ExampleMod",""",
        """  "apiVersion": "1.0",""",
        """  "targets": { "games": ["example.game"], "abis": ["arm64-v8a"] },""",
        """  "dependencies": []""",
        "}"
    ).joinToString("\n")
}
