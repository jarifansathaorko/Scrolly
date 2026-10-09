import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App';
import './index.css';
import { initializeNativeApp, isNative } from './utils/capacitor';

// Initialize Capacitor native features
if (isNative) {
  initializeNativeApp().then(() => {
    console.log('BrainRot native initialized');
  });
}

// PWA Service Worker registration (only on web, not in native wrapper)
if ('serviceWorker' in navigator && !isNative) {
  window.addEventListener('load', () => {
    console.log('BrainRot PWA ready');
  });
}

// Prevent pull-to-refresh on mobile web
let lastTouchY = 0;
document.addEventListener('touchstart', (e) => {
  if (e.touches.length === 1) {
    lastTouchY = e.touches[0].clientY;
  }
}, { passive: false });

document.addEventListener('touchmove', (e) => {
  const touchY = e.touches[0].clientY;
  const touchDiff = touchY - lastTouchY;
  const isAtTop = window.scrollY === 0;
  if (isAtTop && touchDiff > 0) {
    const target = e.target as HTMLElement;
    const isScrollable = target.closest('[data-scrollable]') || target.closest('main');
    if (!isScrollable || window.scrollY === 0) {
      // prevent pull-to-refresh if needed
    }
  }
}, { passive: false });

// Handle viewport height for mobile browsers (100vh issue)
function setVh() {
  const vh = window.innerHeight * 0.01;
  document.documentElement.style.setProperty('--vh', `${vh}px`);
}
setVh();
window.addEventListener('resize', setVh);
window.addEventListener('orientationchange', () => {
  setTimeout(setVh, 100);
});

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
