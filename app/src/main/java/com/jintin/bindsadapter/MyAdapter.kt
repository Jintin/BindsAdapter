package com.jintin.bindsadapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

@BindAdapter([MyViewHolder1::class, MyViewHolder2::class])
abstract class MyAdapter(
    diffCallback: DiffUtil.ItemCallback<String>,
    @BindPrefix val prefix: String,
    @BindSufix val sufix: String,
    @ComplexType val complexObj: List<List<Map<Int, String>>>?,
    @BindListener val listener: (String) -> Unit,
) : ListAdapter<String, RecyclerView.ViewHolder>(diffCallback) {

    override fun getItemViewType(position: Int): Int {
        return if (position % 2 == 0) {
            MyAdapterImpl.TYPE_MY_VIEW_HOLDER1
        } else {
            MyAdapterImpl.TYPE_MY_VIEW_HOLDER2
        }
    }
}


