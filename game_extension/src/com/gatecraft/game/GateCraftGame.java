package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.OnDestroyListener;
import com.google.appinventor.components.runtime.OnPauseListener;
import com.google.appinventor.components.runtime.OnResumeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

@DesignerComponent(
    version = 1,
    description = "GateCraft Workshop Run: isolated offline pixel platformer easter egg.",
    category = ComponentCategory.EXTENSION,
    nonVisible = false,
    iconName = "")
@SimpleObject(external = true)
public class GateCraftGame extends AndroidViewComponent implements OnPauseListener, OnResumeListener, OnDestroyListener {

  private final GameView gameView;
  private boolean started;

  public GateCraftGame(ComponentContainer container) {
    super(container);
    gameView = new GameView(container.$context());
    container.$add(this);
    Width(Component.LENGTH_FILL_PARENT);
    Height(Component.LENGTH_FILL_PARENT);
    container.$form().registerForOnPause(this);
    container.$form().registerForOnResume(this);
    container.$form().registerForOnDestroy(this);
  }

  @Override
  public View getView() {
    return gameView;
  }

  @SimpleFunction(description = "Starts the Workshop Run game at level 1.")
  public void StartGame() {
    started = true;
    gameView.startNewGame();
  }

  @SimpleFunction(description = "Pauses the game loop without losing progress.")
  public void PauseGame() {
    gameView.setPaused(true);
  }

  @SimpleFunction(description = "Resumes a paused game.")
  public void ResumeGame() {
    if (started) gameView.setPaused(false);
  }

  @SimpleFunction(description = "Restarts the current level.")
  public void RestartLevel() {
    if (started) gameView.restartLevel();
  }

  @SimpleFunction(description = "Stops the game and releases transient resources.")
  public void StopGame() {
    started = false;
    gameView.shutdown();
  }

  @SimpleFunction(description = "Returns the current score.")
  public int Score() {
    return gameView.score;
  }

  @SimpleFunction(description = "Returns the current level number (1..3).")
  public int Level() {
    return gameView.level + 1;
  }

  @SimpleFunction(description = "Returns remaining lives.")
  public int Lives() {
    return gameView.lives;
  }

  @SimpleFunction(description = "Returns the extension version.")
  public String Version() {
    return "1.0.0";
  }

  @SimpleEvent(description = "Raised whenever the visible score changes.")
  public void ScoreChanged(int score) {
    EventDispatcher.dispatchEvent(this, "ScoreChanged", score);
  }

  @SimpleEvent(description = "Raised after a level is completed.")
  public void LevelCompleted(int level, int score) {
    EventDispatcher.dispatchEvent(this, "LevelCompleted", level, score);
  }

  @SimpleEvent(description = "Raised after all three workshop levels are completed.")
  public void GameCompleted(int score) {
    EventDispatcher.dispatchEvent(this, "GameCompleted", score);
  }

  @SimpleEvent(description = "Raised after all lives are lost.")
  public void GameOver(int score) {
    EventDispatcher.dispatchEvent(this, "GameOver", score);
  }

  @SimpleEvent(description = "Raised when the player taps the in-game EXIT control.")
  public void ExitRequested() {
    EventDispatcher.dispatchEvent(this, "ExitRequested");
  }

  @Override
  public void onPause() {
    gameView.setPaused(true);
  }

  @Override
  public void onResume() {
    if (started) gameView.setPaused(false);
  }

  @Override
  public void onDestroy() {
    started = false;
    gameView.shutdown();
  }

  private final class GameView extends View {
    private final Paint p = new Paint();
    private final Random rnd = new Random(74291L);
    private ToneGenerator tones;
    private final List<Platform> platforms = new ArrayList<Platform>();
    private final List<Token> tokens = new ArrayList<Token>();
    private final List<Hazard> hazards = new ArrayList<Hazard>();
    private final List<Spark> sparks = new ArrayList<Spark>();

    private float playerX, playerY, vx, vy;
    private float playerW = 34f, playerH = 46f;
    private boolean onGround;
    private boolean leftDown, rightDown, jumpDown, jumpLatch;
    private boolean paused = true;
    private boolean finished;
    private long lastFrame;
    private long levelStart;
    private float cameraX;
    private float worldWidth;
    private int score;
    private int lastReportedScore;
    private int lives = 3;
    private int level;
    private int collected;
    private int totalTokens;
    private float density;

