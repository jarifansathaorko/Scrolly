import React, { createContext, useContext, useState, useEffect } from 'react';
import confetti from 'canvas-confetti';
import {
  DailyStats,
  AppStats,
  AppLimit,
  Achievement,
  FriendBattle,
  UserProfile,
  HealthyChallenge,
  NotchConfiguration,
  NotchType,
  IslandPlacementMode,
} from '../types';
import { getTodayKey, getDayOffsetKey } from '../utils/dateKeys';

const DEFAULT_LIMITS: AppLimit[] = [
  {
    packageName: 'com.instagram.android',
    appName: 'Instagram',
    dailyLimit: 100,
    warningThreshold: 80,
    isBlocked: false,
    isEnabled: true,
  },
  {
    packageName: 'com.google.android.youtube',
    appName: 'Shorts',
    dailyLimit: 80,
    warningThreshold: 64,
    isBlocked: false,
    isEnabled: true,
  },
  {
    packageName: 'com.zhiliaoapp.musically',
    appName: 'TikTok',
    dailyLimit: 60,
    warningThreshold: 48,
    isBlocked: false,
    isEnabled: true,
  },
  {
    packageName: 'com.spotify.music',
    appName: 'Spotify',
    dailyLimit: 120,
    warningThreshold: 96,
    isBlocked: false,
    isEnabled: false,
  },
  {
    packageName: 'com.facebook.katana',
    appName: 'Facebook',
    dailyLimit: 50,
    warningThreshold: 40,
    isBlocked: false,
    isEnabled: true,
  },
];

const DEFAULT_CHALLENGES: HealthyChallenge[] = [
  {
    id: 'walk_5',
    title: 'Touch Grass Walk',
    description: 'Step outside or walk around for 5 minutes without checking screens.',
    rewardScrolls: 15,
    rewardXp: 50,
    iconName: 'walk',
    durationMinutes: 5,
    isCompleted: false,
  },
  {
    id: 'phone_down_10',
    title: 'Phone-Down Reset',
    description: 'Place your phone face down on the table for 10 peaceful minutes.',
    rewardScrolls: 20,
    rewardXp: 75,
    iconName: 'phone',
    durationMinutes: 10,
    isCompleted: false,
  },
  {
    id: 'deep_breaths',
    title: 'Box Breathing Reset',
    description: 'Take 8 deep intentional breaths to calm attention and reset dopamine.',
    rewardScrolls: 10,
    rewardXp: 30,
    iconName: 'wind',
    durationMinutes: 2,
    isCompleted: false,
  },
  {
    id: 'stretch_3',
    title: 'Desk Posture Stretch',
    description: 'Release neck and thumb strain with gentle shoulder rolls & wrist stretches.',
    rewardScrolls: 12,
    rewardXp: 40,
    iconName: 'stretch',
    durationMinutes: 3,
    isCompleted: false,
  },
];

const DEFAULT_ACHIEVEMENTS: Achievement[] = [
  {
    id: 'first_step',
    title: 'First Step',
    description: 'Track your first day of reels without exceeding daily goal.',
    iconName: 'flag',
    targetValue: 1,
    currentValue: 1,
    isUnlocked: true,
    unlockedAt: Date.now() - 86400000 * 7,
  },
  {
    id: 'streak_3',
    title: 'Three-Peat',
    description: 'Maintain a 3-day conscious limit streak.',
    iconName: 'flame',
    targetValue: 3,
    currentValue: 3,
    isUnlocked: true,
    unlockedAt: Date.now() - 86400000 * 4,
  },
  {
    id: 'streak_7',
    title: 'Mindful Monk',
    description: 'Reach a full 7-day under-limit streak.',
    iconName: 'crown',
    targetValue: 7,
    currentValue: 7,
    isUnlocked: true,
    unlockedAt: Date.now() - 86400000,
  },
  {
    id: 'battle_champ',
    title: 'Duel Master',
    description: 'Win 20 scroll battles against friends.',
    iconName: 'swords',
    targetValue: 20,
    currentValue: 23,
    isUnlocked: true,
    unlockedAt: Date.now() - 86400000 * 2,
  },
  {
    id: 'zen_reset',
    title: 'Grass Toucher',
    description: 'Complete 15 healthy reset challenges instead of doomscrolling.',
    iconName: 'sprout',
    targetValue: 15,
    currentValue: 11,
    isUnlocked: false,
    unlockedAt: null,
  },
  {
    id: 'streak_14',
    title: 'Dopamine Detached',
    description: 'Maintain a 14-day conscious limit streak.',
    iconName: 'sparkles',
    targetValue: 14,
    currentValue: 7,
    isUnlocked: false,
    unlockedAt: null,
  },
];

