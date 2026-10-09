import React, { useState, useEffect } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { AppIconBadge } from './AppIconBadge';
import { ScrollyCharacter } from './ScrollyCharacter';
import { X, Minus, Sparkles, Clock, Flame } from 'lucide-react';

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

  const statusConfig = isOverLimit
    ? { label: 'LIMIT REACHED', bg: 'bg-[#B3261E]/10', text: 'text-[#B3261E]', border: 'border-[#B3261E]/20', dot: 'bg-[#B3261E]' }
    : isNearLimit
    ? { label: 'HIGH USAGE', bg: 'bg-amber-500/10', text: 'text-amber-700', border: 'border-amber-500/20', dot: 'bg-amber-500' }
    : { label: 'HEALTHY', bg: 'bg-[#1B6B40]/10', text: 'text-[#1B6B40]', border: 'border-[#1B6B40]/15', dot: 'bg-[#1B6B40]' };

  return (
    <>
      {/* Backdrop when expanded - soft blur */}
      {isExpanded && (
        <div
          className="fixed inset-0 z-40 bg-[#1D1B20]/10 backdrop-blur-[2px] transition-all"
          onClick={() => setIsExpanded(false)}
        />
      )}

      <div
        className="fixed left-0 right-0 z-50 flex justify-center pointer-events-none transition-all duration-300"
        style={{
          top: `${Math.max(8, notchConfig.offsetY)}px`,
          transform: `translateX(${notchConfig.offsetX}px)`,
        }}
      >
        {/* Compact Pill - Dynamic Island */}
        <div
          className={`pointer-events-auto transition-all duration-500 ease-[cubic-bezier(0.32,0.72,0,1)] ${
            isBouncing ? 'scale-[1.03]' : 'scale-100'
          } ${isExpanded ? 'w-[380px] max-w-[92vw]' : 'w-auto'}`}
        >
          {!isExpanded ? (
            <div
              onClick={() => setIsExpanded(true)}
              className="group relative flex items-center justify-between h-[38px] min-w-[200px] px-4 rounded-full cursor-pointer
                         bg-black/90 backdrop-blur-2xl border border-white/[0.08] 
                         shadow-[0_8px_32px_rgba(0,0,0,0.4),inset_0_1px_0_0_rgba(255,255,255,0.12)]
                         hover:bg-black hover:shadow-[0_12px_40px_rgba(0,0,0,0.5)] 
                         active:scale-[0.98] transition-all duration-300"
            >
              {/* Subtle inner glow */}
              <div className="absolute inset-0 rounded-full bg-gradient-to-b from-white/[0.08] to-transparent pointer-events-none" />

              {/* Left: App */}
              <div className="relative flex items-center gap-2.5">
                <div className="relative">
                  <AppIconBadge packageName={currentApp.packageName} size={22} />
                  <div className="absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-black bg-emerald-500 animate-pulse" />
                </div>
                <span className="text-[12px] font-semibold tracking-wide text-white/90 max-w-[80px] truncate">
                  {currentApp.appName}
                </span>
              </div>

              {notchConfig.cutoutGapWidth > 0 && (
                <div
                  style={{ width: `${notchConfig.cutoutGapWidth}px` }}
                  className="h-3 bg-black rounded-full mx-2 flex items-center justify-center opacity-60"
                >
                  <div className="w-2 h-2 rounded-full bg-zinc-800" />
                </div>
              )}

              {/* Right: Daily Count */}
              <div className="relative flex items-center gap-2.5 pl-2">
                <div className="flex items-baseline gap-1">
                  <span className="text-[14px] font-bold tracking-tight text-white tabular-nums">{dailyScrolls}</span>
                  <span className="text-[10px] text-white/40 font-medium">today</span>
                </div>
                <div className={`w-2.5 h-2.5 rounded-full ${statusConfig.dot} shadow-[0_0_8px_currentColor] animate-pulse`} />
              </div>
            </div>
          ) : (
            /* EXPANDED - Modern Glass Window */
            <div className="pointer-events-auto relative overflow-hidden rounded-[32px] 
                            bg-white/[0.82] backdrop-blur-[24px] 
                            border border-white/60 
                            shadow-[0_24px_64px_-12px_rgba(103,80,164,0.25),0_0_0_1px_rgba(255,255,255,0.7)_inset,0_1px_0_0_rgba(255,255,255,0.9)_inset]
                            animate-[scaleIn_0.4s_cubic-bezier(0.32,0.72,0,1)]">
              {/* Subtle purple gradient mesh background */}
              <div className="absolute inset-0 pointer-events-none overflow-hidden">
                <div className="absolute -top-24 -right-24 w-72 h-72 rounded-full bg-gradient-to-br from-[#D0BCFF]/40 to-[#E8DEF8]/30 blur-3xl" />
                <div className="absolute -bottom-20 -left-20 w-64 h-64 rounded-full bg-gradient-to-tr from-[#6750A4]/15 to-[#D0BCFF]/20 blur-3xl" />
                <div className="absolute inset-0 bg-gradient-to-b from-white/60 via-transparent to-white/20" />
              </div>

              {/* Content */}
              <div className="relative z-10 p-5 pb-4">
                {/* Drag handle */}
                <div className="flex justify-center mb-4">
                  <div className="w-10 h-1 rounded-full bg-[#1D1B20]/15" />
                </div>

                {/* Header */}
                <div className="flex items-start justify-between mb-5">
                  <div className="flex items-center gap-3">
                    <div className="relative">
                      <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-[#6750A4] to-[#21005D] blur-[6px] opacity-30" />
                      <div className="relative rounded-2xl overflow-hidden shadow-sm">
                        <AppIconBadge packageName={currentApp.packageName} size={40} />
                      </div>
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="text-[15px] font-bold tracking-tight text-[#1D1B20]">{currentApp.appName}</h3>
                        <span className="px-2 py-0.5 rounded-full bg-[#E8DEF8] border border-[#D0BCFF]/50 text-[10px] font-bold tracking-wider text-[#21005D] uppercase">
                          Today
                        </span>
                      </div>
                      <p className="text-[12px] font-medium text-[#49454F] mt-0.5 flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {currentApp.scrollCount} scrolls • Daily only
                      </p>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setIsExpanded(false);
                      }}
                      className="w-8 h-8 rounded-full bg-[#1D1B20]/5 hover:bg-[#1D1B20]/10 border border-[#1D1B20]/5 flex items-center justify-center text-[#1D1B20]/60 hover:text-[#1D1B20] transition-all active:scale-95"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        setNotchBarPreviewVisible(false);
                      }}
                      className="w-8 h-8 rounded-full bg-[#1D1B20]/5 hover:bg-[#B3261E]/10 border border-[#1D1B20]/5 hover:border-[#B3261E]/20 flex items-center justify-center text-[#1D1B20]/60 hover:text-[#B3261E] transition-all active:scale-95"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                </div>

                {/* Hero Card - Glass */}
                <div className="relative overflow-hidden rounded-[24px] bg-gradient-to-br from-white/90 to-[#F3EDF7]/80 backdrop-blur-xl border border-white/70 shadow-[0_8px_24px_rgba(103,80,164,0.08),inset_0_1px_0_0_rgba(255,255,255,0.9)] p-4 mb-4">
                  <div className="absolute top-0 right-0 w-32 h-32 rounded-full bg-gradient-to-br from-[#D0BCFF]/20 to-transparent blur-2xl pointer-events-none" />
                  
                  <div className="relative flex items-center justify-between">
                    <div className="flex items-center gap-4">
                      <div className="relative">
                        <div className="absolute inset-0 rounded-[20px] bg-gradient-to-br from-[#6750A4]/20 to-[#D0BCFF]/30 blur-xl" />
                        <div className="relative w-[64px] h-[64px] rounded-[20px] bg-gradient-to-br from-[#E8DEF8] to-white border border-white/80 shadow-sm flex items-center justify-center overflow-hidden">
                          <ScrollyCharacter scrollCount={dailyScrolls} size={56} animated={false} />
                        </div>
                      </div>
                      <div>
                        <div className="flex items-center gap-1.5 mb-1">
                          <Sparkles className="w-3 h-3 text-[#6750A4]" />
                          <span className="text-[10px] font-bold tracking-[0.14em] text-[#6750A4] uppercase">Today • Daily Only</span>
                        </div>
                        <div className="flex items-baseline gap-2">
                          <span className="text-[36px] font-black tracking-tighter leading-none text-[#1D1B20] tabular-nums">{dailyScrolls}</span>
                          <span className="text-[14px] font-semibold text-[#49454F]">/ {dailyGoal}</span>
                        </div>
                        <p className="text-[11px] font-medium text-[#79747E] mt-1 flex items-center gap-1">
                          <Clock className="w-3 h-3" /> Resets midnight • Not lifetime
                        </p>
                      </div>
                    </div>

                    <div className="flex flex-col items-end gap-2">
                      <div className={`px-3 py-1 rounded-full text-[10px] font-black tracking-wider border ${statusConfig.bg} ${statusConfig.text} ${statusConfig.border} shadow-sm`}>
                        {statusConfig.label}
                      </div>
                      <div className="text-right">
                        <div className="text-[12px] font-bold text-[#1D1B20]">{Math.max(0, dailyGoal - dailyScrolls)}</div>
                        <div className="text-[10px] font-medium text-[#79747E]">left today</div>
                      </div>
                    </div>
                  </div>

                  {/* Progress */}
                  <div className="mt-4">
                    <div className="h-2.5 w-full rounded-full bg-[#E8DEF8]/80 border border-[#D0BCFF]/30 p-1">
                      <div
                        className="h-full rounded-full bg-gradient-to-r from-[#6750A4] to-[#7D5260] shadow-[0_0_12px_rgba(103,80,164,0.4)] transition-all duration-700 ease-out"
                        style={{ width: `${progressFraction * 100}%` }}
                      />
                    </div>
                    <div className="flex justify-between mt-2 text-[10px] font-medium">
                      <span className="text-[#49454F]">Daily: {dailyScrolls} scrolls</span>
                      <span className="text-[#6750A4] font-semibold">{todayStats.date}</span>
                    </div>
                  </div>
                </div>

                {/* App Distribution - Modern Chips */}
                <div className="mb-4">
                  <div className="flex items-center justify-between mb-2.5 px-1">
                    <h4 className="text-[11px] font-bold tracking-wider text-[#1D1B20] uppercase">Active Today</h4>
                    <span className="text-[10px] font-medium text-[#79747E]">{appStats.length} apps</span>
                  </div>
                  <div className="grid grid-cols-5 gap-2">
                    {appStats.map((app) => {
                      const isSelected = app.packageName === selectedAppForSim;
                      const pct = dailyTotal > 0 ? (app.scrollCount / dailyTotal) * 100 : 0;
                      return (
                        <div
                          key={app.packageName}
                          className={`group relative flex flex-col items-center gap-1.5 p-2.5 rounded-2xl border transition-all ${
                            isSelected
                              ? 'bg-[#E8DEF8] border-[#6750A4]/30 shadow-[0_4px_12px_rgba(103,80,164,0.15)] scale-[1.02]'
                              : 'bg-white/60 border-white/60 hover:bg-white/80 hover:border-[#D0BCFF]/40 hover:shadow-sm'
                          }`}
                        >
                          {isSelected && (
                            <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-[#6750A4]/5 to-transparent pointer-events-none" />
                          )}
                          <AppIconBadge packageName={app.packageName} size={28} />
                          <span className={`text-[9px] font-bold truncate max-w-[48px] ${isSelected ? 'text-[#21005D]' : 'text-[#1D1B20]'}`}>
                            {app.appName}
                          </span>
                          <div className="flex flex-col items-center">
                            <span className={`text-[12px] font-black tabular-nums ${isSelected ? 'text-[#6750A4]' : 'text-[#1D1B20]'}`}>
                              {app.scrollCount}
                            </span>
                            <span className="text-[8px] font-medium text-[#79747E]">{pct.toFixed(0)}%</span>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Footer */}
                <div className="flex items-center justify-between pt-3 border-t border-[#1D1B20]/5">
                  <div className="flex items-center gap-2 text-[11px] font-medium text-[#79747E]">
                    <div className="w-6 h-6 rounded-full bg-gradient-to-br from-[#E8DEF8] to-[#D0BCFF]/50 border border-white/60 flex items-center justify-center">
                      <Flame className="w-3 h-3 text-[#6750A4]" />
                    </div>
                    <span>Daily Counter • Resets midnight</span>
                  </div>
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      setIsExpanded(false);
                    }}
                    className="px-4 py-1.5 rounded-full bg-[#1D1B20] text-white text-[11px] font-bold tracking-wide hover:bg-black active:scale-95 transition-all shadow-[0_4px_12px_rgba(0,0,0,0.15)]"
                  >
                    Collapse
                  </button>
                </div>
              </div>

              {/* Bottom highlight */}
              <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-3/4 h-px bg-gradient-to-r from-transparent via-white/60 to-transparent pointer-events-none" />
            </div>
          )}
        </div>
      </div>

      <style>{`
        @keyframes scaleIn {
          0% { transform: scale(0.92) translateY(-8px); opacity: 0; }
          100% { transform: scale(1) translateY(0); opacity: 1; }
        }
      `}</style>
    </>
  );
};
