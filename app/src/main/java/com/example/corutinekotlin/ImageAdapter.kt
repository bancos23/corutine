package com.example.corutinekotlin

import android.graphics.Bitmap
import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter : ListAdapter<Bitmap, ImageAdapter.ImageViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImageAdapter.ImageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.imageView)

        fun bind(bitmap: Bitmap?) {
            imageView.setImageBitmap(bitmap)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Bitmap>() {
        override fun areItemsTheSame(oldItem: Bitmap, newItem: Bitmap) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Bitmap, newItem: Bitmap): Boolean = oldItem.sameAs(newItem)
    }

}