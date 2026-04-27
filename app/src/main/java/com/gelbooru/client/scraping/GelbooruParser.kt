package com.gelbooru.client.scraping

import com.gelbooru.client.data.model.GelbooruPost
import com.gelbooru.client.data.model.PostRating
import com.gelbooru.client.data.model.SearchResult
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * HTML-to-JSON parser for Gelbooru pages.
 * Extracts post data from HTML DOM without using the API.
 */
class GelbooruParser {

    companion object {
        private const val BASE_URL = "https://gelbooru.com"
        private const val POST_LIST_PATH = "/index.php?page=post&s=list"
        private const val POST_DETAIL_PATH = "/index.php?page=post&s=view&id="
        private const val TAGS_PARAM = "tags="
        private const val PID_PARAM = "pid="
        private const val SHOW_HIGHRES = "show_highres=1"

        // Regex patterns
        private val THUMB_PATTERN = Pattern.compile("id=\"(\\d+)\"")
        private val DIMENSION_PATTERN = Pattern.compile("(\\d+)\\s*x\\s*(\\d+)")
    }

    /**
     * Build the search URL with all necessary parameters.
     */
    fun buildSearchUrl(tags: String, page: Int, pid: Int = 0, showHighRes: Boolean = true): String {
        val encodedTags = URLEncoder.encode(tags.trim(), "UTF-8")
        val pidOffset = (page - 1) * 42 // Gelbooru uses pid offset
        return buildString {
            append(BASE_URL)
            append(POST_LIST_PATH)
            append("?$TAGS_PARAM$encodedTags")
            append("&$PID_PARAM$pidOffset")
            if (showHighRes) {
                append("&$SHOW_HIGHRES")
            }
        }
    }

    /**
     * Build post detail URL for extracting original image.
     */
    fun buildPostDetailUrl(postId: Int): String {
        return "$BASE_URL$POST_DETAIL_PATH$postId"
    }

    /**
     * Parse the post list page HTML and extract post summaries.
     * @param html raw HTML from WebView
     * @return SearchResult with extracted posts
     */
    fun parsePostList(html: String, query: String, currentPage: Int): SearchResult {
        val document = Jsoup.parse(html)
        val posts = mutableListOf<GelbooruPost>()
        val postElements = document.select("div.thumbnail-container, .image-card, article")

        // Fallback: try different selectors for various Gelbooru layouts
        val thumbnailSection = document.select("section#post-list, .post-list, .thumbnail-container")

        if (thumbnailSection.isNotEmpty()) {
            thumbnailSection.forEach { section ->
                extractPostsFromSection(section, posts)
            }
        } else {
            // Try alternative layout parsing
            extractPostsFromDocument(document, posts)
        }

        // Extract pagination info
        val totalPages = extractTotalPages(document, currentPage)

        return SearchResult(
            posts = posts,
            currentPage = currentPage,
            totalPages = totalPages,
            query = query
        )
    }

    /**
     * Parse a post detail page to extract the original/high-res image URL.
     */
    fun parsePostDetail(html: String, postId: Int): String? {
        val document = Jsoup.parse(html)

        // Strategy 1: Look for "Original image" link
        val originalLink = document.select("a[href*=original], a:contains(Original), li:has(a:contains(original)) a")
            .firstOrNull()
            ?.attr("abs:href")

        if (!originalLink.isNullOrBlank()) return originalLink

        // Strategy 2: Look for the main image source in high-res
        val mainImage = document.select("img#image, .main-image img, picture source[type=image/webp]")
            .firstOrNull()
            ?.attr("abs:src")
            ?: document.select("img#image, .main-image img")
                .firstOrNull()
                ?.attr("abs:data-original")
                ?: document.select("img#image, .main-image img")
                    .firstOrNull()
                    ?.attr("abs:data-file-url")

        if (!mainImage.isNullOrBlank()) return mainImage

        // Strategy 3: Check sidebar info for file_url
        val sidebarStats = document.select("li, .stat, .info")
        for (stat in sidebarStats) {
            val text = stat.text().lowercase()
            if (text.contains("file:") || text.contains("image:") || text.contains("source:")) {
                val link = stat.selectFirst("a")
                if (link != null) {
                    val href = link.attr("abs:href")
                    if (href.isNotBlank() && isImageUrl(href)) return href
                }
            }
        }

        // Strategy 4: Extract from JSON-LD or meta tags
        val ogImage = document.selectFirst("meta[property=og:image]")?.attr("content")
        if (!ogImage.isNullOrBlank() && isImageUrl(ogImage)) return ogImage

        val twitterImage = document.selectFirst("meta[name=twitter:image]")?.attr("content")
        if (!twitterImage.isNullOrBlank() && isImageUrl(twitterImage)) return twitterImage

        return null
    }

