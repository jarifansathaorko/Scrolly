package com.example.ui.screens.stats

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ScrollyApp
import com.example.data.local.ScrollyDatabase
import com.example.data.local.dao.AppLifetimeTotal
import com.example.data.repository.TrackingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class StatsTimeframe(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month"),
    YEAR("Year")
}

data class ChartBarData(
    val label: String,
    val subLabel: String = "",
    val count: Int = 0,
    val isCurrent: Boolean = false
)

data class AppUsageItem(
    val packageName: String,
    val appName: String,
    val count: Int,
    val fractionOfTotal: Float
)

data class TimeframeStatsUiState(
    val timeframe: StatsTimeframe = StatsTimeframe.WEEK,
    val periodLabel: String = "",
    val canGoNext: Boolean = false,
    val chartBars: List<ChartBarData> = emptyList(),
    val chartCaption: String = "",
    val totalReels: Int = 0,
    val secondaryStat: Int = 0,
    val secondaryStatLabel: String = "Daily avg.",
    val appBreakdown: List<AppUsageItem> = emptyList(),
    val insightMessage: String = "0 scrolls logged. Great mindfulness!"
)

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModel(application: Application) : AndroidViewModel(application) {

    private val trackingRepo: TrackingRepository = ScrollyApp.instance.trackingRepository

    private val _selectedTimeframe = MutableStateFlow(StatsTimeframe.WEEK)
    val selectedTimeframe: StateFlow<StatsTimeframe> = _selectedTimeframe.asStateFlow()

    /** 0 = current period, -1 = previous, … */
    private val _periodOffset = MutableStateFlow(0)
    val periodOffset: StateFlow<Int> = _periodOffset.asStateFlow()

    fun selectTimeframe(timeframe: StatsTimeframe) {
        _selectedTimeframe.value = timeframe
        _periodOffset.value = 0 // Reset to current period when switching tab
    }

    fun navigatePrevious() {
        _periodOffset.value -= 1
    }

    /** Cannot move past the current period. */
    fun navigateNext() {
        if (_periodOffset.value < 0) _periodOffset.value += 1
    }

    fun resetToCurrent() {
        _periodOffset.value = 0
    }

    val uiState: StateFlow<TimeframeStatsUiState> = combine(
        _selectedTimeframe,
        _periodOffset
    ) { timeframe, offset -> timeframe to offset }
        .flatMapLatest { (timeframe, offset) ->
            when (timeframe) {
                StatsTimeframe.DAY -> dayState(offset)
                StatsTimeframe.WEEK -> weekState(offset)
                StatsTimeframe.MONTH -> monthState(offset)
                StatsTimeframe.YEAR -> yearState(offset)
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            TimeframeStatsUiState()
        )

    // ── Day ───────────────────────────────────────────────────────────────

    private fun dayState(offset: Int): Flow<TimeframeStatsUiState> = flow {
        val date = ScrollyDatabase.getDateOffset(offset)
        val isToday = offset == 0

        combine(
            trackingRepo.getDailyStatsFlow(date),
            trackingRepo.getAppStatsSummaryForDateFlow(date),
            trackingRepo.getScrollsByTimeOfDayFlow(date)
        ) { dailyStats, appSummaries, byPeriod ->
            val total = dailyStats?.totalScrolls ?: 0
            val goal = dailyStats?.goal ?: ScrollyDatabase.DEFAULT_DAILY_GOAL

            // Real per-period counts, bucketed from the raw event log. The previous
            // implementation applied fixed 15/35/40% ratios to the day's total, which
            // produced a Morning/Afternoon/Evening/Night split unrelated to when the
            // user actually scrolled.
            val bars = byPeriod.mapIndexed { index, entry ->
                ChartBarData(
                    label = entry.period.label,
                    subLabel = subLabelFor(index),
                    count = entry.count,
                    isCurrent = false
                )
            }

            TimeframeStatsUiState(
                timeframe = StatsTimeframe.DAY,
                periodLabel = if (isToday) "Today · ${displayDate(date)}" else displayDate(date),
                canGoNext = offset < 0,
                chartBars = bars,
                chartCaption = "Real activity by time of day",
                totalReels = total,
                secondaryStat = goal,
                secondaryStatLabel = "Daily goal",
                appBreakdown = buildAppBreakdown(appSummaries, total),
                insightMessage = buildInsight(total, goal, StatsTimeframe.DAY)
            )
        }.collect { emit(it) }
    }

    private fun subLabelFor(index: Int): String = when (index) {
        0 -> "6am–12"
        1 -> "12–5pm"
        2 -> "5–9pm"
        else -> "9pm–6am"
    }

    // ── Week ──────────────────────────────────────────────────────────────

    private fun weekState(offset: Int): Flow<TimeframeStatsUiState> = flow {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.WEEK_OF_YEAR, offset)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        }

        val dateFmt = dateFormat("yyyy-MM-dd")
        val dayFmt = dateFormat("EEE")
        val shortFmt = dateFormat("MMM d")

        val weekDates = (0..6).map { dayOffset ->
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_WEEK, dayOffset)
            Triple(dateFmt.format(dayCal.time), dayFmt.format(dayCal.time), dayCal.time)
        }

        val startDate = weekDates.first().first
        val endDate = weekDates.last().first

        combine(
            trackingRepo.getDailyStatsBetweenFlow(startDate, endDate),
            trackingRepo.getAppStatsSummaryBetweenFlow(startDate, endDate)
        ) { dailyList, appSummaries ->
                val statsMap = dailyList.associateBy { it.date }
                val todayStr = ScrollyDatabase.getTodayDate()

                val bars = weekDates.map { (dateStr, dayName, timeMillis) ->
                    ChartBarData(
                        label = dayName,
                        subLabel = dateFormat("d").format(timeMillis),
                        count = statsMap[dateStr]?.totalScrolls ?: 0,
                        isCurrent = dateStr == todayStr
                    )
                }

                val total = bars.sumOf { it.count }
                // Days that actually have a row, so a fresh install is not averaged
                // down by seven zeroes.
                val trackedDays = bars.count { it.count > 0 }.coerceAtLeast(1)

                TimeframeStatsUiState(
                    timeframe = StatsTimeframe.WEEK,
                    periodLabel = "${shortFmt.format(weekDates.first().third)} – ${shortFmt.format(weekDates.last().third)}",
                    canGoNext = offset < 0,
                    chartBars = bars,
                    chartCaption = "Scrolls per day",
                    totalReels = total,
                    secondaryStat = total / trackedDays,
                    secondaryStatLabel = "Avg / tracked day",
                    appBreakdown = buildAppBreakdown(appSummaries, total),
                    insightMessage = buildInsight(total, total * 2, StatsTimeframe.WEEK)
                )
        }.collect { emit(it) }
    }

    // ── Month ─────────────────────────────────────────────────────────────

    private fun monthState(offset: Int): Flow<TimeframeStatsUiState> = flow {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        val monthPrefix = dateFormat("yyyy-MM").format(cal.time)
        val periodLabel = dateFormat("MMMM yyyy").format(cal.time)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        combine(
            trackingRepo.getDailyStatsLikeFlow(monthPrefix),
            trackingRepo.getAppStatsSummaryLikeFlow(monthPrefix)
        ) { dailyList, appSummaries ->
                val statsMap = dailyList.associateBy { it.date }

                fun weekTotal(from: Int, to: Int): Int =
                    (from..to).sumOf { day ->
                        statsMap[monthPrefix + "-" + day.toString().padStart(2, '0')]
                            ?.totalScrolls ?: 0
                    }

                val bars = mutableListOf(
                    ChartBarData("W1", "1–7", weekTotal(1, 7)),
                    ChartBarData("W2", "8–14", weekTotal(8, 14)),
                    ChartBarData("W3", "15–21", weekTotal(15, 21)),
                    ChartBarData("W4", "22–28", weekTotal(22, 28))
                )
                if (daysInMonth > 28) {
                    bars += ChartBarData("W5", "29–$daysInMonth", weekTotal(29, daysInMonth))
                }

                val total = bars.sumOf { it.count }
                val trackedDays = statsMap.values.count { it.totalScrolls > 0 }.coerceAtLeast(1)

                TimeframeStatsUiState(
                    timeframe = StatsTimeframe.MONTH,
                    periodLabel = periodLabel,
                    canGoNext = offset < 0,
                    chartBars = bars,
                    chartCaption = "Scrolls per week",
                    totalReels = total,
                    secondaryStat = total / trackedDays,
                    secondaryStatLabel = "Avg / active day",
                    appBreakdown = buildAppBreakdown(appSummaries, total),
                    insightMessage = buildInsight(total, daysInMonth * 100, StatsTimeframe.MONTH)
                )
        }.collect { emit(it) }
    }

    // ── Year ──────────────────────────────────────────────────────────────

    private fun yearState(offset: Int): Flow<TimeframeStatsUiState> = flow {
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, offset) }
        val yearStr = dateFormat("yyyy").format(cal.time)

        combine(
            trackingRepo.getDailyStatsLikeFlow(yearStr),
            trackingRepo.getAppStatsSummaryLikeFlow(yearStr)
        ) { dailyList, appSummaries ->
                val monthNames = listOf(
                    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                )
                val bars = monthNames.mapIndexed { index, name ->
                    val prefix = yearStr + "-" + (index + 1).toString().padStart(2, '0')
                    ChartBarData(
                        label = name,
                        count = dailyList.filter { it.date.startsWith(prefix) }.sumOf { it.totalScrolls }
                    )
                }

                val total = bars.sumOf { it.count }
                val activeMonths = bars.count { it.count > 0 }.coerceAtLeast(1)

                TimeframeStatsUiState(
                    timeframe = StatsTimeframe.YEAR,
                    periodLabel = yearStr,
                    canGoNext = offset < 0,
                    chartBars = bars,
                    chartCaption = "Scrolls per month",
                    totalReels = total,
                    secondaryStat = total / activeMonths,
                    secondaryStatLabel = "Avg / active month",
                    appBreakdown = buildAppBreakdown(appSummaries, total),
                    insightMessage = buildInsight(total, 36_500, StatsTimeframe.YEAR)
                )
        }.collect { emit(it) }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    /**
     * App breakdown, sorted by contribution and including any app that actually has data.
     *
     * The previous version returned a fixed five-app list in a fixed order, so a user's
     * top app was never highlighted and an app they used heavily could be missing.
     */
    private fun buildAppBreakdown(summaries: List<AppLifetimeTotal>, total: Int): List<AppUsageItem> =
        summaries
            .filter { it.totalScrolls > 0 }
            .sortedByDescending { it.totalScrolls }
            .map { summary ->
                AppUsageItem(
                    packageName = summary.packageName,
                    appName = summary.appName,
                    count = summary.totalScrolls,
                    fractionOfTotal = if (total > 0) {
                        (summary.totalScrolls.toFloat() / total).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                )
            }

    private fun buildInsight(total: Int, goal: Int, timeframe: StatsTimeframe): String {
        val period = timeframe.label.lowercase()
        return when {
            total == 0 -> "No scrolls recorded for this $period. Clean, mindful focus."
            total <= goal -> "You stayed within your limit this $period ($total/$goal). Excellent control."
            else -> "$total scrolls this $period. Consider tightening an app limit to protect focus."
        }
    }

    private fun dateFormat(pattern: String) = SimpleDateFormat(pattern, Locale.getDefault())

    private fun displayDate(date: String): String =
        dateFormat("EEE, MMM d, yyyy").format(dateFormat("yyyy-MM-dd").parse(date)!!)
}