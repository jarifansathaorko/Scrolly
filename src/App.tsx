import React, { useState } from 'react';
import { ScrollyProvider, useScrolly } from './context/ScrollyContext';
import { ScrollyTab } from './types';
import { Navigation } from './components/Navigation';
import { NotchBarPreview } from './components/NotchBarPreview';
import { HomeScreen } from './screens/HomeScreen';
import { StatsScreen } from './screens/StatsScreen';
import { BattlesScreen } from './screens/BattlesScreen';
import { BlockScreen } from './screens/BlockScreen';
import { ProfileScreen } from './screens/ProfileScreen';

const MainApp: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<ScrollyTab>('HOME');
  const { notchConfig } = useScrolly();

  return (
    <div className="min-h-screen bg-[#FEF7FF] text-[#1D1B20] flex flex-col justify-between relative selection:bg-[#EADDFF] selection:text-[#21005D]">
      {/* Dynamic Island Floating Bar */}
      <NotchBarPreview />

      {/* Simulated hardware cutout guide if enabled */}
      {notchConfig.showCutoutGuide && (
        <div className="fixed top-0 left-0 right-0 pointer-events-none z-40 flex justify-center">
          <div
            className="h-8 bg-red-500/20 border border-red-500/40 rounded-full flex items-center justify-center text-[10px] text-red-700 font-bold px-3"
            style={{ width: `${Math.max(40, notchConfig.cutoutGapWidth || 60)}px` }}
          >
            Camera
          </div>
        </div>
      )}

      {/* Screen Content */}
      <main className="flex-1 w-full max-w-md mx-auto pt-14">
        {currentTab === 'HOME' && <HomeScreen />}
        {currentTab === 'STATS' && <StatsScreen />}
        {currentTab === 'BATTLES' && <BattlesScreen />}
        {currentTab === 'BLOCK' && <BlockScreen />}
        {currentTab === 'PROFILE' && <ProfileScreen />}
      </main>

      {/* Bottom Navigation */}
      <Navigation currentTab={currentTab} onTabChange={setCurrentTab} />
    </div>
  );
};

export default function App() {
  return (
    <ScrollyProvider>
      <MainApp />
    </ScrollyProvider>
  );
}
