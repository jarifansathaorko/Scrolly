import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { StatsTimeframe, ChartBarData, AppUsageItem } from '../types';
import { AppIconBadge } from '../components/AppIconBadge';
import { ScrollyCharacter } from '../components/ScrollyCharacter';
import { ChevronLeft, ChevronRight, TrendingDown, Sparkles } from 'lucide-react';

export const StatsScreen: React.FC = () => {
  const { todayStats, appStats, dailyTotal, historyDailyStats, historyAppStats } = useScrolly();
  const [timeframe, setTimeframe] = useState<StatsTimeframe>('WEEK');
  const [periodOffset, setPeriodOffset] = useState<number>(0);
  const [selectedBarIndex, setSelectedBarIndex] = useState<number | null>(null);

  // Timeframe calculation - Note: totalReels here is for analytics history, NOT for the floating bar
  // The floating bar (NotchBarPreview) uses dailyTotal ONLY - today's daily scrolls
  let periodLabel = '';
  let chartBars: ChartBarData[] = [];
  let totalReels = 0;
  let secondaryStat = 0;
  let secondaryStatLabel = 'Daily avg.';
  let insightMessage = '';

  if (timeframe === 'DAY') {
    periodLabel =
      periodOffset === 0
        ? 'Today'
        : periodOffset === -1
        ? 'Yesterday'
        : `${Math.abs(periodOffset)} days ago`;

    totalReels = periodOffset === 0 ? dailyTotal : 52;
    secondaryStat = Math.round(totalReels * 0.4);
    secondaryStatLabel = 'Peak evening';

    chartBars = [
      { label: 'Morning', subLabel: '6am-12', count: Math.round(totalReels * 0.15) },
      { label: 'Afternoon', subLabel: '12-5pm', count: Math.round(totalReels * 0.35) },
      { label: 'Evening', subLabel: '5-9pm', count: Math.round(totalReels * 0.4) },
      {
        label: 'Night',
        subLabel: '9pm-6am',
        count: Math.max(0, totalReels - Math.round(totalReels * 0.9)),
      },
    ];

    insightMessage =
      totalReels < 50
        ? 'Super mindful scrolling! Dopamine levels are healthy.'
        : 'Most scrolls occurred during evening wind-down. Try a screen-free book session!';
  } else if (timeframe === 'WEEK') {
    periodLabel = periodOffset === 0 ? 'This Week' : `${Math.abs(periodOffset)} weeks ago`;

    const dayLabels = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
    const mockCounts = [48, 62, 35, 95, 78, 54, dailyTotal];
    totalReels = mockCounts.reduce((a, b) => a + b, 0);
    secondaryStat = Math.round(totalReels / 7);
    secondaryStatLabel = 'Daily avg.';

    chartBars = dayLabels.map((lbl, idx) => ({
      label: lbl,
      subLabel: `${mockCounts[idx]}`,
      count: mockCounts[idx],
      isCurrent: idx === 6 && periodOffset === 0,
    }));

    insightMessage = 'Your lowest scrolling day was Wednesday. Keep building those mindful breaks!';
  } else if (timeframe === 'MONTH') {
    periodLabel = periodOffset === 0 ? 'This Month' : `${Math.abs(periodOffset)} months ago`;

    const weekLabels = ['Week 1', 'Week 2', 'Week 3', 'Week 4'];
    const weekCounts = [340, 290, 310, 260 + dailyTotal];
    totalReels = weekCounts.reduce((a, b) => a + b, 0);
    secondaryStat = Math.round(totalReels / 30);
    secondaryStatLabel = 'Daily avg.';

    chartBars = weekLabels.map((lbl, idx) => ({
      label: lbl,
      subLabel: `${weekCounts[idx]}`,
      count: weekCounts[idx],
      isCurrent: idx === 3 && periodOffset === 0,
    }));

    insightMessage = 'Down 14% compared to last month! Your attention endurance is improving.';
  } else {
    // YEAR
    periodLabel = periodOffset === 0 ? 'This Year' : `${Math.abs(periodOffset)} years ago`;

    const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct'];
    const monthCounts = [1200, 1150, 980, 1050, 920, 890, 840, 780, 750, 700 + dailyTotal];
    totalReels = monthCounts.reduce((a, b) => a + b, 0);
    secondaryStat = Math.round(totalReels / 10);
    secondaryStatLabel = 'Monthly avg.';

    chartBars = months.map((m, idx) => ({
      label: m,
      count: monthCounts[idx],
      isCurrent: idx === months.length - 1 && periodOffset === 0,
    }));

    insightMessage = 'Steady downward trend over the year. Outstanding digital discipline!';
  }

  const maxBarValue = Math.max(10, ...chartBars.map((b) => b.count));

  // App breakdown computation
  const appBreakdown: AppUsageItem[] = appStats.map((app) => ({
    packageName: app.packageName,
    appName: app.appName,
    count: app.scrollCount,
    fractionOfTotal:
      dailyTotal > 0
        ? app.scrollCount / dailyTotal
        : 0,
  }));

  return (
    <div className="flex flex-col space-y-4 pb-24 px-4 pt-2 max-w-md mx-auto">
      {/* Title */}
      <div className="text-center py-2">
        <h1 className="text-2xl font-bold text-[#1D1B20]">Progress & Analytics</h1>
        <p className="text-xs text-[#79747E]">Understand your consumption patterns</p>
      </div>

      {/* Timeframe Tabs */}
      <div className="flex bg-[#F3EDF7] rounded-[24px] p-1 border border-[#CAC4D0] justify-between">
        {(['DAY', 'WEEK', 'MONTH', 'YEAR'] as StatsTimeframe[]).map((tf) => {
          const isSelected = timeframe === tf;
          return (
            <button
              key={tf}
              onClick={() => {
                setTimeframe(tf);
                setPeriodOffset(0);
                setSelectedBarIndex(null);
              }}
              className={`flex-1 py-2 text-xs font-bold rounded-[20px] transition-all capitalize ${
                isSelected
                  ? 'bg-[#6750A4] text-white shadow-sm'
                  : 'text-[#49454F] hover:text-[#1D1B20]'
              }`}
            >
              {tf.toLowerCase()}
            </button>
          );
        })}
      </div>

      {/* Period Navigator */}
      <div className="flex items-center justify-between px-2">
        <button
          onClick={() => setPeriodOffset((p) => p - 1)}
          className="p-1.5 rounded-full hover:bg-[#E8DEF8] text-[#49454F] transition-colors"
        >
          <ChevronLeft className="w-5 h-5" />
        </button>

        <button
          onClick={() => setPeriodOffset(0)}
          className="px-4 py-1 rounded-full bg-[#E8DEF8] border border-[#CAC4D0] text-xs font-bold text-[#1D192B] hover:bg-[#d8cbf0]"
        >
          {periodLabel}
        </button>

        <button
          onClick={() => setPeriodOffset((p) => Math.min(0, p + 1))}
          disabled={periodOffset >= 0}
          className={`p-1.5 rounded-full transition-colors ${
            periodOffset >= 0
              ? 'opacity-30 cursor-not-allowed text-[#79747E]'
              : 'hover:bg-[#E8DEF8] text-[#49454F]'
          }`}
        >
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>

      {/* Hero Metric Card */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div>
            <span className="text-[11px] font-bold tracking-wider text-[#79747E] uppercase">
              Total Reels Consumed
            </span>
            <div className="text-4xl font-black text-[#1D1B20] mt-0.5">{totalReels}</div>
          </div>
          <ScrollyCharacter scrollCount={totalReels} size={54} animated={false} />
        </div>

        <div className="flex items-center justify-between pt-2 border-t border-[#CAC4D0]/60 text-xs">
          <div className="flex items-center space-x-1.5 text-[#1B6B40] font-bold">
            <TrendingDown className="w-4 h-4" />
            <span>-12% vs last {timeframe.toLowerCase()}</span>
          </div>
          <div className="text-[#49454F] font-semibold">
            {secondaryStatLabel}: <strong className="text-[#1D1B20]">{secondaryStat}</strong>
          </div>
        </div>
      </div>

      {/* Interactive Bar Chart */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <div className="flex justify-between items-center mb-1">
          <span className="text-xs font-bold text-[#1D1B20]">Consumption Trend</span>
          <span className="text-[11px] font-medium text-[#79747E]">Tap bar for details</span>
        </div>

        <div className="h-44 flex items-end justify-between space-x-2 pt-6 pb-2 px-1">
          {chartBars.map((bar, idx) => {
            const heightPercent = Math.max(10, Math.round((bar.count / maxBarValue) * 100));
            const isSelected = selectedBarIndex === idx;

            return (
              <div
                key={idx}
                onClick={() => setSelectedBarIndex(isSelected ? null : idx)}
                className="flex-1 flex flex-col items-center h-full justify-end cursor-pointer group"
              >
                {/* Count tooltip on hover/select */}
                <span
                  className={`text-[10px] font-mono font-bold mb-1 transition-opacity ${
                    isSelected ? 'text-[#6750A4] opacity-100 scale-110' : 'text-[#79747E] opacity-70 group-hover:opacity-100'
                  }`}
                >
                  {bar.count}
                </span>

                {/* Bar pill */}
                <div className="w-full max-w-[28px] bg-[#E8DEF8] rounded-t-xl overflow-hidden flex flex-col justify-end h-32">
                  <div
                    className={`w-full rounded-t-xl transition-all duration-300 ${
                      bar.isCurrent || isSelected
                        ? 'bg-[#6750A4]'
                        : 'bg-[#6750A4]/70 group-hover:bg-[#6750A4]'
                    }`}
                    style={{ height: `${heightPercent}%` }}
                  />
                </div>

                {/* Label */}
                <span
                  className={`text-[10px] mt-1.5 truncate max-w-full ${
                    bar.isCurrent || isSelected
                      ? 'font-bold text-[#1D1B20]'
                      : 'text-[#79747E]'
                  }`}
                >
                  {bar.label}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* App Breakdown */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <h3 className="text-xs font-bold text-[#1D1B20] uppercase tracking-wider">
          App Distribution
        </h3>

        <div className="space-y-3">
          {appBreakdown.map((item) => {
            const percentage = Math.round(item.fractionOfTotal * 100);

            return (
              <div key={item.packageName} className="space-y-1">
                <div className="flex items-center justify-between text-xs">
                  <div className="flex items-center space-x-2">
                    <AppIconBadge packageName={item.packageName} size={22} />
                    <span className="font-bold text-[#1D1B20]">{item.appName}</span>
                  </div>
                  <div className="flex items-center space-x-2 text-[#49454F]">
                    <span className="font-mono font-bold text-[#1D1B20]">{item.count}</span>
                    <span className="text-[11px] text-[#79747E]">({percentage}%)</span>
                  </div>
                </div>

                <div className="w-full h-2 bg-[#E8DEF8] rounded-full overflow-hidden">
                  <div
                    className="h-full bg-[#6750A4] rounded-full transition-all duration-300"
                    style={{ width: `${percentage}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Mindfulness Insight Card */}
      <div className="bg-[#EADDFF]/50 rounded-2xl border border-[#6750A4]/30 p-4 flex items-start space-x-3">
        <div className="p-2 rounded-xl bg-[#6750A4]/15 text-[#6750A4] flex-shrink-0">
          <Sparkles className="w-5 h-5" />
        </div>
        <div>
          <div className="text-xs font-bold text-[#21005D]">BrainRot Mindful Insight</div>
          <p className="text-xs text-[#49454F] mt-0.5 leading-relaxed">{insightMessage}</p>
        </div>
      </div>
    </div>
  );
};
