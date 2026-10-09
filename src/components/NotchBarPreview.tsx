import React, { useState, useEffect } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { AppIconBadge } from './AppIconBadge';
import { ScrollyCharacter } from './ScrollyCharacter';

export const NotchBarPreview: React.FC = () => {
  const {
    todayStats,
    appStats,
    dailyTotal,
    isNotchPreviewVisible,
    notchConfig,
    selectedAppForSim,
  } = useScrolly();

  const [isExpanded, setIsExpanded] = useState(false);
  const [isBouncing, setIsBouncing] = useState(false);

  const dailyScrolls = dailyTotal;
  const dailyGoal = todayStats.goal;
  const currentApp = appStats.find((a) => a.packageName === selectedAppForSim) || appStats[0];

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
  const progressFraction = Math.min(1, dailyScrolls / Math.max(1, dailyGoal));

  const statusLabel = isOverLimit ? 'LIMIT' : isNearLimit ? 'WARNING' : 'HEALTHY';
  const statusStyle = isOverLimit
    ? 'bg-[#FFDAD6] dark:bg-[#93000A]/20 text-[#410002] dark:text-[#FFB4AB] border-[#FFB4AB]/50 dark:border-[#FFB4AB]/20'
    : isNearLimit
    ? 'bg-[#FFDE9E] dark:bg-[#5C4100]/30 text-[#281800] dark:text-[#FFDDB2] border-[#FFB957]/40 dark:border-[#FFB957]/20'
    : 'bg-[#D1F0D6] dark:bg-[#1B6B40]/20 text-[#0B3722] dark:text-[#7FCB8B] border-[#A8DAB5]/50 dark:border-[#7FCB8B]/20';

  return (
    <>
      {/* Backdrop */}
      {isExpanded && (
        <div
          className="fixed inset-0 z-40 bg-[#1D1B20]/5 dark:bg-black/20 backdrop-blur-[1px]"
          onClick={() => setIsExpanded(false)}
        />
      )}

      <div
        className="fixed left-0 right-0 z-50 flex justify-center pointer-events-none"
        style={{
          top: `${Math.max(8, notchConfig.offsetY)}px`,
          transform: `translateX(${notchConfig.offsetX}px)`,
        }}
      >
        <div
          className={`pointer-events-auto transition-all duration-500 ease-[cubic-bezier(0.32,0.72,0,1)] ${
            isBouncing ? 'scale-[1.02]' : 'scale-100'
          } ${isExpanded ? 'w-[340px] max-w-[92vw]' : 'w-auto'}`}
        >
          {!isExpanded ? (
            // COMPACT PILL - Dynamic Island
            <div
              onClick={() => setIsExpanded(true)}
              className="relative flex items-center justify-between h-[36px] min-w-[195px] px-3.5 rounded-full cursor-pointer
                         bg-black/90 dark:bg-black/90 backdrop-blur-2xl border border-white/[0.08]
                         shadow-[0_8px_32px_rgba(0,0,0,0.35),inset_0_1px_0_0_rgba(255,255,255,0.12)]
                         hover:bg-black active:scale-[0.98] transition-all"
            >
              <div className="absolute inset-0 rounded-full bg-gradient-to-b from-white/[0.07] to-transparent pointer-events-none" />
              <div className="relative flex items-center gap-2">
                <AppIconBadge packageName={currentApp.packageName} size={20} />
                <span className="text-[11px] font-semibold text-white/90 truncate max-w-[70px]">
                  {currentApp.appName}
                </span>
              </div>
              <div className="relative flex items-center gap-2 pl-2">
                <span className="text-[13px] font-bold text-white tabular-nums">{dailyScrolls}</span>
                <div className={`w-2 h-2 rounded-full ${isOverLimit ? 'bg-red-500' : isNearLimit ? 'bg-amber-400' : 'bg-emerald-400'} animate-pulse shadow-[0_0_6px_currentColor]`} />
              </div>
            </div>
          ) : (
            // EXPANDED - Minimal Glass Tile (ONLY daily summary) - supports dark mode
            <div
              className="relative overflow-hidden rounded-[20px] 
                         bg-white/70 dark:bg-[#2B2930]/80 backdrop-blur-[20px]
                         border border-white/70 dark:border-[#49454F]/50
                         shadow-[0_16px_40px_-12px_rgba(103,80,164,0.2),0_0_0_1px_rgba(255,255,255,0.8)_inset,0_1px_2px_rgba(255,255,255,0.9)_inset]
                         dark:shadow-[0_16px_40px_-12px_rgba(0,0,0,0.4),0_0_0_1px_rgba(255,255,255,0.1)_inset]
                         animate-[glassIn_0.35s_cubic-bezier(0.32,0.72,0,1)]"
              onClick={(e) => e.stopPropagation()}
            >
              {/* Glass highlights */}
              <div className="absolute inset-0 pointer-events-none">
                <div className="absolute inset-0 bg-gradient-to-b from-white/60 dark:from-white/10 via-white/10 dark:via-white/[0.03] to-white/20 dark:to-white/[0.05]" />
                <div className="absolute -top-10 -right-10 w-32 h-32 rounded-full bg-[#D0BCFF]/25 dark:bg-[#4F378B]/30 blur-2xl" />
                <div className="absolute -bottom-8 -left-8 w-24 h-24 rounded-full bg-[#E8DEF8]/40 dark:bg-[#36343B]/50 blur-2xl" />
              </div>

              <div className="relative p-3.5">
                {/* Top row */}
                <div className="flex items-start justify-between gap-3">
                  {/* Left: Mascot + Count */}
                  <div className="flex items-start gap-2.5">
                    {/* Mascot glass tile */}
                    <div className="relative w-[52px] h-[52px] rounded-[16px] overflow-hidden
                                    bg-gradient-to-br from-[#E8DEF8]/90 to-[#F3EDF7]/80 dark:from-[#4F378B]/50 dark:to-[#36343B]/80
                                    backdrop-blur-xl border border-white/70 dark:border-[#49454F]/50
                                    shadow-[0_4px_12px_rgba(103,80,164,0.08),inset_0_1px_0_0_rgba(255,255,255,0.9)]
                                    dark:shadow-[0_4px_12px_rgba(0,0,0,0.2),inset_0_1px_0_0_rgba(255,255,255,0.1)]
                                    flex items-center justify-center shrink-0">
                      <div className="absolute inset-0 bg-gradient-to-br from-white/50 dark:from-white/10 to-transparent pointer-events-none" />
                      <ScrollyCharacter scrollCount={dailyScrolls} size={44} animated={false} />
                    </div>

                    <div className="pt-0.5">
                      {/* TODAY - DAILY ONLY */}
                      <div className="flex items-center gap-1 text-[10px] font-bold tracking-[0.12em] text-[#6750A4] dark:text-[#D0BCFF] uppercase">
                        <span className="text-[12px]">✦</span> TODAY - DAILY ONLY
                      </div>
                      {/* 0 / 100 */}
                      <div className="flex items-baseline gap-1 mt-1">
                        <span className="text-[26px] font-black leading-none tracking-tighter text-[#1D1B20] dark:text-[#E6E0E9] tabular-nums">
                          {dailyScrolls}
                        </span>
                        <span className="text-[14px] font-medium text-[#49454F] dark:text-[#CAC4D0]">/ {dailyGoal}</span>
                      </div>
                      {/* Resets midnight */}
                      <div className="flex items-center gap-1 mt-1 text-[10px] font-medium text-[#79747E] dark:text-[#938F99]">
                        <span className="w-3 h-3 rounded-full border border-[#79747E]/40 dark:border-[#938F99]/40 flex items-center justify-center text-[8px]">◷</span>
                        Resets midnight • Not lifetime
                      </div>
                    </div>
                  </div>

                  {/* Right: HEALTHY + left today */}
                  <div className="flex flex-col items-end gap-1.5 shrink-0">
                    <div className={`px-2.5 py-1 rounded-full text-[10px] font-bold tracking-wider border backdrop-blur-xl shadow-sm ${statusStyle}`}>
                      {statusLabel}
                    </div>
                    <div className="text-right leading-tight">
                      <div className="text-[14px] font-black text-[#1D1B20] dark:text-[#E6E0E9] tabular-nums">{Math.max(0, dailyGoal - dailyScrolls)}</div>
                      <div className="text-[10px] font-medium text-[#49454F] dark:text-[#CAC4D0] -mt-0.5">left today</div>
                    </div>
                  </div>
                </div>

                {/* Progress bar - glass track */}
                <div className="mt-3.5">
                  <div className="h-[8px] w-full rounded-full 
                                  bg-[#E8DEF8]/70 dark:bg-[#49454F]/50 backdrop-blur-xl 
                                  border border-white/60 dark:border-[#49454F]/30
                                  shadow-[inset_0_1px_2px_rgba(0,0,0,0.04)]
                                  p-[2px] overflow-hidden">
                    <div
                      className="h-full rounded-full bg-gradient-to-r from-[#D0BCFF] to-[#B69DF8] dark:from-[#D0BCFF] dark:to-[#7D5260]
                                 shadow-[0_0_8px_rgba(103,80,164,0.3)] transition-all duration-700 ease-out"
                      style={{ width: `${progressFraction * 100}%` }}
                    />
                  </div>
                </div>

                {/* Footer */}
                <div className="mt-2.5 flex items-center justify-between text-[10px] font-medium">
                  <span className="text-[#49454F] dark:text-[#CAC4D0]">Daily: {dailyScrolls} scrolls</span>
                  <span className="text-[#6750A4] dark:text-[#D0BCFF] font-semibold tracking-wide">{todayStats.date}</span>
                </div>
              </div>

              {/* Bottom edge highlight */}
              <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[70%] h-px bg-gradient-to-r from-transparent via-white/80 dark:via-white/20 to-transparent" />
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes glassIn {
          0% { transform: scale(0.88) translateY(-6px); opacity: 0; filter: blur(8px); }
          100% { transform: scale(1) translateY(0); opacity: 1; filter: blur(0); }
        }
      `}</style>
    </>
  );
};
