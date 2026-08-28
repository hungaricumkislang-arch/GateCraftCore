package com.gatecraft.game;

public interface GameMetrics {
  int score();
  int level();
  int lives();
  void setLanguage(int lang);
  void setPaused(boolean paused);
  void restart();
  void shutdown();
}
