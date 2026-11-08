package app.mg4dash.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.mg4dash.R
import app.mg4dash.apps.AppsActivity
import app.mg4dash.car.CarBridge
import app.mg4dash.car.VehicleState
import app.mg4dash.energy.ConsumptionEstimator
import app.mg4dash.map.LocationRepo
import app.mg4dash.map.MapCard
import app.mg4dash.map.NavRepo
import app.mg4dash.media.MediaHub
import app.mg4dash.pages.ClimateActivity
import app.mg4dash.pages.EnergyActivity
import app.mg4dash.pages.PhoneActivity
import app.mg4dash.pages.VehicleActivity
import app.mg4dash.settings.SettingsActivity
import app.mg4dash.shortcuts.Shortcuts
import app.mg4dash.system.Apps
import app.mg4dash.system.Clock
import app.mg4dash.system.ConnectionToggle
import app.mg4dash.system.Units
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A visual home for the existing MG4Dash services and pages. */
class TeslaHome(private val a: AppCompatActivity) {
    private val kit = Kit(a)
    private val motion = TeslaMotionMode()
    private val red = Color.rgb(232, 35, 56)
    private val white = Color.rgb(246, 247, 250)
    private val muted = Color.rgb(159, 166, 179)
    private val black = Color.rgb(7, 9, 13)
    private val panel = Color.rgb(18, 21, 27)