const DEFAULT_BATTLES: FriendBattle[] = [
  {
    id: 'battle_1',
    friendName: 'Alex Rivera',
    friendUsername: 'arivera',
    userScrolls: 36,
    friendScrolls: 93,
    date: getTodayKey(),
    status: 'WINNING',
  },
  {
    id: 'battle_2',
    friendName: 'Sarah Chen',
    friendUsername: 'schen_ai',
    userScrolls: 36,
    friendScrolls: 24,
    date: getTodayKey(),
    status: 'LOSING',
  },
  {
    id: 'battle_3',
    friendName: 'Marcus Vance',
    friendUsername: 'mvance',
    userScrolls: 36,
    friendScrolls: 36,
    date: getTodayKey(),
    status: 'TIED',
  },
];

const DEFAULT_PROFILE: UserProfile = {
  username: 'aorko',
  streakDays: 7,
  bestStreak: 11,
  totalScrollsAllTime: 14832,
  battlesWon: 23,
  battlesLost: 17,
  currentLevel: 8,
  currentXp: 3450,
  xpForNextLevel: 4000,
};

const DEFAULT_NOTCH_CONFIG: NotchConfiguration = {
  notchType: 'DYNAMIC_ISLAND',
  placementMode: 'BELOW_NOTCH',
  offsetX: 0,
  offsetY: 38,
  cutoutGapWidth: 0,
  isDynamicIslandMode: true,
  showCutoutGuide: false,
  autoAdjusted: false,
  detectedCutout: {
    hasCutout: true,
    notchShape: 'PUNCH_HOLE_CENTER',
    centerX: 0,
    top: 6,
    bottom: 34,
    width: 32,
    height: 28,
    safeInsetTop: 36,
  },
};

interface ScrollyContextType {
  todayStats: DailyStats;
  appStats: AppStats[];
  limits: AppLimit[];
  profile: UserProfile;
  battles: FriendBattle[];
  achievements: Achievement[];
  challenges: HealthyChallenge[];
  notchConfig: NotchConfiguration;
  isNotchPreviewVisible: boolean;
  selectedAppForSim: string;
  isServiceActive: boolean;
  isOverlayPermissionGranted: boolean;
  isBatteryOptimizationIgnored: boolean;
  showBatteryDialog: boolean;
  showOverlayDialog: boolean;
  activeInterventionApp: string | null;
  historyDailyStats: Record<string, DailyStats>;
  historyAppStats: Record<string, AppStats[]>;
  setSelectedAppForSim: (pkg: string) => void;
  simulateScroll: (packageName: string, count?: number) => void;
  toggleNotchBarPreview: () => void;
  setNotchBarPreviewVisible: (visible: boolean) => void;
  updateLimit: (packageName: string, newLimit: number, warningThreshold: number, isEnabled: boolean) => void;
  completeChallenge: (challengeId: string) => void;
  updateUsername: (newUsername: string) => void;
  addFriendBattle: (friendName: string, friendUsername: string) => void;
  updateNotchConfig: (configPartial: Partial<NotchConfiguration>) => void;
  autoCalibrateNotch: () => void;
  resetNotchDefaults: () => void;
  openAccessibilitySettings: () => void;
  requestOverlayPermission: () => void;
  requestIgnoreBatteryOptimization: () => void;
  dismissBatteryDialog: () => void;
  dismissOverlayDialog: () => void;
  dismissIntervention: () => void;
  triggerInterventionPreview: (appName: string) => void;
  grantBonusScrolls: (packageName: string, bonus: number) => void;
  resetAllData: () => void;
}

const ScrollyContext = createContext<ScrollyContextType | undefined>(undefined);

