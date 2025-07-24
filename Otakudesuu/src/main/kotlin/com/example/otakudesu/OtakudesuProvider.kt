package com.example.otakudesu

import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.model.MovieSearchResponse
import okhttp3.Request

class OtakuDesuProvider : MainAPI() {
    override var mainUrl = "https://otakudesu.cloud"
    override var name = "OtakuDesu"
    override val supportedTypes = setOf(TvType.Anime)
    override val hasMainPage = true
    override var lang = "en"

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/?s=$query&post_type=anime"
        val document = app.get(url).document

        return document.select("div.last_episodes > ul.items > li").map {
            val title = it.select("p.name > a").text()
            val url = it.select("p.name > a").attr("href")
            val poster = it.select("div.thumb > img").attr("src")

            MovieSearchResponse(
                name = title,
                url = fixUrl(url),
                posterUrl = fixUrl(poster),
                type = TvType.Anime
            )
        }
    }

    override suspend fun getMainPage(page: Int, request: HomePageRequest): HomePageResponse {
        val url = "$mainUrl/page/$page"
        val document = app.get(url).document

        val animeList = document.select("div.last_episodes > ul.items > li").map {
            val title = it.select("p.name > a").text()
            val url = it.select("p.name > a").attr("href")
            val poster = it.select("div.thumb > img").attr("src")

            MovieSearchResponse(
                name = title,
                url = fixUrl(url),
                posterUrl = fixUrl(poster),
                type = TvType.Anime
            )
        }
        return newHomePageResponse("Latest Anime", animeList)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title = document.selectFirst("div.anime_detail_body > h1")?.text() ?: "No Title"
        val poster = document.selectFirst("div.anime_info_body_bg > img")?.attr("src") ?: ""

        val episodes = document.select("div.episode_list > ul > li").map {
            val epName = it.select("a").text()
            val epUrl = it.select("a").attr("href")

            Episode(
                name = epName,
                url = fixUrl(epUrl),
                date_upload = null,
                episode_number = null
            )
        }.reversed()

        return newTvLoadResponse(title, url, TvType.Anime, poster) {
            episodes.forEach { addEpisode(it) }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        val iframeUrl = document.selectFirst("iframe")?.attr("src") ?: ""

        if (iframeUrl.isNotEmpty()) {
            callback.invoke(ExtractorLink("Default", iframeUrl, true))
            return true
        }

        return false
    }

    private fun fixUrl(url: String): String {
        return if (url.startsWith("http")) url else "$mainUrl$url"
    }
}
