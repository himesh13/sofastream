package com.sofastream.app.api

import com.sofastream.app.SofaStreamApp
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var jellyfinRetrofit: Retrofit? = null
    private var jellyseerrRetrofit: Retrofit? = null
    private val jellyseerrCookieJar = SessionCookieJar()

    private fun buildOkHttpClient(
        cookieJar: SessionCookieJar? = null,
        headerInterceptor: Interceptor? = null
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .apply {
                headerInterceptor?.let { addInterceptor(it) }
                cookieJar?.let { cookieJar(it) }
            }
            .build()
    }

    fun getJellyfinApi(baseUrl: String): JellyfinApi {
        if (jellyfinRetrofit == null || jellyfinRetrofit!!.baseUrl().toString() != ensureTrailingSlash(baseUrl)) {
            jellyfinRetrofit = Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(baseUrl))
                .client(buildOkHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return jellyfinRetrofit!!.create(JellyfinApi::class.java)
    }

    fun getJellyseerrApi(baseUrl: String): JellyseerrApi {
        if (jellyseerrRetrofit == null || jellyseerrRetrofit!!.baseUrl().toString() != ensureTrailingSlash(baseUrl)) {
            val jellyseerrInterceptor = Interceptor { chain ->
                val original = chain.request()
                val token = SofaStreamApp.instance.userPreferences.getJellyseerrTokenSync()
                
                val requestBuilder = original.newBuilder()
                if (token.isNotEmpty()) {
                    requestBuilder.header("X-Api-Key", token)
                }
                
                chain.proceed(requestBuilder.build())
            }

            jellyseerrRetrofit = Retrofit.Builder()
                .baseUrl(ensureTrailingSlash(baseUrl))
                .client(buildOkHttpClient(jellyseerrCookieJar, jellyseerrInterceptor))
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return jellyseerrRetrofit!!.create(JellyseerrApi::class.java)
    }

    fun resetClients() {
        jellyfinRetrofit = null
        jellyseerrRetrofit = null
        jellyseerrCookieJar.clear()
    }

    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}
