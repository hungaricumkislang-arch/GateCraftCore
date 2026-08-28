package com.gatecraft.game;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import dalvik.system.DexClassLoader;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;

@DesignerComponent(
    version = 8,
    description = "GateCraft Arcade Suite v6.0 Classic Heritage, startup-safe dynamic runtime edition. The full three-game runtime is loaded only when the player opens the arcade launcher.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "")
@SimpleObject(external = true)
public class GateCraftGame extends AndroidNonvisibleComponent implements OnPauseListener, OnResumeListener, OnDestroyListener {
  private static final String RUNTIME_ASSET = "gatecraft_game_runtime.gcr";
  private final Activity activity;
  private final Handler mainHandler = new Handler(Looper.getMainLooper());
  private Dialog dialog;
  private View activeView;
  private GameMetrics metrics;
  private DexClassLoader runtimeLoader;
  private boolean started, buttonHooked;
  private int calculationCount, language = 2;
  private boolean testMode = true;
  private int previousOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
  private boolean orientationChanged;

  public GateCraftGame(ComponentContainer container) {
    super(container.$form());
    activity = container.$context();
    container.$form().registerForOnPause(this);
    container.$form().registerForOnResume(this);
    container.$form().registerForOnDestroy(this);
    scheduleWorkshopButtonHook(2200L, 0);
  }

  private void scheduleWorkshopButtonHook(final long delay, final int attempt) {
    mainHandler.postDelayed(new Runnable() {
      @Override public void run() {
        if (buttonHooked) return;
        if (hookWorkshopButton()) return;
        if (attempt < 20) scheduleWorkshopButtonHook(700L, attempt + 1);
      }
    }, delay);
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
      CharSequence cs = ((Button) root).getText();
      if (cs != null) {
        String s = cs.toString().trim().toLowerCase();
        if (s.contains("workshop run") || s.equals("workshop")) return (Button) root;
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

  private synchronized void ensureRuntime() throws Exception {
    if (runtimeLoader != null) return;
    File codeDir = activity.getCodeCacheDir();
    if (codeDir == null) codeDir = activity.getCacheDir();
    File jar = new File(codeDir, "gc_arcade_v6_runtime.jar");
    if (jar.exists()) jar.setWritable(true, true);
    InputStream in = activity.getAssets().open(RUNTIME_ASSET);
    FileOutputStream out = new FileOutputStream(jar, false);
    byte[] buf = new byte[16384];
    int n;
    while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    in.close();
    out.flush();
    out.close();
    jar.setReadOnly();
    runtimeLoader = new DexClassLoader(jar.getAbsolutePath(), codeDir.getAbsolutePath(), null, activity.getClassLoader());
  }

  private View newRuntimeView(String className) throws Exception {
    ensureRuntime();
    Class<?> c = Class.forName(className, true, runtimeLoader);
    Constructor<?> k = c.getDeclaredConstructor(Context.class, GateCraftGame.class);
    k.setAccessible(true);
    Object v = k.newInstance(activity, this);
    if (!(v instanceof View)) throw new IllegalStateException("Runtime class is not a View: " + className);
    return (View) v;
  }

  private void enterLandscape() {
    try {
      previousOrientation = activity.getRequestedOrientation();
      if (previousOrientation != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
        orientationChanged = true;
        activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
      }
    } catch (Throwable ignored) { orientationChanged = false; }
  }

  private void restoreOrientation() {
    if (!orientationChanged) return;
    orientationChanged = false;
    try { activity.setRequestedOrientation(previousOrientation); } catch (Throwable ignored) {}
  }

  private void showGameOverlay() {
    try {
      closeGameOverlay(false);
      ensureRuntime();
      enterLandscape();
      dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
      dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      dialog.setCancelable(true);
      dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
        @Override public void onDismiss(DialogInterface d) {
          releaseActive();
          dialog = null;
          started = false;
          restoreOrientation();
        }
      });
      showLauncherInsideDialog();
      Window w = dialog.getWindow();
      if (w != null) {
        w.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
      }
      started = true;
      dialog.show();
      w = dialog.getWindow();
      if (w != null) {
        w.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        View decor = w.getDecorView();
        if (decor != null) decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LOW_PROFILE);
      }
    } catch (Throwable t) {
      closeGameOverlay(false);
    }
  }

  private void setActive(View v) {
    releaseActive();
    activeView = v;
    metrics = (v instanceof GameMetrics) ? (GameMetrics) v : null;
    if (metrics != null) {
      metrics.setLanguage(language);
      metrics.setPaused(false);
    }
    if (dialog != null) dialog.setContentView(v);
  }

  private void showLauncherInsideDialog() throws Exception {
    setActive(newRuntimeView("com.gatecraft.game.GameLauncherV6View"));
  }

  public void launchMode(int mode) {
    try {
      if (mode == 1) setActive(newRuntimeView("com.gatecraft.game.WorkshopRunV6View"));
      else if (mode == 2) setActive(newRuntimeView("com.gatecraft.game.HeroesCraftV6View"));
      else if (mode == 3) setActive(newRuntimeView("com.gatecraft.game.MetalFighterV6View"));
    } catch (Throwable t) {
      closeGameOverlay(false);
    }
  }

  public void returnToLauncher() {
    try { showLauncherInsideDialog(); } catch (Throwable t) { closeGameOverlay(false); }
  }
  public int getCalculationCount() { return calculationCount; }
  public boolean isTestMode() { return testMode; }
  public int getLanguage() { return language; }
  public boolean modeUnlocked(int mode) {
    if (mode <= 1) return calculationCount >= 1;
    if (testMode) return calculationCount >= 1;
    if (mode == 2) return calculationCount >= 10;
    if (mode == 3) return calculationCount >= 20;
    return false;
  }
  public int requiredFor(int mode) {
    if (mode <= 1) return 1;
    if (mode == 2) return 10;
    if (mode == 3) return 20;
    return 999;
  }

  private void releaseActive() {
    if (metrics != null) {
      try { metrics.shutdown(); } catch (Throwable ignored) {}
    }
    activeView = null;
    metrics = null;
  }

  private void closeGameOverlay(boolean dispatchExit) {
    started = false;
    releaseActive();
    try { if (dialog != null && dialog.isShowing()) dialog.dismiss(); } catch (Throwable ignored) {}
    dialog = null;
    restoreOrientation();
    if (dispatchExit) EventDispatcher.dispatchEvent(this, "ExitRequested");
  }

  public void requestExitFromView() { closeGameOverlay(true); }
  public void reportScore(int score) { EventDispatcher.dispatchEvent(this, "ScoreChanged", score); }
  public void reportLevelComplete(int level, int score) { EventDispatcher.dispatchEvent(this, "LevelCompleted", level, score); }
  public void reportGameComplete(int score) { EventDispatcher.dispatchEvent(this, "GameCompleted", score); }
  public void reportGameOver(int score) { EventDispatcher.dispatchEvent(this, "GameOver", score); }

  @SimpleFunction(description="Opens the GateCraft arcade launcher in full-screen landscape.") public void StartGame() { showGameOverlay(); }
  @SimpleFunction(description="Pauses the active arcade game.") public void PauseGame() { if (metrics != null) metrics.setPaused(true); }
  @SimpleFunction(description="Resumes the active arcade game.") public void ResumeGame() { if (started && metrics != null) metrics.setPaused(false); }
  @SimpleFunction(description="Restarts the active arcade game.") public void RestartLevel() { if (metrics != null) metrics.restart(); }
  @SimpleFunction(description="Stops the arcade suite and releases the active game view.") public void StopGame() { closeGameOverlay(false); }
  @SimpleFunction(description="Sets the shared GateCraft calculation counter used by game unlocks.") public void SetCalculationCount(int count) { calculationCount = Math.max(0, count); if (activeView != null) activeView.invalidate(); }
  @SimpleFunction(description="Sets GateCraft language index 1..12.") public void SetLanguage(int lang) { language = Math.max(1, Math.min(12, lang)); if (metrics != null) metrics.setLanguage(language); if (activeView != null) activeView.invalidate(); }
  @SimpleFunction(description="Testing switch. True unlocks staged games after the first calculation; false uses production thresholds.") public void SetTestMode(boolean enabled) { testMode = enabled; if (activeView != null) activeView.invalidate(); }
  @SimpleFunction(description="Returns active game score.") public int Score() { return metrics == null ? 0 : metrics.score(); }
  @SimpleFunction(description="Returns active game level.") public int Level() { return metrics == null ? 1 : metrics.level(); }
  @SimpleFunction(description="Returns active game lives.") public int Lives() { return metrics == null ? 3 : metrics.lives(); }
  @SimpleFunction(description="Returns extension version.") public String Version() { return "6.0.3"; }

  @SimpleEvent(description="Raised whenever visible game score changes.") public void ScoreChanged(int score) { EventDispatcher.dispatchEvent(this, "ScoreChanged", score); }
  @SimpleEvent(description="Raised after a Workshop Run level is completed.") public void LevelCompleted(int level, int score) { EventDispatcher.dispatchEvent(this, "LevelCompleted", level, score); }
  @SimpleEvent(description="Raised after a game is completed.") public void GameCompleted(int score) { EventDispatcher.dispatchEvent(this, "GameCompleted", score); }
  @SimpleEvent(description="Raised when a game is lost.") public void GameOver(int score) { EventDispatcher.dispatchEvent(this, "GameOver", score); }
  @SimpleEvent(description="Raised when player closes the arcade overlay.") public void ExitRequested() { EventDispatcher.dispatchEvent(this, "ExitRequested"); }

  @Override public void onPause() { if (metrics != null) metrics.setPaused(true); }
  @Override public void onResume() { if (started && metrics != null) metrics.setPaused(false); }
  @Override public void onDestroy() {
    closeGameOverlay(false);
    mainHandler.removeCallbacksAndMessages(null);
    runtimeLoader = null;
  }
}
