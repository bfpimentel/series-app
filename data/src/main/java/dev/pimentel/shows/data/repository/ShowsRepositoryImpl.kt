package dev.pimentel.shows.data.repository

import dev.pimentel.shows.data.body.ShowResponseBody
import dev.pimentel.shows.data.body.ShowSearchResponseBody
import dev.pimentel.shows.data.dto.ShowDTO
import dev.pimentel.shows.data.model.ShowModelImpl
import dev.pimentel.shows.data.model.ShowsPageModelImpl
import dev.pimentel.shows.data.sources.local.ShowsLocalDataSource
import dev.pimentel.shows.data.sources.remote.ShowsRemoteDataSource
import dev.pimentel.shows.domain.model.ShowsPageModel
import dev.pimentel.shows.domain.repository.ShowsRepository
import dev.pimentel.shows.domain.usecase.GetShows
import kotlinx.coroutines.flow.*

class ShowsRepositoryImpl(
    private val showsRemoteDataSource: ShowsRemoteDataSource,
    private val showsLocalDataSource: ShowsLocalDataSource
) : ShowsRepository {

    private val getShowsPublisher = MutableSharedFlow<Pair<Int, String?>>()

    override fun getShows(): Flow<ShowsPageModel> =
        combine(
            getShowsPublisher.debounce(GET_SHOWS_DEBOUNCE_INTERVAL),
            showsLocalDataSource.getFavoriteShowsIds()
        ) { (page, query), favoriteIds -> Triple(page, query, favoriteIds) }
            .mapLatest { (page, query, favoriteIds) ->
                val shows = if (query == null) showsRemoteDataSource.getShows(page = page)
                else showsRemoteDataSource.getShows(query = query).map(ShowSearchResponseBody::info)

                Triple(page, query, shows.mapAllToModel(favoriteIds))
            }
            .distinctUntilChanged()
            .catch { emit(Triple(GetShows.NO_MORE_PAGES, null, emptyList())) }
            .scan(
                ShowsPageModelImpl(
                    shows = emptyList(),
                    nextPage = DEFAULT_PAGE
                )
            ) { accumulator, (page, query, shows) ->
                when {
                    query != null -> ShowsPageModelImpl(shows = shows, nextPage = DEFAULT_PAGE)
                    page == DEFAULT_PAGE -> ShowsPageModelImpl(shows = shows, nextPage = page + NEXT_PAGE_MODIFIER)
                    else -> ShowsPageModelImpl(shows = accumulator.shows + shows, nextPage = page + NEXT_PAGE_MODIFIER)
                }
            }

    override suspend fun getMoreShows(nextPage: Int) = getShowsPublisher.emit(Pair(nextPage, null))

    override suspend fun searchShows(query: String) = getShowsPublisher.emit(Pair(DEFAULT_PAGE, query))

    override suspend fun favoriteShow(showId: Int) = showsLocalDataSource.saveFavoriteShow(
        ShowDTO(id = showId, name = "Placeholder") // TODO: Get show details from endpoint and then save it
    )

    override suspend fun removeShowFromFavorites(showId: Int) = showsLocalDataSource.removeShowFromFavorites(showId)

    private fun List<ShowResponseBody>.mapAllToModel(favoriteIds: List<Int>) = map { show ->
        ShowModelImpl(
            id = show.id,
            name = show.name,
            status = show.status,
            premieredDate = show.premieredDate,
            rating = show.rating.average,
            imageUrl = show.image.originalUrl,
            isFavorite = favoriteIds.contains(show.id)
        )
    }

    private companion object {
        const val GET_SHOWS_DEBOUNCE_INTERVAL = 1000L
        const val DEFAULT_PAGE = 0
        const val NEXT_PAGE_MODIFIER = 1
    }
}
