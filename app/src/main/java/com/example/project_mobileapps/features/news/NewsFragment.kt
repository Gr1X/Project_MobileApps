package com.example.project_mobileapps.features.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.project_mobileapps.R

class NewsFragment : Fragment() {
    private val viewModel: NewsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val newsRecyclerView: RecyclerView = view.findViewById(R.id.newsRecyclerView)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)

        newsRecyclerView.layoutManager = LinearLayoutManager(context)

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.isVisible = isLoading
        }

        viewModel.newsArticles.observe(viewLifecycleOwner) { articles ->
            newsRecyclerView.adapter = NewsAdapter(articles)
        }
    }
}