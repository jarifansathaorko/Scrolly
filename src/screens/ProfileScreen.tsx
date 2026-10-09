import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { ScrollyCharacter } from '../components/ScrollyCharacter';
import {
  Edit2,
  Award,
  CheckCircle2,
  Flame,
  Swords,
  Layers,
  RotateCcw,
  Bell,
  Vibrate,
  UserPlus,
  Users,
  Moon,
  Sun,
  Monitor,
} from 'lucide-react';

export const ProfileScreen: React.FC = () => {
  const { profile, achievements, updateUsername, resetAllData, theme, isDarkMode, setTheme } = useScrolly();

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
          <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-[24px] p-6 max-w-xs w-full border border-[#CAC4D0] dark:border-[#49454F] shadow-xl space-y-4">
            <h3 className="text-base font-bold text-[#1D1B20] dark:text-[#E6E0E9]">Edit Username</h3>
            <form onSubmit={handleSaveUsername} className="space-y-3">
              <input
                type="text"
                value={newUsername}
                onChange={(e) => setNewUsername(e.target.value)}
                className="w-full px-3 py-2 rounded-xl border border-[#CAC4D0] dark:border-[#49454F] bg-white dark:bg-[#36343B] text-sm text-[#1D1B20] dark:text-[#E6E0E9] focus:outline-none focus:border-[#6750A4] dark:focus:border-[#D0BCFF]"
                autoFocus
              />
              <div className="flex space-x-2 pt-1">
                <button
                  type="submit"
                  className="flex-1 py-2 rounded-xl bg-[#6750A4] dark:bg-[#D0BCFF] text-white dark:text-[#21005D] font-bold text-xs"
                >
                  Save
                </button>
                <button
                  type="button"
                  onClick={() => setIsEditUserOpen(false)}
                  className="px-3 py-2 rounded-xl text-[#79747E] dark:text-[#938F99] font-medium text-xs hover:text-[#1D1B20] dark:hover:text-[#E6E0E9]"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Profile Header Card */}
      <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-[28px] border border-[#CAC4D0] dark:border-[#49454F] p-6 shadow-sm flex flex-col items-center text-center relative overflow-hidden transition-colors">
        {/* Avatar with Mascot */}
        <div className="w-24 h-24 rounded-full bg-[#EADDFF] dark:bg-[#4F378B] border-2 border-[#6750A4] dark:border-[#D0BCFF] flex items-center justify-center shadow-md mb-3">
          <ScrollyCharacter scrollCount={15} size={76} />
        </div>

        {/* Username with edit button */}
        <div className="flex items-center space-x-1.5">
          <h2 className="text-lg font-black text-[#1D1B20] dark:text-[#E6E0E9]">@{profile.username}</h2>
          <button
            onClick={() => {
              setNewUsername(profile.username);
              setIsEditUserOpen(true);
            }}
            className="p-1 rounded-full text-[#79747E] dark:text-[#938F99] hover:text-[#1D1B20] dark:hover:text-[#E6E0E9] hover:bg-[#E8DEF8] dark:hover:bg-[#36343B]"
          >
            <Edit2 className="w-3.5 h-3.5" />
          </button>
        </div>

        <p className="text-xs text-[#79747E] dark:text-[#938F99]">Conscious scroller since Sept 2026</p>

        {/* Level and XP progress bar */}
        <div className="w-full mt-4 space-y-1.5 bg-white/70 dark:bg-[#36343B]/70 p-3.5 rounded-2xl border border-[#CAC4D0]/60 dark:border-[#49454F]/60">
          <div className="flex justify-between items-center text-xs">
            <span className="font-bold text-[#6750A4] dark:text-[#D0BCFF] uppercase tracking-wider">
              Level {profile.currentLevel}
            </span>
            <span className="text-[11px] font-mono text-[#79747E] dark:text-[#938F99]">
              {profile.currentXp} / {profile.xpForNextLevel} XP
            </span>
          </div>
          <div className="w-full h-2 bg-[#E8DEF8] dark:bg-[#49454F] rounded-full overflow-hidden">
            <div
              className="h-full bg-[#6750A4] dark:bg-[#D0BCFF] rounded-full transition-all duration-300"
              style={{ width: `${xpProgress * 100}%` }}
            />
          </div>
        </div>
      </div>

      {/* All-Time Performance Grid */}
      <div className="grid grid-cols-3 gap-2">
        <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-2xl border border-[#CAC4D0] dark:border-[#49454F] p-3 text-center space-y-0.5 transition-colors">
          <div className="w-7 h-7 rounded-full bg-orange-100 dark:bg-orange-900/30 mx-auto flex items-center justify-center text-orange-600 dark:text-orange-400 mb-1">
            <Flame className="w-4 h-4 fill-orange-500" />
          </div>
          <div className="text-base font-black text-[#1D1B20] dark:text-[#E6E0E9]">{profile.streakDays}d</div>
          <div className="text-[10px] text-[#79747E] dark:text-[#938F99] font-medium leading-none">
            Best: {profile.bestStreak}d
          </div>
        </div>

        <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-2xl border border-[#CAC4D0] dark:border-[#49454F] p-3 text-center space-y-0.5 transition-colors">
          <div className="w-7 h-7 rounded-full bg-purple-100 dark:bg-[#4F378B]/50 mx-auto flex items-center justify-center text-[#6750A4] dark:text-[#D0BCFF] mb-1">
            <Layers className="w-4 h-4" />
          </div>
          <div className="text-base font-black text-[#1D1B20] dark:text-[#E6E0E9]">
            {profile.totalScrollsAllTime.toLocaleString()}
          </div>
          <div className="text-[10px] text-[#79747E] dark:text-[#938F99] font-medium leading-none">
            All-Time Scrolls
          </div>
        </div>

        <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-2xl border border-[#CAC4D0] dark:border-[#49454F] p-3 text-center space-y-0.5 transition-colors">
          <div className="w-7 h-7 rounded-full bg-emerald-100 dark:bg-emerald-900/30 mx-auto flex items-center justify-center text-emerald-700 dark:text-emerald-400 mb-1">
            <Swords className="w-4 h-4" />
          </div>
          <div className="text-base font-black text-[#1D1B20] dark:text-[#E6E0E9]">
            {profile.battlesWon}W / {profile.battlesLost}L
          </div>
          <div className="text-[10px] text-[#79747E] dark:text-[#938F99] font-medium leading-none">
            {winRate}% Win Rate
          </div>
        </div>
      </div>

      {/* Achievements Gallery */}
      <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-[28px] border border-[#CAC4D0] dark:border-[#49454F] p-5 shadow-sm space-y-3 transition-colors">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Award className="w-4 h-4 text-[#6750A4] dark:text-[#D0BCFF]" />
            <h3 className="text-xs font-bold text-[#1D1B20] dark:text-[#E6E0E9] uppercase tracking-wider">
              Achievements
            </h3>
          </div>
          <span className="text-[11px] font-bold text-[#6750A4] dark:text-[#D0BCFF]">
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
                    ? 'bg-white dark:bg-[#36343B] border-[#6750A4]/40 dark:border-[#D0BCFF]/40 shadow-xs'
                    : 'bg-[#E8DEF8]/40 dark:bg-[#36343B]/40 border-[#CAC4D0]/60 dark:border-[#49454F]/60 opacity-75'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="text-lg">
                      {ach.isUnlocked ? '🏅' : '🔒'}
                    </span>
                    {ach.isUnlocked ? (
                      <CheckCircle2 className="w-3.5 h-3.5 text-[#1B6B40] dark:text-[#7FCB8B]" />
                    ) : (
                      <span className="text-[10px] text-[#79747E] dark:text-[#938F99] font-mono">
                        {ach.currentValue}/{ach.targetValue}
                      </span>
                    )}
                  </div>
                  <h4 className="text-xs font-bold text-[#1D1B20] dark:text-[#E6E0E9]">{ach.title}</h4>
                  <p className="text-[10px] text-[#49454F] dark:text-[#CAC4D0] leading-tight mt-0.5">
                    {ach.description}
                  </p>
                </div>

                {!ach.isUnlocked && (
                  <div className="w-full h-1.5 bg-[#CAC4D0]/50 dark:bg-[#49454F]/50 rounded-full overflow-hidden mt-2">
                    <div
                      className="h-full bg-[#6750A4] dark:bg-[#D0BCFF] rounded-full"
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
      <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-[28px] border border-[#CAC4D0] dark:border-[#49454F] p-5 shadow-sm space-y-3 transition-colors">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Users className="w-4 h-4 text-[#6750A4] dark:text-[#D0BCFF]" />
            <h3 className="text-xs font-bold text-[#1D1B20] dark:text-[#E6E0E9] uppercase tracking-wider">
              Connected Friends
            </h3>
          </div>
          <button
            onClick={handleShareFriendCode}
            className="flex items-center space-x-1 text-xs font-bold text-[#6750A4] dark:text-[#D0BCFF] hover:underline"
          >
            <UserPlus className="w-3.5 h-3.5" />
            <span>Invite</span>
          </button>
        </div>

        {friendCodeNotice && (
          <div className="bg-[#EADDFF] dark:bg-[#4F378B] border border-[#6750A4]/30 dark:border-[#D0BCFF]/30 text-[#21005D] dark:text-[#EADDFF] p-2.5 rounded-xl text-xs font-bold text-center animate-fadeIn">
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
              className="bg-white dark:bg-[#36343B] p-2.5 rounded-xl border border-[#CAC4D0]/60 dark:border-[#49454F]/60 flex items-center justify-between"
            >
              <div className="flex items-center space-x-2.5">
                <div className="w-8 h-8 rounded-full bg-[#EADDFF] dark:bg-[#4F378B] flex items-center justify-center font-bold text-xs text-[#21005D] dark:text-[#EADDFF]">
                  {friend.name[0]}
                </div>
                <div>
                  <div className="text-xs font-bold text-[#1D1B20] dark:text-[#E6E0E9]">{friend.name}</div>
                  <div className="text-[10px] text-[#79747E] dark:text-[#938F99]">@{friend.username}</div>
                </div>
              </div>

              <span className="text-[10px] font-semibold text-[#1B6B40] dark:text-[#7FCB8B] bg-[#1B6B40]/10 dark:bg-[#7FCB8B]/15 px-2 py-0.5 rounded-full">
                {friend.status}
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Appearance - Dark Mode */}
      <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-[28px] border border-[#CAC4D0] dark:border-[#49454F] p-5 shadow-sm space-y-3 transition-colors">
        <div className="flex items-center space-x-2">
          {isDarkMode ? <Moon className="w-4 h-4 text-[#D0BCFF]" /> : <Sun className="w-4 h-4 text-[#6750A4]" />}
          <h3 className="text-xs font-bold text-[#1D1B20] dark:text-[#E6E0E9] uppercase tracking-wider">
            Appearance
          </h3>
          <span className="ml-auto text-[10px] font-medium px-2 py-0.5 rounded-full bg-[#E8DEF8] dark:bg-[#4F378B] text-[#21005D] dark:text-[#EADDFF]">
            {isDarkMode ? 'Dark' : 'Light'} • {theme}
          </span>
        </div>

        <div className="grid grid-cols-3 gap-2">
          {[
            { id: 'light', label: 'Light', icon: Sun, desc: 'Always light' },
            { id: 'dark', label: 'Dark', icon: Moon, desc: 'Always dark' },
            { id: 'system', label: 'System', icon: Monitor, desc: 'Auto' },
          ].map((opt) => {
            const isActive = theme === opt.id;
            const Icon = opt.icon;
            return (
              <button
                key={opt.id}
                onClick={() => setTheme(opt.id as any)}
                className={`flex flex-col items-center gap-1.5 p-3 rounded-2xl border transition-all ${
                  isActive
                    ? 'bg-[#E8DEF8] dark:bg-[#4F378B] border-[#6750A4] dark:border-[#D0BCFF] shadow-sm scale-[1.02]'
                    : 'bg-white dark:bg-[#36343B] border-[#CAC4D0] dark:border-[#49454F] hover:bg-[#F3EDF7] dark:hover:bg-[#2B2930]'
                }`}
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center ${isActive ? 'bg-[#6750A4] dark:bg-[#D0BCFF] text-white dark:text-[#21005D]' : 'bg-[#E8DEF8] dark:bg-[#49454F] text-[#49454F] dark:text-[#CAC4D0]'}`}>
                  <Icon className="w-4 h-4" />
                </div>
                <span className={`text-xs font-bold ${isActive ? 'text-[#21005D] dark:text-[#EADDFF]' : 'text-[#1D1B20] dark:text-[#E6E0E9]'}`}>{opt.label}</span>
                <span className="text-[10px] text-[#79747E] dark:text-[#938F99]">{opt.desc}</span>
              </button>
            );
          })}
        </div>

        <p className="text-[10px] text-[#79747E] dark:text-[#938F99] px-1">
          Dark mode uses Material You dark palette #141218 bg, #D0BCFF primary, glass tiles adapt automatically. System follows your device setting.
        </p>
      </div>

      {/* Preferences & Reset */}
      <div className="bg-[#F3EDF7] dark:bg-[#2B2930] rounded-[28px] border border-[#CAC4D0] dark:border-[#49454F] p-5 shadow-sm space-y-3 transition-colors">
        <h3 className="text-xs font-bold text-[#1D1B20] dark:text-[#E6E0E9] uppercase tracking-wider">
          Preferences & Settings
        </h3>

        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Vibrate className="w-4 h-4 text-[#79747E] dark:text-[#938F99]" />
              <span className="text-xs font-semibold text-[#1D1B20] dark:text-[#E6E0E9]">Haptic Feedback on Swipes</span>
            </div>
            <input
              type="checkbox"
              checked={hapticsEnabled}
              onChange={(e) => setHapticsEnabled(e.target.checked)}
              className="accent-[#6750A4] dark:accent-[#D0BCFF] w-4 h-4"
            />
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2.5">
              <Bell className="w-4 h-4 text-[#79747E] dark:text-[#938F99]" />
              <span className="text-xs font-semibold text-[#1D1B20] dark:text-[#E6E0E9]">Daily Limit Warnings</span>
            </div>
            <input
              type="checkbox"
              checked={notificationsEnabled}
              onChange={(e) => setNotificationsEnabled(e.target.checked)}
              className="accent-[#6750A4] dark:accent-[#D0BCFF] w-4 h-4"
            />
          </div>

          <div className="pt-2 border-t border-[#CAC4D0]/60 dark:border-[#49454F]/60 flex items-center justify-between">
            <span className="text-xs text-[#79747E] dark:text-[#938F99]">Clear App Cache & Data</span>
            <button
              onClick={() => {
                if (window.confirm('Reset all BrainRot stats and preferences to initial state?')) {
                  resetAllData();
                }
              }}
              className="flex items-center space-x-1 text-xs font-bold text-[#B3261E] dark:text-[#FFB4AB] hover:underline"
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