export const ScrollyProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const todayKey = getTodayKey();

  // Load state from localStorage or use defaults
  const [todayStats, setTodayStats] = useState<DailyStats>(() => {
    const saved = localStorage.getItem('scrolly_today_stats');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        if (parsed.date === todayKey) return parsed;
      } catch (e) {
        console.error(e);
      }
    }
    return {
      date: todayKey,
      totalScrolls: 36,
      goal: 100,
      isGoalMet: false,
      xpEarned: 80,
    };
  });

  const [appStats, setAppStats] = useState<AppStats[]>(() => {
    const saved = localStorage.getItem('scrolly_app_stats');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
    return [
      { packageName: 'com.instagram.android', appName: 'Instagram', scrollCount: 18 },
      { packageName: 'com.google.android.youtube', appName: 'Shorts', scrollCount: 10 },
      { packageName: 'com.zhiliaoapp.musically', appName: 'TikTok', scrollCount: 6 },
      { packageName: 'com.spotify.music', appName: 'Spotify', scrollCount: 0 },
      { packageName: 'com.facebook.katana', appName: 'Facebook', scrollCount: 2 },
    ];
  });

  const [limits, setLimits] = useState<AppLimit[]>(() => {
    const saved = localStorage.getItem('scrolly_limits');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
    return DEFAULT_LIMITS;
  });

  const [profile, setProfile] = useState<UserProfile>(() => {
    const saved = localStorage.getItem('scrolly_profile');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
    return DEFAULT_PROFILE;
  });

  const [battles, setBattles] = useState<FriendBattle[]>(() => {
    const saved = localStorage.getItem('scrolly_battles');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
    return DEFAULT_BATTLES;
  });

  const [achievements, setAchievements] = useState<Achievement[]>(() => {
    const saved = localStorage.getItem('scrolly_achievements');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
    return DEFAULT_ACHIEVEMENTS;
  });

  const [challenges, setChallenges] = useState<HealthyChallenge[]>(DEFAULT_CHALLENGES);

  const [notchConfig, setNotchConfig] = useState<NotchConfiguration>(() => {
    const saved = localStorage.getItem('scrolly_notch_config');
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
    return DEFAULT_NOTCH_CONFIG;
  });

  const [isNotchPreviewVisible, setIsNotchPreviewVisible] = useState<boolean>(true);
  const [selectedAppForSim, setSelectedAppForSim] = useState<string>('com.instagram.android');

  // Permissions simulation
  const [isServiceActive, setIsServiceActive] = useState<boolean>(true);
  const [isOverlayPermissionGranted, setIsOverlayPermissionGranted] = useState<boolean>(true);
  const [isBatteryOptimizationIgnored, setIsBatteryOptimizationIgnored] = useState<boolean>(false);
  const [showBatteryDialog, setShowBatteryDialog] = useState<boolean>(false);
  const [showOverlayDialog, setShowOverlayDialog] = useState<boolean>(false);

  // Active intervention modal
  const [activeInterventionApp, setActiveInterventionApp] = useState<string | null>(null);

  // Generate historical data for Stats screen
  const [historyDailyStats] = useState<Record<string, DailyStats>>(() => {
    const history: Record<string, DailyStats> = {};
    const sampleCounts = [48, 62, 35, 95, 78, 54, 36];
    for (let i = 6; i >= 0; i--) {
      const dKey = getDayOffsetKey(-i);
      const scrolls = i === 0 ? todayStats.totalScrolls : sampleCounts[6 - i] || 50;
      history[dKey] = {
        date: dKey,
        totalScrolls: scrolls,
        goal: 100,
        isGoalMet: scrolls <= 100,
        xpEarned: Math.min(100, Math.floor(scrolls * 1.5)),
      };
    }
    return history;
  });

  const [historyAppStats] = useState<Record<string, AppStats[]>>(() => {
    const history: Record<string, AppStats[]> = {};
    for (let i = 6; i >= 0; i--) {
      const dKey = getDayOffsetKey(-i);
      history[dKey] = [
        { packageName: 'com.instagram.android', appName: 'Instagram', scrollCount: 22 + (i * 3) },
        { packageName: 'com.google.android.youtube', appName: 'Shorts', scrollCount: 14 + i },
        { packageName: 'com.zhiliaoapp.musically', appName: 'TikTok', scrollCount: 8 + i * 2 },
        { packageName: 'com.spotify.music', appName: 'Spotify', scrollCount: 2 },
        { packageName: 'com.facebook.katana', appName: 'Facebook', scrollCount: 4 },
      ];
    }
    return history;
  });

  // Save changes to localStorage
  useEffect(() => {
    localStorage.setItem('scrolly_today_stats', JSON.stringify(todayStats));
  }, [todayStats]);

  useEffect(() => {
    localStorage.setItem('scrolly_app_stats', JSON.stringify(appStats));
  }, [appStats]);

  useEffect(() => {
    localStorage.setItem('scrolly_limits', JSON.stringify(limits));
  }, [limits]);

  useEffect(() => {
    localStorage.setItem('scrolly_profile', JSON.stringify(profile));
  }, [profile]);

  useEffect(() => {
    localStorage.setItem('scrolly_battles', JSON.stringify(battles));
  }, [battles]);

  useEffect(() => {
    localStorage.setItem('scrolly_achievements', JSON.stringify(achievements));
  }, [achievements]);

  useEffect(() => {
    localStorage.setItem('scrolly_notch_config', JSON.stringify(notchConfig));
  }, [notchConfig]);

  // Simulate scroll on a selected app
  const simulateScroll = (packageName: string, count: number = 1) => {
    let updatedAppScroll = 0;
    let targetAppName = 'App';

    setAppStats((prev) =>
      prev.map((app) => {
        if (app.packageName === packageName) {
          updatedAppScroll = app.scrollCount + count;
          targetAppName = app.appName;
          return { ...app, scrollCount: updatedAppScroll };
        }
        return app;
      })
    );

    const newTotal = todayStats.totalScrolls + count;
    setTodayStats((prev) => ({
      ...prev,
      totalScrolls: newTotal,
      isGoalMet: newTotal <= prev.goal,
    }));

    setProfile((prev) => ({
      ...prev,
      totalScrollsAllTime: prev.totalScrollsAllTime + count,
      currentXp: prev.currentXp + Math.max(1, Math.floor(count * 0.5)),
    }));

    // Update battles user scroll count
    setBattles((prev) =>
      prev.map((b) => {
        const nextUser = newTotal;
        const status =
          nextUser < b.friendScrolls
            ? 'WINNING'
            : nextUser > b.friendScrolls
            ? 'LOSING'
            : 'TIED';
        return {
          ...b,
          userScrolls: nextUser,
          status,
        };
      })
    );

    // Check limit enforcement for this app
    const appLimit = limits.find((l) => l.packageName === packageName);
    if (appLimit && appLimit.isEnabled && updatedAppScroll >= appLimit.dailyLimit) {
      setActiveInterventionApp(targetAppName);
    }
  };

  const toggleNotchBarPreview = () => {
    setIsNotchPreviewVisible((prev) => !prev);
  };

  const setNotchBarPreviewVisible = (visible: boolean) => {
    setIsNotchPreviewVisible(visible);
  };

  const updateLimit = (
    packageName: string,
    newLimit: number,
    warningThreshold: number,
    isEnabled: boolean
  ) => {
    setLimits((prev) =>
      prev.map((item) =>
        item.packageName === packageName
          ? {
              ...item,
              dailyLimit: newLimit,
              warningThreshold,
              isEnabled,
            }
          : item
      )
    );
  };

  const completeChallenge = (challengeId: string) => {
    const ch = challenges.find((c) => c.id === challengeId);
    if (!ch) return;

    confetti({
      particleCount: 50,
      spread: 60,
      origin: { y: 0.7 },
    });

    setChallenges((prev) =>
      prev.map((c) => (c.id === challengeId ? { ...c, isCompleted: true } : c))
    );

    setProfile((prev) => {
      const nextXp = prev.currentXp + ch.rewardXp;
      const leveledUp = nextXp >= prev.xpForNextLevel;
      return {
        ...prev,
        currentXp: leveledUp ? nextXp - prev.xpForNextLevel : nextXp,
        currentLevel: leveledUp ? prev.currentLevel + 1 : prev.currentLevel,
      };
    });

    // Expand daily goal by rewardScrolls
    setTodayStats((prev) => ({
      ...prev,
      goal: prev.goal + ch.rewardScrolls,
    }));
  };

  const grantBonusScrolls = (packageName: string, bonus: number) => {
    setLimits((prev) =>
      prev.map((l) =>
        l.packageName === packageName ? { ...l, dailyLimit: l.dailyLimit + bonus } : l
      )
    );
    setTodayStats((prev) => ({
      ...prev,
      goal: prev.goal + bonus,
    }));
  };

  const updateUsername = (newUsername: string) => {
    if (!newUsername.trim()) return;
    setProfile((prev) => ({ ...prev, username: newUsername.trim() }));
  };

  const addFriendBattle = (friendName: string, friendUsername: string) => {
    const randomFriendScore = Math.floor(Math.random() * 60) + 20;
    const newBattle: FriendBattle = {
      id: `battle_${Date.now()}`,
      friendName,
      friendUsername,
      userScrolls: todayStats.totalScrolls,
      friendScrolls: randomFriendScore,
      date: todayKey,
      status: todayStats.totalScrolls < randomFriendScore ? 'WINNING' : 'LOSING',
    };
    setBattles((prev) => [newBattle, ...prev]);
  };

  const updateNotchConfig = (configPartial: Partial<NotchConfiguration>) => {
    setNotchConfig((prev) => ({ ...prev, ...configPartial }));
  };

  const autoCalibrateNotch = () => {
    setNotchConfig((prev) => ({
      ...prev,
      notchType: 'ROUND_PUNCH_HOLE_CENTER',
      placementMode: 'WRAP_AROUND_NOTCH',
      offsetX: 0,
      offsetY: 6,
      cutoutGapWidth: 40,
      autoAdjusted: true,
      detectedCutout: {
        hasCutout: true,
        notchShape: 'PUNCH_HOLE_CENTER',
        centerX: 0,
        top: 6,
        bottom: 34,
        width: 32,
        height: 28,
        safeInsetTop: 36,
      },
    }));
  };

  const resetNotchDefaults = () => {
    setNotchConfig(DEFAULT_NOTCH_CONFIG);
  };

  const openAccessibilitySettings = () => {
    setIsServiceActive(true);
  };

  const requestOverlayPermission = () => {
    setIsOverlayPermissionGranted(true);
    setShowOverlayDialog(false);
  };

  const requestIgnoreBatteryOptimization = () => {
    setIsBatteryOptimizationIgnored(true);
    setShowBatteryDialog(false);
  };

  const dismissBatteryDialog = () => setShowBatteryDialog(false);
  const dismissOverlayDialog = () => setShowOverlayDialog(false);
  const dismissIntervention = () => setActiveInterventionApp(null);
  const triggerInterventionPreview = (appName: string) => setActiveInterventionApp(appName);

  const resetAllData = () => {
    localStorage.clear();
    setTodayStats({
      date: todayKey,
      totalScrolls: 0,
      goal: 100,
      isGoalMet: true,
      xpEarned: 0,
    });
    setAppStats([
      { packageName: 'com.instagram.android', appName: 'Instagram', scrollCount: 0 },
      { packageName: 'com.google.android.youtube', appName: 'Shorts', scrollCount: 0 },
      { packageName: 'com.zhiliaoapp.musically', appName: 'TikTok', scrollCount: 0 },
      { packageName: 'com.spotify.music', appName: 'Spotify', scrollCount: 0 },
      { packageName: 'com.facebook.katana', appName: 'Facebook', scrollCount: 0 },
    ]);
    setLimits(DEFAULT_LIMITS);
    setProfile(DEFAULT_PROFILE);
    setBattles(DEFAULT_BATTLES);
    setAchievements(DEFAULT_ACHIEVEMENTS);
    setNotchConfig(DEFAULT_NOTCH_CONFIG);
  };

  return (
    <ScrollyContext.Provider
      value={{
        todayStats,
        appStats,
        limits,
        profile,
        battles,
        achievements,
        challenges,
        notchConfig,
        isNotchPreviewVisible,
        selectedAppForSim,
        isServiceActive,
        isOverlayPermissionGranted,
        isBatteryOptimizationIgnored,
        showBatteryDialog,
        showOverlayDialog,
        activeInterventionApp,
        historyDailyStats,
        historyAppStats,
        setSelectedAppForSim,
        simulateScroll,
        toggleNotchBarPreview,
        setNotchBarPreviewVisible,
        updateLimit,
        completeChallenge,
        updateUsername,
        addFriendBattle,
        updateNotchConfig,
        autoCalibrateNotch,
        resetNotchDefaults,
        openAccessibilitySettings,
        requestOverlayPermission,
        requestIgnoreBatteryOptimization,
        dismissBatteryDialog,
        dismissOverlayDialog,
        dismissIntervention,
        triggerInterventionPreview,
        grantBonusScrolls,
        resetAllData,
      }}
    >
      {children}
    </ScrollyContext.Provider>
  );
};

export const useScrolly = () => {
  const context = useContext(ScrollyContext);
  if (!context) {
    throw new Error('useScrolly must be used within a ScrollyProvider');
  }
  return context;
};
