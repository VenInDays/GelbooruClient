package com.gelbooru.client.scraping

import com.gelbooru.client.data.model.PostRating
import com.gelbooru.client.data.model.SearchResult
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Test suite for GelbooruParser with 3 different search scenarios.
 */
class GelbooruParserTest {

    private lateinit var parser: GelbooruParser

    @Before
    fun setUp() {
        parser = GelbooruParser()
    }

    // ========== SCENARIO 1: Standard Safe Content Search ==========

    @Test
    fun `parse standard safe content post list`() {
        val html = buildTestHtml(
            posts = listOf(
                TestPost(
                    id = 123456,
                    previewUrl = "https://gelbooru.com/thumbnails/12/34/thumbnail_abc123.jpg",
                    title = "Score: 15 Rating:safe landscape nature 1920x1080",
                    postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=123456"
                ),
                TestPost(
                    id = 123457,
                    previewUrl = "https://gelbooru.com/thumbnails/12/34/thumbnail_def456.jpg",
                    title = "Score: 42 Rating:safe anime girl blue_eyes 2048x1536",
                    postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=123457"
                )
            )
        )

        val result = parser.parsePostList(html, "landscape", 1)

        assertEquals(2, result.posts.size)
        assertEquals("landscape", result.query)
        assertEquals(1, result.currentPage)

        val firstPost = result.posts[0]
        assertEquals(123456, firstPost.postId)
        assertTrue(firstPost.previewUrl.contains("thumbnail_abc123"))
        assertEquals(PostRating.SAFE, firstPost.rating)
        assertEquals(15, firstPost.score)
        assertEquals(1920, firstPost.width)
        assertEquals(1080, firstPost.height)
        assertTrue(firstPost.tags.contains("landscape"))
        assertTrue(firstPost.tags.contains("nature"))

        val secondPost = result.posts[1]
        assertEquals(123457, secondPost.postId)
        assertEquals(PostRating.SAFE, secondPost.rating)
        assertEquals(42, secondPost.score)
    }

    // ========== SCENARIO 2: NSFW/Explicit Content Search ==========

    @Test
    fun `parse explicit content post list with various ratings`() {
        val html = buildTestHtml(
            posts = listOf(
                TestPost(
                    id = 789001,
                    previewUrl = "https://gelbooru.com/thumbnails/78/90/thumbnail_nsfw1.jpg",
                    title = "Score: -5 Rating:explicit nsfw_tag another_tag 800x600",
                    postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=789001"
                ),
                TestPost(
                    id = 789002,
                    previewUrl = "https://gelbooru.com/thumbnails/78/90/thumbnail_q1.jpg",
                    title = "Score: 88 Rating:questionable questionable_tag 1024x768",
                    postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=789002"
                ),
                TestPost(
                    id = 789003,
                    previewUrl = "https://gelbooru.com/thumbnails/78/90/thumbnail_safe1.jpg",
                    title = "Score: 200 Rating:safe safe_tag 4096x2160",
                    postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=789003"
                )
            )
        )

        val result = parser.parsePostList(html, "mixed_content", 1)

        assertEquals(3, result.posts.size)
        assertEquals(PostRating.EXPLICIT, result.posts[0].rating)
        assertEquals(-5, result.posts[0].score)
        assertEquals(PostRating.QUESTIONABLE, result.posts[1].rating)
        assertEquals(PostRating.SAFE, result.posts[2].rating)
        assertEquals(200, result.posts[2].score)
        assertEquals(4096, result.posts[2].width)
    }

    // ========== SCENARIO 3: Complex Multi-tag Search with Pagination ==========

    @Test
    fun `parse multi-tag search with pagination`() {
        val html = buildTestHtml(
            posts = listOf(
                TestPost(
                    id = 500001,
                    previewUrl = "https://gelbooru.com/thumbnails/50/00/thumbnail_complex1.jpg",
                    title = "Score: 350 Rating:safe sakura_(fate) fate/stay_night saber 3840x2160",
                    postUrl = "https://gelbooru.com/index.php?page=post&s=view&id=500001"
                )
            ),
            paginationPids = listOf(0, 42, 84, 126)
        )

        val result = parser.parsePostList(html, "sakura_(fate) saber", 1)

        assertEquals(1, result.posts.size)
        assertTrue(result.hasNextPage)
        assertEquals(4, result.totalPages) // pid=126 / 42 + 1 = 4
        assertTrue(result.posts[0].tags.contains("sakura_(fate)"))
        assertTrue(result.posts[0].tags.contains("saber"))
    }

