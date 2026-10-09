import React, { useState, useEffect } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { AppIconBadge } from './AppIconBadge';
import { ScrollyCharacter } from './ScrollyCharacter';
import { ChevronUp, X, Sparkles } from 'lucide-react';

export const NotchBarPreview: React.FC = () => {
  const {
    todayStats,
    appStats,
    dailyTotal,
    isNotchPreviewVisible,
    notchConfig,
    setNotchBarPreviewVisible,
    selectedAppForSim,
  } = useScrolly();

  const [isExpanded, setIsExpanded] = useState(false);
  const [isBouncing, setIsBouncing] = useState(false);

  // DAILY SCROLL COUNT - This bar tracks ONLY today's scrolls
  // Never shows lifetime, weekly, or monthly totals
  // Source: dailyTotal = sum of appStats (per-app daily counts)
  const dailyScrolls = dailyTotal; // Explicit daily only
  const dailyGoal = todayStats.goal;
  const currentApp = appStats.find((a) => a.packageName === selectedAppForSim) || appStats[0];

  // Bounce animation when daily count increases
  useEffect(() => {
    if (dailyScrolls > 0) {
      setIsBouncing(true);
      const timer = setTimeout(() => setIsBouncing(false), 400);
      return () => clearTimeout(timer);
    }
  }, [dailyScrolls]);

  if (!isNotchPreviewVisible) return null;

  const isOverLimit = dailyScrolls >= dailyGoal;
  const isNearLimit = dailyScrolls >= dailyGoal * 0.75;
  const statusColor = isOverLimit ? '#B3261E' : isNearLimit ? '#FFA000' : '#1B6B40';
  const progressFraction = Math.min(1, dailyScrolls / Math.max(1, dailyGoal));

  return (
    <div
      className="fixed left-0 right-0 pointer-events-none z-50 flex justify-center transition-all duration-300"
      style={{
        top: `${Math.max(6, notchConfig.offsetY)}px`,
        transform: `translateX(${notchConfig.offsetX}px)`,
      }}
    >
      <div
        className={`pointer-events-auto bg-black text-white rounded-full shadow-2xl transition-all duration-300 border border-white/10 ${
          isBouncing ? 'scale-105' : 'scale-100'
        } ${
          isExpanded
            ? 'w-[320px] rounded-[28px] p-4 bg-zinc-950 border-white/20'
            : 'h-[36px] min-w-[190px] px-3.5 flex items-center justify-between cursor-pointer hover:bg-zinc-900'
        }`}
        onClick={() => {
          if (!isExpanded) setIsExpanded(true);
        }}
      >
        {!isExpanded ? (
          // Compact Dynamic Island Pill - DAILY COUNT ONLY
          <div className="flex items-center justify-between w-full select-none">
            {/* Left wing: App Icon & Name */}
            <div className="flex items-center space-x-2">
              <AppIconBadge packageName={currentApp.packageName} size={20} />
              <span className="text-[11px] font-semibold tracking-wide text-zinc-300 truncate max-w-[70px]">
                {currentApp.appName}
              </span>
            </div>

            {notchConfig.cutoutGapWidth > 0 && (
              <div
                style={{ width: `${notchConfig.cutoutGapWidth}px` }}
                className="h-3 bg-black/60 rounded-full mx-1 flex items-center justify-center"
              >
                <div className="w-2.5 h-2.5 rounded-full bg-zinc-900 border border-zinc-800" />
              </div>
            )}

            {/* Right wing: DAILY Scroll Count - TODAY ONLY */}
            <div className="flex items-center space-x-2 pl-2">
              <span className="text-[13px] font-mono font-bold tracking-tight text-white" title={`Today's scrolls: ${dailyScrolls}`}>
                {dailyScrolls}
              </span>
              <div
                className="w-2.5 h-2.5 rounded-full animate-pulse"
                style={{ backgroundColor: statusColor }}
              />
            </div>
          </div>
        ) : (
          // Expanded Dynamic Island Card - DAILY BREAKDOWN
          <div className="flex flex-col w-full select-none space-y-3">
            <div className="flex items-center justify-between border-b border-zinc-800/80 pb-2">
              <div className="flex items-center space-x-2">
                <AppIconBadge packageName={currentApp.packageName} size={24} />
                <div>
                  <div className="text-[12px] font-bold text-white flex items-center space-x-1">
                    <span>{currentApp.appName}</span>
                    <span className="text-[10px] text-zinc-400 font-normal">Today</span>
                  </div>
                  <div className="text-[10px] text-zinc-400">
                    {currentApp.scrollCount} scrolls today • Daily only
                  </div>
                </div>
              </div>

              <div className="flex items-center space-x-1">
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setIsExpanded(false);
                  }}
                  className="p-1 rounded-full hover:bg-zinc-800 text-zinc-400 hover:text-white transition-colors"
                  title="Collapse"
                >
                  <ChevronUp className="w-4 h-4" />
                </button>
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setNotchBarPreviewVisible(false);
                  }}
                  className="p-1 rounded-full hover:bg-zinc-800 text-zinc-400 hover:text-white transition-colors"
                  title="Hide overlay"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            </div>

            {/* Middle Section: Mascot & DAILY Count */}
            <div className="flex items-center justify-between bg-zinc-900/60 p-2.5 rounded-2xl border border-zinc-800/60">
              <div className="flex items-center space-x-3">
                <ScrollyCharacter scrollCount={dailyScrolls} size={48} animated={false} />
                <div>
                  <div className="text-[10px] font-medium text-zinc-400 uppercase tracking-wider">
                    Today • Daily Only
                  </div>
                  <div className="text-2xl font-black text-white leading-none">
                    {dailyScrolls}
                    <span className="text-xs font-normal text-zinc-400 ml-1">
                      / {dailyGoal}
                    </span>
                  </div>
                  <div className="text-[9px] text-zinc-500 mt-0.5">Resets at midnight • Not lifetime</div>
                </div>
              </div>

              <div className="text-right">
                <div
                  className="inline-block text-[10px] font-bold px-2 py-0.5 rounded-full"
                  style={{
                    backgroundColor: `${statusColor}22`,
                    color: statusColor,
                    border: `1px solid ${statusColor}44`,
                  }}
                >
                  {isOverLimit ? 'LIMIT REACHED' : isNearLimit ? 'WARNING' : 'HEALTHY'}
                </div>
                <div className="text-[10px] text-zinc-400 mt-1">
                  {Math.max(0, dailyGoal - dailyScrolls)} left today
                </div>
              </div>
            </div>

            {/* Progress bar - daily progress */}
            <div>
              <div className="w-full h-2 bg-zinc-800 rounded-full overflow-hidden">
                <div
                  className="h-full rounded-full transition-all duration-300"
                  style={{
                    width: `${progressFraction * 100}%`,
                    backgroundColor: statusColor,
                  }}
                />
              </div>
              <div className="flex justify-between text-[9px] text-zinc-500 mt-1">
                <span>Daily: {dailyScrolls} scrolls</span>
                <span>{todayStats.date}</span>
              </div>
            </div>

            <div className="flex justify-between items-center text-[10px] text-zinc-500 pt-1">
              <span className="flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-purple-400" />
                Daily Counter • Resets midnight
              </span>
              <button
                onClick={(e) => {
                  e.stopPropagation();
                  setIsExpanded(false);
                }}
                className="text-purple-400 hover:underline font-semibold"
              >
                Tap to collapse
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
