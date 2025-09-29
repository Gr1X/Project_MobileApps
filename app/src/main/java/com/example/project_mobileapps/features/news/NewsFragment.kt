package com.example.project_mobileapps.features.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.project_mobileapps.R

class NewsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Kita akan buat layout XML sederhana untuk placeholder
        return inflater.inflate(R.layout.fragment_news, container, false)
    }
}