    private final RectF exitRect = new RectF();
    private final RectF leftRect = new RectF();
    private final RectF rightRect = new RectF();
    private final RectF jumpRect = new RectF();

    GameView(Context context) {
      super(context);
      p.setAntiAlias(false);
      density = Math.max(1f, getResources().getDisplayMetrics().density);
      setFocusable(true);
      setBackgroundColor(Color.rgb(11, 18, 30));
      try {
        tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 38);
      } catch (Throwable ignored) {
        tones = null;
      }
    }

    void startNewGame() {
      score = 0;
      lastReportedScore = 0;
      lives = 3;
      level = 0;
      finished = false;
      loadLevel(0);
      paused = false;
      lastFrame = SystemClock.uptimeMillis();
      invalidate();
    }

    void restartLevel() {
      loadLevel(level);
      paused = false;
      lastFrame = SystemClock.uptimeMillis();
      invalidate();
    }

    void setPaused(boolean value) {
      paused = value;
      lastFrame = SystemClock.uptimeMillis();
      if (!value) invalidate();
    }

    void shutdown() {
      paused = true;
      leftDown = rightDown = jumpDown = false;
      platforms.clear();
      tokens.clear();
      hazards.clear();
      sparks.clear();
      if (tones != null) {
        try { tones.release(); } catch (Throwable ignored) {}
        tones = null;
      }
    }

    private void loadLevel(int which) {
      level = Math.max(0, Math.min(2, which));
      platforms.clear();
      tokens.clear();
      hazards.clear();
      sparks.clear();
      collected = 0;
      cameraX = 0;
      playerX = 80;
      playerY = 250;
      vx = vy = 0;
      onGround = false;
      worldWidth = level == 0 ? 2200 : (level == 1 ? 2600 : 3000);
      float floorY = 520;
      addPlatform(0, floorY, 520, 80);
      addPlatform(610, floorY, 360, 80);
      addPlatform(1060, floorY, 460, 80);
      addPlatform(1610, floorY, 590, 80);
      if (level >= 1) addPlatform(2290, floorY, 310, 80);
      if (level >= 2) addPlatform(2680, floorY, 320, 80);

      if (level == 0) {
        addPlatform(250, 410, 230, 25);
        addPlatform(555, 330, 190, 25);
        addPlatform(835, 390, 180, 25);
        addPlatform(1150, 300, 230, 25);
        addPlatform(1460, 390, 180, 25);
        addPlatform(1740, 315, 220, 25);
        addHazard(520, 500, 55, 20, 0);
        addHazard(985, 500, 55, 20, 0);
        addHazard(1535, 500, 55, 20, 0);
        addHazard(1370, 365, 28, 28, 1);
        tokenLine(300, 360, 3, 60);
        tokenLine(575, 280, 3, 55);
        tokenLine(1180, 250, 4, 55);
        tokenLine(1755, 265, 4, 55);
      } else if (level == 1) {
        addPlatform(210, 405, 180, 25);
        addPlatform(470, 340, 220, 25);
        addPlatform(780, 280, 190, 25);
        addPlatform(1080, 390, 250, 25);
        addPlatform(1430, 325, 180, 25);
        addPlatform(1710, 260, 220, 25);
        addPlatform(2050, 360, 220, 25);
        addPlatform(2360, 285, 180, 25);
        addHazard(400, 495, 65, 25, 2);
        addHazard(980, 495, 65, 25, 2);
        addHazard(1540, 495, 65, 25, 2);
        addHazard(2215, 495, 65, 25, 2);
        addHazard(690, 315, 30, 30, 1);
        addHazard(1605, 300, 30, 30, 1);
        tokenLine(230, 355, 3, 55);
        tokenLine(500, 290, 4, 50);
        tokenLine(805, 230, 3, 55);
        tokenLine(1740, 210, 4, 55);
        tokenLine(2380, 235, 3, 55);
      } else {
        addPlatform(180, 420, 190, 25);
        addPlatform(460, 350, 180, 25);
        addPlatform(730, 290, 180, 25);
        addPlatform(1000, 230, 180, 25);
        addPlatform(1280, 330, 220, 25);
        addPlatform(1600, 260, 180, 25);
        addPlatform(1880, 390, 250, 25);
        addPlatform(2220, 305, 180, 25);
        addPlatform(2500, 245, 190, 25);
        addPlatform(2780, 385, 180, 25);
        addHazard(375, 495, 70, 25, 2);
        addHazard(920, 495, 70, 25, 0);
        addHazard(1510, 495, 70, 25, 2);
        addHazard(2140, 495, 70, 25, 0);
        addHazard(2630, 495, 70, 25, 2);
        addHazard(640, 320, 31, 31, 1);
        addHazard(1485, 300, 31, 31, 1);
        addHazard(2405, 275, 31, 31, 1);
        tokenLine(200, 370, 3, 55);
        tokenLine(485, 300, 3, 55);
        tokenLine(755, 240, 3, 55);
        tokenLine(1020, 180, 3, 55);
        tokenLine(1630, 210, 3, 55);
        tokenLine(2240, 255, 3, 55);
        tokenLine(2520, 195, 3, 55);
        tokenLine(2800, 335, 3, 55);
      }
      totalTokens = tokens.size();
      levelStart = SystemClock.uptimeMillis();
      lastFrame = levelStart;
    }

