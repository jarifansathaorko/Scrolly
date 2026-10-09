import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { NotchType, IslandPlacementMode } from '../types';
import {
  Focus,
  ChevronDown,
  ChevronUp,
  Sparkles,
  RotateCcw,
  Eye,
  Sliders,
  Smartphone,
  ArrowUp,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  PlusCircle,
} from 'lucide-react';

export const NotchDynamicIslandCustomizer: React.FC = () => {
  const {
    notchConfig,
    updateNotchConfig,
    autoCalibrateNotch,
    resetNotchDefaults,
    simulateScroll,
    selectedAppForSim,
  } = useScrolly();

  const [isOpen, setIsOpen] = useState(false);

  const notchTypes: { type: NotchType; title: string; desc: string; emoji: string }[] = [
    {
      type: 'DYNAMIC_ISLAND',
      title: 'Dynamic Island (iPhone)',
      desc: 'Apple-style compact pill safely below cutout',
      emoji: '🏝️',
    },
    {
      type: 'ROUND_PUNCH_HOLE_CENTER',
      title: 'Punch-Hole (Center)',
      desc: 'Standard round top-center selfie camera',
      emoji: '⚪',
    },
    {
      type: 'WIDE_NOTCH',
      title: 'Wide Notch (Classic)',
      desc: 'Broad notch requiring wide center clearance',
      emoji: '📱',
    },
    {
      type: 'WATERDROP_TEARDROP',
      title: 'Waterdrop (Teardrop)',
      desc: 'U/V tear-shaped camera notch',
      emoji: '💧',
    },
    {
      type: 'PUNCH_HOLE_LEFT',
      title: 'Punch-Hole (Left)',
      desc: 'Top-left corner camera cutout',
      emoji: '↖️',
    },
    {
      type: 'PUNCH_HOLE_RIGHT',
      title: 'Punch-Hole (Right)',
      desc: 'Top-right corner camera cutout',
      emoji: '↗️',
    },
    {
      type: 'BELOW_STATUS_BAR',
      title: 'Below Status Bar',
      desc: 'Clean placement below system status bar',
      emoji: '⬇️',
    },
  ];

  return (
    <div className="w-full bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] overflow-hidden transition-all shadow-sm">
      {/* Header Accordion */}
      <div
        className="p-5 flex items-center justify-between cursor-pointer hover:bg-[#E8DEF8]/40 transition-colors"
        onClick={() => setIsOpen(!isOpen)}
      >
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-full bg-[#6750A4]/15 flex items-center justify-center text-[#6750A4]">
            <Focus className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-[16px] font-bold text-[#1D1B20]">Device Notch Finder</h3>
            <p className="text-[12px] text-[#49454F]">
              {notchConfig.autoAdjusted
                ? '✓ Automatically Adjusted Over Notch Area'
                : 'Calibrate floating pill for screen cutout'}
            </p>
          </div>
        </div>

        <div className="flex items-center space-x-2">
          {notchConfig.autoAdjusted && (
            <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full bg-[#1B6B40]/15 text-[#1B6B40] border border-[#1B6B40]/30">
              Calibrated
            </span>
          )}
          <button className="text-[#79747E] p-1">
            {isOpen ? <ChevronUp className="w-5 h-5" /> : <ChevronDown className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {isOpen && (
        <div className="px-5 pb-6 pt-1 space-y-5 border-t border-[#CAC4D0]/60">
          {/* Auto-Calibrate Action */}
          <div className="bg-[#EADDFF]/50 p-3.5 rounded-2xl flex items-center justify-between border border-[#6750A4]/20">
            <div className="flex items-center space-x-2.5">
              <Sparkles className="w-5 h-5 text-[#6750A4]" />
              <div>
                <div className="text-[13px] font-bold text-[#21005D]">Auto-Detect Hardware Cutout</div>
                <div className="text-[11px] text-[#49454F]">
                  Instantly aligns overlay over simulated camera lens
                </div>
              </div>
            </div>
            <button
              onClick={autoCalibrateNotch}
              className="px-3.5 py-1.5 rounded-xl bg-[#6750A4] text-white text-[12px] font-bold shadow-sm hover:bg-[#523d8c] transition-colors"
            >
              Auto-Align
            </button>
          </div>

          {/* Notch Type Selection */}
          <div>
            <label className="block text-[13px] font-bold text-[#1D1B20] mb-2">
              Hardware Cutout Profile
            </label>
            <div className="grid grid-cols-2 gap-2">
              {notchTypes.map((item) => {
                const isSelected = notchConfig.notchType === item.type;
                return (
                  <button
                    key={item.type}
                    onClick={() => {
                      updateNotchConfig({
                        notchType: item.type,
                        offsetX:
                          item.type === 'PUNCH_HOLE_LEFT'
                            ? -80
                            : item.type === 'PUNCH_HOLE_RIGHT'
                            ? 80
                            : 0,
                        offsetY: item.type === 'WIDE_NOTCH' ? 48 : 38,
                        cutoutGapWidth: item.type === 'WIDE_NOTCH' ? 60 : 0,
                      });
                    }}
                    className={`p-2.5 rounded-xl text-left border transition-all flex items-start space-x-2 ${
                      isSelected
                        ? 'bg-[#E8DEF8] border-[#6750A4] text-[#1D192B]'
                        : 'bg-white border-[#CAC4D0]/60 text-[#49454F] hover:bg-zinc-50'
                    }`}
                  >
                    <span className="text-base">{item.emoji}</span>
                    <div className="overflow-hidden">
                      <div className="text-[12px] font-bold truncate">{item.title}</div>
                      <div className="text-[10px] text-[#79747E] truncate">{item.desc}</div>
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Placement Mode */}
          <div>
            <label className="block text-[13px] font-bold text-[#1D1B20] mb-2">
              Island Placement Mode
            </label>
            <div className="grid grid-cols-2 gap-2">
              <button
                onClick={() =>
                  updateNotchConfig({
                    placementMode: 'WRAP_AROUND_NOTCH',
                    offsetY: 6,
                    cutoutGapWidth: Math.max(30, notchConfig.cutoutGapWidth || 40),
                  })
                }
                className={`p-3 rounded-xl border text-left flex items-center space-x-2.5 ${
                  notchConfig.placementMode === 'WRAP_AROUND_NOTCH'
                    ? 'bg-[#E8DEF8] border-[#6750A4] font-bold'
                    : 'bg-white border-[#CAC4D0]/60'
                }`}
              >
                <span className="text-lg">🏝️</span>
                <div>
                  <div className="text-[12px] font-bold text-[#1D1B20]">Wrap Around Notch</div>
                  <div className="text-[10px] text-[#79747E]">Camera in center gap</div>
                </div>
              </button>

              <button
                onClick={() =>
                  updateNotchConfig({
                    placementMode: 'BELOW_NOTCH',
                    offsetY: 38,
                    cutoutGapWidth: 0,
                  })
                }
                className={`p-3 rounded-xl border text-left flex items-center space-x-2.5 ${
                  notchConfig.placementMode === 'BELOW_NOTCH'
                    ? 'bg-[#E8DEF8] border-[#6750A4] font-bold'
                    : 'bg-white border-[#CAC4D0]/60'
                }`}
              >
                <span className="text-lg">⬇️</span>
                <div>
                  <div className="text-[12px] font-bold text-[#1D1B20]">Below Notch</div>
                  <div className="text-[10px] text-[#79747E]">Floats under camera</div>
                </div>
              </button>
            </div>
          </div>

          {/* Cutout Gap Slider */}
          <div>
            <div className="flex justify-between items-center mb-1">
              <span className="text-[12px] font-bold text-[#1D1B20]">Cutout Gap Width</span>
              <span className="text-[12px] font-mono font-bold text-[#6750A4]">
                {notchConfig.cutoutGapWidth} px
              </span>
            </div>
            <input
              type="range"
              min="0"
              max="120"
              value={notchConfig.cutoutGapWidth}
              onChange={(e) => updateNotchConfig({ cutoutGapWidth: parseInt(e.target.value) })}
              className="w-full accent-[#6750A4]"
            />
          </div>

          {/* Position Calibration: X & Y Offsets */}
          <div className="space-y-3 bg-white p-3.5 rounded-2xl border border-[#CAC4D0]/60">
            <div className="text-[12px] font-bold text-[#1D1B20] flex items-center justify-between">
              <span>Position Calibration</span>
              <span className="text-[11px] font-mono text-[#79747E]">
                X: {notchConfig.offsetX}px | Y: {notchConfig.offsetY}px
              </span>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <span className="text-[11px] text-[#79747E] block mb-1">Horizontal Offset (X)</span>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => updateNotchConfig({ offsetX: Math.max(-150, notchConfig.offsetX - 5) })}
                    className="p-1.5 rounded-lg bg-[#E8DEF8] text-[#1D192B] hover:bg-[#d8cbf0]"
                  >
                    <ArrowLeft className="w-3.5 h-3.5" />
                  </button>
                  <input
                    type="range"
                    min="-150"
                    max="150"
                    value={notchConfig.offsetX}
                    onChange={(e) => updateNotchConfig({ offsetX: parseInt(e.target.value) })}
                    className="flex-1 accent-[#6750A4]"
                  />
                  <button
                    onClick={() => updateNotchConfig({ offsetX: Math.min(150, notchConfig.offsetX + 5) })}
                    className="p-1.5 rounded-lg bg-[#E8DEF8] text-[#1D192B] hover:bg-[#d8cbf0]"
                  >
                    <ArrowRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              <div>
                <span className="text-[11px] text-[#79747E] block mb-1">Vertical Offset (Y)</span>
                <div className="flex items-center space-x-2">
                  <button
                    onClick={() => updateNotchConfig({ offsetY: Math.max(0, notchConfig.offsetY - 4) })}
                    className="p-1.5 rounded-lg bg-[#E8DEF8] text-[#1D192B] hover:bg-[#d8cbf0]"
                  >
                    <ArrowUp className="w-3.5 h-3.5" />
                  </button>
                  <input
                    type="range"
                    min="0"
                    max="140"
                    value={notchConfig.offsetY}
                    onChange={(e) => updateNotchConfig({ offsetY: parseInt(e.target.value) })}
                    className="flex-1 accent-[#6750A4]"
                  />
                  <button
                    onClick={() => updateNotchConfig({ offsetY: Math.min(140, notchConfig.offsetY + 4) })}
                    className="p-1.5 rounded-lg bg-[#E8DEF8] text-[#1D192B] hover:bg-[#d8cbf0]"
                  >
                    <ArrowDown className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* Action Row */}
          <div className="flex items-center justify-between pt-2">
            <button
              onClick={resetNotchDefaults}
              className="flex items-center space-x-1.5 text-[12px] font-semibold text-[#79747E] hover:text-[#1D1B20] transition-colors"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Reset Defaults</span>
            </button>

            <button
              onClick={() => simulateScroll(selectedAppForSim, 1)}
              className="flex items-center space-x-1.5 text-[12px] font-bold px-3 py-1.5 rounded-xl bg-[#E8DEF8] text-[#1D192B] hover:bg-[#dcd0f0] transition-colors"
            >
              <PlusCircle className="w-4 h-4 text-[#6750A4]" />
              <span>Test +1 Scroll</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
