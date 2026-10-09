import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { ScrollyCharacter } from '../components/ScrollyCharacter';
import {
  Edit2,
  Award,
  Lock,
  CheckCircle2,
  Flame,
  Swords,
  Layers,
  RotateCcw,
  Bell,
  Vibrate,
  Shield,
  UserPlus,
  Users,
} from 'lucide-react';

export const ProfileScreen: React.FC = () => {
  const { profile, achievements, updateUsername, resetAllData } = useScrolly();

  const [isEditUserOpen, setIsEditUserOpen] = useState(false);
  const [newUsername, setNewUsername] = useState(profile.username);
  const [hapticsEnabled, setHapticsEnabled] = useState(true);
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [friendCodeNotice, setFriendCodeNotice] = useState<string | null>(null);

  const xpProgress = Math.min(1, profile.currentXp / Math.max(1, profile.xpForNextLevel));
  const totalBattles = profile.battlesWon + profile.battlesLost;
  const winRate = totalBattles > 0 ? Math.round((profile.battlesWon / totalBattles) * 100) : 0;

  const handleSaveUsername = (e: React.FormEvent) => {
    e.preventDefault();
    if (newUsername.trim()) {
      updateUsername(newUsername.trim());
      setIsEditUserOpen(false);
    }
  };

  const handleShareFriendCode = () => {
    setFriendCodeNotice(`Friend invite code: SCROLLY-${profile.username.toUpperCase()}-77`);
    setTimeout(() => setFriendCodeNotice(null), 4000);
  };

  return (
    <div className="flex flex-col space-y-4 pb-24 px-4 pt-2 max-w-md mx-auto">
      {/* Edit Username Modal */}
      {isEditUserOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#F3EDF7] rounded-[24px] p-6 max-w-xs w-full border border-[#CAC4D0] shadow-xl space-y-4">
            <h3 className="text-base font-bold text-[#1D1B20]">Edit Username</h3>
            <form onSubmit={handleSaveUsername} className="space-y-3">
              <input
                type="text"
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-[#CAC4D0] bg-white text-sm text-[#1D1B20] focus:outline-none focus:border-[#6750A4]"
                autoFocus
              />
              <div className="flex space-x-2 pt-1">
                <button
                  type="submit"
                  className="flex-1 py-2 rounded-xl bg-[#6750A4] text-white font-bold text-xs"
                >
                  Save
                </button>
                <button
                  type="button"
                  onClick={() => setIsEditUserOpen(false)}
                  className="px-3 py-2 rounded-xl text-[#79747E] font-medium text-xs hover:text-[#1D1B20]"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Profile Header Card */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-6 shadow-sm flex flex-col items-center text-center relative overflow-hidden">
        {/* Avatar with Mascot */}
        <div className="w-24 h-24 rounded-full bg-[#EADDFF] border-2 border-[#6750A4] flex items-center justify-center shadow-md mb-3">
          <ScrollyCharacter scrollCount={15} size={76} />
        </div>

        {/* Username with edit button */}
        <div className="flex items-center space-x-1.5">
          <h2 className="text-lg font-black text-[#1D1B20]">@{profile.username}</h2>
          <button
            onClick={() => {
              setNewUsername(profile.username);
              setIsEditUserOpen(true);
            }}
            className="p-1 rounded-full text-[#79747E] hover:text-[#1D1B20] hover:bg-[#E8DEF8]"
          >
            <Edit2 className="w-3.5 h-3.5" />
          </button>
        </div>

        <p className="text-xs text-[#79747E]">Conscious scroller since Sept 2026</p>

        {/* Level and XP progress bar */}
        <div className="w-full mt-4 space-y-1.5 bg-white/70 p-3.5 rounded-2xl border border-[#CAC4D0]/60">
          <div className="flex justify-between items-center text-xs">
            <span className="font-bold text-[#6750A4] uppercase tracking-wider">
              Level {profile.currentLevel}
            </span>
            <span className="text-[11px] font-mono text-[#79747E]">
              {profile.currentXp} / {profile.xpForNextLevel} XP
            </span>
          </div>
          <div className="w-full h-2 bg-[#E8DEF8] rounded-full overflow-hidden">
            <div
              className="h-full bg-[#6750A4] rounded-full transition-all duration-300"
              style={{ width: `${xpProgress * 100}%` }}
            />
          </div>
        </div>
      </div>

      {/* All-Time Performance Grid */}
      <div className="grid grid-cols-3 gap-2">
        <div className="bg-[#F3EDF7] rounded-2xl border border-[#CAC4D0] p-3 text-center space-y-0.5">
          <div className="w-7 h-7 rounded-full bg-orange-100 mx-auto flex items-center justify-center text-orange-600 mb-1">
            <Flame className="w-4 h-4 fill-orange-500" />
          </div>
          <div className="text-base font-black text-[#1D1B20]">{profile.streakDays}d</div>
          <div className="text-[10px] text-[#79747E] font-medium leading-none">
            Best: {profile.bestStreak}d
          </div>
        </div>

        <div className="bg-[#F3EDF7] rounded-2xl border border-[#CAC4D0] p-3 text-center space-y-0.5">
          <div className="w-7 h-7 rounded-full bg-purple-100 mx-auto flex items-center justify-center text-[#6750A4] mb-1">
            <Layers className="w-4 h-4" />
          </div>
          <div className="text-base font-black text-[#1D1B20]">
            {profile.totalScrollsAllTime.toLocaleString()}
          </div>
          <div className="text-[10px] text-[#79747E] font-medium leading-none">
            All-Time Scrolls
          </div>
        </div>

        <div className="bg-[#F3EDF7] rounded-2xl border border-[#CAC4D0] p-3 text-center space-y-0.5">
          <div className="w-7 h-7 rounded-full bg-emerald-100 mx-auto flex items-center justify-center text-emerald-700 mb-1">
            <Swords className="w-4 h-4" />
          </div>
          <div className="text-base font-black text-[#1D1B20]">
            {profile.battlesWon}W / {profile.battlesLost}L
          </div>
          <div className="text-[10px] text-[#79747E] font-medium leading-none">
            {winRate}% Win Rate
          </div>
        </div>
      </div>

      {/* Achievements Gallery */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Award className="w-4 h-4 text-[#6750A4]" />
            <h3 className="text-xs font-bold text-[#1D1B20] uppercase tracking-wider">
              Achievements
            </h3>
          </div>
          <span className="text-[11px] font-bold text-[#6750A4]">
            {achievements.filter((a) => a.isUnlocked).length} / {achievements.length} Unlocked
          </span>
        </div>

        <div className="grid grid-cols-2 gap-2">
          {achievements.map((ach) => {
            return (
              <div
                key={ach.id}
                className={`p-3 rounded-2xl border flex flex-col justify-between ${
                  ach.isUnlocked
                    ? 'bg-white border-[#6750A4]/40 shadow-xs'
                    : 'bg-[#E8DEF8]/40 border-[#CAC4D0]/60 opacity-75'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-lg">
                      {ach.isUnlocked ? '🏅' : '🔒'}
                    </span>
                    {ach.isUnlocked ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-[#1B6B40]" />
                    ) : (
                      <span className="text-[10px] text-[#79747E] font-mono">
                        {ach.currentValue}/{ach.targetValue}
                      </span>
                    )}
                  </div>
                  <h4 className="text-xs font-bold text-[#1D1B20]">{ach.title}</h4>
                  <p className="text-[10px] text-[#49454F] leading-tight mt-0.5">
                    {ach.description}
                  </p>
                </div>

                {!ach.isUnlocked && (
                  <div className="w-full h-1.5 bg-[#CAC4D0]/50 rounded-full overflow-hidden mt-2">
                    <div
                      className="h-full bg-[#6750A4] rounded-full"
                      style={{
                        width: `${Math.min(100, (ach.currentValue / ach.targetValue) * 100)}%`,
                      }}
                    />
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Friends & Social */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Users className="w-4 h-4 text-[#6750A4]" />
            <h3 className="text-xs font-bold text-[#1D1B20] uppercase tracking-wider">
              Connected Friends
            </h3>
          </div>
          <button
            onClick={handleShareFriendCode}
            className="flex items-center space-x-1 text-xs font-bold text-[#6750A4] hover:underline"
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>Invite</span>
          </button>
        </div>

        {friendCodeNotice && (
          <div className="bg-[#EADDFF] border border-[#6750A4]/30 text-[#21005D] p-2.5 rounded-xl text-xs font-bold text-center animate-fadeIn">
            {friendCodeNotice}
          </div>
        )}

        <div className="space-y-2">
          {[
            { name: 'Alex Rivera', username: 'arivera', status: 'In Battle' },
            { name: 'Sarah Chen', username: 'schen_ai', status: 'In Battle' },
            { name: 'Marcus Vance', username: 'mvance', status: 'Online' },
          ].map((friend) => (
            <div
              key={friend.username}
              className="bg-white p-2.5 rounded-xl border border-[#CAC4D0]/60 flex items-center justify-between"
            >
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-full bg-[#EADDFF] flex items-center justify-center font-bold text-xs text-[#21005D]">
                  {friend.name[0]}
                </div>
                <div>
                  <div className="text-xs font-bold text-[#1D1B20]">{friend.name}</div>
                  <div className="text-[10px] text-[#79747E]">@{friend.username}</div>
                </div>
              </div>

              <span className="text-[10px] font-semibold text-[#1B6B40] bg-[#1B6B40]/10 px-2 py-0.5 rounded-full">
                {friend.status}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Preferences & Reset */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <h3 className="text-xs font-bold text-[#1D1B20] uppercase tracking-wider">
          Preferences & Settings
        </h3>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Vibrate className="w-4 h-4 text-[#79747E]" />
              <span className="text-xs font-semibold text-[#1D1B20]">Haptic Feedback on Swipes</span>
            </div>
            <input
              type="checkbox"
              checked={hapticsEnabled}
              onChange={(e) => setHapticsEnabled(e.target.checked)}
              className="accent-[#6750A4] w-4 h-4"
            />
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Bell className="w-4 h-4 text-[#79747E]" />
              <span className="text-xs font-semibold text-[#1D1B20]">Daily Limit Warnings</span>
            </div>
            <input
              type="checkbox"
              checked={notificationsEnabled}
              onChange={(e) => setNotificationsEnabled(e.target.checked)}
              className="accent-[#6750A4] w-4 h-4"
            />
          </div>

          <div className="pt-2 border-t border-[#CAC4D0]/60 flex items-center justify-between">
            <span className="text-xs text-[#79747E]">Clear App Cache & Data</span>
            <button
              onClick={() => {
                if (window.confirm('Reset all BrainRot stats and preferences to initial state?')) {
                  resetAllData();
                }
              }}
              className="flex items-center space-x-1 text-xs font-bold text-[#B3261E] hover:underline"
            >
              <RotateCcw className="w-3.5 h-3.5" />
              <span>Reset Data</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