    private void addPlatform(float x, float y, float w, float h) {
      platforms.add(new Platform(x, y, w, h));
    }

    private void addHazard(float x, float y, float w, float h, int type) {
      hazards.add(new Hazard(x, y, w, h, type));
    }

    private void tokenLine(float x, float y, int n, float gap) {
      for (int i = 0; i < n; i++) tokens.add(new Token(x + i * gap, y));
    }

    @Override
    protected void onDraw(Canvas c) {
      super.onDraw(c);
      long now = SystemClock.uptimeMillis();
      float dt = Math.min(0.035f, Math.max(0f, (now - lastFrame) / 1000f));
      lastFrame = now;
      if (!paused && !finished && getWidth() > 0 && getHeight() > 0) update(dt, now);
      drawScene(c, now);
      if (!paused && !finished) postInvalidateOnAnimation();
    }

    private void update(float dt, long now) {
      float speed = 230f;
      float accel = 1100f;
      float target = leftDown == rightDown ? 0 : (leftDown ? -speed : speed);
      if (vx < target) vx = Math.min(target, vx + accel * dt);
      if (vx > target) vx = Math.max(target, vx - accel * dt);
      if (jumpDown && !jumpLatch && onGround) {
        vy = -475f;
        onGround = false;
        jumpLatch = true;
        tone(ToneGenerator.TONE_PROP_BEEP, 55);
      }
      if (!jumpDown) jumpLatch = false;
      vy += 980f * dt;
      if (vy > 780f) vy = 780f;

      float oldY = playerY;
      playerX += vx * dt;
      playerY += vy * dt;
      playerX = Math.max(0, Math.min(worldWidth - playerW, playerX));
      onGround = false;

      RectF pr = playerRect();
      for (Platform pl : platforms) {
        if (playerX + playerW > pl.x && playerX < pl.x + pl.w) {
          float prevBottom = oldY + playerH;
          float newBottom = playerY + playerH;
          if (vy >= 0 && prevBottom <= pl.y + 6 && newBottom >= pl.y && playerY < pl.y) {
            playerY = pl.y - playerH;
            vy = 0;
            onGround = true;
            pr = playerRect();
          }
        }
      }

      for (Token t : tokens) {
        if (!t.got && RectF.intersects(pr, new RectF(t.x - 15, t.y - 15, t.x + 15, t.y + 15))) {
          t.got = true;
          collected++;
          score += 100;
          tone(ToneGenerator.TONE_PROP_ACK, 45);
        }
      }

      for (Hazard h : hazards) {
        h.phase += dt;
        float hx = h.x;
        if (h.type == 2) hx += (float)Math.sin(h.phase * 2.2f) * 32f;
        if (RectF.intersects(pr, new RectF(hx, h.y, hx + h.w, h.y + h.h))) {
          loseLife();
          break;
        }
      }

      if (playerY > 680) loseLife();

      float viewW = getWidth();
      cameraX = playerX - viewW * 0.38f;
      cameraX = Math.max(0, Math.min(Math.max(0, worldWidth - viewW), cameraX));

      if (playerX > worldWidth - 105) completeLevel();
      if (score != lastReportedScore) {
        lastReportedScore = score;
        GateCraftGame.this.ScoreChanged(score);
      }

      if (rnd.nextFloat() < 0.12f) {
        Hazard nearest = hazards.size() == 0 ? null : hazards.get(rnd.nextInt(hazards.size()));
        if (nearest != null && nearest.type == 0) sparks.add(new Spark(nearest.x + rnd.nextFloat() * nearest.w, nearest.y - 3));
      }
      for (int i = sparks.size() - 1; i >= 0; i--) {
        Spark s = sparks.get(i);
        s.life -= dt;
        s.x += s.vx * dt;
        s.y += s.vy * dt;
        s.vy += 250f * dt;
        if (s.life <= 0) sparks.remove(i);
      }
    }

