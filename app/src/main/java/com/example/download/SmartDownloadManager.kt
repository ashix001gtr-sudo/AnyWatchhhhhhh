package com.example.download

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import com.example.data.model.DownloadStatus
import com.example.data.model.DownloadTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URLDecoder
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class SmartDownloadManager(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    init {
        scanExistingCompletedFiles()
    }

    private fun getDownloadDirectory(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
            ?: File(context.filesDir, "downloads")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun scanExistingCompletedFiles() {
        val dir = getDownloadDirectory()
        val files = dir.listFiles { file -> file.isFile && (file.extension.equals("mp4", true) || file.extension.equals("mkv", true) || file.extension.equals("webm", true)) }
            ?: return

        val existingList = _tasks.value.toMutableList()
        files.sortedByDescending { it.lastModified() }.forEach { file ->
            if (existingList.none { it.file?.absolutePath == file.absolutePath }) {
                existingList.add(
                    DownloadTask(
                        id = UUID.randomUUID().toString(),
                        title = file.nameWithoutExtension,
                        url = "",
                        fileName = file.name,
                        file = file,
                        totalBytes = file.length(),
                        downloadedBytes = file.length(),
                        status = DownloadStatus.COMPLETED,
                        createdAt = file.lastModified()
                    )
                )
            }
        }
        _tasks.value = existingList
    }

    fun startDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        cookie: String?,
        referer: String?,
        suggestedTitle: String? = null
    ): String {
        val taskId = UUID.randomUUID().toString()
        val resolvedFileName = resolveFileName(url, contentDisposition, mimeType, suggestedTitle)
        
        val initialTask = DownloadTask(
            id = taskId,
            title = suggestedTitle ?: resolvedFileName.removeSuffix(".mp4"),
            url = url,
            fileName = resolvedFileName,
            totalBytes = if (contentLength > 0) contentLength else 0L,
            downloadedBytes = 0L,
            status = DownloadStatus.PENDING,
            mimeType = mimeType ?: "video/mp4",
            referer = referer,
            userAgent = userAgent,
            cookie = cookie
        )

        _tasks.value = listOf(initialTask) + _tasks.value

        val job = scope.launch {
            executeDownload(taskId, url, userAgent, cookie, referer, resolvedFileName)
        }
        activeJobs[taskId] = job

        return taskId
    }

    private suspend fun executeDownload(
        taskId: String,
        url: String,
        userAgent: String?,
        cookie: String?,
        referer: String?,
        fileName: String
    ) {
        updateTask(taskId) { it.copy(status = DownloadStatus.DOWNLOADING) }

        val downloadDir = getDownloadDirectory()
        var targetFile = File(downloadDir, fileName)
        var counter = 1
        val baseName = fileName.substringBeforeLast(".")
        val extension = fileName.substringAfterLast(".", "mp4")
        while (targetFile.exists()) {
            targetFile = File(downloadDir, "${baseName}_$counter.$extension")
            counter++
        }

        val requestBuilder = Request.Builder().url(url)

        // CRITICAL HEADER INJECTION: Fix .bin and 403 Forbidden / 0% issues
        if (!userAgent.isNullOrBlank()) {
            requestBuilder.header("User-Agent", userAgent)
        } else {
            requestBuilder.header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
        }

        if (!cookie.isNullOrBlank()) {
            requestBuilder.header("Cookie", cookie)
        }

        if (!referer.isNullOrBlank()) {
            requestBuilder.header("Referer", referer)
        }

        requestBuilder.header("Accept", "*/*")
        requestBuilder.header("Accept-Encoding", "identity") // Avoid gzip stream size mismatches
        requestBuilder.header("Connection", "keep-alive")

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Server returned HTTP ${response.code}"
                    )
                }
                return
            }

            val body = response.body
            if (body == null) {
                updateTask(taskId) {
                    it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Response body was empty"
                    )
                }
                return
            }

            val totalLength = body.contentLength()
            updateTask(taskId) {
                it.copy(
                    totalBytes = if (totalLength > 0) totalLength else it.totalBytes,
                    file = targetFile
                )
            }

            inputStream = body.byteStream()
            outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            var totalBytesRead = 0L

            var lastTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeed = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!scope.isActive) {
                    targetFile.delete()
                    updateTask(taskId) { it.copy(status = DownloadStatus.CANCELLED) }
                    return
                }

                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                bytesSinceLastSample += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastTime
                if (elapsed >= 500) {
                    currentSpeed = (bytesSinceLastSample * 1000) / elapsed
                    val remainingBytes = if (totalLength > 0) (totalLength - totalBytesRead).coerceAtLeast(0L) else 0L
                    val eta = if (currentSpeed > 0) remainingBytes / currentSpeed else 0L

                    updateTask(taskId) {
                        it.copy(
                            downloadedBytes = totalBytesRead,
                            speedBytesPerSec = currentSpeed,
                            etaSeconds = eta,
                            status = DownloadStatus.DOWNLOADING
                        )
                    }

                    lastTime = now
                    bytesSinceLastSample = 0L
                }
            }

            outputStream.flush()

            // Download completed successfully
            updateTask(taskId) {
                it.copy(
                    downloadedBytes = totalBytesRead,
                    totalBytes = if (totalBytesRead > 0) totalBytesRead else it.totalBytes,
                    speedBytesPerSec = 0L,
                    etaSeconds = 0L,
                    status = DownloadStatus.COMPLETED,
                    file = targetFile
                )
            }
        } catch (e: Exception) {
            targetFile.delete()
            updateTask(taskId) {
                it.copy(
                    status = if (it.status == DownloadStatus.CANCELLED) DownloadStatus.CANCELLED else DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download error"
                )
            }
        } finally {
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            activeJobs.remove(taskId)
        }
    }

    fun cancelDownload(taskId: String) {
        val job = activeJobs.remove(taskId)
        job?.cancel()
        updateTask(taskId) {
            it.file?.delete()
            it.copy(status = DownloadStatus.CANCELLED, speedBytesPerSec = 0L, etaSeconds = 0L)
        }
    }

    fun deleteTask(taskId: String) {
        val task = _tasks.value.find { it.id == taskId }
        if (task != null) {
            cancelDownload(taskId)
            task.file?.delete()
            _tasks.value = _tasks.value.filter { it.id != taskId }
        }
    }

    private fun updateTask(taskId: String, block: (DownloadTask) -> DownloadTask) {
        _tasks.value = _tasks.value.map {
            if (it.id == taskId) block(it) else it
        }
    }

    /**
     * Resolves and forces correct media extension, preventing .bin downloads
     */
    private fun resolveFileName(
        url: String,
        contentDisposition: String?,
        mimeType: String?,
        suggestedTitle: String?
    ): String {
        var rawName: String? = null

        // 1. Try Content-Disposition
        if (!contentDisposition.isNullOrBlank()) {
            val matcher = Pattern.compile("filename\\s*=\\s*\"?([^\"]+)\"?", Pattern.CASE_INSENSITIVE).matcher(contentDisposition)
            if (matcher.find()) {
                rawName = matcher.group(1)
            }
        }

        // 2. Try URLUtil or URL path
        if (rawName.isNullOrBlank()) {
            rawName = try {
                val cleanUrl = URLDecoder.decode(url, "UTF-8")
                URLUtil.guessFileName(cleanUrl, contentDisposition, mimeType)
            } catch (_: Exception) {
                "AnyWatch_Video_${System.currentTimeMillis()}.mp4"
            }
        }

        // Clean name from unwanted characters
        rawName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        // CRITICAL EXTENSION FIX: Never allow .bin or empty extension for video media
        val extension = rawName.substringAfterLast(".", "").lowercase()
        val isVideoMime = mimeType?.startsWith("video/") == true ||
                mimeType?.contains("mp4") == true ||
                mimeType?.contains("octet-stream") == true

        if (extension == "bin" || extension.isBlank() || extension == "tmp" || (isVideoMime && extension != "mp4" && extension != "mkv" && extension != "webm")) {
            val nameWithoutExt = if (extension.isBlank()) rawName else rawName.substringBeforeLast(".")
            rawName = "$nameWithoutExt.mp4"
        }

        if (rawName == "downloadfile.mp4" || rawName == "download.mp4" || rawName.startsWith("videoplayback")) {
            val timestamp = System.currentTimeMillis() % 100000
            val prefix = suggestedTitle?.replace(Regex("[^a-zA-Z0-9]"), "_") ?: "AnyWatch"
            rawName = "${prefix}_$timestamp.mp4"
        }

        return rawName
    }
}
