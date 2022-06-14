package com.jintin.bindsadapter

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.jintin.bindsadapter.databinding.ActivityMainBinding

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val adapter = MyAdapterImpl(StringDiffCallback, "start:", "...", null) {
            Toast.makeText(this, "$it is clicked !!", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.adapter = adapter
        adapter.submitList((0..100).map { "Index $it" })
    }

}