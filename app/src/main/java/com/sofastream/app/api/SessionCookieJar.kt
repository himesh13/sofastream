package com.sofastream.app.api

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class SessionCookieJar : CookieJar {
    private val cookies = mutableListOf<Cookie>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        this.cookies.removeAll { cookie ->
            cookies.any { it.name == cookie.name }
        }
        this.cookies.addAll(cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookies.filter { it.matches(url) }
    }

    fun clear() {
        cookies.clear()
    }
}
