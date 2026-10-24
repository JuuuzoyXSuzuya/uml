package dev.uml.core

import android.app.Activity
import android.app.DownloadManager
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var cpmStatus: TextView
    private lateinit var cpmUrlInput: EditText
    private val pickModRequest = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = TextView(this).apply {
            text = "Universal Mod Loader"
            textSize = 24f
            gravity = Gravity.CENTER
            setPadding(16, 28, 16, 12)
        }
        val intro = TextView(this).apply {
            text = "UML Core v0.2\nПроверка манифестов и загрузка community-пакетов."
            textSize = 16f
            setPadding(16, 8, 16, 12)
        }
        status = TextView(this).apply {
            textSize = 15f
            text = "Готов к проверке пакета.\nИсполнение кода модов не реализовано."
            setPadding(16, 16, 16, 16)
        }
        val importButton = Button(this).apply {
            text = "Выбрать mod.json"
            setOnClickListener { chooseManifest() }
        }
        val sampleButton = Button(this).apply {
            text = "Проверить встроенный пример"
            setOnClickListener { validateManifest(sampleManifest()) }
        }

        val cpmTitle = TextView(this).apply {
            text = "Car Parking Multiplayer · загрузка пакета"
            textSize = 20f
            setPadding(16, 24, 16, 8)
        }
        val cpmDescription = TextView(this).apply {
            text = "Вставь прямую HTTPS-ссылку от автора на .zip, .json или .umlmod. Файл будет сохранён в папку Downloads. UML не устанавливает пакет в игру и не запускает его код."
            textSize = 14f
            setPadding(16, 4, 16, 12)
        }
        cpmUrlInput = EditText(this).apply {
            hint = "https://example.org/car-mod.zip"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            setPadding(12, 12, 12, 12)
        }
        val downloadButton = Button(this).apply {
            text = "Скачать CPM-пакет"
            setOnClickListener { downloadCpmPackage() }
        }
        cpmStatus = TextView(this).apply {
            textSize = 14f
            text = "Поддерживаются архивы и манифесты, не APK-файлы."
            setPadding(16, 12, 16, 20)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 20)
            addView(title)
            addView(intro)
            addView(importButton)
            addView(sampleButton)
            addView(status)
            addView(cpmTitle)
            addView(cpmDescription)
            addView(cpmUrlInput)
            addView(downloadButton)
            addView(cpmStatus)
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

    private fun downloadCpmPackage() {
        val rawUrl = cpmUrlInput.text?.toString()?.trim().orEmpty()
        val uri = try {
            Uri.parse(rawUrl)
        } catch (e: Exception) {
            null
        }

        if (uri == null || uri.scheme != "https" || uri.host.isNullOrBlank()) {
            cpmStatus.text = "Нужна корректная прямая HTTPS-ссылка на файл."
            return
        }

        val originalName = uri.lastPathSegment.orEmpty()
        val extension = originalName.substringAfterLast('.', "").lowercase()
        val allowedExtensions = setOf("zip", "json", "umlmod")
        if (extension !in allowedExtensions) {
            cpmStatus.text = "Этот тип файла не поддерживается. Разрешены только .zip, .json и .umlmod; APK и исполняемые файлы не скачиваются."
            return
        }

        val safeName = originalName
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .takeLast(80)
            .ifBlank { "cpm-package." + extension }
        val destinationName = "UML_CPM_" + System.currentTimeMillis() + "_" + safeName

        try {
            val request = DownloadManager.Request(uri)
                .setTitle("UML · CPM community package")
                .setDescription("Скачивание " + safeName + ". Файл не устанавливается и не запускается.")
                .setMimeType(
                    when (extension) {
                        "json" -> "application/json"
                        "zip" -> "application/zip"
                        else -> "application/octet-stream"
                    }
                )
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, destinationName)

            val downloadId = (getSystemService(DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            cpmStatus.text = "Загрузка добавлена в системный менеджер.\nФайл: Downloads/" + destinationName +
                "\nID загрузки: " + downloadId +
                "\n\nВажно: в этой версии пакет только сохраняется на устройстве. Автоматического импорта в Car Parking Multiplayer нет."
        } catch (e: Exception) {
            cpmStatus.text = "Не удалось запустить загрузку: " + (e.message ?: "неизвестная ошибка")
        }
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
            showResult(false, "Ошибка чтения: " + (e.message ?: "неизвестная ошибка"))
        }
    }

    private fun validateManifest(raw: String) {
        try {
            val json = JSONObject(raw)
            val requiredStrings = listOf("id", "name", "version", "runtime", "entrypoint", "apiVersion")
            val missing = requiredStrings.filter { json.optString(it).isBlank() }
            if (missing.isNotEmpty()) {
                showResult(false, "Отсутствуют обязательные поля: " + missing.joinToString())
                return
            }
            val id = json.getString("id")
            if (!id.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{2,127}"))) {
                showResult(false, "Некорректный id мода: " + id)
                return
            }
            val runtime = json.getString("runtime")
            if (runtime != "managed") {
                showResult(false, "Runtime '" + runtime + "' пока не поддерживается. Допустимо: managed")
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
            showResult(true,
                "Манифест корректен\n\nИмя: " + json.getString("name") +
                "\nID: " + id +
                "\nВерсия: " + json.getString("version") +
                "\nRuntime: " + runtime +
                "\n\nЭто только проверка метаданных. Код мода не загружается и не исполняется."
            )
        } catch (e: Exception) {
            showResult(false, "Некорректный JSON: " + (e.message ?: "ошибка разбора"))
        }
    }

    private fun showResult(ok: Boolean, message: String) {
        status.text = (if (ok) "ПРОВЕРКА ПРОЙДЕНА" else "ПРОВЕРКА НЕ ПРОЙДЕНА") + "\n\n" + message
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