    val root = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; setBackgroundColor(black) }
    private val body = FrameLayout(a)
    private val parked = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
    private val driving = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL; visibility = View.GONE }
    private val parkedVehicleSlot = FrameLayout(a)
    private val drivingVehicleSlot = FrameLayout(a)
    private val parkedMapSlot = FrameLayout(a)
    private val drivingMapSlot = FrameLayout(a)
    private val focusSlot = FrameLayout(a).apply { visibility = View.GONE; setBackgroundColor(black) }
    private val shortcutsShade = FrameLayout(a).apply { visibility = View.GONE; setBackgroundColor(0xE8000000.toInt()); isClickable = true }
    private val climateFooter = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
    private val driveFooter = LinearLayout(a).apply { gravity = Gravity.CENTER; visibility = View.GONE }
    private val vehiclePanel = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
    private val mapPanel = RoundedFrame(a, radiusDp = 18f)
    private val mapHolder = FrameLayout(a)
    private val mapCard: MapCard
    private val navBanner = NavBanner(a)
    private val mediaPanel = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }

    private lateinit var speed: TextView
    private lateinit var speedUnit: TextView
    private lateinit var mode: TextView
    private lateinit var battery: TextView
    private lateinit var range: TextView
    private lateinit var consumption: TextView
    private lateinit var batteryBar: ProgressBar
    private lateinit var tripBattery: TextView
    private lateinit var tripConsumption: TextView
    private lateinit var odometer: TextView
    private lateinit var temp: TextView
    private lateinit var fan: TextView
    private lateinit var ac: TextView
    private lateinit var auto: TextView
    private lateinit var defrost: ImageButton
    private lateinit var recirc: ImageButton
    private lateinit var mediaSource: TextView
    private lateinit var mediaTitle: TextView
    private lateinit var mediaArtist: TextView
    private lateinit var mediaArt: ImageView
    private lateinit var mediaPlay: ImageButton
    private lateinit var mediaProgress: ProgressBar
    private lateinit var clock: TextView
    private lateinit var outside: TextView
    private lateinit var projectionButton: ImageButton
    private var mapFocused = false

    init {
        val rail = buildRail()
        root.addView(rail, LinearLayout.LayoutParams(kit.dp(76), -1))
        val content = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(kit.dp(8), kit.dp(7), kit.dp(12), kit.dp(8))
        }
        root.addView(content, LinearLayout.LayoutParams(0, -1, 1f))
        content.addView(buildHeader(), LinearLayout.LayoutParams(-1, kit.dp(50)))
        content.addView(body, LinearLayout.LayoutParams(-1, 0, 1f).apply { bottomMargin = kit.dp(9) })
        buildParked()
        buildDriving()
        body.addView(parked, FrameLayout.LayoutParams(-1, -1))
        body.addView(driving, FrameLayout.LayoutParams(-1, -1))
        body.addView(focusSlot, FrameLayout.LayoutParams(-1, -1))
        buildShortcutsShade()
        body.addView(shortcutsShade, FrameLayout.LayoutParams(-1, -1))

        buildVehicle()
        buildMap()
        mapCard = MapCard(a, mapHolder, forceDark = true)
        mapCard.onDoubleTap = { toggleMapFocus() }
        buildMedia()
        buildClimateFooter()
        buildDriveFooter()
        content.addView(climateFooter, LinearLayout.LayoutParams(-1, kit.dp(76)))
        content.addView(driveFooter, LinearLayout.LayoutParams(-1, kit.dp(76)))
        showLayout(TeslaMotionMode.Layout.PARKED)
    }

    private fun shape(color: Int = panel, radius: Float = 18f, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = kit.dp(radius.toInt()).toFloat()
        if (stroke != null) setStroke(kit.dp(1), stroke)
    }

    private fun text(value: String, size: Float, color: Int = white, medium: Boolean = false) = TextView(a).apply {
        this.text = value; setTextColor(color); setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
        typeface = Typeface.create(if (medium) "sans-serif-medium" else "sans-serif", Typeface.NORMAL)
        includeFontPadding = false; maxLines = 1
    }

    private fun icon(drawable: Int, size: Int = 56, onClick: () -> Unit) = ImageButton(a).apply {
        setImageResource(drawable); setColorFilter(muted); background = shape(Color.TRANSPARENT, 12f)
        scaleType = ImageView.ScaleType.FIT_CENTER
        setPadding(kit.dp(size / 4), kit.dp(size / 4), kit.dp(size / 4), kit.dp(size / 4))
        contentDescription = a.resources.getResourceEntryName(drawable)
        isFocusable = true; setOnClickListener { onClick() }
    }

    private fun addIcon(row: LinearLayout, drawable: Int, label: Int, action: () -> Unit): ImageButton {
        val button = icon(drawable, 58, action).apply { contentDescription = a.getString(label) }
        row.addView(button, LinearLayout.LayoutParams(kit.dp(58), kit.dp(58)))
        return button
    }

    private fun buildRail(): LinearLayout = LinearLayout(a).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL
        setBackgroundColor(Color.rgb(10, 12, 16))
        val compact = a.resources.displayMetrics.heightPixels < kit.dp(690)
        val iconSize = if (compact) 52 else 62
        val iconGap = if (compact) 1 else 3
        // The SAIC fog-light control overlays this area on the car.
        addView(Space(a), LinearLayout.LayoutParams(1, kit.dp(if (compact) 60 else 74)))
        fun item(drawable: Int, label: Int, action: () -> Unit) {
            addView(icon(drawable, iconSize, action).apply { contentDescription = a.getString(label) },
                LinearLayout.LayoutParams(kit.dp(iconSize), kit.dp(iconSize)).apply { bottomMargin = kit.dp(iconGap) })
        }
        item(R.drawable.ic_home, R.string.dock_home) { closeOverlays() }
        item(R.drawable.ic_map, R.string.wc_map) { if (!mapFocused) toggleMapFocus() else exitMapFocus() }
        projectionButton = icon(R.drawable.ic_projection, iconSize) { Dock.openProjection(a) }.apply {
            contentDescription = a.getString(R.string.dock_aa)
        }
        addView(projectionButton, LinearLayout.LayoutParams(kit.dp(iconSize), kit.dp(iconSize)).apply { bottomMargin = kit.dp(iconGap) })
        Dock.aaGestures(a, projectionButton, {
            if (ConnectionToggle.isOff(a)) Toasts.show(a, a.getString(R.string.dock_conn_off, a.getString(R.string.dock_aa)))
            else Dock.openProjection(a)
        }, { refreshProjection() })
        refreshProjection()
        item(R.drawable.ic_music, R.string.title_media) { openMedia() }
        item(R.drawable.ic_phone, R.string.dock_phone) { page(PhoneActivity::class.java) }
        item(R.drawable.ic_car, R.string.dock_vehicle) { page(VehicleActivity::class.java) }
        item(R.drawable.ic_apps, R.string.dock_apps) { page(AppsActivity::class.java) }
        item(R.drawable.ic_ha, R.string.sc_ha) { showShortcuts() }
        item(R.drawable.ic_settings, R.string.dock_settings) { page(SettingsActivity::class.java) }
    }

    private fun refreshProjection() {
        projectionButton.setColorFilter(if (ConnectionToggle.isOff(a)) red else muted)
    }

    private fun buildHeader(): View {
        val row = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        val name = text("MG4", 23f, white, true)
        row.addView(name)
        row.addView(text("Dash", 23f, red, true))
        row.addView(Space(a), LinearLayout.LayoutParams(0, 1, 1f))
        outside = text("—°", 17f, muted)
        row.addView(outside, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = kit.dp(28) })
        clock = text("", 17f, white, true)
        row.addView(clock, LinearLayout.LayoutParams(kit.dp(65), -2))
        clock.post(object : Runnable {
            override fun run() {
                if (!clock.isAttachedToWindow) return
                clock.text = Clock.hm(a)
                clock.postDelayed(this, 60_000L - System.currentTimeMillis() % 60_000L)
            }
        })
        return row
    }

    private fun buildParked() {
        parked.addView(parkedVehicleSlot, LinearLayout.LayoutParams(0, -1, 36f).apply { rightMargin = kit.dp(12) })
        val right = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        parked.addView(right, LinearLayout.LayoutParams(0, -1, 64f))
        right.addView(parkedMapSlot, LinearLayout.LayoutParams(-1, 0, 63f).apply { bottomMargin = kit.dp(10) })
        val lower = LinearLayout(a).apply { orientation = LinearLayout.HORIZONTAL }
        right.addView(lower, LinearLayout.LayoutParams(-1, 0, 37f))
        lower.addView(buildTripPanel(), LinearLayout.LayoutParams(0, -1, 34f).apply { rightMargin = kit.dp(10) })
        lower.addView(mediaPanel, LinearLayout.LayoutParams(0, -1, 66f))
    }

    private fun buildDriving() {
        driving.addView(drivingVehicleSlot, LinearLayout.LayoutParams(0, -1, 34f).apply { rightMargin = kit.dp(12) })
        driving.addView(drivingMapSlot, LinearLayout.LayoutParams(0, -1, 44f).apply { rightMargin = kit.dp(12) })
        val actions = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        driving.addView(actions, LinearLayout.LayoutParams(0, -1, 22f))
        fun tile(drawable: Int, label: Int, action: () -> Unit) {
            val cell = LinearLayout(a).apply {
                gravity = Gravity.CENTER_VERTICAL; background = shape()
                isClickable = true; isFocusable = true; setOnClickListener { action() }
            }
            cell.addView(ImageView(a).apply { setImageResource(drawable); setColorFilter(muted) },
                LinearLayout.LayoutParams(kit.dp(36), kit.dp(36)).apply { leftMargin = kit.dp(20) })
            cell.addView(text(a.getString(label), 19f, white, true), LinearLayout.LayoutParams(-1, -2).apply { leftMargin = kit.dp(18) })
            actions.addView(cell, LinearLayout.LayoutParams(-1, 0, 1f).apply { bottomMargin = kit.dp(9) })
        }
        tile(R.drawable.ic_music, R.string.title_media) { openMedia() }
        tile(R.drawable.ic_snow, R.string.title_climate) { page(ClimateActivity::class.java) }
        tile(R.drawable.ic_apps, R.string.dock_apps) { page(AppsActivity::class.java) }
        tile(R.drawable.ic_ha, R.string.sc_ha) { showShortcuts() }
    }

    private fun buildVehicle() {
        vehiclePanel.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(black, Color.rgb(16, 18, 24), black)).apply { cornerRadius = kit.dp(18).toFloat() }
        vehiclePanel.setPadding(kit.dp(16), kit.dp(14), kit.dp(16), kit.dp(12))
        val numbers = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        val speedBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        speed = text("—", 82f, white).apply { gravity = Gravity.CENTER; fontFeatureSettings = "tnum" }
        speedUnit = text(Units.speedUnit(a), 16f, muted).apply { gravity = Gravity.CENTER }
        speedBox.addView(speed)
        speedBox.addView(speedUnit)
        numbers.addView(speedBox, LinearLayout.LayoutParams(0, -2, 1f))
        val gearBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        gearBox.addView(text(a.getString(R.string.tesla_gear), 15f, muted))
        gearBox.addView(text("—", 30f, white, true).apply { contentDescription = a.getString(R.string.tesla_gear_unknown) })
        numbers.addView(gearBox, LinearLayout.LayoutParams(kit.dp(120), -2))
        vehiclePanel.addView(numbers, LinearLayout.LayoutParams(-1, -2))
        mode = text("", 17f, red, true).apply { gravity = Gravity.CENTER; visibility = View.GONE }
        vehiclePanel.addView(mode, LinearLayout.LayoutParams(-1, -2))
        val car = ImageView(a).apply {
            setImageResource(R.drawable.mg4_tesla_white); scaleType = ImageView.ScaleType.FIT_CENTER
            contentDescription = a.getString(R.string.tesla_car_decorative)
        }
        vehiclePanel.addView(car, LinearLayout.LayoutParams(-1, 0, 1f))
        val values = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        val batteryBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        battery = text("— %", 27f, white, true)
        batteryBox.addView(battery)
        batteryBox.addView(text(a.getString(R.string.vh_battery), 14f, muted))
        values.addView(batteryBox, LinearLayout.LayoutParams(0, -2, 1f))
        val rangeBox = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        range = text("— ${Units.distUnit(a)}", 27f, white, true)
        rangeBox.addView(range)
        rangeBox.addView(text(a.getString(R.string.range_label), 14f, muted))
        values.addView(rangeBox, LinearLayout.LayoutParams(0, -2, 1f))
        vehiclePanel.addView(values, LinearLayout.LayoutParams(-1, -2))
        batteryBar = ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100; progressTintList = android.content.res.ColorStateList.valueOf(red)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(53, 58, 66))
        }
        vehiclePanel.addView(batteryBar, LinearLayout.LayoutParams(-1, kit.dp(5)).apply { topMargin = kit.dp(10) })
        consumption = text("— ${Units.consUnit(a)}", 15f, muted)
        vehiclePanel.addView(consumption, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(9) })
    }

    private fun buildTripPanel(): View {
        val box = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL; background = shape()
            setPadding(kit.dp(18), kit.dp(12), kit.dp(14), kit.dp(12))
            isClickable = true; setOnClickListener { page(EnergyActivity::class.java) }
        }
        val batteryRow = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        batteryRow.addView(ImageView(a).apply { setImageResource(R.drawable.ic_battery); setColorFilter(red) }, LinearLayout.LayoutParams(kit.dp(28), kit.dp(28)))
        tripBattery = text("— %", 22f, white, true)
        batteryRow.addView(tripBattery, LinearLayout.LayoutParams(-1, -2).apply { leftMargin = kit.dp(8) })
        box.addView(batteryRow)
        tripConsumption = text("— ${Units.consUnit(a)}", 16f, muted)
        box.addView(tripConsumption, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(10) })
        odometer = text("— ${Units.distUnit(a)}", 15f, muted)
        box.addView(odometer, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(8) })
        return box
    }

    private fun buildMap() {
        mapPanel.background = shape(Color.rgb(17, 20, 27))
        mapPanel.addView(mapHolder, FrameLayout.LayoutParams(-1, -1))
        mapPanel.addView(navBanner, FrameLayout.LayoutParams(kit.dp(440), -2, Gravity.TOP or Gravity.START).apply {
            leftMargin = kit.dp(14); topMargin = kit.dp(14)
        })
        val controls = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        val recenter = icon(R.drawable.ic_car_marker, 52) { mapCard.recenter(LocationRepo.location.value) }
        recenter.contentDescription = a.getString(R.string.map_recenter)
        controls.addView(recenter, LinearLayout.LayoutParams(kit.dp(52), kit.dp(52)))
        val full = icon(R.drawable.ic_fullscreen, 52) { toggleMapFocus() }
        full.contentDescription = a.getString(R.string.map_fullscreen)
        controls.addView(full, LinearLayout.LayoutParams(kit.dp(52), kit.dp(52)).apply { topMargin = kit.dp(8) })
        mapPanel.addView(controls, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
            topMargin = kit.dp(14); rightMargin = kit.dp(14)
        })
    }

    private fun buildMedia() {
        mediaPanel.background = shape()
        mediaPanel.setPadding(kit.dp(17), kit.dp(12), kit.dp(17), kit.dp(10))
        mediaSource = text(a.getString(R.string.title_media) + " ▾", 15f, muted, true)
        SourceMenu.attach(a, mediaSource)
        mediaPanel.addView(mediaSource)
        val row = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        mediaArt = ImageView(a).apply { scaleType = ImageView.ScaleType.CENTER_CROP; background = shape(Color.rgb(39, 43, 52)) }
        row.addView(mediaArt, LinearLayout.LayoutParams(kit.dp(90), kit.dp(90)))
        val meta = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        mediaTitle = text(a.getString(R.string.no_media), 20f, white, true).apply { ellipsize = android.text.TextUtils.TruncateAt.END }
        mediaArtist = text("", 15f, muted).apply { ellipsize = android.text.TextUtils.TruncateAt.END }
        meta.addView(mediaTitle)
        meta.addView(mediaArtist, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(5) })
        meta.setOnClickListener { openMedia() }
        row.addView(meta, LinearLayout.LayoutParams(0, -1, 1f).apply { leftMargin = kit.dp(14) })
        addIcon(row, R.drawable.ic_prev, R.string.cd_previous) { MediaHub.previous() }
        mediaPlay = addIcon(row, R.drawable.ic_play, R.string.cd_play_pause) { MediaHub.playPause() }
        mediaPlay.setOnLongClickListener { page(app.mg4dash.pages.SoundActivity::class.java); true }
        addIcon(row, R.drawable.ic_next, R.string.cd_next) { MediaHub.next() }
        mediaPanel.addView(row, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = kit.dp(8) })
        mediaProgress = ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000; progressTintList = android.content.res.ColorStateList.valueOf(red)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.rgb(54, 59, 67))
        }
        mediaPanel.addView(mediaProgress, LinearLayout.LayoutParams(-1, kit.dp(3)).apply { topMargin = kit.dp(7) })
    }

    private fun climateButton(drawable: Int, description: Int, action: () -> Unit): ImageButton =
        icon(drawable, 62, action).apply { contentDescription = a.getString(description) }

    private fun buildClimateFooter() {
        climateFooter.background = shape(Color.rgb(13, 16, 21))
        climateFooter.setPadding(kit.dp(10), 0, kit.dp(10), 0)
        climateFooter.addView(climateButton(R.drawable.ic_minus, R.string.cd_temp_down) { CarBridge.stepTemp(-1) }, LinearLayout.LayoutParams(kit.dp(58), kit.dp(58)))
        temp = text("—", 24f, white, true).apply { gravity = Gravity.CENTER; setOnClickListener { page(ClimateActivity::class.java) } }
        climateFooter.addView(temp, LinearLayout.LayoutParams(kit.dp(90), -1))
        climateFooter.addView(climateButton(R.drawable.ic_plus, R.string.cd_temp_up) { CarBridge.stepTemp(1) }, LinearLayout.LayoutParams(kit.dp(58), kit.dp(58)))
        climateFooter.addView(Space(a), LinearLayout.LayoutParams(0, 1, 1f))
        climateFooter.addView(climateButton(R.drawable.ic_minus, R.string.cd_fan_down) { CarBridge.stepFan(-1) }, LinearLayout.LayoutParams(kit.dp(58), kit.dp(58)))
        fan = text("—", 21f, white, true).apply { gravity = Gravity.CENTER; setOnClickListener { page(ClimateActivity::class.java) } }
        climateFooter.addView(fan, LinearLayout.LayoutParams(kit.dp(95), -1))
        climateFooter.addView(climateButton(R.drawable.ic_plus, R.string.cd_fan_up) { CarBridge.stepFan(1) }, LinearLayout.LayoutParams(kit.dp(58), kit.dp(58)))
        climateFooter.addView(Space(a), LinearLayout.LayoutParams(0, 1, 1f))
        ac = text(a.getString(R.string.chip_ac), 18f, white, true).apply {
            gravity = Gravity.CENTER; background = shape(Color.rgb(30, 34, 42), 12f)
            setOnClickListener { CarBridge.toggleAc() }
        }
        climateFooter.addView(ac, LinearLayout.LayoutParams(kit.dp(74), kit.dp(54)))
        auto = text(a.getString(R.string.chip_auto), 18f, white, true).apply {
            gravity = Gravity.CENTER; background = shape(Color.rgb(30, 34, 42), 12f)
            setOnClickListener { CarBridge.toggleAuto() }
        }
        climateFooter.addView(auto, LinearLayout.LayoutParams(kit.dp(88), kit.dp(54)).apply { leftMargin = kit.dp(8) })
        defrost = climateButton(R.drawable.ic_flow_screen, R.string.chip_front) { CarBridge.toggleDefrostFront() }
        climateFooter.addView(defrost, LinearLayout.LayoutParams(kit.dp(62), kit.dp(62)).apply { leftMargin = kit.dp(12) })
        recirc = climateButton(R.drawable.ic_recirc, R.string.chip_recirc) { CarBridge.toggleRecirc() }
        climateFooter.addView(recirc, LinearLayout.LayoutParams(kit.dp(62), kit.dp(62)).apply { leftMargin = kit.dp(6) })
        climateFooter.addView(Space(a), LinearLayout.LayoutParams(0, 1, 1f))
        climateFooter.addView(text(a.getString(R.string.title_climate), 17f, muted, true).apply {
            gravity = Gravity.CENTER; setOnClickListener { page(ClimateActivity::class.java) }
        }, LinearLayout.LayoutParams(kit.dp(156), -1))
    }

    private fun buildDriveFooter() {
        driveFooter.background = shape(Color.rgb(13, 16, 21))
        val items = listOf(
            Triple(R.drawable.ic_home, R.string.dock_home, { closeOverlays() }),
            Triple(R.drawable.ic_car, R.string.dock_vehicle, { page(VehicleActivity::class.java) }),
            Triple(R.drawable.ic_map, R.string.wc_map, { toggleMapFocus() }),
            Triple(R.drawable.ic_projection, R.string.dock_aa, { Dock.openProjection(a) }),
            Triple(R.drawable.ic_music, R.string.title_media, { openMedia() }),
            Triple(R.drawable.ic_phone, R.string.dock_phone, { page(PhoneActivity::class.java) }),
            Triple(R.drawable.ic_snow, R.string.dock_climate, { page(ClimateActivity::class.java) }),
            Triple(R.drawable.ic_apps, R.string.dock_apps, { page(AppsActivity::class.java) }),
            Triple(R.drawable.ic_settings, R.string.dock_settings, { page(SettingsActivity::class.java) }),
        )
        items.forEach { (drawable, label, action) ->
            val cell = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isClickable = true; isFocusable = true; setOnClickListener { action() } }
            cell.addView(ImageView(a).apply { setImageResource(drawable); setColorFilter(if (drawable == R.drawable.ic_home) red else muted) },
                LinearLayout.LayoutParams(kit.dp(28), kit.dp(28)))
            cell.addView(text(a.getString(label), 13f, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = kit.dp(3) })
            driveFooter.addView(cell, LinearLayout.LayoutParams(0, -1, 1f))
        }
    }

    private fun buildShortcutsShade() {
        shortcutsShade.setOnClickListener { shortcutsShade.visibility = View.GONE }
        val box = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL; background = shape(Color.rgb(20, 23, 29), 20f, red)
            setPadding(kit.dp(22), kit.dp(14), kit.dp(22), kit.dp(22)); isClickable = true
        }
        val top = LinearLayout(a).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(text(a.getString(R.string.title_shortcuts), 24f, white, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(icon(R.drawable.ic_fullscreen_exit, 56) { shortcutsShade.visibility = View.GONE }.apply { contentDescription = a.getString(R.string.close) },
            LinearLayout.LayoutParams(kit.dp(56), kit.dp(56)))
        box.addView(top)
        val holder = LinearLayout(a).apply { orientation = LinearLayout.VERTICAL }
        box.addView(holder, LinearLayout.LayoutParams(-1, 0, 1f))
        shortcutsShade.addView(box, FrameLayout.LayoutParams(kit.dp(950), kit.dp(400), Gravity.CENTER))
        shortcutsHolder = holder
    }

    private lateinit var shortcutsHolder: LinearLayout

    private fun showShortcuts() {
        exitMapFocus()
        Shortcuts.render(a, shortcutsHolder, 0, rows = 2) { }
        shortcutsShade.visibility = View.VISIBLE
    }

    private fun closeOverlays() {
        shortcutsShade.visibility = View.GONE
        exitMapFocus()
    }

    private fun move(view: View, target: ViewGroup) {
        if (view.parent === target) return
        (view.parent as? ViewGroup)?.removeView(view)
        target.addView(view, ViewGroup.LayoutParams(-1, -1))
    }

    private fun showLayout(layout: TeslaMotionMode.Layout) {
        val isDriving = layout == TeslaMotionMode.Layout.DRIVING
        parked.visibility = if (isDriving) View.GONE else View.VISIBLE
        driving.visibility = if (isDriving) View.VISIBLE else View.GONE
        climateFooter.visibility = if (isDriving) View.GONE else View.VISIBLE
        driveFooter.visibility = if (isDriving) View.VISIBLE else View.GONE
        move(vehiclePanel, if (isDriving) drivingVehicleSlot else parkedVehicleSlot)
        if (!mapFocused) move(mapPanel, if (isDriving) drivingMapSlot else parkedMapSlot)
    }

    private fun toggleMapFocus() {
        if (mapFocused) { exitMapFocus(); return }
        shortcutsShade.visibility = View.GONE
        mapFocused = true
        focusSlot.visibility = View.VISIBLE
        move(mapPanel, focusSlot)
        mapCard.oneFingerPan = true
    }

    fun exitMapFocus() {
        if (!mapFocused) return
        mapFocused = false
        focusSlot.visibility = View.GONE
        move(mapPanel, if (motion.layout == TeslaMotionMode.Layout.DRIVING) drivingMapSlot else parkedMapSlot)
        mapCard.oneFingerPan = false
    }

    private fun page(target: Class<*>) {
        closeOverlays()
        a.startActivity(Intent(a, target).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION))
        @Suppress("DEPRECATION") a.overridePendingTransition(0, 0)
    }

    private fun openMedia() {
        if (MediaHub.openCarPlayNowPlaying() || MediaHub.openServiceMediaApp()) return
        val owner = MediaHub.ownerComponent()
        if (owner != null) Apps.open(a, owner, "Media") else Apps.open(a, Apps.RADIO, "Radio")
    }

    fun start() {
        a.lifecycleScope.launch {
            a.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { CarBridge.state.collect { renderVehicle(it); renderClimate(it) } }
                launch { ConsumptionEstimator.average.collect { renderConsumption(it) } }
                launch { MediaHub.now.collect { renderMedia(it) } }
                launch { LocationRepo.location.collect { it?.let(mapCard::update) } }
                launch { NavRepo.nav.collect { navBanner.show(it) } }
                launch {
                    while (true) {
                        val s = CarBridge.state.value
                        val layout = motion.update(s.speedKmh.takeIf { s.connected }, SystemClock.elapsedRealtime())
                        if ((layout == TeslaMotionMode.Layout.DRIVING) != (driving.visibility == View.VISIBLE)) showLayout(layout)
                        delay(250)
                    }
                }
            }
        }
    }

    fun onResume() { mapCard.onResume() }
    fun onPause() { mapCard.onPause() }

    private fun renderVehicle(s: VehicleState) {
        val v = s.speedKmh?.takeIf { s.connected && it.isFinite() && it in 0f..300f }
        speed.text = v?.let { Math.round(Units.dist(a, it)).toString() } ?: "—"
        speedUnit.text = Units.speedUnit(a)
        mode.visibility = if (s.driveMode == null) View.GONE else View.VISIBLE
        mode.text = if (s.driveMode == null) "" else a.getString(R.string.mode) + "  " + VehicleState.driveModeLabel(s.driveMode)
        battery.text = VehicleState.socLabel(s) ?: "— %"
        tripBattery.text = battery.text
        batteryBar.progress = s.batteryPercent?.coerceIn(0, 100) ?: 0
        range.text = s.rangeKm?.let { Units.distText(a, it.toFloat()) } ?: "— ${Units.distUnit(a)}"
        odometer.text = s.odometerKm?.let { Units.distText(a, it.toFloat()) } ?: "— ${Units.distUnit(a)}"
        outside.text = s.outsideTemp?.let { "${Math.round(it)}°C" } ?: "—°"
    }

    private fun renderConsumption(value: Float?) {
        val shown = value?.takeIf { it in 3f..60f }?.let { Units.consText(a, it) + " " + Units.consUnit(a) }
            ?: "— " + Units.consUnit(a)
        consumption.text = shown
        tripConsumption.text = shown
    }

    private fun renderClimate(s: VehicleState) {
        val known = s.connected && s.hvacOn != null
        temp.text = s.driverTemp?.let(VehicleState::tempLabel) ?: "—"
        fan.text = if (s.autoOn == true) "AUTO" else s.fanSpeed?.toString() ?: "—"
        ac.setTextColor(if (s.acOn == true) red else white)
        auto.setTextColor(if (s.autoOn == true) red else white)
        defrost.setColorFilter(if (s.defrostFront == true) red else muted)
        recirc.setColorFilter(if (s.recirc == 0) red else muted)
        listOf<View>(ac, auto, defrost, recirc).forEach { it.isEnabled = known }
    }

    private fun renderMedia(m: MediaHub.NowPlaying) {
        mediaSource.text = (m.source.ifBlank { a.getString(R.string.title_media) }) + "  ▾"
        mediaTitle.text = if (m.available) m.title.ifBlank { m.source } else a.getString(R.string.no_media)
        mediaArtist.text = listOf(m.artist, m.album, m.radioText).filter { it.isNotBlank() }.joinToString(" · ")
        mediaArt.setImageBitmap(m.art)
        mediaPlay.setImageResource(if (m.playing) R.drawable.ic_pause else R.drawable.ic_play)
        mediaPlay.isEnabled = m.available
        mediaProgress.progress = if (m.durationMs > 0L) (m.positionMs * 1000L / m.durationMs).toInt().coerceIn(0, 1000) else 0
    }
}
