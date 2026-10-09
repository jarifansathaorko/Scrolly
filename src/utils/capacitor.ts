import { Capacitor } from '@capacitor/core';
import { StatusBar, Style } from '@capacitor/status-bar';
import { SplashScreen } from '@capacitor/splash-screen';
import { Haptics, ImpactStyle } from '@capacitor/haptics';
import { App as CapacitorApp } from '@capacitor/app';
import { Keyboard } from '@capacitor/keyboard';

export const isNative = Capacitor.isNativePlatform();
export const isAndroid = Capacitor.getPlatform() === 'android';
export const isIOS = Capacitor.getPlatform() === 'ios';
export const isWeb = Capacitor.getPlatform() === 'web';

export async function initializeNativeApp() {
  if (!isNative) return;

  try {
    // Configure Status Bar
    if (isAndroid || isIOS) {
      await StatusBar.setOverlaysWebView({ overlay: false });
      await StatusBar.setBackgroundColor({ color: '#FEF7FF' });
      await StatusBar.setStyle({ style: Style.Light });
    }

    // Hide splash screen after app is ready
    setTimeout(async () => {
      try {
        await SplashScreen.hide();
      } catch (e) {
        console.log('Splash hide error', e);
      }
    }, 500);

    // Handle back button on Android
    if (isAndroid) {
      CapacitorApp.addListener('backButton', ({ canGoBack }) => {
        if (!canGoBack) {
          CapacitorApp.exitApp();
        } else {
          window.history.back();
        }
      });

      // Keyboard listeners for better UX
      Keyboard.addListener('keyboardWillShow', () => {
        document.body.classList.add('keyboard-open');
      });
      Keyboard.addListener('keyboardWillHide', () => {
        document.body.classList.remove('keyboard-open');
      });
    }

    // App state listeners
    CapacitorApp.addListener('appStateChange', ({ isActive }) => {
      console.log('App state changed. Is active?', isActive);
    });

    // Deep link handling
    CapacitorApp.addListener('appUrlOpen', (data) => {
      console.log('App opened with URL:', data.url);
      const url = new URL(data.url);
      const tab = url.searchParams.get('tab');
      if (tab) {
        // Could trigger tab change via custom event
        window.dispatchEvent(new CustomEvent('brainrot:openTab', { detail: tab }));
      }
    });

  } catch (error) {
    console.error('Failed to initialize native app:', error);
  }
}

export async function triggerHaptic(style: 'light' | 'medium' | 'heavy' = 'light') {
  if (!isNative) return;
  try {
    const impactStyle = 
      style === 'heavy' ? ImpactStyle.Heavy :
      style === 'medium' ? ImpactStyle.Medium :
      ImpactStyle.Light;
    await Haptics.impact({ style: impactStyle });
  } catch (e) {
    console.log('Haptics not available', e);
  }
}

export async function triggerNotificationHaptic() {
  if (!isNative) return;
  try {
    await Haptics.notification({ type: 'SUCCESS' as any });
  } catch (e) {
    // fallback
    await triggerHaptic('medium');
  }
}

export function getSafeAreaInsets() {
  if (typeof window === 'undefined') return { top: 0, bottom: 0, left: 0, right: 0 };
  const style = getComputedStyle(document.documentElement);
  return {
    top: parseInt(style.getPropertyValue('--sat') || '0') || 0,
    bottom: parseInt(style.getPropertyValue('--sab') || '0') || 0,
    left: parseInt(style.getPropertyValue('--sal') || '0') || 0,
    right: parseInt(style.getPropertyValue('--sar') || '0') || 0,
  };
}

// Overlay permission check (Android specific simulation)
export async function checkOverlayPermission(): Promise<boolean> {
  if (!isNative) return true;
  // On real Android, this would check SYSTEM_ALERT_WINDOW permission via native bridge
  // For now we simulate as granted if native
  return true;
}

export async function requestOverlayPermissionNative(): Promise<boolean> {
  if (!isNative) return true;
  try {
    // In a production app, you'd create a custom native plugin to request overlay permission
    // For demo, we just return true and let JS handle UI
    await triggerHaptic('medium');
    return true;
  } catch {
    return false;
  }
}
