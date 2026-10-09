import React from 'react';

interface AppIconBadgeProps {
  packageName: string;
  size?: number;
  className?: string;
}

export const AppIconBadge: React.FC<AppIconBadgeProps> = ({
  packageName,
  size = 32,
  className = '',
}) => {
  const isInstagram = packageName.includes('instagram');
  const isYouTube = packageName.includes('youtube') || packageName.includes('shorts');
  const isTikTok = packageName.includes('musically') || packageName.includes('tiktok');
  const isSpotify = packageName.includes('spotify');
  const isFacebook = packageName.includes('katana') || packageName.includes('facebook');

  return (
    <div
      className={`inline-flex items-center justify-center rounded-2xl shadow-sm overflow-hidden flex-shrink-0 ${className}`}
      style={{ width: `${size}px`, height: `${size}px` }}
    >
      {isInstagram ? (
        <div className="w-full h-full bg-gradient-to-tr from-[#f09433] via-[#dc2743] to-[#bc1888] flex items-center justify-center text-white">
          <svg className="w-3/5 h-3/5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
            <rect width="20" height="20" x="2" y="2" rx="5" ry="5"/>
            <path d="M16 11.37A4 4 0 1 1 12.63 8 4 4 0 0 1 16 11.37z"/>
            <line x1="17.5" x2="17.51" y1="6.5" y2="6.5"/>
          </svg>
        </div>
      ) : isYouTube ? (
        <div className="w-full h-full bg-[#FF0000] flex items-center justify-center text-white">
          <svg className="w-3/5 h-3/5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M23.498 6.186a3.016 3.016 0 0 0-2.122-2.136C19.505 3.545 12 3.545 12 3.545s-7.505 0-9.377.505A3.017 3.017 0 0 0 .502 6.186C0 8.07 0 12 0 12s0 3.93.502 5.814a3.016 3.016 0 0 0 2.122 2.136c1.871.505 9.376.505 9.376.505s7.505 0 9.377-.505a3.015 3.015 0 0 0 2.122-2.136C24 15.93 24 12 24 12s0-3.93-.502-5.814zM9.545 15.568V8.432L15.818 12l-6.273 3.568z"/>
          </svg>
        </div>
      ) : isTikTok ? (
        <div className="w-full h-full bg-[#010101] flex items-center justify-center text-[#25F4EE]">
          <svg className="w-3/5 h-3/5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M19.59 6.69a4.83 4.83 0 0 1-3.77-4.25V2h-3.45v13.67a2.89 2.89 0 0 1-5.2 1.74 2.89 2.89 0 0 1 2.31-4.64c.298-.002.595.042.88.13V9.4a6.33 6.33 0 0 0-1-.08A6.34 6.34 0 0 0 3 15.66a6.34 6.34 0 0 0 10.82 4.49 6.3 6.3 0 0 0 1.87-4.49V8.69a8.18 8.18 0 0 0 4.79 1.52V6.75a4.85 4.85 0 0 1-.89-.06z"/>
          </svg>
        </div>
      ) : isSpotify ? (
        <div className="w-full h-full bg-[#1DB954] flex items-center justify-center text-white">
          <svg className="w-3/5 h-3/5" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.373 0 0 5.373 0 12s5.373 12 12 12 12-5.373 12-12S18.627 0 12 0zm5.5 17.3c-.2.3-.6.4-.9.2-2.5-1.5-5.6-1.9-9.3-1-.4.1-.7-.2-.8-.5-.1-.4.2-.7.5-.8 4.1-1 7.6-.5 10.3 1.2.3.2.4.6.2.9zm1.5-3.3c-.3.4-.8.5-1.2.3-3-1.8-7.5-2.4-11-1.3-.4.1-.9-.1-1-.6-.1-.4.1-.9.6-1 4-1.2 9-.6 12.4 1.5.4.1.5.7.2 1.1zm.1-3.5C15.5 8.4 9.1 8.2 5.3 9.3c-.6.2-1.2-.2-1.4-.7-.2-.6.2-1.2.7-1.4 4.4-1.3 11.5-1.1 16 1.6.5.3.7 1 .4 1.5-.3.5-1 .7-1.9.2z"/>
          </svg>
        </div>
      ) : isFacebook ? (
        <div className="w-full h-full bg-[#1877F2] flex items-center justify-center text-white font-black text-xl leading-none">
          f
        </div>
      ) : (
        <div className="w-full h-full bg-[#6750A4] flex items-center justify-center text-white">
          <svg className="w-3/5 h-3/5" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
          </svg>
        </div>
      )}
    </div>
  );
};
