import React from 'react';
import { ScrollyTab } from '../types';
import { Home, BarChart2, Zap, Ban, User } from 'lucide-react';
import { triggerHaptic } from '../utils/capacitor';

interface NavigationProps {
  currentTab: ScrollyTab;
  onTabChange: (tab: ScrollyTab) => void;
}

export const Navigation: React.FC<NavigationProps> = ({ currentTab, onTabChange }) => {
  const tabs: { id: ScrollyTab; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { id: 'HOME', label: 'Home', icon: Home },
    { id: 'STATS', label: 'Stats', icon: BarChart2 },
    { id: 'BATTLES', label: 'Battles', icon: Zap },
    { id: 'BLOCK', label: 'Block', icon: Ban },
    { id: 'PROFILE', label: 'Profile', icon: User },
  ];

  const handleTabChange = (tab: ScrollyTab) => {
    if (tab !== currentTab) {
      triggerHaptic('light');
    }
    onTabChange(tab);
  };

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#F3EDF7]/95 dark:bg-[#2B2930]/95 backdrop-blur-xl border-t border-[#CAC4D0] dark:border-[#49454F] rounded-t-[28px] shadow-[0_-4px_24px_rgba(0,0,0,0.08)] dark:shadow-[0_-4px_24px_rgba(0,0,0,0.3)] max-w-md mx-auto transition-colors">
      <div className="flex items-center justify-around h-[68px] px-2 pb-1" style={{ paddingBottom: 'max(4px, env(safe-area-inset-bottom))' }}>
        {tabs.map((tab) => {
          const isSelected = currentTab === tab.id;
          const Icon = tab.icon;

          return (
            <button
              key={tab.id}
              onClick={() => handleTabChange(tab.id)}
              className={`flex flex-col items-center justify-center flex-1 py-1.5 transition-all relative active:scale-95 ${
                isSelected ? 'text-[#1D192B] dark:text-[#EADDFF]' : 'text-[#79747E] dark:text-[#938F99] hover:text-[#49454F] dark:hover:text-[#CAC4D0]'
              }`}
            >
              {isSelected && (
                <div className="absolute top-1 w-12 h-7 bg-[#E8DEF8] dark:bg-[#4F378B] rounded-full -z-0 animate-fadeIn" />
              )}
              <div className="relative z-10 flex flex-col items-center">
                <Icon
                  className={`w-5 h-5 transition-all duration-200 ${
                    isSelected ? 'scale-110 stroke-[2.4]' : 'scale-100 stroke-[1.8]'
                  }`}
                />
                <span
                  className={`text-[11px] mt-0.5 transition-all ${
                    isSelected ? 'font-bold text-[#1D192B] dark:text-[#EADDFF]' : 'font-medium text-[#79747E] dark:text-[#938F99]'
                  }`}
                >
                  {tab.label}
                </span>
              </div>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
