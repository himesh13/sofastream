package com.sofastream.app

import com.sofastream.app.data.model.MediaItem
import com.sofastream.app.data.model.MediaType
import org.junit.Test
import org.junit.Assert.*

class MediaItemTest {

    private fun createMovieItem(runtimeTicks: Long? = null, rating: Float? = null) = MediaItem(
        id = "1",
        title = "Test Movie",
        overview = "An overview",
        type = MediaType.MOVIE,
        year = 2023,
        rating = rating,
        contentRating = "PG-13",
        runtimeTicks = runtimeTicks,
        backdropUrl = null,
        posterUrl = null,
        thumbUrl = null,
        genres = listOf("Action", "Drama"),
        studios = listOf("Test Studio"),
        seriesId = null,
        seriesName = null,
        seasonId = null,
        episodeNumber = null,
        seasonNumber = null,
        tagline = "A great movie",
        cast = null
    )

    @Test
    fun `getRuntime returns empty string for null runtimeTicks`() {
        val item = createMovieItem(runtimeTicks = null)
        assertEquals("", item.getRuntime())
    }

    @Test
    fun `getRuntime returns minutes only for short movie`() {
        // 45 minutes in ticks: 45 * 60 * 10_000_000 = 27_000_000_000
        val item = createMovieItem(runtimeTicks = 27_000_000_000L)
        assertEquals("45m", item.getRuntime())
    }

    @Test
    fun `getRuntime returns hours and minutes for long movie`() {
        // 2h 30m = 150 minutes = 90_000_000_000 ticks
        val item = createMovieItem(runtimeTicks = 90_000_000_000L)
        assertEquals("2h 30m", item.getRuntime())
    }

    @Test
    fun `getRuntime returns 1h 0m for exactly one hour`() {
        // 60 minutes = 36_000_000_000 ticks
        val item = createMovieItem(runtimeTicks = 36_000_000_000L)
        assertEquals("1h 0m", item.getRuntime())
    }

    @Test
    fun `MediaItem default values are correct`() {
        val item = createMovieItem()
        assertFalse(item.isFavorite)
        assertEquals(0L, item.playbackPositionTicks)
        assertEquals(0.0, item.playedPercentage, 0.001)
        assertFalse(item.isPlayed)
    }
}
