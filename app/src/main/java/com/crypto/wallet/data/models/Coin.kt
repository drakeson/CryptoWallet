package com.crypto.wallet.data.models

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity("coins")
data class Coin(
    @PrimaryKey() val id: String,
    val symbol: String,
    val name: String,
    val image: String,
    val current_price: Double,
    val market_cap: Double,
    val market_cap_rank: Int,
    val price_change_percentage_24h: Double
): Parcelable