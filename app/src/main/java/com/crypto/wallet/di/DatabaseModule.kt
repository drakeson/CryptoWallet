package com.crypto.wallet.di

import android.content.Context
import androidx.room.Room
import com.crypto.wallet.data.db.AppDatabase
import com.crypto.wallet.data.db.CoinDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "crypto_db").build()

    @Provides
    fun provideDao(db: AppDatabase): CoinDao = db.coinDao()
}