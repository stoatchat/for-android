package chat.stoat.screens.chat.views.channel

import org.junit.Assert.assertEquals
import org.junit.Test

class MessageWindowTest {
    @Test
    fun normalizeByUlidSortsDescendingAndKeepsFirstDuplicate() {
        data class Value(val id: String, val content: String)

        val normalized = normalizeByUlid(
            listOf(
                Value(id(2), "two"),
                Value(id(4), "four"),
                Value(id(2), "duplicate"),
                Value(id(1), "one"),
                Value(id(3), "three"),
            )
        ) { it.id }

        assertEquals(listOf(id(4), id(3), id(2), id(1)), normalized.map { it.id })
        assertEquals("two", normalized.first { it.id == id(2) }.content)
    }

    @Test
    fun nearbyBoundariesAreOpenWhenBothSidesFillTheirCapacity() {
        val target = 50
        val ids = ((target - 26)..(target + 25)).map(::id)

        assertEquals(
            NearbyBoundaries(canLoadNewer = true, canLoadOlder = true),
            calculateNearbyBoundaries(ids, id(target), requestedLimit = 50),
        )
    }

    @Test
    fun nearbyBoundariesCloseAtLatestAndOldestEdges() {
        val target = 25
        val ids = (0..target).map(::id)

        assertEquals(
            NearbyBoundaries(canLoadNewer = false, canLoadOlder = false),
            calculateNearbyBoundaries(ids, id(target), requestedLimit = 50),
        )
    }

    private fun id(value: Int): String = value.toString().padStart(26, '0')
}
