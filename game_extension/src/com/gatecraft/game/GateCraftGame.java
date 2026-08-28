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
    version = 3,
    description = "GateCraft Game Suite v3: screenless, lazy-loaded retro easter egg games.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "")
@SimpleObject(external = true)
public class GateCraftGame extends AndroidNonvisibleComponent
    implements OnPauseListener, OnResumeListener, OnDestroyListener {

  private final Activity activity;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private Dialog gameDialog;
  private GameSuiteView gameView;
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
    } catch (Throwable ignored) {
      return false;
    }
  }

  private Button findWorkshopButton(View root) {
    if (root == null) return null;
    if (root instanceof Button) {
      CharSequence cs = ((Button) root).getText();
      if (cs != null) {
        String text = cs.toString().trim().toLowerCase();
        if (text.contains("workshop run") || text.equals("workshop")) return (Button) root;
      }
    }
    if (root instanceof ViewGroup) {
      ViewGroup g = (ViewGroup) root;
      for (int i = 0; i < g.getChildCount(); i++) {
        Button b = findWorkshopButton(g.getChildAt(i));
        if (b != null) return b;
      }
    }
    return null;
  }

  private void showGameOverlay() {
    try {
      closeGameOverlay(false);
      gameView = new GameSuiteView(activity, this);
      gameView.setLanguage(language);
      gameView.setCalculationCount(calculationCount);
      gameView.setTestMode(testMode);
      gameDialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
      gameDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      gameDialog.setCancelable(true);
      gameDialog.setContentView(gameView);
      gameDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
        @Override public void onDismiss(DialogInterface dialog) {
          if (gameView != null) gameView.shutdown();
          gameView = null;
          gameDialog = null;
          started = false;
        }
      });
      Window w = gameDialog.getWindow();
      if (w != null) {
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT);
      }
      started = true;
      gameDialog.show();
      w = gameDialog.getWindow();
      if (w != null) {
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT);
      }
    } catch (Throwable ignored) {
      started = false;
      if (gameView != null) gameView.shutdown();
      gameView = null;
      gameDialog = null;
    }
  }

  private void closeGameOverlay(boolean dispatchExit) {
    started = false;
    if (gameView != null) gameView.shutdown();
    try {
      if (gameDialog != null && gameDialog.isShowing()) gameDialog.dismiss();
    } catch (Throwable ignored) {}
    gameView = null;
    gameDialog = null;
    if (dispatchExit) EventDispatcher.dispatchEvent(this, "ExitRequested");
  }

  void requestExitFromView() {
    closeGameOverlay(true);
  }

  void reportScore(int score) {
    EventDispatcher.dispatchEvent(this, "ScoreChanged", score);
  }

  void reportLevelComplete(int level, int score) {
    EventDispatcher.dispatchEvent(this, "LevelCompleted", level, score);
  }

  void reportGameComplete(int score) {
    EventDispatcher.dispatchEvent(this, "GameCompleted", score);
  }

  void reportGameOver(int score) {
    EventDispatcher.dispatchEvent(this, "GameOver", score);
  }

  @SimpleFunction(description = "Opens the GateCraft game hub.")
  public void StartGame() { showGameOverlay(); }

  @SimpleFunction(description = "Pauses the active game without losing progress.")
  public void PauseGame() {
    if (gameView != null) gameView.setPaused(true);
  }

  @SimpleFunction(description = "Resumes the active game.")
  public void ResumeGame() {
    if (started && gameView != null) gameView.setPaused(false);
  }

  @SimpleFunction(description = "Restarts the active game mode.")
  public void RestartLevel() {
    if (gameView != null) gameView.restartActiveGame();
  }

  @SimpleFunction(description = "Stops the game and releases all transient game resources.")
  public void StopGame() { closeGameOverlay(false); }

  @SimpleFunction(description = "Sets the shared GateCraft calculation counter used by game unlocks.")
  public void SetCalculationCount(int count) {
    calculationCount = Math.max(0, count);
    if (gameView != null) gameView.setCalculationCount(calculationCount);
  }

  @SimpleFunction(description = "Sets GateCraft language index 1..12.")
  public void SetLanguage(int lang) {
    language = Math.max(1, Math.min(12, lang));
    if (gameView != null) gameView.setLanguage(language);
  }

  @SimpleFunction(description = "Testing switch. True unlocks the staged games after the first calculation; false uses 10/20/30/40 thresholds.")
  public void SetTestMode(boolean enabled) {
    testMode = enabled;
    if (gameView != null) gameView.setTestMode(enabled);
  }

  @SimpleFunction(description = "Returns the active game score.")
  public int Score() { return gameView == null ? 0 : gameView.getScore(); }

  @SimpleFunction(description = "Returns the active Workshop Run level number.")
  public int Level() { return gameView == null ? 1 : gameView.getLevel(); }

  @SimpleFunction(description = "Returns active Workshop Run lives.")
  public int Lives() { return gameView == null ? 3 : gameView.getLives(); }

  @SimpleFunction(description = "Returns the extension version.")
  public String Version() { return "3.0.0"; }

  @SimpleEvent(description = "Raised whenever a visible game score changes.")
  public void ScoreChanged(int score) { EventDispatcher.dispatchEvent(this, "ScoreChanged", score); }

  @SimpleEvent(description = "Raised after a Workshop Run level is completed.")
  public void LevelCompleted(int level, int score) { EventDispatcher.dispatchEvent(this, "LevelCompleted", level, score); }

  @SimpleEvent(description = "Raised after a game is completed.")
  public void GameCompleted(int score) { EventDispatcher.dispatchEvent(this, "GameCompleted", score); }

  @SimpleEvent(description = "Raised when a game is lost.")
  public void GameOver(int score) { EventDispatcher.dispatchEvent(this, "GameOver", score); }

  @SimpleEvent(description = "Raised when the player closes the game overlay.")
  public void ExitRequested() { EventDispatcher.dispatchEvent(this, "ExitRequested"); }

  @Override public void onPause() {
    if (gameView != null) gameView.setPaused(true);
  }

  @Override public void onResume() {
    if (started && gameView != null) gameView.setPaused(false);
  }

  @Override public void onDestroy() {
    closeGameOverlay(false);
    mainHandler.removeCallbacksAndMessages(null);
  }
}
