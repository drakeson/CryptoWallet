package com.crypto.wallet.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.crypto.wallet.R
import com.crypto.wallet.data.models.Coin
import com.crypto.wallet.databinding.ItemCoinBinding

class CoinAdapter(
    private val onItemClick: (Coin) -> Unit
) : ListAdapter<Coin, CoinAdapter.ViewHolder>(
    object : DiffUtil.ItemCallback<Coin>() {
        override fun areItemsTheSame(old: Coin, new: Coin) = old.id == new.id
        override fun areContentsTheSame(old: Coin, new: Coin) = old == new
    }
) {
    inner class ViewHolder(private val binding: ItemCoinBinding)
        : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: Coin) {
            binding.coinName.text = item.name
            binding.coinSymbol.text = item.symbol.uppercase()
            binding.coinPrice.text = "$${formatNumber(item.current_price)}"
            binding.coinMarketCap.text = "MCap: $${formatNumber(item.market_cap)}"
            Glide.with(binding.root.context)
                .load(item.image)
                .placeholder(R.drawable.ic_launcher_foreground)
                .error(R.drawable.ic_launcher_foreground)
                .circleCrop()
                .into(binding.coinImage)

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun formatNumber(number: Double): String {
            return String.format("%,.2f", number)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCoinBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}