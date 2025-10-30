package com.crypto.wallet.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.crypto.wallet.data.models.Coin
import com.crypto.wallet.data.repository.CryptoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@OptIn(ExperimentalCoroutinesApi::class)
class CryptoViewModelTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var mockRepository: CryptoRepository

    private lateinit var viewModel: CryptoViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testCoins = listOf(
        Coin(
            id = "bitcoin",
            symbol = "btc",
            name = "Bitcoin",
            image = "",
            current_price = 45000.0,
            market_cap = 850000000000.0,
            market_cap_rank = 1,
            price_change_percentage_24h = 2.5
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        MockitoAnnotations.openMocks(this)

        val coinsLiveData = MutableLiveData<List<Coin>>()
        coinsLiveData.value = testCoins
        whenever(mockRepository.coins).thenReturn(coinsLiveData)

        viewModel = CryptoViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `refreshData calls repository on success`() = runTest {
        viewModel.refreshData()
        advanceUntilIdle()

        assertNull(viewModel.error.value)
    }

    @Test
    fun `refreshData sets error when repository throws exception`() = runTest {
        val errorMessage = "Network error"

        viewModel.refreshData()
        advanceUntilIdle()
    }

    @Test
    fun `coins LiveData exposes repository data`() {
        assertEquals(testCoins, viewModel.coins.value)
    }
}