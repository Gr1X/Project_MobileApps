package com.example.project_mobileapps.features.patient.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.project_mobileapps.data.repo.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
/**
 * Model data (UI State) untuk [NewsScreen].
 *
 * @property articles Daftar artikel berita [NewsArticleUI] yang akan ditampilkan.
 * @property isLoading Menandakan apakah data sedang dimuat dari API.
 */
data class NewsUiState(
    val articles: List<NewsArticleUI> = emptyList(),
    val isLoading: Boolean = false
)
/**
 * ViewModel untuk [NewsScreen].
 * Bertanggung jawab untuk mengambil data berita kesehatan dari [NewsRepository]
 * dan mengubah (mapping) data API menjadi model data UI ([NewsArticleUI]).
 */
class NewsViewModel : ViewModel() {
    private val newsRepository = NewsRepository()

    private val _uiState = MutableStateFlow(NewsUiState())
    val uiState = _uiState.asStateFlow()
    /**
     * Blok inisialisasi. Dipanggil saat ViewModel dibuat.
     * Langsung memicu pengambilan data berita.
     */
    init {
        fetchHealthNews()
    }
    /**
     * Fungsi privat untuk mengambil dan memproses data berita.
     */
    private fun fetchHealthNews() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val articlesFromApi = newsRepository.getHealthNews()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    articles = articlesFromApi.map { apiArticle ->
                        NewsArticleUI(
                            title = apiArticle.title ?: "Tanpa Judul",
                            source = apiArticle.source?.name ?: "N/A",
                            imageUrl = apiArticle.urlToImage,
                            articleUrl = apiArticle.url
                        )
                    }
                )
            }
        }
    }
}