    private RectF playerRect() {
      return new RectF(playerX + 5, playerY + 3, playerX + playerW - 5, playerY + playerH);
    }

    private void loseLife() {
      lives--;
      tone(ToneGenerator.TONE_PROP_NACK, 130);
      if (lives <= 0) {
        paused = true;
        finished = true;
        GateCraftGame.this.GameOver(score);
      } else {
        playerX = Math.max(50, cameraX + 70);
        playerY = 180;
        vx = vy = 0;
      }
    }

    private void completeLevel() {
      int bonus = Math.max(0, 3000 - (int)((SystemClock.uptimeMillis() - levelStart) / 10));
      score += 1000 + bonus + collected * 25;
      tone(ToneGenerator.TONE_PROP_ACK, 180);
      GateCraftGame.this.LevelCompleted(level + 1, score);
      if (level >= 2) {
        paused = true;
        finished = true;
        GateCraftGame.this.GameCompleted(score);
      } else {
        loadLevel(level + 1);
      }
    }

    private void tone(int type, int ms) {
      if (tones != null) {
        try { tones.startTone(type, ms); } catch (Throwable ignored) {}
      }
    }

    private void drawScene(Canvas c, long now) {
      int w = getWidth(), h = getHeight();
      if (w <= 0 || h <= 0) return;
      float sy = h / 620f;
      c.save();
      c.scale(sy, sy);
      float logicalW = w / sy;

      drawBackground(c, logicalW, now);
      c.save();
      c.translate(-cameraX, 0);
      drawWorld(c, now);
      c.restore();
      drawHudAndControls(c, logicalW, h / sy, now);
      c.restore();
    }

    private void drawBackground(Canvas c, float logicalW, long now) {
      p.setStyle(Paint.Style.FILL);
      p.setColor(Color.rgb(11, 18, 30));
      c.drawRect(0, 0, logicalW, 620, p);
      p.setColor(Color.rgb(23, 35, 53));
      for (int x = -40; x < logicalW + 80; x += 120) c.drawRect(x, 72, x + 4, 520, p);
      p.setColor(Color.rgb(28, 44, 64));
      for (int y = 100; y < 510; y += 84) c.drawRect(0, y, logicalW, y + 3, p);
      p.setColor(Color.rgb(17, 28, 42));
      for (int i = 0; i < 8; i++) {
        float gx = ((i * 157) - (cameraX * 0.18f)) % (logicalW + 180) - 90;
        drawGear(c, gx, 155 + (i % 3) * 105, 24 + (i % 2) * 8, false);
      }
      p.setColor(Color.rgb(61, 67, 77));
      c.drawRect(logicalW - 150, 60, logicalW - 140, 220, p);
      c.drawRect(logicalW - 210, 60, logicalW - 200, 220, p);
      p.setColor(Color.rgb(197, 126, 46));
      c.drawRect(logicalW - 213, 205, logicalW - 137, 213, p);
      p.setColor(Color.rgb(255, 202, 85));
      c.drawCircle(logicalW - 175, 226, 12 + (float)Math.sin(now / 160.0) * 2, p);
    }

    private void drawWorld(Canvas c, long now) {
      for (Platform pl : platforms) drawPlatform(c, pl);
      for (Token t : tokens) if (!t.got) drawToken(c, t.x, t.y, now);
      for (Hazard hz : hazards) drawHazard(c, hz, now);
      for (Spark s : sparks) {
        p.setColor(s.life > 0.25f ? Color.rgb(255, 220, 70) : Color.rgb(255, 120, 40));
        c.drawRect(s.x, s.y, s.x + 4, s.y + 4, p);
      }
      drawExitGate(c, worldWidth - 86, 432, now);
      drawHero(c, playerX, playerY, vx, vy);
    }

