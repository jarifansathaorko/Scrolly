import React from 'react';

export interface MascotStateInfo {
  type: 'FRESH' | 'CHILL' | 'DAZED' | 'FRIED' | 'COOKED' | 'NUCLEAR';
  title: string;
  quote: string;
  minScrolls: number;
  maxScrolls: number;
  baseColor: string;
  cheekColor: string;
}

export const MASCOT_STATES: Record<string, MascotStateInfo> = {
  FRESH: {
    type: 'FRESH',
    title: 'Super Fresh',
    quote: 'Brain is crystal clear! Ready to conquer the day.',
    minScrolls: 0,
    maxScrolls: 20,
    baseColor: '#FF9E8E',
    cheekColor: '#FF6350',
  },
  CHILL: {
    type: 'CHILL',
    title: 'Cruising',
    quote: 'Cruising along nicely. Keep it intentional!',
    minScrolls: 21,
    maxScrolls: 50,
    baseColor: '#FFAE75',
    cheekColor: '#FF7A45',
  },
  DAZED: {
    type: 'DAZED',
    title: 'Dazed',
    quote: "You're doing okay. Maybe touch some grass 😌",
    minScrolls: 51,
    maxScrolls: 100,
    baseColor: '#FFBC60',
    cheekColor: '#EE8A2A',
  },
  FRIED: {
    type: 'FRIED',
    title: 'Brain Fried',
    quote: 'Thumb is getting overtime. Your focus is melting! ⚡',
    minScrolls: 101,
    maxScrolls: 200,
    baseColor: '#E58867',
    cheekColor: '#C75530',
  },
  COOKED: {
    type: 'COOKED',
    title: 'Completely Cooked',
    quote: 'Your brain is well-done. Put the phone down! 🍳',
    minScrolls: 201,
    maxScrolls: 500,
    baseColor: '#B5705C',
    cheekColor: '#8A4030',
  },
  NUCLEAR: {
    type: 'NUCLEAR',
    title: 'Nuclear Brainrot',
    quote: 'Emergency! Dopamine overload catastrophic level! 💀',
    minScrolls: 501,
    maxScrolls: Infinity,
    baseColor: '#82544B',
    cheekColor: '#5A2E26',
  },
};

export function getMascotState(scrollCount: number): MascotStateInfo {
  if (scrollCount <= 20) return MASCOT_STATES.FRESH;
  if (scrollCount <= 50) return MASCOT_STATES.CHILL;
  if (scrollCount <= 100) return MASCOT_STATES.DAZED;
  if (scrollCount <= 200) return MASCOT_STATES.FRIED;
  if (scrollCount <= 500) return MASCOT_STATES.COOKED;
  return MASCOT_STATES.NUCLEAR;
}

interface ScrollyCharacterProps {
  scrollCount: number;
  size?: number | string;
  animated?: boolean;
  className?: string;
}

