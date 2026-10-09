# Scrolly

> See how much you scroll. Take back control with playful gamified tracking, brain mascot states, app breakdown, and friend battles.

Scrolly has been rewritten as a modern, high-performance React application on Vite and Tailwind CSS, preserving all core business logic and features from the original Android app.

## Features

- **Brain Mascot (`ScrollyCharacter`)**: 6 dynamic expressive states based on daily scrolling:
  - *Super Fresh* (0–20 reels): clear crystal mind, sparkling anime eyes
  - *Cruising* (21–50 reels): chill, mindful
  - *Dazed* (51–100 reels): wide-eyed, gentle reminder
  - *Brain Fried* (101–200 reels): dizzy spiral eyes, sweat drop
  - *Completely Cooked* (201–500 reels): X eyes, open tongue mouth
  - *Nuclear Brainrot* (501+ reels): emergency dopamine overload
- **Interactive Dynamic Island Overlay (`NotchBarPreview`)**:
  - Live floating capsule at top of display
  - Real-time scroll counter with bounce animation and status pulse
  - Expandable into full dynamic card with progress bar and remaining scroll allowances
- **Hardware Notch Finder & Auto-Adjuster**:
  - Notch profile configurations (iPhone Dynamic Island, Center Punch-Hole, Wide Notch, Waterdrop, Corner Punch-Holes, Below Status Bar)
  - Placement modes (*Wrap Around Notch* vs *Below Notch*)
  - Gap width and X/Y offset fine-tuning
- **Active Apps Tracking**:
  - Live breakdown across Instagram, YouTube Shorts, TikTok, Spotify, and Facebook
  - Real-time simulation tester (+1, +5, +20)
- **Daily App Limits & Interceptor (`BlockScreen`)**:
  - Custom scroll limit sliders (10 to 200 reels/day)
  - Full-screen intervention overlay when daily limits are exceeded
  - Earn extra scrolls by completing healthy offline resets (Touch Grass Walk, Box Breathing, Phone-Down Reset)
- **Friend Battles (`BattlesScreen`)**:
  - Head-to-head scroll duels (lower scroll count wins!)
  - Live duel card with status banner (Winning, Behind, Tied)
  - Challenge friends and send mindful nudges
  - Daily attention leaderboard
- **Analytics & History (`StatsScreen`)**:
  - Day, Week, Month, and Year timeframe breakdown
  - Quarter-hour daily segments, day-by-day weekly comparison
  - App distribution share and mindful insights
- **Profile & Gamification (`ProfileScreen`)**:
  - Multi-day streak tracking
  - All-time scroll counts and battle win rate
  - Achievements gallery with unlock criteria and progress bars

## Tech Stack

- React 19 + TypeScript
- Vite 6
- Tailwind CSS 4
- Lucide React & Canvas Confetti
