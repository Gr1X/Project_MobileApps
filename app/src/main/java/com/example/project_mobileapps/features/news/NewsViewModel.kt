
package com.example.project_mobileapps.features.news

import android.util.Log
import androidx.lifecycle.*
import com.example.project_mobileapps.data.repo.NewsRepository
import kotlinx.coroutines.launch

class NewsViewModel : ViewModel() {
    private val repository = NewsRepository()

    private val _newsArticles = MutableLiveData<List<NewsArticleUI>>()
    val newsArticles: LiveData<List<NewsArticleUI>> = _newsArticles

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        // Log untuk memastikan ViewModel dibuat
        Log.d("NewsAppDebug", "NewsViewModel init block is called.")
        fetchHealthNews()
    }

    private fun fetchHealthNews() {
        _isLoading.value = true
        viewModelScope.launch {
            Log.d("NewsAppDebug", "Coroutine launched, calling repository...")
            val articlesFromApi = repository.getHealthNews()

            // Log untuk melihat berapa artikel yang kembali dari repository
            Log.d("NewsAppDebug", "Repository returned ${articlesFromApi.size} articles.")

            _newsArticles.value = articlesFromApi.map { apiArticle ->
                NewsArticleUI(
                    title = apiArticle.title ?: "Tanpa Judul",
                    source = apiArticle.source?.name ?: "N/A",
                    imageUrl = apiArticle.urlToImage,
                    articleUrl = apiArticle.url
                )
            }
            _isLoading.value = false
        }
    }
}