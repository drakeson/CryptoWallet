package com.crypto.wallet.data.db

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.crypto.wallet.data.models.Coin
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoinDaoTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: AppDatabase
    private lateinit var coinDao: CoinDao

    private val testCoin = Coin(
        id = "bitcoin",
        symbol = "btc",
        name = "Bitcoin",
        image = "https://example.com/btc.png",
        current_price = 45000.0,
        market_cap = 850000000000.0,
        market_cap_rank = 1,
        price_change_percentage_24h = 2.5
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).allowMainThreadQueries()
            .build()

        coinDao = database.coinDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveCoin() = runBlocking {
        coinDao.insertAll(listOf(testCoin))

        val coins = coinDao.getCoins().value
        assertNotNull(coins)
        assertEquals(1, coins?.size)
        assertEquals(testCoin.id, coins?.first()?.id)
    }

    @Test
    fun insertDuplicateCoinReplacesOld() = runBlocking {
        coinDao.insertAll(listOf(testCoin))

        val updatedCoin = testCoin.copy(current_price = 50000.0)
        coinDao.insertAll(listOf(updatedCoin))

        val coins = coinDao.getCoins().value
        assertEquals(1, coins?.size)
        assertEquals(50000.0, coins?.first()?.current_price!!, 0.01)
    }
}