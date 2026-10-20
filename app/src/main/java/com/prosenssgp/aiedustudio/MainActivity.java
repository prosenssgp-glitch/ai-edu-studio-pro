package com.prosenssgp.aiedustudio;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;

public class MainActivity extends Activity {
  private static final int FILE_CHOOSER_REQUEST = 4102;
  private WebView webView;
  private ValueCallback<Uri[]> fileChooserCallback;

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
    s.setSupportZoom(false);
    s.setBuiltInZoomControls(false);
    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
        fileChooserCallback = callback;
        Intent intent;
        try {
          intent = params.createIntent();
          startActivityForResult(intent, FILE_CHOOSER_REQUEST);
          return true;
        } catch (ActivityNotFoundException ex) {
          fileChooserCallback = null;
          Toast.makeText(MainActivity.this, "No file picker available", Toast.LENGTH_LONG).show();
          return false;
        }
      }
    });
    webView.setDownloadListener(new DownloadListener() {
      @Override public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
        if (url == null || url.startsWith("blob:") || url.startsWith("data:")) {
          Toast.makeText(MainActivity.this, "এই ভিডিও ফরম্যাটটি সরাসরি ডাউনলোড করা যাচ্ছে না।", Toast.LENGTH_LONG).show();
          return;
        }
        try {
          DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
          request.setMimeType(mimeType);
          request.addRequestHeader("User-Agent", userAgent);
          request.setTitle("AI Edu Studio download");
          request.setDescription("Downloading file");
          request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
          request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AI-Edu-Studio-" + System.currentTimeMillis());
          ((DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(request);
          Toast.makeText(MainActivity.this, "ডাউনলোড শুরু হয়েছে", Toast.LENGTH_SHORT).show();
        } catch (Exception ex) {
          Toast.makeText(MainActivity.this, "ডাউনলোড শুরু করা যায়নি", Toast.LENGTH_LONG).show();
        }
      }
    });
    setContentView(webView);
    if (state == null) webView.loadUrl("file:///android_asset/index.html");
    else webView.restoreState(state);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (requestCode == FILE_CHOOSER_REQUEST && fileChooserCallback != null) {
      Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
      fileChooserCallback.onReceiveValue(result);
      fileChooserCallback = null;
    }
  }

  @Override protected void onSaveInstanceState(Bundle outState) {
    if (webView != null) webView.saveState(outState);
    super.onSaveInstanceState(outState);
  }

  @Override public void onBackPressed() {
    if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed();
  }

  @Override protected void onDestroy() {
    if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
    if (webView != null) webView.destroy();
    super.onDestroy();
  }
}