    private void drawPlatform(Canvas c, Platform pl) {
      p.setColor(Color.rgb(54, 62, 73));
      c.drawRect(pl.x, pl.y, pl.x + pl.w, pl.y + pl.h, p);
      p.setColor(Color.rgb(92, 72, 62));
      c.drawRect(pl.x + 3, pl.y + 3, pl.x + pl.w - 3, pl.y + 10, p);
      p.setColor(Color.rgb(133, 85, 54));
      c.drawRect(pl.x + 5, pl.y + 5, pl.x + pl.w - 5, pl.y + 7, p);
      p.setColor(Color.rgb(28, 33, 41));
      c.drawRect(pl.x, pl.y + pl.h - 8, pl.x + pl.w, pl.y + pl.h, p);
      p.setColor(Color.rgb(104, 112, 122));
      for (float bx = pl.x + 18; bx < pl.x + pl.w - 8; bx += 42) c.drawCircle(bx, pl.y + 16, 3, p);
    }

    private void drawHero(Canvas c, float x, float y, float dx, float dy) {
      boolean faceLeft = dx < -5;
      float bob = onGround && Math.abs(dx) > 20 ? ((SystemClock.uptimeMillis() / 90) % 2 == 0 ? 2 : 0) : 0;
      y += bob;
      p.setColor(Color.rgb(31, 45, 55));
      c.drawRect(x + 8, y + 12, x + 28, y + 39, p);
      p.setColor(Color.rgb(38, 87, 105));
      c.drawRect(x + 5, y + 18, x + 31, y + 36, p);
      p.setColor(Color.rgb(196, 131, 76));
      c.drawRect(x + 11, y + 5, x + 27, y + 18, p);
      p.setColor(Color.rgb(24, 32, 38));
      c.drawRect(x + 8, y + 1, x + 30, y + 8, p);
      p.setColor(Color.rgb(44, 177, 190));
      c.drawRect(x + 9, y + 8, x + 29, y + 13, p);
      p.setColor(Color.rgb(17, 22, 27));
      c.drawRect(x + 11, y + 10, x + 27, y + 12, p);
      p.setColor(Color.rgb(38, 87, 105));
      c.drawRect(x + 5, y + 36, x + 14, y + 46, p);
      c.drawRect(x + 22, y + 36, x + 31, y + 46, p);
      p.setColor(Color.rgb(57, 48, 42));
      c.drawRect(x + 3, y + 44, x + 15, y + 48, p);
      c.drawRect(x + 21, y + 44, x + 33, y + 48, p);
      p.setColor(Color.rgb(196, 131, 76));
      c.drawRect(faceLeft ? x : x + 29, y + 21, faceLeft ? x + 7 : x + 36, y + 27, p);
      p.setColor(Color.rgb(178, 184, 188));
      if (faceLeft) {
        c.drawRect(x - 10, y + 20, x + 1, y + 23, p);
        c.drawRect(x - 14, y + 17, x - 10, y + 25, p);
      } else {
        c.drawRect(x + 35, y + 20, x + 46, y + 23, p);
        c.drawRect(x + 45, y + 17, x + 49, y + 25, p);
      }
    }

    private void drawToken(Canvas c, float x, float y, long now) {
      float pulse = 1f + 0.10f * (float)Math.sin(now / 120.0 + x);
      p.setColor(Color.rgb(255, 194, 44));
      c.drawCircle(x, y, 11 * pulse, p);
      p.setColor(Color.rgb(120, 75, 22));
      c.drawCircle(x, y, 5 * pulse, p);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(3);
      p.setColor(Color.rgb(255, 231, 122));
      c.drawCircle(x, y, 8 * pulse, p);
      p.setStyle(Paint.Style.FILL);
    }