    // ========== Post Detail Parsing Tests ==========

    @Test
    fun `parse post detail with original image link`() {
        val html = """
            <html>
            <body>
                <img id="image" src="https://gelbooru.com/images/sample.jpg"/>
                <ul class="info">
                    <li><a href="https://gelbooru.com/images/original.jpg">Original image</a></li>
                    <li>Score: 42</li>
                    <li>Rating: safe</li>
                </ul>
            </body>
            </html>
        """.trimIndent()

        val originalUrl = parser.parsePostDetail(html, 123456)
        assertEquals("https://gelbooru.com/images/original.jpg", originalUrl)
    }

    @Test
    fun `parse post detail with meta og image`() {
        val html = """
            <html>
            <head>
                <meta property="og:image" content="https://gelbooru.com/images/og_image.jpg"/>
            </head>
            <body>
                <img id="image" src="https://gelbooru.com/images/sample.jpg"/>
            </body>
            </html>
        """.trimIndent()

        val originalUrl = parser.parsePostDetail(html, 123456)
        assertEquals("https://gelbooru.com/images/og_image.jpg", originalUrl)
    }

    @Test
    fun `parse post detail with main image fallback`() {
        val html = """
            <html>
            <body>
                <img id="image" src="https://gelbooru.com/images/main_image.png"/>
                <p>No original link available</p>
            </body>
            </html>
        """.trimIndent()

        val originalUrl = parser.parsePostDetail(html, 123456)
        assertEquals("https://gelbooru.com/images/main_image.png", originalUrl)
    }

    // ========== URL Building Tests ==========

    @Test
    fun `build search URL encodes tags correctly`() {
        val url = parser.buildSearchUrl("blue eyes long_hair", 1)
        assertTrue(url.contains("tags=blue+eyes+long_hair"))
        assertTrue(url.contains("pid=0"))
    }

    @Test
    fun `build search URL includes highres param`() {
        val url = parser.buildSearchUrl("test", 1, showHighRes = true)
        assertTrue(url.contains("show_highres=1"))
    }

    @Test
    fun `build search URL paginates with pid offset`() {
        val url = parser.buildSearchUrl("test", 2)
        assertTrue(url.contains("pid=42")) // (2-1) * 42
    }

    // ========== Cookie Building Tests ==========

    @Test
    fun `build cookie string with all preferences`() {
        val cookies = parser.buildCookieString(showNsfw = true, showHighRes = true)
        assertTrue(cookies.contains("nsfw=1"))
        assertTrue(cookies.contains("show_highres=1"))
        assertTrue(cookies.contains("always_show_nsfw=1"))
    }

    @Test
    fun `build cookie string safe mode only`() {
        val cookies = parser.buildCookieString(showNsfw = false, showHighRes = true)
        assertFalse(cookies.contains("nsfw=1"))
        assertTrue(cookies.contains("show_highres=1"))
    }

    // ========== Helper Methods ==========

    private data class TestPost(
        val id: Int,
        val previewUrl: String,
        val title: String,
        val postUrl: String
    )

    private fun buildTestHtml(
        posts: List<TestPost>,
        paginationPids: List<Int> = emptyList()
    ): String {
        val postHtml = posts.joinToString("\n") { post ->
            """
            <div class="thumbnail-container">
                <a href="${post.postUrl}">
                    <img src="${post.previewUrl}" title="${post.title}" alt="Post ${post.id}"/>
                </a>
            </div>
            """.trimIndent()
        }

        val paginationHtml = if (paginationPids.isNotEmpty()) {
            val links = paginationPids.map { pid ->
                """<a href="?pid=$pid">Page ${pid / 42 + 1}</a>"""
            }.joinToString("\n            ")
            """
            <nav class="pagination">
                $links
            </nav>
            """.trimIndent()
        } else ""

        return """
        <!DOCTYPE html>
        <html>
        <head><title>Gelbooru - Test</title></head>
        <body>
            <section id="post-list">
                $postHtml
            </section>
            $paginationHtml
        </body>
        </html>
        """.trimIndent()
    }
}
