import React, { useState, useEffect } from 'react';
import { ScrollyProvider, useScrolly } from './context/ScrollyContext';
import { ScrollyTab } from './types';
import { Navigation } from './components/Navigation';
import { NotchBarPreview } from './components/NotchBarPreview';
import { HomeScreen } from './screens/HomeScreen';
import { StatsScreen } from './screens/StatsScreen';
import { BattlesScreen } from './screens/BattlesScreen';
import { BlockScreen } from './screens/BlockScreen';
import { ProfileScreen } from './screens/ProfileScreen';
import { isNative, isAndroid } from './utils/capacitor';

const MainApp: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<ScrollyTab>('HOME');
  const { notchConfig } = useScrolly();

  useEffect(() => {
    const handler = (e: Event) => {
      const custom = e as CustomEvent<string>;
      const tab = custom.detail as ScrollyTab;
      if (['HOME','STATS','BATTLES','BLOCK','PROFILE'].includes(tab)) {
        setCurrentTab(tab);
      }
    };
    window.addEventListener('brainrot:openTab', handler as EventListener);
    return () => window.removeEventListener('brainrot:openTab', handler as EventListener);
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    const tab = params.get('tab') as ScrollyTab | null;
    if (tab && ['HOME','STATS','BATTLES','BLOCK','PROFILE'].includes(tab)) {
      setCurrentTab(tab);
      window.history.replaceState({}, '', window.location.pathname);
    }
  }, []);

  return (
    <div 
      className="min-h-screen bg-[#FEF7FF] text-[#1D1B20] flex flex-col justify-between relative selection:bg-[#EADDFF] selection:text-[#21005D]"
      style={{
        paddingTop: isNative ? 'env(safe-area-inset-top)' : undefined,
        paddingBottom: isNative ? 'env(safe-area-inset-bottom)' : undefined,
        minHeight: 'calc(var(--vh, 1vh) * 100)',
      }}
    >
      <NotchBarPreview />

      {notchConfig.showCutoutGuide && (
        <div className="fixed top-0 left-0 right-0 pointer-events-none z-40 flex justify-center" style={{ paddingTop: 'env(safe-area-inset-top)' }}>
          <div
            className="h-8 bg-red-500/20 border border-red-500/40 rounded-full flex items-center justify-center text-[10px] text-red-700 font-bold px-3 backdrop-blur-sm"
            style={{ width: `${Math.max(40, notchConfig.cutoutGapWidth || 60)}px` }}
          >
            Camera
          </div>
        </div>
      )}

      <main className="flex-1 w-full max-w-md mx-auto pt-14 pb-2" data-scrollable>
        {currentTab === 'HOME' && <HomeScreen />}
        {currentTab === 'STATS' && <StatsScreen />}
        {currentTab === 'BATTLES' && <BattlesScreen />}
        {currentTab === 'BLOCK' && <BlockScreen />}
        {currentTab === 'PROFILE' && <ProfileScreen />}
      </main>

      <div style={{ paddingBottom: isAndroid ? 'env(safe-area-inset-bottom)' : undefined }}>
        <Navigation currentTab={currentTab} onTabChange={setCurrentTab} />
      </div>

      {isNative && (
        <div className="fixed bottom-20 right-2 pointer-events-none opacity-20 text-[8px] font-mono">
          {isAndroid ? 'ANDROID' : 'NATIVE'} • v1.0
        </div>
      )}
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
