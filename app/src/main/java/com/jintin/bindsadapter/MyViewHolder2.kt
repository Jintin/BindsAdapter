package com.jintin.bindsadapter

import androidx.recyclerview.widget.RecyclerView
import com.jintin.bindsadapter.databinding.AdapterHolder2Binding

class MyViewHolder2(
    @BindListener val listener: (String) -> Unit,
    val binding: AdapterHolder2Binding,
) : RecyclerView.ViewHolder(binding.root) {

    @BindFunction
    fun bindHolder2(@BindSufix tag: String, data: String) {
        binding.text.text = data + tag
        binding.root.setOnClickListener { listener.invoke(data) }
    }
}