package com.gatecraft.game;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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

@DesignerComponent(
    version = 4,
    description = "GateCraft Arcade Suite v4: isolated lazy-loaded Workshop Run, Heroes of Craft & Gates and Metal Fighter.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "")
@SimpleObject(external = true)
public class GateCraftGame extends AndroidNonvisibleComponent
    implements OnPauseListener, OnResumeListener, OnDestroyListener {

  private final Activity activity;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private Dialog dialog;
  private View activeView;
  private GameMetrics metrics;
  private boolean started;
  private boolean buttonHooked;
  private int calculationCount;
  private int language = 2;
  private boolean testMode = true;

  public GateCraftGame(ComponentContainer container) {
    super(container.$form());
    activity = container.$context();
    container.$form().registerForOnPause(this);
    container.$form().registerForOnResume(this);
    container.$form().registerForOnDestroy(this);
    scheduleWorkshopButtonHook(0);
  }

  private void scheduleWorkshopButtonHook(final int attempt) {
    mainHandler.postDelayed(new Runnable() {
      @Override public void run() {
        if (buttonHooked) return;
        if (hookWorkshopButton()) return;
        if (attempt < 48) scheduleWorkshopButtonHook(attempt + 1);
      }
    }, attempt == 0 ? 0L : 250L);
  }

  private boolean hookWorkshopButton() {
    try {
      View root = activity.findViewById(android.R.id.content);
      final Button b = findWorkshopButton(root);
      if (b == null) return false;
      b.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) { showGameOverlay(); }
      });
      buttonHooked = true;
      return true;
    } catch (Throwable ignored) { return false; }
  }

  private Button findWorkshopButton(View root) {
    if (root == null) return null;
    if (root instanceof Button) {
      CharSequence cs=((Button)root).getText();
      if (cs!=null) {
        String s=cs.toString().trim().toLowerCase();
        if (s.contains("workshop run") || s.equals("workshop")) return (Button)root;
      }
    }
    if (root instanceof ViewGroup) {
      ViewGroup g=(ViewGroup)root;
      for(int i=0;i<g.getChildCount();i++){Button b=findWorkshopButton(g.getChildAt(i));if(b!=null)return b;}
    }
    return null;
  }

  private void showGameOverlay() {
    try {
      closeGameOverlay(false);
      dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      dialog.setCancelable(true);
      dialog.setOnDismissListener(new DialogInterface.OnDismissListener(){@Override public void onDismiss(DialogInterface d){releaseActive();dialog=null;started=false;}});
      showLauncherInsideDialog();
      Window w=dialog.getWindow();
      if(w!=null){w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);}
      started=true; dialog.show();
      w=dialog.getWindow(); if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT);
    } catch(Throwable t){closeGameOverlay(false);}
  }

  private void setActive(View v, GameMetrics m) {
    releaseActive();
    activeView=v; metrics=m;
    if(metrics!=null){metrics.setLanguage(language);metrics.setPaused(false);}
    if(dialog!=null) dialog.setContentView(v);
  }

  private void showLauncherInsideDialog(){setActive(new GameLauncherView(activity,this),null);}

  void launchMode(int mode){
    if(mode==1) setActive(new WorkshopArcadeView(activity,this), null);
    else if(mode==2) setActive(new HeroesCraftView(activity,this), null);
    else if(mode==3) setActive(new MetalFighterView(activity,this), null);
    if(activeView instanceof GameMetrics) metrics=(GameMetrics)activeView;
    if(metrics!=null){metrics.setLanguage(language);metrics.setPaused(false);}
  }

  void returnToLauncher(){showLauncherInsideDialog();}
  int getCalculationCount(){return calculationCount;}
  boolean isTestMode(){return testMode;}
  int getLanguage(){return language;}
  boolean modeUnlocked(int mode){if(mode<=1)return calculationCount>=1; if(testMode)return calculationCount>=1; if(mode==2)return calculationCount>=10; if(mode==3)return calculationCount>=20; return false;}
  int requiredFor(int mode){if(mode<=1)return 1;if(mode==2)return 10;if(mode==3)return 20;return 999;}

  private void releaseActive(){
    if(metrics!=null){try{metrics.shutdown();}catch(Throwable ignored){}}
    activeView=null; metrics=null;
  }

  private void closeGameOverlay(boolean dispatchExit){
    started=false; releaseActive();
    try{if(dialog!=null&&dialog.isShowing())dialog.dismiss();}catch(Throwable ignored){}
    dialog=null;
    if(dispatchExit) EventDispatcher.dispatchEvent(this,"ExitRequested");
  }

  void requestExitFromView(){closeGameOverlay(true);}
  void reportScore(int score){EventDispatcher.dispatchEvent(this,"ScoreChanged",score);}
  void reportLevelComplete(int level,int score){EventDispatcher.dispatchEvent(this,"LevelCompleted",level,score);}
  void reportGameComplete(int score){EventDispatcher.dispatchEvent(this,"GameCompleted",score);}
  void reportGameOver(int score){EventDispatcher.dispatchEvent(this,"GameOver",score);}

  @SimpleFunction(description="Opens the GateCraft arcade launcher.") public void StartGame(){showGameOverlay();}
  @SimpleFunction(description="Pauses the active arcade game.") public void PauseGame(){if(metrics!=null)metrics.setPaused(true);}
  @SimpleFunction(description="Resumes the active arcade game.") public void ResumeGame(){if(started&&metrics!=null)metrics.setPaused(false);}
  @SimpleFunction(description="Restarts the active arcade game.") public void RestartLevel(){if(metrics!=null)metrics.restart();}
  @SimpleFunction(description="Stops the arcade suite and releases the active game view.") public void StopGame(){closeGameOverlay(false);}
  @SimpleFunction(description="Sets the shared GateCraft calculation counter used by game unlocks.") public void SetCalculationCount(int count){calculationCount=Math.max(0,count);if(activeView!=null)activeView.invalidate();}
  @SimpleFunction(description="Sets GateCraft language index 1..12.") public void SetLanguage(int lang){language=Math.max(1,Math.min(12,lang));if(metrics!=null)metrics.setLanguage(language);if(activeView!=null)activeView.invalidate();}
  @SimpleFunction(description="Testing switch. True unlocks staged games after the first calculation; false uses production thresholds.") public void SetTestMode(boolean enabled){testMode=enabled;if(activeView!=null)activeView.invalidate();}
  @SimpleFunction(description="Returns active game score.") public int Score(){return metrics==null?0:metrics.score();}
  @SimpleFunction(description="Returns active game level.") public int Level(){return metrics==null?1:metrics.level();}
  @SimpleFunction(description="Returns active game lives.") public int Lives(){return metrics==null?3:metrics.lives();}
  @SimpleFunction(description="Returns extension version.") public String Version(){return "4.0.0";}

  @SimpleEvent(description="Raised whenever visible game score changes.") public void ScoreChanged(int score){EventDispatcher.dispatchEvent(this,"ScoreChanged",score);}
  @SimpleEvent(description="Raised after a Workshop Run level is completed.") public void LevelCompleted(int level,int score){EventDispatcher.dispatchEvent(this,"LevelCompleted",level,score);}
  @SimpleEvent(description="Raised after a game is completed.") public void GameCompleted(int score){EventDispatcher.dispatchEvent(this,"GameCompleted",score);}
  @SimpleEvent(description="Raised when a game is lost.") public void GameOver(int score){EventDispatcher.dispatchEvent(this,"GameOver",score);}
  @SimpleEvent(description="Raised when player closes the arcade overlay.") public void ExitRequested(){EventDispatcher.dispatchEvent(this,"ExitRequested");}

  @Override public void onPause(){if(metrics!=null)metrics.setPaused(true);}
  @Override public void onResume(){if(started&&metrics!=null)metrics.setPaused(false);}
  @Override public void onDestroy(){closeGameOverlay(false);mainHandler.removeCallbacksAndMessages(null);}
}