    /**
     * Parse autocomplete suggestions from Gelbooru's tag suggestion HTML/JSON.
     */
    fun parseTagSuggestions(html: String): List<String> {
        val suggestions = mutableListOf<String>()
        val document = Jsoup.parse(html)

        document.select(".autocomplete-item, .tag-suggestion, option").forEach { el ->
            val text = el.text().trim()
            if (text.isNotBlank()) {
                suggestions.add(text)
            }
        }

        return suggestions
    }

    /**
     * Inject cookie parameters to bypass content warnings.
     */
    fun buildCookieString(showNsfw: Boolean = true, showHighRes: Boolean = true): String {
        return buildString {
            append("ccode=us")
            if (showNsfw) {
                append("; nsfw=1")
                append("; always_show_nsfw=1")
            }
            if (showHighRes) {
                append("; show_highres=1")
            }
        }
    }

    // --- Private extraction methods ---

    private fun extractPostsFromSection(section: Element, posts: MutableList<GelbooruPost>) {
        section.select("a[href*=\"page=post&s=view\"], a[href*=\"/post/\"]").forEach { anchor ->
            try {
                val post = extractPostFromAnchor(anchor)
                if (post != null) {
                    posts.add(post)
                }
            } catch (_: Exception) {
                // Skip malformed entries
            }
        }
    }

    private fun extractPostsFromDocument(document: Document, posts: MutableList<GelbooruPost>) {
        // Try to find thumbnail images with parent links
        document.select("img[src*=thumbnails], img[src*=preview], img[class*=thumb]").forEach { img ->
            try {
                val anchor = img.closest("a") ?: return@forEach
                val post = extractPostFromAnchor(anchor)
                if (post != null) {
                    posts.add(post)
                }
            } catch (_: Exception) {
                // Skip
            }
        }
    }

    private fun extractPostFromAnchor(anchor: Element): GelbooruPost? {
        val href = anchor.attr("abs:href") ?: return null
        if (!href.contains("post") && !href.contains("id=")) return null

        val postId = extractPostId(href) ?: return null
        val img = anchor.selectFirst("img") ?: return null

        val previewUrl = img.attr("abs:src") ?: img.attr("abs:data-src") ?: return null
        if (previewUrl.isBlank()) return null

        val title = img.attr("title") ?: img.attr("alt") ?: ""
        val tags = parseTitleTags(title)
        val rating = parseRatingFromTitle(title)

        // Try to extract score and dimensions from title
        val score = parseScoreFromTitle(title)
        val dimensions = parseDimensionsFromTitle(title)

        return GelbooruPost(
            id = postId,
            postId = postId,
            previewUrl = previewUrl,
            sampleUrl = upgradeToSampleUrl(previewUrl),
            fileUrl = upgradeToFileUrl(previewUrl),
            tags = tags,
            score = score,
            rating = rating,
            width = dimensions.first,
            height = dimensions.second,
            postUrl = href
        )
    }

