package com.gatecraft.grindweld;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.OnPauseListener;
import com.google.appinventor.components.runtime.OnResumeListener;

@DesignerComponent(version = 6, description = "GRIND & WELD: RUST & STEEL v2.6 offline lazy isometric Three.js ARPG with Diablo-style front menu, animated gate loading, multi-act campaign, multi-floor dungeons, contracts, Nemesis hunts, Rune Words, richer itemization and Rust Rift endgame.", category = ComponentCategory.EXTENSION, nonVisible = true, iconName = "")
@SimpleObject(external = true)
public class GateCraftGrindWeld extends AndroidNonvisibleComponent implements OnPauseListener, OnResumeListener, OnDestroyListener {
  private final Activity activity;
  private final Handler main = new Handler(Looper.getMainLooper());
  private Dialog dialog;
  private WebView web;
  private Button launchButton;
  private boolean hooked;
  private int calculationCount;
  private int language = 2;
  private boolean testMode = true;
  private String saveData = "";
  private int previousOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
  private boolean orientationChanged;

  public GateCraftGrindWeld(ComponentContainer container) {
    super(container.$form());
    activity = container.$context();
    container.$form().registerForOnPause(this);
    container.$form().registerForOnResume(this);
    container.$form().registerForOnDestroy(this);
    scheduleHook(0);
  }

