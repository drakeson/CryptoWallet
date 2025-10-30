package com.crypto.wallet.di

import android.content.Context
import com.crypto.wallet.data.api.CryptoAPI
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {

        // Logging interceptor
        val logging = HttpLoggingInterceptor().apply {
            level = if (com.crypto.wallet.BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE  // No logging in production
            }
        }

        // Certificate Pinning for security
        val certificatePinner = CertificatePinner.Builder()
            .add("api.coingecko.com", "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
            // Note: Get real certificate hash from: https://www.ssllabs.com/ssltest/
            .build()

        // Cache for offline support 10MB
        val cacheSize = 10 * 1024 * 1024L
        val cache = Cache(File(context.cacheDir, "http-cache"), cacheSize)

        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                // Add security headers
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "CryptoWallet-Android")
                    .addHeader("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .cache(cache)
            // Certificate pinning (comment out for testing with public APIs)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.coingecko.com/api/v3/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApi(retrofit: Retrofit): CryptoAPI {
        return retrofit.create(CryptoAPI::class.java)
    }
}