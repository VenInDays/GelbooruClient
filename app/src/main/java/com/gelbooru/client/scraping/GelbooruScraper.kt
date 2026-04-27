package com.gelbooru.client.scraping

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.data.model.SearchResult
import com.gelbooru.client.data.model.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Headless WebView-based scraper for Gelbooru.
 * Loads pages in background WebView and extracts data via HTML parsing.
 */
class GelbooruScraper(private val context: Context) {

    private val parser = GelbooruParser()
    private val cookieManager = CookieManager.getInstance()

    init {
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(null, true)
    }

    /**
     * Inject user preferences as cookies to bypass content warnings.
     */
    fun applyPreferences(preferences: UserPreferences) {
        val cookieString = parser.buildCookieString(
            showNsfw = preferences.showNsfw,
            showHighRes = preferences.showHighRes
        )
        cookieManager.setCookie("https://gelbooru.com", cookieString)
        cookieManager.setCookie(".gelbooru.com", cookieString)
        cookieManager.flush()
    }

    /**
     * Search posts by tags. Runs WebView loading on Main dispatcher,
     * parsing on Default dispatcher.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun searchPosts(
        tags: String,
        page: Int,
        preferences: UserPreferences
    ): SearchResult = withContext(Dispatchers.Main) {
        applyPreferences(preferences)
        val url = parser.buildSearchUrl(tags, page, showHighRes = preferences.showHighRes)

        val html = loadUrl(url)
        withContext(Dispatchers.Default) {
            parser.parsePostList(html, tags, page)
        }
    }

    /**
     * Fetch the original image URL for a specific post.
     * Navigates to the post detail page and extracts the image source.
     */
    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchOriginalImageUrl(
        postId: Int,
        preferences: UserPreferences
    ): String? = withContext(Dispatchers.Main) {
        applyPreferences(preferences)
        val url = parser.buildPostDetailUrl(postId)

        try {
            val html = loadUrl(url)
            withContext(Dispatchers.Default) {
                parser.parsePostDetail(html, postId)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolve the best available image URL for a post.
     * Tries detail page parsing if preview/sample URLs need upgrading.
     */
    suspend fun resolveFullImageUrl(
        post: GelbooruPost,
        preferences: UserPreferences
    ): String {
        // If we already have a good URL, return it
        if (post.originalUrl.isNotBlank() && post.originalUrl != post.previewUrl) {
            return post.originalUrl
        }
        if (post.fileUrl.isNotBlank() && post.fileUrl != post.previewUrl) {
            return post.fileUrl
        }

        // Otherwise, fetch from detail page
        return fetchOriginalImageUrl(post.postId, preferences) ?: post.getBestImageUrl()
    }

    /**
     * Suspend function that loads a URL in a headless WebView and returns the HTML.
     */
    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun loadUrl(url: String): String = suspendCancellableCoroutine { continuation ->
        val webView = WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.userAgentString = USER_AGENT
            setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Stay on the same domain
                return !request.url.toString().contains("gelbooru.com")
            }

            override fun onPageFinished(view: WebView, loadedUrl: String?) {
                if (loadedUrl == url) {
                    // Small delay to let JS render
                    view.postDelayed({
                        try {
                            val html = view.evaluateJavascript(
                                "document.documentElement.outerHTML",
                                null
                            )
                            // evaluateJavascript wraps result in quotes
                            val cleanHtml = html?.trim('"')?.replace("\\u003C", "<")
                                ?.replace("\\u003E", ">")
                                ?.replace("\\\"", "\"")
                                ?.replace("\\n", "\n")
                                ?: ""

                            if (cleanHtml.isNotBlank()) {
                                continuation.resume(cleanHtml)
                            } else {
                                continuation.resumeWithException(
                                    IllegalStateException("Empty HTML from WebView")
                                )
                            }
                        } catch (e: Exception) {
                            continuation.resumeWithException(e)
                        } finally {
                            view.destroy()
                        }
                    }, 500)
                }
            }
        }

        continuation.invokeOnCancellation {
            try {
                webView.stopLoading()
                webView.destroy()
            } catch (_: Exception) {}
        }

        webView.loadUrl(url)
    }

    companion object {
        // Mimic a desktop browser to get full content
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
    }
}
