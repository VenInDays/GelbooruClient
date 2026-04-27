package com.gelbooru.client

import android.app.Application
import com.gelbooru.client.network.ImageCache
import com.gelbooru.client.network.ImageDownloader
import com.gelbooru.client.scraping.GelbooruScraper

class GelbooruApp : Application() {

    lateinit var scraper: GelbooruScraper
    lateinit var downloader: ImageDownloader
    lateinit var imageCache: ImageCache

    override fun onCreate() {
        super.onCreate()
        instance = this
        scraper = GelbooruScraper(this)
        downloader = ImageDownloader(this)
        imageCache = ImageCache(this)
    }

    companion object {
        lateinit var instance: GelbooruApp
            private set
    }
}