  private boolean unlocked() { return calculationCount >= (testMode ? 1 : 50); }
  private void scheduleHook(final int attempt) {
    main.postDelayed(new Runnable() { @Override public void run() { if (hooked) { syncButton(); return; } if (hookButton()) return; if (attempt < 48) scheduleHook(attempt + 1); } }, attempt == 0 ? 0L : 250L);
  }
  private boolean hookButton() {
    try {
      Button b = findButton(activity.findViewById(android.R.id.content));
      if (b == null) return false;
      launchButton = b;
      b.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { StartGame(); } });
      hooked = true; syncButton(); return true;
    } catch (Throwable ignored) { return false; }
  }
  private Button findButton(View root) {
    if (root == null) return null;
    if (root instanceof Button) {
      CharSequence cs = ((Button) root).getText();
      if (cs != null) { String s = cs.toString().toLowerCase(); if (s.contains("grind & weld") || s.contains("rust & steel")) return (Button) root; }
    }
    if (root instanceof ViewGroup) { ViewGroup g = (ViewGroup) root; for (int i=0;i<g.getChildCount();i++) { Button b=findButton(g.getChildAt(i)); if (b!=null) return b; } }
    return null;
  }
  private void syncButton() { if (launchButton != null) launchButton.setVisibility(unlocked() ? View.VISIBLE : View.GONE); }

  @SimpleFunction public void SetCalculationCount(int count) { calculationCount=Math.max(0,count); main.post(new Runnable(){@Override public void run(){syncButton();}}); }
  @SimpleFunction public void SetLanguage(int lang) { language=Math.max(1,Math.min(12,lang)); }
  @SimpleFunction public void SetTestMode(boolean enabled) { testMode=enabled; main.post(new Runnable(){@Override public void run(){syncButton();}}); }
  @SimpleFunction public void SetSaveData(String json) { saveData=json==null?"":json; }
  @SimpleFunction public String LastSaveData() { return saveData; }
  @SimpleFunction public boolean IsUnlocked() { return unlocked(); }
  @SimpleFunction public void StartGame() { if (!unlocked()) return; main.post(new Runnable(){@Override public void run(){openGame();}}); }
  @SimpleFunction public void StopGame() { main.post(new Runnable(){@Override public void run(){closeGame(false);}}); }
  @SimpleFunction public String Version() { return "2.6.0"; }

  private void enterLandscape() {
    try {
      previousOrientation = activity.getRequestedOrientation();
      if (previousOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        orientationChanged = true;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }
    } catch(Throwable ignored) { orientationChanged = false; }
  }

  private void restoreOrientation() {
    if (!orientationChanged) return;
    orientationChanged = false;
    try { activity.setRequestedOrientation(previousOrientation); } catch(Throwable ignored) {}
  }

  private void openGame() {
    closeGame(false);
    try {
      enterLandscape();
      final WebView wv = new WebView(activity); web = wv;
      WebSettings s=wv.getSettings();
      s.setJavaScriptEnabled(true);
      s.setDomStorageEnabled(true);
      s.setAllowFileAccess(false);
      s.setAllowContentAccess(false);
      s.setMediaPlaybackRequiresUserGesture(true);
      s.setCacheMode(WebSettings.LOAD_NO_CACHE);
      wv.setWebChromeClient(new WebChromeClient());
      wv.setLayerType(View.LAYER_TYPE_HARDWARE,null);
      wv.setBackgroundColor(android.graphics.Color.BLACK);
      wv.addJavascriptInterface(new Bridge(),"GateCraftNative");
      dialog=new Dialog(activity,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      dialog.setCancelable(true);
      dialog.setContentView(wv);
      dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){@Override public void onDismiss(DialogInterface d){destroyWebView();restoreOrientation();}});
      Window win=dialog.getWindow();
      if(win!=null){win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);win.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);}
      dialog.show();
      win=dialog.getWindow();
      if(win!=null){win.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);View decor=win.getDecorView();if(decor!=null)decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_LOW_PROFILE);}
      wv.loadDataWithBaseURL("https://gatecraft.local/",Payload.html(),"text/html","UTF-8",null);
    } catch(Throwable t){closeGame(false);GameError(String.valueOf(t.getMessage()));}
  }
  private void closeGame(boolean dispatchExit) {
    try { if(web!=null) web.evaluateJavascript("window.GW&&GW.shutdown&&GW.shutdown();",null); } catch(Throwable ignored){}
    try { if(dialog!=null&&dialog.isShowing())dialog.dismiss(); } catch(Throwable ignored){}
    destroyWebView(); dialog=null; restoreOrientation(); if(dispatchExit)ExitRequested();
  }
  private void destroyWebView() {
    final WebView wv=web; web=null; if(wv==null)return;
    try{wv.onPause();}catch(Throwable ignored){}
    try{wv.stopLoading();}catch(Throwable ignored){}
    try{wv.loadUrl("about:blank");}catch(Throwable ignored){}
    try{wv.clearHistory();}catch(Throwable ignored){}
    try{wv.clearCache(false);}catch(Throwable ignored){}
    try{wv.removeJavascriptInterface("GateCraftNative");}catch(Throwable ignored){}
    try{ViewGroup p=(ViewGroup)wv.getParent();if(p!=null)p.removeView(wv);}catch(Throwable ignored){}
    try{wv.destroy();}catch(Throwable ignored){}
  }
  private final class Bridge {
    @JavascriptInterface public int language(){return language;}
    @JavascriptInterface public String load(){return saveData==null?"":saveData;}
    @JavascriptInterface public void save(final String json){saveData=json==null?"":json;main.post(new Runnable(){@Override public void run(){SaveChanged(saveData);}});}
    @JavascriptInterface public void exit(){main.post(new Runnable(){@Override public void run(){closeGame(true);}});}
    @JavascriptInterface public void ready(){main.post(new Runnable(){@Override public void run(){GameReady();}});}
  }
  @SimpleEvent public void SaveChanged(String json){EventDispatcher.dispatchEvent(this,"SaveChanged",json);}
  @SimpleEvent public void GameReady(){EventDispatcher.dispatchEvent(this,"GameReady");}
  @SimpleEvent public void ExitRequested(){EventDispatcher.dispatchEvent(this,"ExitRequested");}
  @SimpleEvent public void GameError(String message){EventDispatcher.dispatchEvent(this,"GameError",message);}
  @Override public void onPause(){if(web!=null)try{web.onPause();}catch(Throwable ignored){}}
  @Override public void onResume(){if(web!=null)try{web.onResume();}catch(Throwable ignored){}}
  @Override public void onDestroy(){closeGame(false);main.removeCallbacksAndMessages(null);}
}
