package com.crypto.wallet.data.repository

import androidx.lifecycle.LiveData
import com.crypto.wallet.data.api.CryptoAPI
import com.crypto.wallet.data.db.CoinDao
import com.crypto.wallet.data.models.Coin
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoRepository @Inject constructor(
    private val api: CryptoAPI,
    private val dao: CoinDao
) {

    val coins: LiveData<List<Coin>> = dao.getCoins()

    suspend fun refreshCoins() {
        try {
            val response = api.getCoins()
            Timber.tag("API_LOG").d("Fetched ${response.size} coins successfully")
            dao.insertAll(response)
        } catch (e: HttpException) {
            Timber.tag("API_ERROR").e("HttpException: ${e.code()} ${e.message()}")
        } catch (e: IOException) {
            Timber.tag("API_ERROR").e("IOException: ${e.message}")
        } catch (e: Exception) {
            Timber.tag("API_ERROR").e("Unexpected error: ${e.message}")
        }
    }
}
