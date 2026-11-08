package app.mg4dash.ui

/** Stable home layout selected only from the vehicle's own speed. */
class TeslaMotionMode {
    enum class Layout { PARKED, DRIVING }

    var layout = Layout.PARKED
        private set
    private var candidate: Layout? = null
    private var candidateSince = 0L

    fun update(speedKmh: Float?, nowMs: Long): Layout {
        val wanted = when {
            speedKmh == null || !speedKmh.isFinite() || speedKmh < 0f || speedKmh > 300f -> null
            speedKmh >= 5f -> Layout.DRIVING
            speedKmh <= 1f -> Layout.PARKED
            else -> null
        }
        if (wanted == null || wanted == layout) {
            candidate = null
            return layout
        }
        if (candidate != wanted) {
            candidate = wanted
            candidateSince = nowMs
            return layout
        }
        val delay = if (wanted == Layout.DRIVING) 1_500L else 3_000L
        if (nowMs - candidateSince >= delay) {
            layout = wanted
            candidate = null
        }
        return layout
    }
}
