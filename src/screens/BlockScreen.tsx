import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { AppIconBadge } from '../components/AppIconBadge';
import { ScrollyCharacter } from '../components/ScrollyCharacter';
import {
  Ban,
  Eye,
  Footprints,
  Smartphone,
  Wind,
  Activity,
  Check,
  AlertOctagon,
  Timer,
  Clock,
} from 'lucide-react';

export const BlockScreen: React.FC = () => {
  const {
    limits,
    updateLimit,
    challenges,
    completeChallenge,
    grantBonusScrolls,
    triggerInterventionPreview,
    activeInterventionApp,
    dismissIntervention,
  } = useScrolly();

  const [claimedNotice, setClaimedNotice] = useState<string | null>(null);

  const handleClaim = (chId: string, scrolls: number, title: string) => {
    completeChallenge(chId);
    grantBonusScrolls('com.instagram.android', scrolls);
    setClaimedNotice(`+${scrolls} bonus scrolls unlocked with "${title}"!`);
    setTimeout(() => setClaimedNotice(null), 4000);
  };

  const getChallengeIcon = (iconName: string) => {
    switch (iconName) {
      case 'walk':
        return <Footprints className="w-5 h-5 text-[#21005D]" />;
      case 'phone':
        return <Smartphone className="w-5 h-5 text-[#21005D]" />;
      case 'wind':
        return <Wind className="w-5 h-5 text-[#21005D]" />;
      default:
        return <Activity className="w-5 h-5 text-[#21005D]" />;
    }
  };

  return (
    <div className="flex flex-col space-y-4 pb-24 px-4 pt-2 max-w-md mx-auto">
      {/* Title */}
      <div className="text-center py-2">
        <h1 className="text-3xl font-black text-[#1D1B20] tracking-tight">Block Reels</h1>
        <p className="text-xs text-[#49454F] mt-1 font-medium">
          Set strict caps before doomscrolling takes over your day.
        </p>
      </div>

      {/* Sleek Coach Mascot Card */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm flex items-center space-x-4">
        <ScrollyCharacter scrollCount={75} size={70} />
        <div className="flex-1 space-y-2">
          <div>
            <h3 className="text-sm font-bold text-[#1D1B20]">BrainRot Interceptor</h3>
            <p className="text-xs text-[#49454F] mt-0.5 leading-snug">
              When your daily limit is hit, BrainRot puts up a full-screen intervention to break the dopamine loop.
            </p>
          </div>

          <button
            onClick={() => triggerInterventionPreview('Instagram')}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-xl border border-[#6750A4] text-[#6750A4] text-xs font-bold hover:bg-[#6750A4]/10 transition-colors"
          >
            <Eye className="w-3.5 h-3.5" />
            <span>Preview Intervention</span>
          </button>
        </div>
      </div>

      {/* Daily App Limits List */}
      <div>
        <h2 className="text-sm font-bold text-[#1D1B20] mb-2 px-1">Daily App Limits</h2>
        <div className="space-y-2.5">
          {limits.map((limit) => {
            return (
              <div
                key={limit.packageName}
                className="bg-[#F3EDF7] rounded-[24px] border border-[#CAC4D0] p-4 shadow-sm space-y-3"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <AppIconBadge packageName={limit.packageName} size={36} />
                    <div>
                      <div className="text-sm font-bold text-[#1D1B20]">{limit.appName}</div>
                      <div className="text-xs font-semibold text-[#6750A4]">
                        {limit.dailyLimit} reels per day
                      </div>
                    </div>
                  </div>

                  {/* Switch */}
                  <label className="relative inline-flex items-center cursor-pointer">
                    <input
                      type="checkbox"
                      checked={limit.isEnabled}
                      onChange={(e) =>
                        updateLimit(
                          limit.packageName,
                          limit.dailyLimit,
                          Math.round(limit.dailyLimit * 0.8),
                          e.target.checked
                        )
                      }
                      className="sr-only peer"
                    />
                    <div className="w-11 h-6 bg-[#CAC4D0] peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#6750A4]" />
                  </label>
                </div>

                {limit.isEnabled && (
                  <div className="pt-1">
                    <div className="flex justify-between items-center text-[10px] text-[#79747E] mb-1 font-semibold">
                      <span>10 reels</span>
                      <span className="font-mono text-xs font-bold text-[#1D1B20]">
                        {limit.dailyLimit} reels
                      </span>
                      <span>200 reels</span>
                    </div>
                    <input
                      type="range"
                      min="10"
                      max="200"
                      step="5"
                      value={limit.dailyLimit}
                      onChange={(e) =>
                        updateLimit(
                          limit.packageName,
                          parseInt(e.target.value),
                          Math.round(parseInt(e.target.value) * 0.8),
                          limit.isEnabled
                        )
                      }
                      className="w-full accent-[#6750A4]"
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Earn Extra Scrolls Section */}
      <div className="space-y-2 pt-1">
        <div>
          <h2 className="text-sm font-bold text-[#1D1B20] px-1">Earn Extra Scrolls</h2>
          <p className="text-xs text-[#79747E] px-1">
            Complete healthy, intentional resets to unlock small scroll allowances.
          </p>
        </div>

        {claimedNotice && (
          <div className="bg-[#1B6B40]/15 border border-[#1B6B40]/30 text-[#1B6B40] p-3 rounded-2xl text-center text-xs font-bold animate-fadeIn">
            ✨ {claimedNotice}
          </div>
        )}

        <div className="space-y-2">
          {challenges.map((challenge) => {
            return (
              <div
                key={challenge.id}
                className="bg-[#F3EDF7] rounded-2xl border border-[#CAC4D0] p-3.5 flex items-center justify-between shadow-sm"
              >
                <div className="flex items-center space-x-3">
                  <div className="w-10 h-10 rounded-full bg-[#EADDFF] flex items-center justify-center flex-shrink-0">
                    {getChallengeIcon(challenge.iconName)}
                  </div>
                  <div>
                    <div className="text-xs font-bold text-[#1D1B20]">{challenge.title}</div>
                    <div className="text-[11px] text-[#49454F] leading-tight mt-0.5">
                      {challenge.description}
                    </div>
                    <div className="text-[10px] font-bold text-[#6750A4] mt-1">
                      +{challenge.rewardScrolls} scrolls • +{challenge.rewardXp} XP • {challenge.durationMinutes} min
                    </div>
                  </div>
                </div>

                <button
                  disabled={challenge.isCompleted}
                  onClick={() =>
                    handleClaim(challenge.id, challenge.rewardScrolls, challenge.title)
                  }
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold flex-shrink-0 transition-all ${
                    challenge.isCompleted
                      ? 'bg-[#1B6B40]/15 text-[#1B6B40] cursor-default'
                      : 'bg-[#E8DEF8] text-[#1D192B] hover:bg-[#d8cbf0]'
                  }`}
                >
                  {challenge.isCompleted ? (
                    <span className="flex items-center space-x-1">
                      <Check className="w-3.5 h-3.5" />
                      <span>Done</span>
                    </span>
                  ) : (
                    'Claim'
                  )}
                </button>
              </div>
            );
          })}
        </div>
      </div>

      {/* --- SLEEK FULL SCREEN SCROLL BLOCK INTERVENTION MODAL --- */}
      {activeInterventionApp && (
        <div className="fixed inset-0 z-50 bg-[#FEF7FF]/95 backdrop-blur-md flex items-center justify-center p-6 animate-fadeIn">
          <div className="max-w-sm w-full flex flex-col items-center text-center space-y-4">
            {/* Red Blocked Icon */}
            <div className="w-16 h-16 rounded-full bg-[#B3261E]/15 border-2 border-[#B3261E] flex items-center justify-center text-[#B3261E] shadow-lg animate-pulse">
              <Ban className="w-9 h-9" />
            </div>

            <div className="space-y-1">
              <h2 className="text-2xl font-black text-[#1D1B20] tracking-tight">
                That's enough scrolling.
              </h2>
              <p className="text-xs text-[#49454F] max-w-xs leading-relaxed">
                You've reached your daily <strong className="text-[#1D1B20]">{activeInterventionApp}</strong> limit of scrolls. Your brain needs a breather.
              </p>
            </div>

            {/* Cooked Mascot */}
            <div className="my-2">
              <ScrollyCharacter scrollCount={250} size={130} />
            </div>

            {/* Action Buttons */}
            <div className="w-full space-y-2.5 pt-2">
              <button
                onClick={dismissIntervention}
                className="w-full py-3.5 rounded-2xl bg-[#6750A4] text-white font-black text-sm shadow hover:bg-[#523d8c] transition-colors"
              >
                Take a Break
              </button>

              <button
                onClick={() => {
                  grantBonusScrolls('com.instagram.android', 10);
                  dismissIntervention();
                }}
                className="w-full py-3 rounded-2xl border border-[#CAC4D0] bg-[#F3EDF7] text-[#1D1B20] font-bold text-xs hover:bg-[#E8DEF8] transition-colors"
              >
                Earn +10 Scrolls with Focus Reset
              </button>

              <button
                onClick={dismissIntervention}
                className="text-[11px] text-[#79747E] hover:text-[#1D1B20] font-medium pt-1"
              >
                Emergency Override (5 mins)
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
