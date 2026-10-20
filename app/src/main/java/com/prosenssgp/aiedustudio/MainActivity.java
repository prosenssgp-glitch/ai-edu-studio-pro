package com.prosenssgp.aiedustudio;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.graphics.Color;
import android.view.Window;
import android.view.WindowManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Locale;

public class MainActivity extends Activity {
  private static final int FILE_CHOOSER_REQUEST = 4102;
  private static final int STORAGE_REQUEST = 4103;
  private WebView webView;
  private ValueCallback<Uri[]> fileChooserCallback;
  private TextToSpeech tts;
  private OutputStream videoOut;
  private File videoTemp;
  private String videoName = "QuizCraft-Video.webm";

  @Override public void onCreate(Bundle state) {
    super.onCreate(state);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    getWindow().setStatusBarColor(Color.BLACK);
    if (Build.VERSION.SDK_INT >= 23 && Build.VERSION.SDK_INT <= 28 &&
        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
      requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_REQUEST);
    }
    tts = new TextToSpeech(this, status -> {
      if (status == TextToSpeech.SUCCESS) tts.setLanguage(Locale.getDefault());
    });
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
    webView.addJavascriptInterface(new AndroidBridge(), "Android");
    webView.setWebViewClient(new WebViewClient());
    webView.setWebChromeClient(new WebChromeClient() {
      @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
        if (fileChooserCallback != null) fileChooserCallback.onReceiveValue(null);
        fileChooserCallback = callback;
        try {
          startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
          return true;
        } catch (ActivityNotFoundException ex) {
          fileChooserCallback = null;
          Toast.makeText(MainActivity.this, "No file picker available", Toast.LENGTH_LONG).show();
          return false;
        }
      }
    });
    setContentView(webView);
    if (state == null) webView.loadUrl("file:///android_asset/index.html");
    else webView.restoreState(state);
  }

  private class AndroidBridge {
    @JavascriptInterface public void say(String text, String language) {
      runOnUiThread(() -> {
        if (tts == null) return;
        Locale locale = "bn".equals(language) ? new Locale("bn", "BD") :
          ("hi".equals(language) ? new Locale("hi", "IN") : Locale.US);
        int result = tts.setLanguage(locale);
        tts.setSpeechRate(0.92f);
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
          Toast.makeText(MainActivity.this, "এই ভাষার Text-to-Speech voice ফোনে নেই। Google Speech Services-এ voice install করুন।", Toast.LENGTH_LONG).show();
          return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "quizcraft-voice");
      });
    }
    @JavascriptInterface public void stopVoice() {
      runOnUiThread(() -> { if (tts != null) tts.stop(); });
    }
    @JavascriptInterface public synchronized boolean beginVideo(String name) {
      try {
        if (videoOut != null) videoOut.close();
        videoName = (name == null || !name.matches("[A-Za-z0-9._-]+")) ? "QuizCraft-Video.webm" : name;
        videoTemp = new File(getCacheDir(), "quizcraft-video.part");
        videoOut = new BufferedOutputStream(new FileOutputStream(videoTemp, false));
        return true;
      } catch (Exception e) { videoOut = null; return false; }
    }
    @JavascriptInterface public synchronized boolean appendVideoChunk(String base64) {
      try {
        if (videoOut == null) return false;
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        videoOut.write(bytes);
        return true;
      } catch (Exception e) { return false; }
    }
    @JavascriptInterface public synchronized String finishVideo() {
      try {
        if (videoOut == null || videoTemp == null) return "ERROR: no video data";
        videoOut.flush(); videoOut.close(); videoOut = null;
        String mime = "video/webm";
        if (Build.VERSION.SDK_INT >= 29) {
          ContentValues values = new ContentValues();
          values.put(MediaStore.MediaColumns.DISPLAY_NAME, videoName);
          values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
          values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
          Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
          if (uri == null) return "ERROR: Downloads folder unavailable";
          try (InputStream in = new FileInputStream(videoTemp); OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) return "ERROR: cannot write download";
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
          }
        } else {
          File dir;
          if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
          } else {
            dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
          }
          if (dir == null) return "ERROR: Downloads folder unavailable";
          if (!dir.exists() && !dir.mkdirs()) return "ERROR: cannot create Downloads folder";
          File target = new File(dir, videoName);
          try (InputStream in = new FileInputStream(videoTemp); OutputStream out = new FileOutputStream(target)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
          }
          if (dir.equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))) {
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(target)));
          }
        }
        videoTemp.delete();
        return "OK";
      } catch (Exception e) {
        try { if (videoOut != null) videoOut.close(); } catch (Exception ignored) {}
        videoOut = null;
        return "ERROR: " + e.getMessage();
      }
    }
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
    if (videoOut != null) try { videoOut.close(); } catch (Exception ignored) {}
    if (tts != null) { tts.stop(); tts.shutdown(); }
    if (webView != null) webView.destroy();
    super.onDestroy();
  }
}