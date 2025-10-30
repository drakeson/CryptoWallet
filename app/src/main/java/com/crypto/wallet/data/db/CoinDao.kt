package com.crypto.wallet.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.crypto.wallet.data.models.Coin

@Dao
interface CoinDao {
    @Query("SELECT * FROM coins")
    fun getCoins(): LiveData<List<Coin>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(coins: List<Coin>)
}