package dev.sam.countri

import dev.sam.countri.data.map.WorldMapData
import dev.sam.countri.ui.map.MapProjection
import dev.sam.countri.ui.map.MapViewport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class MapProjectionTest {

    private val w = 1080f
    private val h = 1920f

    /** Tiny map: a handful of loose vertices, one ring. */
    private fun tinyData(points: List<Pair<Float, Float>>): WorldMapData {
        val lon = FloatArray(points.size) { points[it].first }
        val lat = FloatArray(points.size) { points[it].second }
        return WorldMapData(
            ringCount = 1,
            ringCountry = intArrayOf(1),
            ringStart = intArrayOf(0),
            ringSize = intArrayOf(points.size),
            lon = lon,
            lat = lat,
        )
    }

    private fun project(
        data: WorldMapData,
        morph: Float,
        rotationDeg: Float = 0f,
        viewport: MapViewport = MapViewport.World,
    ): Triple<FloatArray, FloatArray, FloatArray> {
        val x = FloatArray(data.vertexCount)
        val y = FloatArray(data.vertexCount)
        val d = FloatArray(data.vertexCount)
        MapProjection.projectAll(data, viewport, w, h, rotationDeg, morph, x, y, d)
        return Triple(x, y, d)
    }

    @Test
    fun `flat projection round-trips through its inverse`() {
        val pts = listOf(2.35f to 48.85f, -74f to 40.7f, 138f to 36f, 0f to 0f)
        val data = tinyData(pts)
        val (x, y, _) = project(data, morph = 0f)
        pts.forEachIndexed { i, (lon, lat) ->
            val (invLon, invLat) = MapProjection.inverseFlat(x[i], y[i], MapViewport.World, w, h)!!
            assertEquals(lon, invLon, 0.01f)
            assertEquals(lat, invLat, 0.01f)
        }
    }

    @Test
    fun `globe projection round-trips through its inverse on the front hemisphere`() {
        val pts = listOf(10f to 20f, -40f to -10f, 0f to 55f)
        val data = tinyData(pts)
        for (rotation in listOf(0f, 33f, -120f, 260f)) {
            val (x, y, d) = project(data, morph = 1f, rotationDeg = rotation)
            pts.forEachIndexed { i, (lon, lat) ->
                if (d[i] > 0.05f) {
                    val (invLon, invLat) = MapProjection.inverseGlobe(x[i], y[i], w, h, rotation)!!
                    val dLon = abs(((invLon - lon + 540f) % 360f) - 180f)
                    assertEquals("lon@rot$rotation", 0f, dLon, 0.05f)
                    assertEquals("lat@rot$rotation", lat, invLat, 0.05f)
                }
            }
        }
    }

    @Test
    fun `morph endpoints match the pure projections and midpoint is between`() {
        val data = tinyData(listOf(30f to 30f))
        val (fx, fy, _) = project(data, morph = 0f)
        val (gx, gy, _) = project(data, morph = 1f)
        val (mx, my, _) = project(data, morph = 0.5f)
        assertEquals((fx[0] + gx[0]) / 2f, mx[0], 0.01f)
        assertEquals((fy[0] + gy[0]) / 2f, my[0], 0.01f)
    }

    @Test
    fun `depth separates front and back hemispheres`() {
        val data = tinyData(listOf(0f to 0f, 180f to 0f))
        val (_, _, d) = project(data, morph = 1f, rotationDeg = 0f)
        assertTrue("front facing", d[0] > 0.9f)
        assertTrue("back facing", d[1] < -0.9f)
    }

    @Test
    fun `inverse globe rejects points off the sphere`() {
        assertEquals(null, MapProjection.inverseGlobe(0f, 0f, w, h, 0f))
    }

    @Test
    fun `zoomed viewport magnifies distances around its center`() {
        val data = tinyData(listOf(2f to 46f, 3f to 46f))
        val world = project(data, 0f, viewport = MapViewport.World)
        val zoomed = project(data, 0f, viewport = MapViewport(2.5f, 46f, 5f))
        val dWorld = abs(world.first[1] - world.first[0])
        val dZoom = abs(zoomed.first[1] - zoomed.first[0])
        assertEquals(dWorld * 5f, dZoom, 0.5f)
    }
}
