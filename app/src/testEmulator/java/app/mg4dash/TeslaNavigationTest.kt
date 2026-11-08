package app.mg4dash

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.mg4dash.apps.AppsActivity
import app.mg4dash.pages.ClimateActivity
import app.mg4dash.pages.PhoneActivity
import app.mg4dash.pages.VehicleActivity
import app.mg4dash.settings.SettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "fr-w1920dp-h720dp-land-mdpi")
class TeslaNavigationTest {
    @Before fun customHome() {
        RuntimeEnvironment.getApplication().getSharedPreferences("mg4launcher", Context.MODE_PRIVATE)
            .edit().putInt("home_style", 3).putBoolean("vertical_dock", false).commit()
    }

    @Test fun existingPagesRemainReachableFromCustomHome() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        val root = activity.window.decorView
        fun find(predicate: (View) -> Boolean): View? {
            fun walk(v: View): View? {
                if (predicate(v)) return v
                if (v is ViewGroup) for (i in 0 until v.childCount) walk(v.getChildAt(i))?.let { return it }
                return null
            }
            return walk(root)
        }
        val railPages = listOf(
            R.string.dock_phone to PhoneActivity::class.java,
            R.string.dock_vehicle to VehicleActivity::class.java,
            R.string.dock_apps to AppsActivity::class.java,
            R.string.dock_settings to SettingsActivity::class.java,
        )
        railPages.forEach { (label, target) ->
            val button = find { it.contentDescription == activity.getString(label) && it.isShown }
            assertNotNull("Missing navigation: ${activity.getString(label)}", button)
            button!!.performClick()
            assertEquals(target.name, shadowOf(activity).nextStartedActivity.component?.className)
        }
        val climate = find { it is TextView && it.text == activity.getString(R.string.title_climate) && it.isShown }
        assertNotNull("Missing climate access", climate)
        climate!!.performClick()
        assertEquals(ClimateActivity::class.java.name, shadowOf(activity).nextStartedActivity.component?.className)
    }
}
