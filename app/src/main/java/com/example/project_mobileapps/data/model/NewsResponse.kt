package com.example.project_mobileapps.data.model.news
import com.squareup.moshi.Json

data class NewsResponse(@Json(name = "articles") val articles: List<Article>)

data class Article(
    @Json(name = "title")
    val title: String?,
    @Json(name = "source")
    val source: Source?,
    @Json(name = "urlToImage")
    val urlToImage: String?,

    @Json(name = "url")
    val url: String?
)

data class Source(@Json(name = "name") val name: String?)