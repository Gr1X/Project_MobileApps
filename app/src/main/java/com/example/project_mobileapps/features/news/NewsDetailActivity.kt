// file: features/news/NewsDetailActivity.kt
package com.example.project_mobileapps.features.news

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.example.project_mobileapps.R

class NewsDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_detail)

        val webView: WebView = findViewById(R.id.newsDetailWebView)
        val progressBar: ProgressBar = findViewById(R.id.detailProgressBar)

        // Ambil URL yang dikirim dari NewsAdapter
        val url = intent.getStringExtra("ARTICLE_URL")

        if (url != null) {
            webView.settings.javaScriptEnabled = true
            webView.webViewClient = object : WebViewClient() {
                // Tampilkan ProgressBar saat halaman mulai dimuat
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    progressBar.visibility = View.VISIBLE
                }

                // Sembunyikan ProgressBar saat halaman selesai dimuat
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progressBar.visibility = View.GONE
                }
            }
            // Muat URL-nya
            webView.loadUrl(url)
        }
    }
}