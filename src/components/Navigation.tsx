import React from 'react';
import { ScrollyTab } from '../types';
import { Home, BarChart2, Zap, Ban, User } from 'lucide-react';

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

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-[#F3EDF7] border-t border-[#CAC4D0] rounded-t-[28px] shadow-lg max-w-md mx-auto">
      <div className="flex items-center justify-around h-16 px-2">
        {tabs.map((tab) => {
          const isSelected = currentTab === tab.id;
          const Icon = tab.icon;

          return (
            <button
              key={tab.id}
              onClick={() => onTabChange(tab.id)}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all relative ${
                isSelected ? 'text-[#1D192B]' : 'text-[#79747E] hover:text-[#49454F]'
              }`}
            >
              {isSelected && (
                <div className="absolute top-1 w-12 h-7 bg-[#E8DEF8] rounded-full -z-0" />
              )}
              <div className="relative z-10 flex flex-col items-center">
                <Icon
                  className={`w-5 h-5 transition-transform ${
                    isSelected ? 'scale-110 stroke-[2.4]' : 'scale-100 stroke-[1.8]'
                  }`}
                />
                <span
                  className={`text-[11px] mt-0.5 ${
                    isSelected ? 'font-bold text-[#1D192B]' : 'font-medium text-[#79747E]'
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
