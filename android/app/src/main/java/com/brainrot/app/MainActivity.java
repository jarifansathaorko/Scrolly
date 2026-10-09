package com.brainrot.app;

import android.os.Bundle;
import android.webkit.WebView;
import android.graphics.Color;
import android.view.View;
import android.view.WindowManager;
import androidx.core.view.WindowCompat;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Bridge;

public class MainActivity extends BridgeActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge before super
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        super.onCreate(savedInstanceState);
        
        // Configure WebView for best performance and UX
        Bridge bridge = this.getBridge();
        if (bridge != null) {
            WebView webView = bridge.getWebView();
            if (webView != null) {
                // Enable hardware acceleration for smooth animations
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                
                // Improve scrolling
                webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
                webView.setVerticalScrollBarEnabled(false);
                webView.setHorizontalScrollBarEnabled(false);
                
                // Better text rendering
                webView.getSettings().setLoadWithOverviewMode(true);
                webView.getSettings().setUseWideViewPort(true);
            }
        }
        
        // Set status bar to light with custom color
        getWindow().setStatusBarColor(Color.parseColor("#FEF7FF"));
        getWindow().setNavigationBarColor(Color.parseColor("#F3EDF7"));
        
        // For Android 15+ edge-to-edge enforcement
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getWindow().setStatusBarContrastEnforced(false);
            getWindow().setNavigationBarContrastEnforced(false);
        }
        
        // Keep screen on while using? No, let it sleep normally
        // getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Re-apply status bar colors on resume
        getWindow().setStatusBarColor(Color.parseColor("#FEF7FF"));
    }
}