    private void drawHazard(Canvas c, Hazard h, long now) {
      float x = h.x;
      if (h.type == 2) x += (float)Math.sin(h.phase * 2.2f) * 32f;
      if (h.type == 0) {
        p.setColor(Color.rgb(120, 75, 42));
        c.drawRect(x, h.y, x + h.w, h.y + h.h, p);
        p.setColor(Color.rgb(255, 183, 48));
        c.drawRect(x + 8, h.y - 3, x + h.w - 8, h.y + 3, p);
        p.setColor(Color.rgb(255, 224, 96));
        for (int i = 0; i < 4; i++) {
          float sx = x + 9 + i * (h.w - 18) / 3f;
          c.drawRect(sx, h.y - 10 - (i % 2) * 6, sx + 3, h.y - 2, p);
        }
      } else if (h.type == 1) {
        drawGear(c, x + h.w / 2f, h.y + h.h / 2f, h.w / 2f, true);
      } else {
        p.setColor(Color.rgb(117, 70, 44));
        c.drawOval(new RectF(x, h.y, x + h.w, h.y + h.h), p);
        p.setColor(Color.rgb(53, 38, 31));
        c.drawRect(x + 5, h.y + 6, x + h.w - 5, h.y + 9, p);
        c.drawRect(x + 5, h.y + h.h - 9, x + h.w - 5, h.y + h.h - 6, p);
      }
    }

    private void drawGear(Canvas c, float cx, float cy, float r, boolean danger) {
      p.setColor(danger ? Color.rgb(115, 121, 128) : Color.rgb(34, 48, 64));
      c.drawCircle(cx, cy, r, p);
      for (int i = 0; i < 8; i++) {
        double a = Math.PI * 2 * i / 8.0;
        float tx = cx + (float)Math.cos(a) * r;
        float ty = cy + (float)Math.sin(a) * r;
        c.drawRect(tx - 5, ty - 5, tx + 5, ty + 5, p);
      }
      p.setColor(danger ? Color.rgb(45, 50, 55) : Color.rgb(14, 24, 36));
      c.drawCircle(cx, cy, r * 0.42f, p);
    }

    private void drawExitGate(Canvas c, float x, float y, long now) {
      p.setColor(Color.rgb(84, 90, 99));
      c.drawRect(x, y - 95, x + 12, y + 88, p);
      c.drawRect(x + 55, y - 95, x + 67, y + 88, p);
      c.drawRect(x, y - 95, x + 67, y - 82, p);
      p.setColor(((now / 300) % 2 == 0) ? Color.rgb(56, 220, 112) : Color.rgb(28, 125, 66));
      c.drawRect(x + 22, y - 72, x + 45, y - 49, p);
      p.setColor(Color.rgb(20, 30, 35));
      c.drawRect(x + 29, y - 66, x + 38, y - 55, p);
    }

    private void drawHudAndControls(Canvas c, float logicalW, float logicalH, long now) {
      p.setColor(Color.rgb(24, 35, 49));
      c.drawRect(0, 0, logicalW, 48, p);
      p.setColor(Color.rgb(83, 98, 116));
      c.drawRect(0, 45, logicalW, 48, p);
      p.setTypeface(android.graphics.Typeface.MONOSPACE);
      p.setFakeBoldText(true);
      p.setTextSize(20);
      p.setColor(Color.WHITE);
      String lvl = "LEVEL 1-" + (level + 1);
      String scr = String.format(Locale.US, "SCORE %06d", score);
      String liv = "LIVES " + lives;
      long elapsed = Math.max(0, (now - levelStart) / 1000);
      long remain = Math.max(0, 180 - elapsed);
      String tim = String.format(Locale.US, "TIME %d:%02d", remain / 60, remain % 60);
      c.drawText(lvl, 20, 31, p);
      c.drawText(scr, Math.max(180, logicalW * 0.27f), 31, p);
      c.drawText(liv, Math.max(390, logicalW * 0.56f), 31, p);
      c.drawText(tim, Math.max(560, logicalW - 150), 31, p);
      p.setFakeBoldText(false);
      p.setTextSize(12);
      p.setColor(Color.rgb(255, 215, 86));
      c.drawText("GEARS " + collected + "/" + totalTokens, 20, 66, p);

      exitRect.set(logicalW - 84, 56, logicalW - 14, 94);
      p.setColor(Color.argb(190, 90, 45, 38));
      c.drawRect(exitRect, p);
      p.setColor(Color.WHITE);
      p.setTextSize(16);
      p.setFakeBoldText(true);
      c.drawText("EXIT", logicalW - 72, 81, p);
      p.setFakeBoldText(false);

      float by = logicalH - 88;
      leftRect.set(20, by, 88, by + 64);
      rightRect.set(102, by, 170, by + 64);
      jumpRect.set(logicalW - 112, by - 4, logicalW - 20, by + 68);
      drawControl(c, leftRect, "<", leftDown);
      drawControl(c, rightRect, ">", rightDown);
      drawControl(c, jumpRect, "JUMP", jumpDown);

      if (paused && started && !finished) {
        p.setColor(Color.argb(175, 5, 8, 13));
        c.drawRect(0, 95, logicalW, logicalH - 105, p);
        p.setColor(Color.WHITE);
        p.setTextSize(32);
        p.setFakeBoldText(true);
        c.drawText("PAUSED", logicalW / 2f - 62, logicalH / 2f, p);
        p.setFakeBoldText(false);
      }
      if (finished) {
        p.setColor(Color.argb(190, 5, 8, 13));
        c.drawRect(0, 95, logicalW, logicalH - 105, p);
        p.setColor(Color.rgb(255, 215, 86));
        p.setTextSize(28);
        p.setFakeBoldText(true);
        String msg = lives > 0 ? "MASTER OF THE 10 MM WRENCH" : "SHIFT OVER";
        c.drawText(msg, Math.max(30, logicalW / 2f - msg.length() * 8f), logicalH / 2f - 8, p);
        p.setColor(Color.WHITE);
        p.setTextSize(18);
        c.drawText("SCORE " + score + "   tap JUMP to restart", Math.max(30, logicalW / 2f - 145), logicalH / 2f + 30, p);
        p.setFakeBoldText(false);
      }
    }

