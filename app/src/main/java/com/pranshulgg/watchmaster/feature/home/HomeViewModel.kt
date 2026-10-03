package com.pranshulgg.watchmaster.feature.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pranshulgg.watchmaster.core.network.TmdbApi
import com.pranshulgg.watchmaster.core.network.TmdbResult
import com.pranshulgg.watchmaster.data.SimpleMovie
import com.pranshulgg.watchmaster.data.repository.WatchlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


data class HomeMediaItem(
    val id: Long,
    val mediaType: String,
    val title: String,
    val posterPath: String?,
    val releaseDate: String?,
    val rating: Double,
)

data class HomeSection(
    val title: String,
    val items: List<HomeMediaItem>
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val sections: List<HomeSection> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: TmdbApi,
    private val watchlistRepository: WatchlistRepository,
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    init {
        load()
    }

    fun refresh() {
        load()
    }

    private fun load() {
        if (uiState.isLoading && uiState.sections.isNotEmpty()) return

        viewModelScope.launch {
            uiState = HomeUiState(isLoading = true)

            try {
                val watchlist = watchlistRepository.getWatchlist().first()
                val preferredGenres = watchlist
                    .flatMap { it.genreIds.orEmpty() }
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .sortedByDescending { it.value }
                    .take(3)
                    .joinToString(",") { it.key.toString() }

                val results = listOf(
                    async { "Trending" to fetchTrending() },
                    async { "Popular Movies" to fetchMovies { api.getPopularMovies().body()?.results.orEmpty() } },
                    async { "Popular TV Shows" to fetchTv() },
                    async { "Now Playing" to fetchMovies { api.getNowPlayingMovies().body()?.results.orEmpty() } },
                    async { "Upcoming" to fetchMovies { api.getUpcomingMovies().body()?.results.orEmpty() } },
                    async {
                        if (preferredGenres.isNotBlank()) {
                            "Recommended for you" to fetchMovies {
                                api.discoverMovies(withGenres = preferredGenres).body()?.results.orEmpty()
                            }
                        } else {
                            "Recommended for you" to fetchTrending()
                        }
                    },
                ).awaitAll()

                val sections = results.mapNotNull { (title, items) ->
                    val cleaned = items.distinctBy { it.id to it.mediaType }.take(12)
                    if (cleaned.isNotEmpty()) HomeSection(title, cleaned) else null
                }

                uiState = HomeUiState(
                    isLoading = false,
                    sections = sections,
                    error = if (sections.isEmpty()) "No home content could be loaded." else null,
                )
            } catch (t: Throwable) {
                uiState = HomeUiState(
                    isLoading = false,
                    error = "Unable to load Home right now. Check your internet connection and try again."
                )
            }
        }
    }

    private suspend fun fetchMovies(block: suspend () -> List<SimpleMovie>): List<HomeMediaItem> =
        runCatching { block() }.getOrDefault(emptyList()).mapNotNull { movie ->
            val title = movie.title ?: return@mapNotNull null
            HomeMediaItem(
                id = movie.id,
                mediaType = "movie",
                title = title,
                posterPath = movie.poster_path,
                releaseDate = movie.release_date,
                rating = movie.vote_average,
            )
        }

    private suspend fun fetchTv(): List<HomeMediaItem> = runCatching {
        api.getPopularTv().body()?.results.orEmpty().map {
            HomeMediaItem(
                id = it.id,
                mediaType = "tv",
                title = it.name,
                posterPath = it.poster_path,
                releaseDate = it.first_air_date,
                rating = it.vote_average,
            )
        }
    }.getOrDefault(emptyList())

    private suspend fun fetchTrending(): List<HomeMediaItem> = runCatching {
        api.getTrending().body()?.results.orEmpty().mapNotNull { item: TmdbResult ->
            val mediaType = item.media_type ?: return@mapNotNull null
            if (mediaType != "movie" && mediaType != "tv") return@mapNotNull null
            HomeMediaItem(
                id = item.id,
                mediaType = mediaType,
                title = item.title ?: item.name ?: return@mapNotNull null,
                posterPath = item.poster_path,
                releaseDate = item.releaseDate ?: item.firstAirDate,
                rating = item.vote_average,
            )
        }
    }.getOrDefault(emptyList())
}
