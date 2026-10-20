package com.prosenssgp.aiedustudio;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends Activity {
  private WebView webView;
  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().setStatusBarColor(Color.BLACK);
    webView = new WebView(this);
    webView.setBackgroundColor(Color.rgb(10, 15, 30));
    WebSettings s = webView.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient());
    setContentView(webView);
    webView.loadUrl("file:///android_asset/index.html");
  }
  @Override public void onBackPressed() {
    if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
  }
}
