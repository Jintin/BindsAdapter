package com.jintin.bindsadapter

import androidx.recyclerview.widget.RecyclerView
import com.jintin.bindsadapter.databinding.AdapterHolder1Binding

class MyViewHolder1(
    private val binding: AdapterHolder1Binding,
    @BindPrefix private val tag: String
) : RecyclerView.ViewHolder(binding.root) {

    @BindFunction
    fun bindHolder1(data: String, @BindListener listener: (String) -> Unit) {
        binding.text.text = tag + data
        binding.root.setOnClickListener { listener.invoke(data) }
    }
}