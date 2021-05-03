package dev.pimentel.shows.domain.usecase

import dev.pimentel.shows.domain.entity.Show
import dev.pimentel.shows.domain.model.ShowModel
import dev.pimentel.shows.domain.repository.ShowsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetFavoritesTest {

    private val showsRepository = mockk<ShowsRepository>()
    private val getFavorites = GetFavorites(showsRepository)

    @Test
    fun `should get shows and map them to entities`() = runBlockingTest {
        val showsModel = listOf(
            object : ShowModel {
                override val id: Int = 1
                override val name: String = "name1"
                override val status: String = "status1"
                override val premieredDate: String = "date1"
                override val rating: Float = 1F
                override val imageUrl: String = "image1"
                override val isFavorite: Boolean = false
            },
            object : ShowModel {
                override val id: Int = 2
                override val name: String = "name2"
                override val status: String = "status2"
                override val premieredDate: String = "date2"
                override val rating: Float = 2F
                override val imageUrl: String = "image2"
                override val isFavorite: Boolean = false
            }
        )

        val shows = listOf(
            Show(
                id = 1,
                name = "name1",
                status = "status1",
                premieredDate = "date1",
                rating = 1F,
                imageUrl = "image1",
                isFavorite = false
            ),
            Show(
                id = 2,
                name = "name2",
                status = "status2",
                premieredDate = "date2",
                rating = 2F,
                imageUrl = "image2",
                isFavorite = false
            ),
        )

        coEvery { showsRepository.getFavoriteShows() } returns flowOf(showsModel)

        assertEquals(getFavorites(NoParams).first(), shows)

        coVerify(exactly = 1) { showsRepository.getFavoriteShows() }
        confirmVerified(showsRepository)
    }
}
