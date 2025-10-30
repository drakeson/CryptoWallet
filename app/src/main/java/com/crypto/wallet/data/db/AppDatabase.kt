package com.crypto.wallet.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.crypto.wallet.data.models.Coin

@Database(entities = [Coin::class], version = 1)
abstract class AppDatabase: RoomDatabase() {
    abstract fun coinDao(): CoinDao
}