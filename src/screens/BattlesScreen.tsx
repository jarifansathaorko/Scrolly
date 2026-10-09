import React, { useState } from 'react';
import { useScrolly } from '../context/ScrollyContext';
import { ScrollyCharacter } from '../components/ScrollyCharacter';
import {
  Zap,
  Plus,
  Trophy,
  Flame,
  Swords,
  UserCheck,
  Send,
  Sparkles,
} from 'lucide-react';
import confetti from 'canvas-confetti';

export const BattlesScreen: React.FC = () => {
  const { battles, profile, todayStats, dailyTotal, addFriendBattle } = useScrolly();

  const [isChallengeModalOpen, setIsChallengeModalOpen] = useState(false);
  const [friendName, setFriendName] = useState('');
  const [friendUsername, setFriendUsername] = useState('');
  const [nudgedFriend, setNudgedFriend] = useState<string | null>(null);

  const featuredBattle = battles[0] || {
    id: 'featured',
    friendName: 'Alex Rivera',
    friendUsername: 'arivera',
    userScrolls: dailyTotal,
    friendScrolls: 93,
    date: 'Today',
    status: 'WINNING' as const,
  };

  const isUserWinning = dailyTotal < featuredBattle.friendScrolls;
  const isTied = dailyTotal === featuredBattle.friendScrolls;
  const diff = Math.abs(featuredBattle.friendScrolls - dailyTotal);

  const handleCreateChallenge = (e: React.FormEvent) => {
    e.preventDefault();
    if (!friendName.trim()) return;

    addFriendBattle(
      friendName.trim(),
      friendUsername.trim() || friendName.toLowerCase().replace(/\s+/g, '')
    );
    confetti({
      particleCount: 40,
      spread: 50,
      origin: { y: 0.6 },
    });
    setFriendName('');
    setFriendUsername('');
    setIsChallengeModalOpen(false);
  };

  const handleNudge = (friendName: string) => {
    setNudgedFriend(friendName);
    setTimeout(() => setNudgedFriend(null), 3000);
  };

  return (
    <div className="flex flex-col space-y-4 pb-24 px-4 pt-2 max-w-md mx-auto">
      {/* Title */}
      <div className="text-center py-2">
        <h1 className="text-3xl font-black text-[#1D1B20] tracking-tight leading-tight">
          Battle your <br /> Friends
        </h1>
        <p className="text-xs text-[#49454F] mt-1 font-medium">
          Lower scroll count wins! Control your feed together.
        </p>
      </div>

      {/* Featured Matchup Card */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-6 shadow-sm flex flex-col items-center relative overflow-hidden">
        {/* Header Badge */}
        <div className="px-3.5 py-1 rounded-full bg-[#E8DEF8] border border-[#CAC4D0] text-[10px] font-extrabold tracking-wider text-[#1D192B] uppercase mb-4">
          LIVE BATTLE • TODAY
        </div>

        {/* Duel Versus Row */}
        <div className="w-full flex items-center justify-around my-2">
          {/* Left: You */}
          <div className="flex flex-col items-center text-center">
            <div className="relative">
              <ScrollyCharacter scrollCount={dailyTotal} size={70} />
            </div>
            <div className="text-xs font-bold text-[#1D1B20] mt-1">You</div>
            <div className="text-[10px] text-[#79747E]">@{profile.username}</div>
            <div className="text-2xl font-black text-[#21005D] mt-1">
              {dailyTotal}
              <span className="text-[10px] font-normal text-[#79747E] ml-0.5">reels</span>
            </div>
          </div>

          {/* Versus Center Badge */}
          <div className="flex flex-col items-center mx-2">
            <div className="w-11 h-11 rounded-full bg-[#EADDFF] border border-[#6750A4] flex items-center justify-center text-[#6750A4] shadow-md animate-pulse">
              <Zap className="w-6 h-6 fill-[#6750A4]" />
            </div>
            <span className="text-[11px] font-black text-[#6750A4] mt-1">VS</span>
          </div>

          {/* Right: Opponent */}
          <div className="flex flex-col items-center text-center">
            <div className="relative">
              <ScrollyCharacter scrollCount={featuredBattle.friendScrolls} size={70} />
            </div>
            <div className="text-xs font-bold text-[#1D1B20] mt-1 truncate max-w-[80px]">
              {featuredBattle.friendName}
            </div>
            <div className="text-[10px] text-[#79747E] truncate max-w-[80px]">
              @{featuredBattle.friendUsername}
            </div>
            <div className="text-2xl font-black text-[#21005D] mt-1">
              {featuredBattle.friendScrolls}
              <span className="text-[10px] font-normal text-[#79747E] ml-0.5">reels</span>
            </div>
          </div>
        </div>

        {/* Live Status Pill */}
        <div className="w-full mt-4 pt-3 border-t border-[#CAC4D0]/60 text-center space-y-1">
          <div
            className={`inline-block px-3 py-0.5 rounded-full text-xs font-black uppercase tracking-wider ${
              isUserWinning
                ? 'bg-[#1B6B40]/15 text-[#1B6B40] border border-[#1B6B40]/30'
                : isTied
                ? 'bg-amber-500/15 text-amber-700 border border-amber-500/30'
                : 'bg-[#B3261E]/15 text-[#B3261E] border border-[#B3261E]/30'
            }`}
          >
            {isUserWinning ? '🏆 WINNING' : isTied ? '🤝 TIED' : '⚠️ BEHIND'}
          </div>

          <p className="text-xs text-[#49454F] font-medium">
            {isUserWinning
              ? `You've scrolled ${diff} fewer reels than ${featuredBattle.friendName}!`
              : isTied
              ? "You're dead even! Put the phone down to pull ahead."
              : `${featuredBattle.friendName} has scrolled ${diff} fewer reels than you.`}
          </p>
        </div>
      </div>

      {/* Challenge Button */}
      <button
        onClick={() => setIsChallengeModalOpen(true)}
        className="w-full py-3 px-4 rounded-2xl bg-[#6750A4] text-white font-bold text-xs flex items-center justify-center space-x-2 shadow-sm hover:bg-[#523d8c] transition-all"
      >
        <Plus className="w-4 h-4" />
        <span>Challenge a Friend</span>
      </button>

      {/* Nudge notification banner */}
      {nudgedFriend && (
        <div className="bg-[#1B6B40]/15 border border-[#1B6B40]/30 text-[#1B6B40] p-2.5 rounded-xl text-center text-xs font-bold animate-fadeIn">
          💨 Sent a mindful dopamine nudge to {nudgedFriend}!
        </div>
      )}

      {/* Active Battles List */}
      <div className="space-y-2">
        <h3 className="text-sm font-bold text-[#1D1B20] px-1">Active Challenges</h3>

        {battles.map((battle) => {
          const winning = dailyTotal < battle.friendScrolls;
          const tied = dailyTotal === battle.friendScrolls;

          return (
            <div
              key={battle.id}
              className="bg-[#F3EDF7] rounded-2xl border border-[#CAC4D0] p-3.5 flex items-center justify-between shadow-sm"
            >
              <div className="flex items-center space-x-3">
                <ScrollyCharacter scrollCount={battle.friendScrolls} size={42} animated={false} />
                <div>
                  <div className="text-xs font-bold text-[#1D1B20]">{battle.friendName}</div>
                  <div className="text-[10px] text-[#79747E]">@{battle.friendUsername}</div>
                  <div className="text-[11px] font-semibold text-[#49454F] mt-0.5">
                    You: <strong className="text-[#21005D]">{dailyTotal}</strong> vs
                    Them: <strong className="text-[#21005D]">{battle.friendScrolls}</strong>
                  </div>
                </div>
              </div>

              <div className="flex flex-col items-end space-y-1.5">
                <span
                  className={`text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full ${
                    winning
                      ? 'bg-[#1B6B40]/15 text-[#1B6B40]'
                      : tied
                      ? 'bg-amber-500/15 text-amber-700'
                      : 'bg-[#B3261E]/15 text-[#B3261E]'
                  }`}
                >
                  {winning ? 'Winning' : tied ? 'Tied' : 'Losing'}
                </span>

                <button
                  onClick={() => handleNudge(battle.friendName)}
                  className="flex items-center space-x-1 text-[10px] font-bold px-2 py-1 rounded-lg bg-[#E8DEF8] text-[#1D192B] hover:bg-[#d8cbf0]"
                >
                  <Send className="w-2.5 h-2.5" />
                  <span>Nudge</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Friends Daily Leaderboard */}
      <div className="bg-[#F3EDF7] rounded-[28px] border border-[#CAC4D0] p-5 shadow-sm space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Trophy className="w-4 h-4 text-amber-600" />
            <h3 className="text-xs font-bold text-[#1D1B20] uppercase tracking-wider">
              Daily Attention Leaderboard
            </h3>
          </div>
          <span className="text-[10px] text-[#79747E]">Lowest scrolls rank first</span>
        </div>

        <div className="space-y-2">
          {[
            { rank: 1, name: 'Sarah Chen', count: 24, streak: 9, isYou: false },
            { rank: 2, name: 'You (aorko)', count: dailyTotal, streak: profile.streakDays, isYou: true },
            { rank: 3, name: 'Marcus Vance', count: 36, streak: 4, isYou: false },
            { rank: 4, name: 'Alex Rivera', count: 93, streak: 2, isYou: false },
          ]
            .sort((a, b) => a.count - b.count)
            .map((entry, idx) => (
              <div
                key={entry.name}
                className={`flex items-center justify-between p-2.5 rounded-xl border ${
                  entry.isYou
                    ? 'bg-[#EADDFF] border-[#6750A4] font-bold'
                    : 'bg-white border-[#CAC4D0]/60'
                }`}
              >
                <div className="flex items-center space-x-3">
                  <span
                    className={`w-6 h-6 rounded-full flex items-center justify-center text-xs font-black ${
                      idx === 0
                        ? 'bg-amber-400 text-amber-950'
                        : idx === 1
                        ? 'bg-zinc-300 text-zinc-900'
                        : 'bg-amber-700/20 text-amber-900'
                    }`}
                  >
                    {idx + 1}
                  </span>
                  <div>
                    <span className="text-xs text-[#1D1B20]">{entry.name}</span>
                    <div className="flex items-center space-x-1 text-[10px] text-[#79747E]">
                      <Flame className="w-3 h-3 text-orange-500 fill-orange-500" />
                      <span>{entry.streak} day streak</span>
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <span className="text-xs font-mono font-bold text-[#21005D]">
                    {entry.count} reels
                  </span>
                </div>
              </div>
            ))}
        </div>
      </div>

      {/* Challenge Modal */}
      {isChallengeModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-[#F3EDF7] rounded-[28px] p-6 max-w-sm w-full border border-[#CAC4D0] shadow-xl space-y-4">
            <div className="flex items-center space-x-2 text-[#6750A4]">
              <Swords className="w-6 h-6" />
              <h3 className="text-lg font-bold text-[#1D1B20]">Start Scroll Battle</h3>
            </div>
            <p className="text-xs text-[#49454F]">
              Enter a friend's details to duel today. The player with the fewest scrolls at midnight wins bragging rights!
            </p>

            <form onSubmit={handleCreateChallenge} className="space-y-3">
              <div>
                <label className="block text-xs font-bold text-[#1D1B20] mb-1">
                  Friend's Name
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Jordan Blake"
                  value={friendName}
                  onChange={(e) => setFriendName(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#CAC4D0] bg-white text-xs text-[#1D1B20] focus:outline-none focus:border-[#6750A4]"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-[#1D1B20] mb-1">
                  Friend's Username (optional)
                </label>
                <input
                  type="text"
                  placeholder="e.g. jblake"
                  value={friendUsername}
                  onChange={(e) => setFriendUsername(e.target.value)}
                  className="w-full px-3 py-2 rounded-xl border border-[#CAC4D0] bg-white text-xs text-[#1D1B20] focus:outline-none focus:border-[#6750A4]"
                />
              </div>

              <div className="flex space-x-2 pt-2">
                <button
                  type="submit"
                  className="flex-1 py-2.5 rounded-xl bg-[#6750A4] text-white font-bold text-xs shadow hover:bg-[#523d8c]"
                >
                  Create Battle
                </button>
                <button
                  type="button"
                  onClick={() => setIsChallengeModalOpen(false)}
                  className="px-4 py-2.5 rounded-xl text-[#79747E] font-medium text-xs hover:text-[#1D1B20]"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