    private fun extractPostId(href: String): Int? {
        // Try id=123 pattern
        val idMatch = Pattern.compile("[?&]id=(\\d+)").matcher(href)
        if (idMatch.find()) return idMatch.group(1)?.toIntOrNull()

        // Try /post/123 pattern
        val slugMatch = Pattern.compile("/post/(\\d+)").matcher(href)
        if (slugMatch.find()) return slugMatch.group(1)?.toIntOrNull()

        // Try hash #123456 pattern
        val hashMatch = Pattern.compile("#(\\d{5,})").matcher(href)
        if (hashMatch.find()) return hashMatch.group(1)?.toIntOrNull()

        return null
    }

    private fun parseTitleTags(title: String): List<String> {
        if (title.isBlank()) return emptyList()
        // Tags in title are usually space-separated, ignoring score/rating info
        return title.split("\\s+".toRegex())
            .filter { !it.matches("Score:\\d*".toRegex()) }
            .filter { !it.matches("\\d+x\\d+".toRegex()) }
            .filter { !it.matches("Rating:\\w+".toRegex()) }
            .filter { !it.matches("ID:\\d*".toRegex()) }
            .filter { it.isNotBlank() }
            .take(30)
    }

    private fun parseRatingFromTitle(title: String): PostRating {
        return when {
            title.contains("Rating:explicit", ignoreCase = true) ||
            title.contains("Rating:e", ignoreCase = true) -> PostRating.EXPLICIT
            title.contains("Rating:questionable", ignoreCase = true) ||
            title.contains("Rating:q", ignoreCase = true) -> PostRating.QUESTIONABLE
            title.contains("Rating:safe", ignoreCase = true) ||
            title.contains("Rating:s", ignoreCase = true) -> PostRating.SAFE
            else -> PostRating.UNKNOWN
        }
    }

    private fun parseScoreFromTitle(title: String): Int {
        val match = Pattern.compile("Score:\\s*(-?\\d+)").matcher(title)
        return if (match.find()) match.group(1)?.toIntOrNull() ?: 0 else 0
    }

    private fun parseDimensionsFromTitle(title: String): Pair<Int, Int> {
        val match = DIMENSION_PATTERN.matcher(title)
        return if (match.find()) {
            Pair(match.group(1)?.toIntOrNull() ?: 0, match.group(2)?.toIntOrNull() ?: 0)
        } else Pair(0, 0)
    }

    private fun upgradeToSampleUrl(previewUrl: String): String {
        return previewUrl
            .replace("/thumbnails/", "/samples/")
            .replace("thumbnail_", "sample_")
            .replace(".jpg", ".jpg") // keep extension
    }

    private fun upgradeToFileUrl(previewUrl: String): String {
        return previewUrl
            .replace("/thumbnails/", "/images/")
            .replace("/samples/", "/images/")
            .replace("thumbnail_", "")
            .replace("sample_", "")
    }

    private fun isImageUrl(url: String): Boolean {
        return url.lowercase().matches(Regex(".*\\.(jpg|jpeg|png|gif|webp|mp4|avi)\\?.*"))
            || url.lowercase().matches(Regex(".*\\.(jpg|jpeg|png|gif|webp)$"))
            || url.contains("gelbooru.com/images/")
    }

    private fun extractTotalPages(document: Document, currentPage: Int): Int {
        // Look for pagination links
        val pageLinks = document.select("a[href*=\"pid=\"]")
        val nextPages = mutableListOf<Int>()

        pageLinks.forEach { link ->
            val href = link.attr("href")
            val match = Pattern.compile("pid=(\\d+)").matcher(href)
            if (match.find()) {
                val pid = match.group(1)?.toIntOrNull() ?: 0
                val pageNum = (pid / 42) + 1
                if (pageNum > 0) nextPages.add(pageNum)
            }
        }

        val maxPage = nextPages.maxOrNull() ?: currentPage

        // Check for "next" button
        val nextButton = document.select("a.next, a:contains(Next), a[alt=Next]")
            .firstOrNull()
        return if (nextButton != null) maxPage + 1 else maxPage
    }
}
