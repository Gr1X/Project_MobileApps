package com.example.project_mobileapps.features.news

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.project_mobileapps.R
import android.content.Intent

data class NewsArticleUI(
    val title: String,
    val source: String,
    val imageUrl: String?,
    val articleUrl: String?
)

class NewsAdapter(private val newsList: List<NewsArticleUI>) :
    RecyclerView.Adapter<NewsAdapter.NewsViewHolder>() {

    class NewsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.newsTitleTextView)
        val sourceTextView: TextView = view.findViewById(R.id.newsSourceTextView)
        val imageView: ImageView = view.findViewById(R.id.newsImageView)
        val readMoreTextView: TextView = view.findViewById(R.id.newsReadMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_news, parent, false)
        return NewsViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewsViewHolder, position: Int) {
        val news = newsList[position]
        holder.titleTextView.text = news.title
        holder.sourceTextView.text = news.source
        Glide.with(holder.itemView.context)
            .load(news.imageUrl)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imageView)

        holder.itemView.setOnClickListener {
            openDetail(holder, news)
        }

        holder.readMoreTextView.setOnClickListener {
            openDetail(holder, news)
        }
    }

    override fun getItemCount() = newsList.size

    private fun openDetail(holder: NewsViewHolder, news: NewsArticleUI) {
        val intent = Intent(holder.itemView.context, NewsDetailActivity::class.java).apply {
            putExtra("ARTICLE_URL", news.articleUrl)
        }
        holder.itemView.context.startActivity(intent)
    }
}
