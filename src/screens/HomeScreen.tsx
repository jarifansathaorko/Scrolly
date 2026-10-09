import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { ScrollyCharacter, getMascotState } from '../components/ScrollyCharacter';
import { AppIconBadge } from '../components/AppIconBadge';
import { NotchDynamicIslandCustomizer } from '../components/NotchDynamicIslandCustomizer';
import {
  Flame,
  Zap,
  Eye,
  EyeOff,
  BatteryCharging,
  Layers,
  ShieldCheck,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';

export const HomeScreen: React.FC = () => {
  const {
    todayStats,
    appStats,
    dailyTotal,
    profile,
    selectedAppForSim,
    setSelectedAppForSim,
    simulateScroll,
    isNotchPreviewVisible,
    toggleNotchBarPreview,
    isServiceActive,
    isOverlayPermissionGranted,
    isBatteryOptimizationIgnored,
    openAccessibilitySettings,
    requestOverlayPermission,
    requestIgnoreBatteryOptimization,
    showBatteryDialog,
    showOverlayDialog,
    dismissBatteryDialog,
    dismissOverlayDialog,
  } = useScrolly();

  const [localBatteryDialogOpen, setLocalBatteryDialogOpen] = useState(false);
  const [localOverlayDialogOpen, setLocalOverlayDialogOpen] = useState(false);

  // DAILY SCROLL COUNT - Only today's scrolls, never lifetime/weekly/monthly
  // dailyTotal is sum of appStats (per-app daily counts), resets at midnight
  const totalScrolls = dailyTotal;
  const dailyLimit = todayStats.goal;
  const mascotState = getMascotState(totalScrolls);
  const progressFraction = Math.min(1, totalScrolls / Math.max(1, dailyLimit));
  const isNearLimit = totalScrolls >= dailyLimit * 0.8;
  const remainingScrolls = Math.max(0, dailyLimit - totalScrolls);

  return (
    <div className="flex flex-col space-y-4 pb-24 px-4 pt-2 max-w-md mx-auto">
      {/* Battery Optimization Dialog */}
      {(showBatteryDialog || localBatteryDialogOpen) && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#F3EDF7] rounded-[24px] p-6 max-w-sm w-full border border-[#CAC4D0] shadow-xl text-center space-y-4">
            <div className="w-14 h-14 rounded-full bg-[#1B6B40]/15 mx-auto flex items-center justify-center text-[#1B6B40]">
              <BatteryCharging className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-[#1D1B20]">Enable Background Usage</h3>
            <p className="text-sm text-[#49454F] text-left">
              Android aggressively pauses background services to save battery, which can cause the floating counter to not show when switching between apps.
            </p>
            <p className="text-xs text-[#1D1B20] font-medium bg-[#E8DEF8] p-3 rounded-xl text-left">
              ⚡ BrainRot's detection engine is built to draw less than 1% battery charge per day while staying active.
            </p>
            <div className="flex space-x-2 pt-2">
              <button
                onClick={() => {
                  requestIgnoreBatteryOptimization();
                  setLocalBatteryDialogOpen(false);
                }}
                className="flex-1 py-2.5 rounded-xl bg-[#6750A4] text-white font-bold text-sm shadow hover:bg-[#523d8c]"
              >
                Turn Off Optimization
              </button>
              <button
                onClick={() => {
                  dismissBatteryDialog();
                  setLocalBatteryDialogOpen(false);
                }}
                className="px-4 py-2.5 rounded-xl text-[#79747E] font-medium text-sm hover:text-[#1D1B20]"
              >
                Later
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Overlay Permission Dialog */}
      {(showOverlayDialog || localOverlayDialogOpen) && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#F3EDF7] rounded-[24px] p-6 max-w-sm w-full border border-[#CAC4D0] shadow-xl text-center space-y-4">
            <div className="w-14 h-14 rounded-full bg-[#6750A4]/15 mx-auto flex items-center justify-center text-[#6750A4]">
              <Layers className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-[#1D1B20]">Display Over Other Apps</h3>
            <p className="text-sm text-[#49454F]">
              To float the Dynamic Island counter above Facebook, Instagram, YouTube, and TikTok, Android requires the 'Display over other apps' permission.
            </p>
            <div className="flex space-x-2 pt-2">
              <button
                onClick={() => {
                  requestOverlayPermission();
                  setLocalOverlayDialogOpen(false);
                }}
                className="flex-1 py-2.5 rounded-xl bg-[#6750A4] text-white font-bold text-sm shadow hover:bg-[#523d8c]"
              >
                Grant Permission
              </button>
              <button
                onClick={() => {
                  dismissOverlayDialog();
                  setLocalOverlayDialogOpen(false);
                }}
                className="px-4 py-2.5 rounded-xl text-[#79747E] font-medium text-sm hover:text-[#1D1B20]"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}

      {/* --- TOP BAR --- */}
      <div className="flex items-center justify-between py-2">
        <div>
          <h1 className="text-2xl font-black tracking-tight text-[#1D1B20]">BrainRot</h1>
          <p className="text-xs text-[#79747E] font-medium">Take back your attention</p>
        </div>

        {/* Streak Pill */}
        <div className="flex items-center space-x-1.5 px-3.5 py-1.5 rounded-full bg-[#E8DEF8] border border-[#CAC4D0]">
          <span className="text-sm font-black text-[#21005D]">{profile.streakDays}</span>
          <Flame className="w-4 h-4 text-orange-600 fill-orange-500" />
          <span className="text-[10px] font-bold tracking-wider text-[#1D192B] uppercase">
            DAY STREAK
          </span>
        </div>
      </div>

      {/* --- HERO CARD - DAILY SCROLL COUNT ONLY --- */}
      <div className="bg-[#D0BCFF] rounded-[28px] border border-[#CAC4D0] p-6 shadow-sm flex flex-col items-center relative overflow-hidden">
        {/* Subtle background circle decoration */}
        <div className="absolute -top-12 -right-12 w-40 h-40 rounded-full bg-white/20 pointer-events-none" />

        <div className="flex items-center justify-between w-full mb-3 z-10">
          <span className="text-[11px] font-bold tracking-widest text-[#21005D] uppercase">
            TODAY'S SCROLLS • DAILY ONLY
          </span>
          <div className="flex items-center space-x-1 px-2.5 py-0.5 rounded-xl bg-[#EADDFF] border border-[#21005D]/20">
            <span className="w-1.5 h-1.5 rounded-full bg-[#21005D] animate-ping" />
            <span className="text-[10px] font-extrabold text-[#21005D]">LIVE DAILY</span>
          </div>
        </div>

        {/* Giant Daily Scroll Count - TODAY ONLY, not lifetime/weekly/monthly */}
        <div className="text-7xl font-light text-[#21005D] tracking-tighter my-1" title={`Daily scrolls today (${todayStats.date}): ${totalScrolls}`}>
          {totalScrolls}
        </div>
        <p className="text-xs font-semibold text-[#21005D]/80 mb-1">
          Shorts & Reels consumed today
        </p>
        <p className="text-[10px] font-medium text-[#21005D]/60 mb-4">
          {todayStats.date} • Resets at midnight • Not lifetime
        </p>

        {/* Animated Mascot */}
        <div className="my-1">
          <ScrollyCharacter scrollCount={totalScrolls} size={128} />
        </div>

        {/* Mascot Reaction Speech Card */}
        <div className="w-full bg-[#F3EDF7] rounded-2xl p-3 border border-[#CAC4D0] text-center my-3 shadow-inner">
          <div className="text-sm font-bold text-[#6750A4]">{mascotState.title}</div>
          <div className="text-xs text-[#1D1B20] font-medium italic mt-0.5">
            "{mascotState.quote}"
          </div>
        </div>

        {/* Progress Bar Toward Daily Limit */}
        <div className="w-full space-y-1.5 pt-1 z-10">
          <div className="flex justify-between items-center text-xs font-semibold text-[#21005D]">
            <span>{totalScrolls} / {dailyLimit} limit</span>
            <span className={isNearLimit ? 'text-[#B3261E] font-black' : 'text-[#21005D]'}>
              {remainingScrolls} left
            </span>
          </div>
          <div className="w-full h-2.5 bg-[#EADDFF] rounded-full overflow-hidden border border-[#21005D]/10">
            <div
              className={`h-full rounded-full transition-all duration-300 ${
                isNearLimit ? 'bg-[#B3261E]' : 'bg-[#6750A4]'
              }`}
              style={{ width: `${progressFraction * 100}%` }}
            />
          </div>
        </div>
      </div>

      {/* --- ACTIVE APPS DISTRIBUTION ROW --- */}
      <div>
        <h2 className="text-sm font-bold text-[#1D1B20] mb-2 px-1">Active Apps</h2>
        <div className="grid grid-cols-5 gap-1.5">
          {appStats.map((app) => {
            const isSelected = selectedAppForSim === app.packageName;
            return (
              <button
                key={app.packageName}
                onClick={() => setSelectedAppForSim(app.packageName)}
                className={`flex flex-col items-center py-2.5 px-1 rounded-2xl border transition-all ${
                  isSelected
                    ? 'bg-[#E8DEF8] border-[#6750A4] shadow-sm'
                    : 'bg-[#F3EDF7] border-[#CAC4D0] hover:bg-zinc-100'
                }`}
              >
                <AppIconBadge packageName={app.packageName} size={28} />
                <span
                  className={`text-[10px] font-bold mt-1.5 truncate max-w-[55px] ${
                    isSelected ? 'text-[#21005D]' : 'text-[#1D1B20]'
                  }`}
                >
                  {app.appName}
                </span>
                <span
                  className={`text-[11px] font-semibold ${
                    isSelected ? 'text-[#6750A4]' : 'text-[#49454F]'
                  }`}
                >
                  {app.scrollCount}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* --- HARDWARE NOTCH CUSTOMIZER --- */}
      <NotchDynamicIslandCustomizer />

      {/* Toggle Dynamic Island Visibility Button */}
      <button
        onClick={toggleNotchBarPreview}
        className={`w-full py-3 px-4 rounded-2xl font-bold text-xs flex items-center justify-center space-x-2 transition-all shadow-sm ${
          isNotchPreviewVisible
            ? 'bg-[#E8DEF8] text-[#1D192B] border border-[#CAC4D0]'
            : 'bg-[#6750A4] text-white hover:bg-[#523d8c]'
        }`}
      >
        {isNotchPreviewVisible ? (
          <>
            <EyeOff className="w-4 h-4 text-[#1D192B]" />
            <span>Hide Live Island Overlay</span>
          </>
        ) : (
          <>
            <Eye className="w-4 h-4 text-white" />
            <span>Show Live Island Overlay</span>
          </>
        )}
      </button>

      {/* --- LIVE SCROLL TESTER (Dev & Demo) --- */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-4.5 space-y-3">
        <div className="flex items-center space-x-2">
          <Zap className="w-4 h-4 text-[#6750A4]" />
          <h3 className="text-xs font-bold text-[#6750A4]">
            Live Scroll Tester (Dev & Simulation)
          </h3>
        </div>
        <p className="text-[11px] text-[#49454F]">
          Tap to simulate scroll events on selected app (
          <strong className="text-[#1D1B20]">
            {appStats.find((a) => a.packageName === selectedAppForSim)?.appName}
          </strong>
          ) and verify real-time counter & floating island:
        </p>

        <div className="flex space-x-2">
          <button
            onClick={() => simulateScroll(selectedAppForSim, 1)}
            className="flex-1 py-2.5 rounded-xl bg-[#6750A4] text-white font-bold text-xs shadow hover:bg-[#523d8c] transition-colors"
          >
            +1 Scroll
          </button>
          <button
            onClick={() => simulateScroll(selectedAppForSim, 5)}
            className="flex-1 py-2.5 rounded-xl bg-[#E8DEF8] text-[#1D192B] font-bold text-xs border border-[#CAC4D0] hover:bg-[#d8cbf0] transition-colors"
          >
            +5
          </button>
          <button
            onClick={() => simulateScroll(selectedAppForSim, 20)}
            className="flex-1 py-2.5 rounded-xl bg-[#E8DEF8] text-[#1D192B] font-bold text-xs border border-[#CAC4D0] hover:bg-[#d8cbf0] transition-colors"
          >
            +20
          </button>
        </div>
      </div>

      {/* --- SYSTEM SETUP & RELIABILITY CONTROL CENTER --- */}
      <div className="bg-[#F3EDF7] rounded-[24px] border border-[#CAC4D0] p-4 space-y-3.5">
        <h3 className="text-sm font-bold text-[#1D1B20]">System Setup & Reliability</h3>

        {/* 1. Accessibility Service */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div
              className={`w-9 h-9 rounded-full flex items-center justify-center ${
                isServiceActive ? 'bg-[#1B6B40]/15 text-[#1B6B40]' : 'bg-[#CAC4D0] text-[#79747E]'
              }`}
            >
              {isServiceActive ? <CheckCircle2 className="w-5 h-5" /> : <ShieldCheck className="w-5 h-5" />}
            </div>
            <div>
              <div className="text-xs font-bold text-[#1D1B20]">Accessibility Service</div>
              <div className="text-[10px] text-[#49454F]">
                {isServiceActive
                  ? 'Active • Counting Reels & Shorts'
                  : 'Required to detect swipes in Reels'}
              </div>
            </div>
          </div>

          {isServiceActive ? (
            <span className="text-[11px] font-bold text-[#1B6B40]">Active</span>
          ) : (
            <button
              onClick={openAccessibilitySettings}
              className="text-xs font-bold px-3 py-1 rounded-lg border border-[#6750A4] text-[#6750A4]"
            >
              Enable
            </button>
          )}
        </div>

        {/* 2. Display Over Other Apps */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div
              className={`w-9 h-9 rounded-full flex items-center justify-center ${
                isOverlayPermissionGranted
                  ? 'bg-[#1B6B40]/15 text-[#1B6B40]'
                  : 'bg-[#CAC4D0] text-[#79747E]'
              }`}
            >
              {isOverlayPermissionGranted ? <CheckCircle2 className="w-5 h-5" /> : <Layers className="w-5 h-5" />}
            </div>
            <div>
              <div className="text-xs font-bold text-[#1D1B20]">Display Over Other Apps</div>
              <div className="text-[10px] text-[#49454F]">
                {isOverlayPermissionGranted
                  ? 'Active • Dynamic Island overlay ready'
                  : 'Allows island counter above apps'}
              </div>
            </div>
          </div>

          {isOverlayPermissionGranted ? (
            <span className="text-[11px] font-bold text-[#1B6B40]">Active</span>
          ) : (
            <button
              onClick={() => setLocalOverlayDialogOpen(true)}
              className="text-xs font-bold px-3 py-1 rounded-lg border border-[#6750A4] text-[#6750A4]"
            >
              Enable
            </button>
          )}
        </div>

        {/* 3. Battery Optimization */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div
              className={`w-9 h-9 rounded-full flex items-center justify-center ${
                isBatteryOptimizationIgnored
                  ? 'bg-[#1B6B40]/15 text-[#1B6B40]'
                  : 'bg-amber-500/15 text-amber-700'
              }`}
            >
              {isBatteryOptimizationIgnored ? (
                <CheckCircle2 className="w-5 h-5" />
              ) : (
                <BatteryCharging className="w-5 h-5" />
              )}
            </div>
            <div>
              <div className="text-xs font-bold text-[#1D1B20]">Background Optimization</div>
              <div className="text-[10px] text-[#49454F]">
                {isBatteryOptimizationIgnored
                  ? 'Unrestricted background running'
                  : 'Recommended to keep counter active'}
              </div>
            </div>
          </div>

          {isBatteryOptimizationIgnored ? (
            <span className="text-[11px] font-bold text-[#1B6B40]">Optimized</span>
          ) : (
            <button
              onClick={() => setLocalBatteryDialogOpen(true)}
              className="text-xs font-bold px-3 py-1 rounded-lg border border-amber-600 text-amber-700 hover:bg-amber-50"
            >
              Configure
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
