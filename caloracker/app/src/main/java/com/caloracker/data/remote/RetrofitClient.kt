package com.caloracker.data.remote

import com.caloracker.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton object to provide Retrofit client instances for USDA FoodData Central API.
 * Configures OkHttp with logging and timeout settings.
 *
 * NOTE: USDA API is completely free and requires no authentication!
 */
object RetrofitClient {

    private const val TIMEOUT_SECONDS = 30L

    /**
     * OkHttp client with logging interceptor.
     * Logging is only enabled for debug builds.
     */
    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)

        // Add logging only in debug mode
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(loggingInterceptor)
        }

        builder.build()
    }

    /**
     * Retrofit instance configured with Gson converter and OkHttp client
     * for USDA FoodData Central API.
     */
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.USDA_API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Get the USDA API service instance.
     * @return UsdaApi instance
     */
    val usdaApi: UsdaApi by lazy {
        retrofit.create(UsdaApi::class.java)
    }
}
