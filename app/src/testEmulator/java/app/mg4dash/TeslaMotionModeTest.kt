package app.mg4dash

import app.mg4dash.ui.TeslaMotionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class TeslaMotionModeTest {
    @Test fun requiresStableVehicleSpeedToChangeLayout() {
        val mode = TeslaMotionMode()
        val parked = TeslaMotionMode.Layout.PARKED
        val driving = TeslaMotionMode.Layout.DRIVING
        assertEquals(parked, mode.update(null, 0))
        assertEquals(parked, mode.update(90f, 1_000))
        assertEquals(parked, mode.update(90f, 2_400))
        assertEquals(driving, mode.update(90f, 2_500))
        assertEquals(driving, mode.update(0f, 3_000))
        assertEquals(driving, mode.update(0f, 5_999))
        assertEquals(parked, mode.update(0f, 6_000))
    }

    @Test fun unknownAndBorderlineSpeedNeverInventOrFlicker() {
        val mode = TeslaMotionMode()
        val driving = TeslaMotionMode.Layout.DRIVING
        assertEquals(TeslaMotionMode.Layout.PARKED, mode.update(Float.NaN, 0))
        mode.update(6f, 100)
        assertEquals(TeslaMotionMode.Layout.PARKED, mode.update(null, 2_000))
        mode.update(6f, 2_100)
        assertEquals(driving, mode.update(6f, 3_600))
        assertEquals(driving, mode.update(2f, 20_000))
        assertEquals(driving, mode.update(500f, 25_000))
        assertEquals(driving, mode.update(null, 30_000))
    }
}