    private void drawControl(Canvas c, RectF r, String label, boolean down) {
      p.setColor(down ? Color.argb(210, 201, 126, 45) : Color.argb(125, 95, 105, 118));
      c.drawRect(r, p);
      p.setStyle(Paint.Style.STROKE);
      p.setStrokeWidth(2);
      p.setColor(Color.argb(220, 220, 224, 228));
      c.drawRect(r, p);
      p.setStyle(Paint.Style.FILL);
      p.setTextSize(label.length() > 1 ? 14 : 28);
      p.setColor(Color.WHITE);
      p.setFakeBoldText(true);
      float tx = r.centerX() - (label.length() > 1 ? 18 : 8);
      c.drawText(label, tx, r.centerY() + (label.length() > 1 ? 5 : 10), p);
      p.setFakeBoldText(false);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      float sy = getHeight() / 620f;
      if (sy <= 0) sy = 1;
      int action = e.getActionMasked();
      int idx = e.getActionIndex();
      if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
        float x = e.getX(idx) / sy, y = e.getY(idx) / sy;
        if (exitRect.contains(x, y)) {
          GateCraftGame.this.ExitRequested();
          return true;
        }
        if (finished && jumpRect.contains(x, y)) {
          startNewGame();
          return true;
        }
      }
      leftDown = rightDown = jumpDown = false;
      if (action != MotionEvent.ACTION_UP && action != MotionEvent.ACTION_CANCEL) {
        for (int i = 0; i < e.getPointerCount(); i++) {
          if (action == MotionEvent.ACTION_POINTER_UP && i == idx) continue;
          float x = e.getX(i) / sy, y = e.getY(i) / sy;
          if (leftRect.contains(x, y)) leftDown = true;
          if (rightRect.contains(x, y)) rightDown = true;
          if (jumpRect.contains(x, y)) jumpDown = true;
        }
      }
      invalidate();
      return true;
    }
  }

  private static final class Platform {
    final float x, y, w, h;
    Platform(float x, float y, float w, float h) { this.x=x; this.y=y; this.w=w; this.h=h; }
  }
  private static final class Token {
    final float x, y; boolean got;
    Token(float x, float y) { this.x=x; this.y=y; }
  }
  private static final class Hazard {
    final float x, y, w, h; final int type; float phase;
    Hazard(float x, float y, float w, float h, int type) { this.x=x; this.y=y; this.w=w; this.h=h; this.type=type; }
  }
  private static final class Spark {
    float x, y, vx, vy, life;
    Spark(float x, float y) {
      this.x=x; this.y=y; this.vx=(float)(Math.random()*80-40); this.vy=(float)(-100-Math.random()*100); this.life=0.45f;
    }
  }
}