export const ScrollyCharacter: React.FC<ScrollyCharacterProps> = ({
  scrollCount,
  size = 140,
  animated = true,
  className = '',
}) => {
  const state = getMascotState(scrollCount);

  return (
    <div
      className={`inline-flex items-center justify-center relative select-none ${
        animated ? 'animate-[bounce_2.5s_infinite_ease-in-out]' : ''
      } ${className}`}
      style={{
        width: typeof size === 'number' ? `${size}px` : size,
        height: typeof size === 'number' ? `${size}px` : size,
      }}
    >
      <svg
        viewBox="0 0 140 140"
        className="w-full h-full drop-shadow-md overflow-visible"
      >
        <defs>
          <radialGradient id={`glow-${state.type}`} cx="50%" cy="50%" r="50%">
            <stop offset="0%" stopColor={state.baseColor} stopOpacity="0.4" />
            <stop offset="100%" stopColor={state.baseColor} stopOpacity="0" />
          </radialGradient>
        </defs>

        {/* Aura ambient backglow */}
        <circle cx="70" cy="70" r="62" fill={`url(#glow-${state.type})`} />

        {/* Brain Body Outline Path matching Android drawBrainBody */}
        <path
          d="M 70 32
             C 97 18.5, 126 50, 119.3 70
             C 130.5 99, 101.4 121.5, 81.2 112.5
             C 70 119.3, 58.8 112.5, 58.8 112.5
             C 38.6 121.5, 9.5 99, 20.7 70
             C 14 50, 43 18.5, 70 32 Z"
          fill={state.baseColor}
          stroke="#1D1B20"
          strokeWidth="3.5"
          strokeLinejoin="round"
        />

        {/* Inner Brain Wrinkle Grooves */}
        <path
          d="M 39 53 A 11 11 0 0 1 61 46"
          fill="none"
          stroke={state.cheekColor}
          strokeWidth="3.5"
          strokeLinecap="round"
          strokeOpacity="0.8"
        />
        <path
          d="M 79 46 A 11 11 0 0 1 101 53"
          fill="none"
          stroke={state.cheekColor}
          strokeWidth="3.5"
          strokeLinecap="round"
          strokeOpacity="0.8"
        />
        <path
          d="M 68 38 Q 70 54 72 65"
          fill="none"
          stroke={state.cheekColor}
          strokeWidth="2.5"
          strokeLinecap="round"
          strokeOpacity="0.6"
        />

        {/* Cheeks */}
        <circle cx="35" cy="87" r="7.7" fill={state.cheekColor} fillOpacity="0.75" />
        <circle cx="105" cy="87" r="7.7" fill={state.cheekColor} fillOpacity="0.75" />

        {/* Eyes & Facial Expression */}
        {state.type === 'FRESH' || state.type === 'CHILL' ? (
          <>
            {/* Left Eye */}
            <circle cx="48" cy="71" r="10.5" fill="#1D1B20" />
            <circle cx="45" cy="68" r="4.4" fill="#FFFFFF" />
            <circle cx="51.5" cy="74.5" r="2.1" fill="#FFFFFF" />

            {/* Right Eye */}
            <circle cx="92" cy="71" r="10.5" fill="#1D1B20" />
            <circle cx="89" cy="68" r="4.4" fill="#FFFFFF" />
            <circle cx="95.5" cy="74.5" r="2.1" fill="#FFFFFF" />

            {/* Sweet Smile */}
            <path
              d="M 62 87 Q 70 95 78 87"
              fill="none"
              stroke="#1D1B20"
              strokeWidth="4"
              strokeLinecap="round"
            />
          </>
        ) : state.type === 'DAZED' ? (
          <>
            {/* Left Eye (wide) */}
            <circle cx="48" cy="71" r="11.5" fill="#1D1B20" />
            <circle cx="47" cy="69" r="3.5" fill="#FFFFFF" />

            {/* Right Eye (wide) */}
            <circle cx="92" cy="71" r="11.5" fill="#1D1B20" />
            <circle cx="91" cy="69" r="3.5" fill="#FFFFFF" />

            {/* Tiny 'o' mouth */}
            <circle cx="70" cy="89" r="4.5" fill="#1D1B20" />
          </>
        ) : state.type === 'FRIED' ? (
          <>
            {/* Spiral / Dizzy Left Eye */}
            <circle cx="48" cy="71" r="10" fill="none" stroke="#1D1B20" strokeWidth="3.2" />
            <circle cx="48" cy="71" r="5" fill="none" stroke="#1D1B20" strokeWidth="3.2" />

            {/* Spiral / Dizzy Right Eye */}
            <circle cx="92" cy="71" r="10" fill="none" stroke="#1D1B20" strokeWidth="3.2" />
            <circle cx="92" cy="71" r="5" fill="none" stroke="#1D1B20" strokeWidth="3.2" />

            {/* Wobbly wavy mouth */}
            <path
              d="M 60 89 Q 65 85 70 89 Q 75 93 80 89"
              fill="none"
              stroke="#1D1B20"
              strokeWidth="3.5"
              strokeLinecap="round"
            />

            {/* Sweat Drop on temple */}
            <ellipse cx="114" cy="56" rx="4.2" ry="5.5" fill="#6EC6FF" />
            <polygon points="114,48 111,54 117,54" fill="#6EC6FF" />
          </>
        ) : (
          /* COOKED or NUCLEAR: X Eyes & open tongue mouth */
          <>
            {/* Left X Eye */}
            <line x1="40" y1="63" x2="56" y2="79" stroke="#1D1B20" strokeWidth="5" strokeLinecap="round" />
            <line x1="56" y1="63" x2="40" y2="79" stroke="#1D1B20" strokeWidth="5" strokeLinecap="round" />

            {/* Right X Eye */}
            <line x1="84" y1="63" x2="100" y2="79" stroke="#1D1B20" strokeWidth="5" strokeLinecap="round" />
            <line x1="100" y1="63" x2="84" y2="79" stroke="#1D1B20" strokeWidth="5" strokeLinecap="round" />

            {/* Open tongue/stressed mouth */}
            <path
              d="M 62 88 Q 70 102 78 88 Z"
              fill="#1D1B20"
              stroke="#1D1B20"
              strokeWidth="2"
            />
            <path
              d="M 66 92 Q 70 99 74 92 Z"
              fill="#FF6350"
            />

            {/* Nuclear flame/steam lines if NUCLEAR */}
            {state.type === 'NUCLEAR' && (
              <>
                <path d="M 60 22 Q 58 12 62 8" stroke="#B3261E" strokeWidth="2.5" fill="none" strokeLinecap="round" />
                <path d="M 70 20 Q 72 10 68 5" stroke="#B3261E" strokeWidth="2.5" fill="none" strokeLinecap="round" />
                <path d="M 80 22 Q 82 12 78 8" stroke="#B3261E" strokeWidth="2.5" fill="none" strokeLinecap="round" />
              </>
            )}
          </>
        )}
      </svg>
    </div>
  );
};
