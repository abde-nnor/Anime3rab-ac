package com.example.cloudstream.anime3rb

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class Anime3rbProvider : MainAPI() {
    override var mainUrl = "https://anime3rb.com"
    override var name = "Anime3rb"
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)
    override var lang = "ar"
    override val hasMainPage = true
    override val hasDownloadSupport = true

    // الصفحة الرئيسية
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("$mainUrl/page/$page").document
        val items = document.select("div.post").map { post ->
            val title = post.selectFirst("h2.title")?.text() ?: ""
            val href = post.selectFirst("a")?.attr("href") ?: ""
            val poster = post.selectFirst("img")?.attr("src") ?: ""
            AnimeSearchResponse(
                name = title,
                url = href,
                apiName = this.name,
                type = TvType.Anime,
                posterUrl = poster
            )
        }
        return HomePageResponse(listOf(HomePageList("آخر الأنمي", items)))
    }

    // البحث
    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$mainUrl/?s=$query").document
        return response.select("div.post").map { post ->
            val title = post.selectFirst("h2.title")?.text() ?: ""
            val href = post.selectFirst("a")?.attr("href") ?: ""
            val poster = post.selectFirst("img")?.attr("src") ?: ""
            AnimeSearchResponse(
                name = title,
                url = href,
                apiName = this.name,
                type = TvType.Anime,
                posterUrl = poster
            )
        }
    }

    // تحميل تفاصيل الأنمي والحلقات
    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        val title = document.selectFirst("h1.entry-title")?.text() ?: ""
        val poster = document.selectFirst("div.post img")?.attr("src") ?: ""
        val episodes = document.select("div.episodes a").map { ep ->
            val epUrl = ep.attr("href")
            val epName = ep.text()
            Episode(epUrl, epName)
        }
        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    // استخراج روابط الفيديوهات
    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        document.select("div.video-server a").forEach { link ->
            val videoUrl = link.attr("href")
            loadExtractor(videoUrl, mainUrl, subtitleCallback, callback)
        }
        return true
    }
}
