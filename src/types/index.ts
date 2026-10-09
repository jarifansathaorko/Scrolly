export type ScrollyTab = 'HOME' | 'STATS' | 'BATTLES' | 'BLOCK' | 'PROFILE';

export interface DailyStats {
  date: string; // YYYY-MM-DD
  totalScrolls: number;
  goal: number;
  isGoalMet: boolean;
  xpEarned: number;
}

export interface AppStats {
  packageName: string;
  appName: string;
  scrollCount: number;
}

export interface AppLimit {
  packageName: string;
  appName: string;
  dailyLimit: number;
  warningThreshold: number;
  isBlocked: boolean;
  isEnabled: boolean;
}

export interface Achievement {
  id: string;
  title: string;
  description: string;
  iconName: string;
  targetValue: number;
  currentValue: number;
  isUnlocked: boolean;
  unlockedAt: number | null;
}

export interface FriendBattle {
  id: string;
  friendName: string;
  friendUsername: string;
  userScrolls: number;
  friendScrolls: number;
  date: string;
  status: 'WINNING' | 'LOSING' | 'TIED' | 'COMPLETED';
}

export interface UserProfile {
  username: string;
  streakDays: number;
  bestStreak: number;
  totalScrollsAllTime: number;
  battlesWon: number;
  battlesLost: number;
  currentLevel: number;
  currentXp: number;
  xpForNextLevel: number;
}

export interface HealthyChallenge {
  id: string;
  title: string;
  description: string;
  rewardScrolls: number;
  rewardXp: number;
  iconName: string;
  durationMinutes: number;
  isCompleted: boolean;
}

export type NotchShape =
  | 'PUNCH_HOLE_CENTER'
  | 'PUNCH_HOLE_LEFT'
  | 'PUNCH_HOLE_RIGHT'
  | 'WIDE_NOTCH'
  | 'WATER_DROP'
  | 'NO_NOTCH';

export type IslandPlacementMode = 'WRAP_AROUND_NOTCH' | 'BELOW_NOTCH';

export type NotchType =
  | 'DYNAMIC_ISLAND'
  | 'ROUND_PUNCH_HOLE_CENTER'
  | 'WIDE_NOTCH'
  | 'WATERDROP_TEARDROP'
  | 'PUNCH_HOLE_LEFT'
  | 'PUNCH_HOLE_RIGHT'
  | 'BELOW_STATUS_BAR';

export interface HardwareCutoutInfo {
  hasCutout: boolean;
  notchShape: NotchShape;
  centerX: number;
  top: number;
  bottom: number;
  width: number;
  height: number;
  safeInsetTop: number;
}

export interface NotchConfiguration {
  notchType: NotchType;
  placementMode: IslandPlacementMode;
  offsetX: number;
  offsetY: number;
  cutoutGapWidth: number;
  isDynamicIslandMode: boolean;
  showCutoutGuide: boolean;
  autoAdjusted: boolean;
  detectedCutout: HardwareCutoutInfo;
}

export type StatsTimeframe = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

export interface ChartBarData {
  label: string;
  subLabel?: string;
  count: number;
  isCurrent?: boolean;
}

export interface AppUsageItem {
  packageName: string;
  appName: string;
  count: number;
  fractionOfTotal: number;
}
