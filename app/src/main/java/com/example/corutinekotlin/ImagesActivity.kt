package com.example.corutinekotlin

import android.content.ContentValues.TAG
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.X509TrustManager
import kotlin.system.measureTimeMillis
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager

class ImagesActivity : AppCompatActivity() {
    private val imageUrls = (0..9).map { "http://cti.ubm.ro/cmo/digits/img$it.jpg" }
    private val imageAdapter = ImageAdapter()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_images)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = imageAdapter

        loadImages()
    }

    private fun loadImages() {
        scope.launch {
            val loadingTime = measureTimeMillis {
                val images = imageUrls.map { url ->
                    async { downloadImage(url) }
                }.awaitAll().filterNotNull()

                withContext(Dispatchers.Main) {
                    imageAdapter.submitList(images)
                }

                Log.d("ImagesActivity", "Downloaded: ${images.size} images")
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(this@ImagesActivity, "Timp incarcare: $loadingTime ms", Toast.LENGTH_LONG).show()
            }
        }
    }

    private val trustAllManager = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
    private val unsafeSslSocketFactory by lazy {
        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        ctx.socketFactory
    }

    private val unsafeHostnameVerifier = HostnameVerifier { _, _ -> true }

    private suspend fun downloadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        downloadWithRedirects(url, 0)
    }
    private fun downloadWithRedirects(url: String, depth: Int): Bitmap? {
        if (depth > 5) return null

        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            instanceFollowRedirects = false // IMPORTANT: redirect manual
        }

        // dacă URL-ul e https, aplică “unsafe”
        if (conn is HttpsURLConnection) {
            conn.sslSocketFactory = unsafeSslSocketFactory
            conn.hostnameVerifier = unsafeHostnameVerifier
        }

        return try {
            val code = conn.responseCode
            val type = conn.contentType
            Log.d("ImagesActivity", "URL=$url code=$code type=$type")

            // redirect?
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                val location = conn.getHeaderField("Location") ?: return null
                val nextUrl = if (location.startsWith("http")) location else URL(URL(url), location).toString()
                return downloadWithRedirects(nextUrl, depth + 1)
            }

            if (code != 200) return null
            if (type?.startsWith("image/") != true) return null

            conn.inputStream.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.e("ImagesActivity", "Eroare download: $url", e)
            null
        } finally {
            conn.disconnect()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

}