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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

final class GameSuiteView extends View {
  private static final float LW = 960f;
  private static final float LH = 600f;
  private final GateCraftGame owner;
  private final Paint p = new Paint();
  private final Random rnd = new Random(90210L);
  private ToneGenerator tones;
  private int lang = 2;
  private int calcCount;
  private boolean testMode = true;
  private boolean paused;
  private int active; // 0 hub, 1 workshop, 2 heroes, 3 fighter, 4 inspection, 5 chronicles
  private long lastFrame;
  private float scale = 1f, ox, oy;
  private final RectF backRect = new RectF(788, 12, 864, 48);
  private final RectF exitRect = new RectF(874, 12, 948, 48);

  // Workshop Run
  private final List<Platform> platforms = new ArrayList<Platform>();
  private final List<Token> tokens = new ArrayList<Token>();
  private final List<Hazard> hazards = new ArrayList<Hazard>();
  private float wx, wy, wvx, wvy, cameraX, worldW;
  private boolean wGround, wLeft, wRight, wJump, wJumpLatch;
  private int wScore, wLives = 3, wLevel, wCollected;
  private final RectF wLeftRect = new RectF(28, 526, 88, 576);
  private final RectF wRightRect = new RectF(100, 526, 160, 576);
  private final RectF wJumpRect = new RectF(856, 520, 928, 578);

  // Heroes of Craft & Gates
  private int heroPage; // 0 map, 1 town, 2 combat
  private int heroX = 1, heroY = 6;
  private int iron, cement, tools, gold, heroScore;
  private int shopLevel = 1, weldLevel = 0, truckLevel = 0;
  private int battleEnemyHp = 160, battleOwnHp = 180;
  private final List<ResourceNode> heroNodes = new ArrayList<ResourceNode>();
  private final RectF hUp = new RectF(70, 472, 118, 520);
  private final RectF hDown = new RectF(70, 532, 118, 580);
  private final RectF hLeft = new RectF(16, 532, 64, 580);
  private final RectF hRight = new RectF(124, 532, 172, 580);
  private final RectF hTown = new RectF(720, 510, 810, 558);
  private final RectF hCombat = new RectF(820, 510, 940, 558);

  // Metal Fighter
  private float fPX = 190, fEX = 720;
  private int fPHP = 100, fEHP = 100, fScore;
  private float fAiTimer, fSpecialCd;
  private boolean fLeft, fRight, fBlock;
  private final RectF fLeftRect = new RectF(24, 520, 82, 574);
  private final RectF fRightRect = new RectF(94, 520, 152, 574);
  private final RectF fHitRect = new RectF(735, 520, 800, 574);
  private final RectF fBlockRect = new RectF(810, 520, 875, 574);
  private final RectF fArcRect = new RectF(885, 514, 945, 574);

  // The Inspection
  private float iHunger = 78, iEnergy = 74, iCoffee = 62, iSafety = 55, iHeat = 24;
  private int iScore;
  private float iWorkTime;
  private final RectF[] inspectionButtons = new RectF[] {
      new RectF(52, 505, 210, 566), new RectF(225, 505, 383, 566),
      new RectF(398, 505, 556, 566), new RectF(571, 505, 729, 566),
      new RectF(744, 505, 902, 566)};

  // GateCraft Chronicles
  private float cX = 480, cY = 310, cJoyX, cJoyY;
  private int cHp = 100, cXp, cLevel = 1, cLoot, cScore;
  private float cAttackCd, cSparkCd, cShield, cHammerCd;
  private final List<Enemy> enemies = new ArrayList<Enemy>();
  private final List<Loot> loots = new ArrayList<Loot>();
  private final RectF cAttackRect = new RectF(770, 500, 835, 565);
  private final RectF cSparkRect = new RectF(842, 492, 902, 552);
  private final RectF cShieldRect = new RectF(905, 425, 955, 475);
  private final RectF cHammerRect = new RectF(905, 492, 955, 552);
  private static final float CJOY_X = 105f, CJOY_Y = 520f, CJOY_R = 62f;

  private static final String[] TXT_GAMES = {"Játékok","Games","Spiele","Juegos","Jeux","游戏","Giochi","Jogos","Gry","Spellen","Jocuri","Игры"};
  private static final String[] TXT_LOCKED = {"Zárolva","Locked","Gesperrt","Bloqueado","Verrouillé","未解锁","Bloccato","Bloqueado","Zablokowane","Vergrendeld","Blocat","Заблокировано"};
  private static final String[] TXT_BACK = {"Vissza","Back","Zurück","Atrás","Retour","返回","Indietro","Voltar","Wstecz","Terug","Înapoi","Назад"};
  private static final String[] TXT_EXIT = {"Kilép","Exit","Ende","Salir","Quitter","退出","Esci","Sair","Wyjdź","Afsluiten","Ieșire","Выход"};
  private static final String[] TXT_SCORE = {"Pont","Score","Punkte","Puntos","Score","得分","Punti","Pontos","Punkty","Score","Scor","Счёт"};
  private static final String[] TXT_LEVEL = {"Szint","Level","Stufe","Nivel","Niveau","等级","Livello","Nível","Poziom","Niveau","Nivel","Уровень"};
  private static final String[] TXT_TOWN = {"Műhely","Town","Werkstatt","Taller","Atelier","工坊","Officina","Oficina","Warsztat","Werkplaats","Atelier","Мастерская"};
  private static final String[] TXT_COMBAT = {"Harc","Combat","Kampf","Combate","Combat","战斗","Battaglia","Combate","Walka","Gevecht","Luptă","Бой"};
  private static final String[] TXT_UPGRADE = {"Fejleszt","Upgrade","Ausbau","Mejorar","Améliorer","升级","Migliora","Melhorar","Ulepsz","Upgrade","Upgrade","Улучшить"};
  private static final String[] TXT_ATTACK = {"ÜTÉS","HIT","SCHLAG","GOLPE","FRAPPE","攻击","COLPO","GOLPE","CIOs","SLAG","LOVIT","УДАР"};
  private static final String[] TXT_BLOCK = {"VÉD","BLOCK","BLOCK","BLOQ","BLOC","防御","PARA","DEFESA","BLOK","BLOK","BLOC","БЛОК"};
  private static final String[] TXT_SPECIAL = {"ÍV","ARC","LICHTB.","ARCO","ARC","电弧","ARCO","ARCO","ŁUK","BOOG","ARC","ДУГА"};
  private static final String[] TXT_FOOD = {"Lángos","Food","Essen","Comida","Manger","食物","Cibo","Comida","Jedzenie","Eten","Mâncare","Еда"};
  private static final String[] TXT_ENERGY = {"Energia","Energy","Energie","Energía","Énergie","能量","Energia","Energia","Energia","Energie","Energie","Энергия"};
  private static final String[] TXT_COFFEE = {"Kávé","Coffee","Kaffee","Café","Café","咖啡","Caffè","Café","Kawa","Koffie","Cafea","Кофе"};
  private static final String[] TXT_GLASSES = {"Szemüveg","Glasses","Brille","Gafas","Lunettes","护目镜","Occhiali","Óculos","Okulary","Bril","Ochelari","Очки"};
  private static final String[] TXT_SHADE = {"Árnyék","Shade","Schatten","Sombra","Ombre","阴凉","Ombra","Sombra","Cień","Schaduw","Umbră","Тень"};

  GameSuiteView(Context context, GateCraftGame owner) {
    super(context);
    this.owner = owner;
    p.setAntiAlias(false);
    setFocusable(true);
    setBackgroundColor(Color.rgb(7, 12, 20));
    lastFrame = SystemClock.uptimeMillis();
  }

  void setLanguage(int value) { lang = Math.max(1, Math.min(12, value)); invalidate(); }
  void setCalculationCount(int value) { calcCount = Math.max(0, value); invalidate(); }
  void setTestMode(boolean value) { testMode = value; invalidate(); }
  int getScore() { if (active == 1) return wScore; if (active == 2) return heroScore; if (active == 3) return fScore; if (active == 4) return iScore; if (active == 5) return cScore; return 0; }
  int getLevel() { return wLevel + 1; }
  int getLives() { return wLives; }

  void setPaused(boolean value) { paused = value; lastFrame = SystemClock.uptimeMillis(); if (!value) invalidate(); }
  void restartActiveGame() { startMode(active == 0 ? 1 : active); }

  void shutdown() {
    paused = true;
    wLeft = wRight = wJump = fLeft = fRight = fBlock = false;
    cJoyX = cJoyY = 0;
    platforms.clear(); tokens.clear(); hazards.clear(); heroNodes.clear(); enemies.clear(); loots.clear();
    if (tones != null) { try { tones.release(); } catch (Throwable ignored) {} tones = null; }
  }

  private String pick(String[] values) { return values[Math.max(0, Math.min(values.length - 1, lang - 1))]; }
  private int required(int mode) {
    if (mode <= 1) return 1;
    if (testMode) return 1;
    if (mode == 2) return 10;
    if (mode == 3) return 20;
    if (mode == 4) return 30;
    return 40;
  }
  private boolean unlocked(int mode) { return calcCount >= required(mode); }

  private void tone(int code, int ms) {
    try {
      if (tones == null) tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 28);
      tones.startTone(code, ms);
    } catch (Throwable ignored) {}
  }

  @Override protected void onDraw(Canvas c) {
    super.onDraw(c);
    if (getWidth() <= 0 || getHeight() <= 0) return;
    scale = Math.min(getWidth() / LW, getHeight() / LH);
    ox = (getWidth() - LW * scale) / 2f;
    oy = (getHeight() - LH * scale) / 2f;
    c.save(); c.translate(ox, oy); c.scale(scale, scale);
    p.setStyle(Paint.Style.FILL);
    p.setColor(Color.rgb(7, 12, 20)); c.drawRect(0, 0, LW, LH, p);
    long now = SystemClock.uptimeMillis();
    float dt = Math.min(0.04f, Math.max(0f, (now - lastFrame) / 1000f)); lastFrame = now;
    if (!paused) {
      if (active == 1) updateWorkshop(dt);
      else if (active == 3) updateFighter(dt);
      else if (active == 4) updateInspection(dt);
      else if (active == 5) updateChronicles(dt);
    }
    if (active == 0) drawHub(c);
    else if (active == 1) drawWorkshop(c);
    else if (active == 2) drawHeroes(c);
    else if (active == 3) drawFighter(c);
    else if (active == 4) drawInspection(c);
    else drawChronicles(c);
    if (active != 0) drawTopButtons(c);
    c.restore();
    if (!paused && (active == 1 || active == 3 || active == 4 || active == 5)) postInvalidateDelayed(24L);
  }

  private void drawHub(Canvas c) {
    p.setColor(Color.rgb(16, 28, 43)); c.drawRect(0, 0, LW, LH, p);
    p.setColor(Color.rgb(34, 48, 63)); for (int x=0;x<960;x+=48) c.drawRect(x, 0, x+2, 600, p);
    text(c, 38, 54, "GATECRAFT // " + pick(TXT_GAMES), 28, Color.WHITE, Paint.Align.LEFT, true);
    text(c, 38, 82, "CALC " + calcCount + (testMode ? "  ·  TEST UNLOCK" : ""), 15, Color.rgb(255,210,80), Paint.Align.LEFT, false);
    String[] names = {"WORKSHOP RUN", "HEROES OF CRAFT & GATES", "METAL FIGHTER", "THE INSPECTION", "GATECRAFT CHRONICLES"};
    String[] sub = {"pixel platformer", "retro strategy / workshop / combat", "1v1 construction-site fighter", "virtual workshop life", "isometric industrial ARPG"};
    int[] colors = {Color.rgb(34,139,94),Color.rgb(154,104,39),Color.rgb(168,54,54),Color.rgb(55,116,173),Color.rgb(112,72,163)};
    for (int i=0;i<5;i++) {
      float y=112+i*88; RectF r=new RectF(38,y,922,y+72);
      p.setColor(unlocked(i+1)?colors[i]:Color.rgb(50,55,62)); c.drawRoundRect(r,8,8,p);
      p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); p.setColor(Color.rgb(215,220,226)); c.drawRoundRect(r,8,8,p); p.setStyle(Paint.Style.FILL);
      text(c,58,y+29,names[i],18,Color.WHITE,Paint.Align.LEFT,true);
      text(c,58,y+54,sub[i],12,Color.rgb(225,229,234),Paint.Align.LEFT,false);
      if (!unlocked(i+1)) text(c,900,y+42,pick(TXT_LOCKED)+" "+required(i+1),13,Color.rgb(255,190,90),Paint.Align.RIGHT,true);
      else text(c,900,y+42,"▶",24,Color.WHITE,Paint.Align.RIGHT,true);
    }
    button(c, exitRect, pick(TXT_EXIT), Color.rgb(135,48,48), false);
  }

  private void drawTopButtons(Canvas c) {
    button(c, backRect, pick(TXT_BACK), Color.rgb(55,70,88), false);
    button(c, exitRect, pick(TXT_EXIT), Color.rgb(135,48,48), false);
  }

  private void startMode(int mode) {
    if (mode < 1 || mode > 5 || !unlocked(mode)) { tone(ToneGenerator.TONE_PROP_NACK,80); return; }
    active = mode; paused = false; lastFrame = SystemClock.uptimeMillis();
    if (mode == 1) startWorkshop();
    else if (mode == 2) startHeroes();
    else if (mode == 3) startFighter();
    else if (mode == 4) startInspection();
    else startChronicles();
    invalidate();
  }

  // ---------------- Workshop Run ----------------
  private void startWorkshop() { wScore=0; wLives=3; wLevel=0; loadWorkshopLevel(0); }
  private void loadWorkshopLevel(int level) {
    wLevel=Math.max(0,Math.min(2,level)); platforms.clear(); tokens.clear(); hazards.clear();
    wx=90; wy=390; wvx=wvy=0; wGround=false; cameraX=0; wCollected=0;
    worldW=2400+wLevel*350; float floor=500;
    addPlatform(0,floor,430,100); addPlatform(520,floor,430,100); addPlatform(1040,floor,520,100); addPlatform(1650,floor,worldW-1650,100);
    int n=8+wLevel*3; for(int i=0;i<n;i++){float x=220+i*210; float y=390-(i%3)*62; addPlatform(x,y,145,18); if(i%2==0) addToken(x+70,y-30); if(i%4==2) hazards.add(new Hazard(x+155,480,38,20,i%3));}
    for(int i=0;i<8+wLevel*4;i++) addToken(300+i*180,445-(i%2)*55);
  }
  private void addPlatform(float x,float y,float w,float h){platforms.add(new Platform(x,y,w,h));}
  private void addToken(float x,float y){tokens.add(new Token(x,y));}
  private void updateWorkshop(float dt) {
    float target=(wLeft==wRight)?0:(wLeft?-240:240), acc=1250;
    if(wvx<target)wvx=Math.min(target,wvx+acc*dt); if(wvx>target)wvx=Math.max(target,wvx-acc*dt);
    if(wJump&&!wJumpLatch&&wGround){wvy=-475;wGround=false;wJumpLatch=true;tone(ToneGenerator.TONE_PROP_BEEP,45);} if(!wJump)wJumpLatch=false;
    wvy=Math.min(780,wvy+1000*dt); float oldY=wy; wx+=wvx*dt; wy+=wvy*dt; wx=Math.max(0,Math.min(worldW-32,wx)); wGround=false;
    for(Platform pl:platforms){if(wx+30>pl.x&&wx<pl.x+pl.w){float pb=oldY+44,nb=wy+44;if(wvy>=0&&pb<=pl.y+6&&nb>=pl.y){wy=pl.y-44;wvy=0;wGround=true;}}}
    RectF pr=new RectF(wx,wy,wx+30,wy+44);
    for(Token t:tokens)if(!t.got&&RectF.intersects(pr,new RectF(t.x-11,t.y-11,t.x+11,t.y+11))){t.got=true;wCollected++;wScore+=100;owner.reportScore(wScore);tone(ToneGenerator.TONE_PROP_ACK,35);}
    for(Hazard h:hazards){float hx=h.x+(h.type==2?(float)Math.sin(SystemClock.uptimeMillis()/350.0)*28:0);if(RectF.intersects(pr,new RectF(hx,h.y,hx+h.w,h.y+h.h))){wLives--;wx=Math.max(50,wx-160);wy=320;wvx=wvy=0;tone(ToneGenerator.TONE_PROP_NACK,90);if(wLives<=0){owner.reportGameOver(wScore);wLives=3;wScore=0;loadWorkshopLevel(wLevel);}break;}}
    if(wy>620){wLives--;wx=Math.max(50,wx-180);wy=300;wvy=0;}
    if(wx>worldW-120){if(wLevel<2){wLevel++;wScore+=500;owner.reportLevelComplete(wLevel,wScore);loadWorkshopLevel(wLevel);}else{owner.reportGameComplete(wScore);wScore+=1000;loadWorkshopLevel(0);}}
    float wanted=Math.max(0,Math.min(worldW-960,wx-300)); float screenX=wx-cameraX; if(screenX>650)wanted=Math.max(wanted,wx-650); if(screenX<170)wanted=Math.min(wanted,Math.max(0,wx-170)); cameraX+=(wanted-cameraX)*Math.min(1f,dt*10f); cameraX=Math.max(0,Math.min(worldW-960,cameraX));
  }
  private void drawWorkshop(Canvas c) {
    p.setColor(Color.rgb(19,35,52));c.drawRect(0,0,960,600,p); p.setColor(Color.rgb(28,52,68));c.drawRect(0,110,960,500,p);
    // workshop skyline and overhead beams
    p.setColor(Color.rgb(42,59,69)); for(int x=-((int)cameraX%180);x<1000;x+=180){c.drawRect(x,130,x+12,500,p);c.drawRect(x,150,x+150,160,p);} p.setColor(Color.rgb(75,86,92));c.drawRect(0,476,960,500,p);
    c.save();c.translate(-cameraX,0);
    for(Platform pl:platforms){p.setColor(pl.h>40?Color.rgb(73,78,82):Color.rgb(109,117,119));c.drawRect(pl.x,pl.y,pl.x+pl.w,pl.y+pl.h,p);p.setColor(Color.rgb(180,185,184));c.drawRect(pl.x,pl.y,pl.x+pl.w,pl.y+5,p);}
    for(Token t:tokens)if(!t.got){p.setColor(Color.rgb(244,186,57));c.drawCircle(t.x,t.y,9,p);p.setColor(Color.rgb(75,55,20));c.drawCircle(t.x,t.y,3,p);}
    for(Hazard h:hazards){float hx=h.x+(h.type==2?(float)Math.sin(SystemClock.uptimeMillis()/350.0)*28:0);p.setColor(h.type==0?Color.rgb(205,75,48):Color.rgb(120,126,132));c.drawRect(hx,h.y,hx+h.w,h.y+h.h,p);for(int i=0;i<3;i++){p.setColor(Color.rgb(245,151,45));c.drawCircle(hx+8+i*12,h.y-4-(i%2)*5,3,p);}}
    drawWorker(c,wx,wy,false); c.restore();
    p.setColor(Color.rgb(15,23,34));c.drawRect(0,0,960,66,p);text(c,18,27,"WORKSHOP RUN",18,Color.WHITE,Paint.Align.LEFT,true);text(c,18,52,pick(TXT_LEVEL)+" "+(wLevel+1)+"  ·  "+pick(TXT_SCORE)+" "+wScore+"  ·  ♥ "+wLives+"  ·  ⚙ "+wCollected,14,Color.rgb(255,213,75),Paint.Align.LEFT,false);
    button(c,wLeftRect,"◀",Color.rgb(42,101,190),wLeft);button(c,wRightRect,"▶",Color.rgb(42,101,190),wRight);button(c,wJumpRect,"JUMP",Color.rgb(218,132,38),wJump);
  }

  // ---------------- Heroes of Craft & Gates ----------------
  private void startHeroes(){heroPage=0;heroX=1;heroY=6;iron=cement=tools=gold=0;heroScore=0;shopLevel=1;weldLevel=truckLevel=0;battleEnemyHp=160;battleOwnHp=180;heroNodes.clear();int[][] n={{2,6,0},{4,4,1},{6,2,2},{8,5,0},{10,1,1},{3,1,2},{9,7,0}};for(int[]q:n)heroNodes.add(new ResourceNode(q[0],q[1],q[2]));}
  private void drawHeroes(Canvas c){p.setColor(Color.rgb(32,57,45));c.drawRect(0,0,960,600,p);text(c,18,34,"HEROES OF CRAFT & GATES",22,Color.rgb(255,220,105),Paint.Align.LEFT,true);text(c,18,62,"Fe "+iron+"  Cement "+cement+"  Tools "+tools+"  $ "+gold,13,Color.WHITE,Paint.Align.LEFT,false);if(heroPage==0)drawHeroMap(c);else if(heroPage==1)drawHeroTown(c);else drawHeroCombat(c);}
  private void drawHeroMap(Canvas c){float sx=190,sy=82,tw=50,th=48;for(int gy=0;gy<8;gy++)for(int gx=0;gx<12;gx++){p.setColor(((gx+gy)&1)==0?Color.rgb(77,112,65):Color.rgb(69,102,60));c.drawRect(sx+gx*tw,sy+gy*th,sx+(gx+1)*tw-2,sy+(gy+1)*th-2,p);} // roads
    p.setColor(Color.rgb(150,126,88));for(int x=0;x<12;x++)c.drawRect(sx+x*tw,sy+3*th+17,sx+(x+1)*tw,sy+3*th+28,p);
    drawBuilding(c,sx+10*tw+5,sy+1*th+6,40,35,Color.rgb(174,114,67),"CLIENT");drawBuilding(c,sx+7*tw+5,sy+6*th+6,40,35,Color.rgb(118,89,63),"MARKET");drawRival(c,sx+9*tw+25,sy+6*th+25);
    for(ResourceNode n:heroNodes)if(!n.got){float x=sx+n.x*tw+25,y=sy+n.y*th+24;if(n.type==0){p.setColor(Color.LTGRAY);c.drawRect(x-12,y-6,x+12,y+6,p);}else if(n.type==1){p.setColor(Color.rgb(220,210,185));c.drawRect(x-10,y-12,x+10,y+12,p);text(c,x,y+4,"C",10,Color.DKGRAY,Paint.Align.CENTER,true);}else{p.setColor(Color.rgb(245,180,45));c.drawCircle(x,y,10,p);p.setColor(Color.DKGRAY);c.drawRect(x-2,y-12,x+2,y+12,p);}}
    drawHero(c,sx+heroX*tw+25,sy+heroY*th+25);button(c,hUp,"▲",Color.rgb(59,93,142),false);button(c,hDown,"▼",Color.rgb(59,93,142),false);button(c,hLeft,"◀",Color.rgb(59,93,142),false);button(c,hRight,"▶",Color.rgb(59,93,142),false);button(c,hTown,pick(TXT_TOWN),Color.rgb(136,94,44),false);button(c,hCombat,pick(TXT_COMBAT),Color.rgb(139,55,55),false);}
  private void moveHero(int dx,int dy){heroX=Math.max(0,Math.min(11,heroX+dx));heroY=Math.max(0,Math.min(7,heroY+dy));for(ResourceNode n:heroNodes)if(!n.got&&n.x==heroX&&n.y==heroY){n.got=true;if(n.type==0)iron+=3;else if(n.type==1)cement+=2;else tools+=1;heroScore+=80;tone(ToneGenerator.TONE_PROP_ACK,40);}if(heroX==10&&heroY==1){gold+=100;heroScore+=100;}if(heroX==9&&heroY==6)heroPage=2;invalidate();}
  private void drawHeroTown(Canvas c){p.setColor(Color.rgb(74,63,51));c.drawRect(80,95,880,475,p);drawBuilding(c,130,215,220,180,Color.rgb(125,102,74),"WORKSHOP Lv"+shopLevel);drawBuilding(c,390,235,190,160,Color.rgb(80,91,97),"WELD Lv"+weldLevel);drawBuilding(c,620,245,190,150,Color.rgb(91,83,70),"TRUCK Lv"+truckLevel);button(c,new RectF(130,420,350,468),pick(TXT_UPGRADE)+" 3Fe/2C",Color.rgb(136,94,44),false);button(c,new RectF(390,420,580,468),pick(TXT_UPGRADE)+" 2Fe/1T",Color.rgb(92,104,112),false);button(c,new RectF(620,420,810,468),pick(TXT_UPGRADE)+" $100",Color.rgb(104,89,64),false);button(c,hCombat,pick(TXT_COMBAT),Color.rgb(139,55,55),false);button(c,hTown,"MAP",Color.rgb(45,91,65),false);}
  private void drawHeroCombat(Canvas c){float ox=180,oy=130,cw=75,ch=65;for(int y=0;y<5;y++)for(int x=0;x<8;x++){p.setColor(((x+y)&1)==0?Color.rgb(102,91,68):Color.rgb(88,79,61));c.drawRect(ox+x*cw,oy+y*ch,ox+(x+1)*cw-3,oy+(y+1)*ch-3,p);}drawUnit(c,ox+cw,oy+ch*1.5f,"HELPER",Color.rgb(75,150,190));drawUnit(c,ox+cw,oy+ch*2.6f,"WELDER",Color.rgb(70,125,165));drawUnit(c,ox+cw,oy+ch*3.7f,"HEAVY",Color.rgb(75,88,105));drawEnemy(c,ox+cw*6.5f,oy+ch*2.5f,"RUST",Color.rgb(158,73,45));drawEnemy(c,ox+cw*6.5f,oy+ch*3.7f,"KÓKLER",Color.rgb(125,55,55));text(c,190,490,"TEAM HP "+battleOwnHp+"   ENEMY HP "+battleEnemyHp,15,Color.WHITE,Paint.Align.LEFT,true);button(c,new RectF(700,490,840,545),pick(TXT_ATTACK),Color.rgb(165,65,50),false);button(c,hTown,"MAP",Color.rgb(45,91,65),false);}
  private void heroBattleAttack(){if(battleEnemyHp<=0||battleOwnHp<=0){battleEnemyHp=160;battleOwnHp=180;}int dmg=22+weldLevel*8+shopLevel*3;battleEnemyHp-=dmg;if(battleEnemyHp>0)battleOwnHp-=Math.max(5,18-truckLevel*2);else{gold+=180;heroScore+=500;tone(ToneGenerator.TONE_PROP_ACK,100);}if(battleOwnHp<=0){heroScore=Math.max(0,heroScore-100);tone(ToneGenerator.TONE_PROP_NACK,100);}invalidate();}

  // ---------------- Metal Fighter ----------------
  private void startFighter(){fPX=190;fEX=720;fPHP=fEHP=100;fScore=0;fAiTimer=0;fSpecialCd=0;fLeft=fRight=fBlock=false;}
  private void updateFighter(float dt){if(fPHP<=0||fEHP<=0)return;float sp=190;if(fLeft)fPX-=sp*dt;if(fRight)fPX+=sp*dt;fPX=Math.max(70,Math.min(700,fPX));fSpecialCd=Math.max(0,fSpecialCd-dt);fAiTimer-=dt;float dist=fPX-fEX;if(Math.abs(dist)>95)fEX+=Math.signum(dist)*105*dt;if(fAiTimer<=0){fAiTimer=.75f+(float)rnd.nextDouble()*.8f;if(Math.abs(fPX-fEX)<105){int dmg=fBlock?3:8+rnd.nextInt(6);fPHP=Math.max(0,fPHP-dmg);tone(ToneGenerator.TONE_PROP_NACK,35);}}}
  private void fighterHit(boolean special){if(fEHP<=0||fPHP<=0){startFighter();return;}if(special){if(fSpecialCd>0)return;fSpecialCd=2.2f;if(Math.abs(fPX-fEX)<210){fEHP=Math.max(0,fEHP-24);fScore+=120;}tone(ToneGenerator.TONE_PROP_BEEP2,90);}else if(Math.abs(fPX-fEX)<110){fEHP=Math.max(0,fEHP-12);fScore+=50;tone(ToneGenerator.TONE_PROP_ACK,45);}if(fEHP<=0){fScore+=500;owner.reportGameComplete(fScore);}owner.reportScore(fScore);invalidate();}
  private void drawFighter(Canvas c){p.setColor(Color.rgb(42,52,61));c.drawRect(0,0,960,600,p);p.setColor(Color.rgb(76,83,84));c.drawRect(0,380,960,505,p);p.setColor(Color.rgb(104,77,52));for(int x=0;x<960;x+=120)c.drawRect(x,190,x+9,380,p);p.setColor(Color.rgb(156,163,164));c.drawRect(0,380,960,390,p);text(c,25,38,"METAL FIGHTER",24,Color.WHITE,Paint.Align.LEFT,true);healthBar(c,55,70,340,fPHP,Color.rgb(52,180,92),"LAKATOS SZAKI");healthBar(c,565,70,340,fEHP,Color.rgb(210,66,54),"KÓKLER KONTÁR");drawFighterSprite(c,fPX,385,Color.rgb(49,113,160),false);drawFighterSprite(c,fEX,385,Color.rgb(155,62,52),true);if(fSpecialCd>0)text(c,850,480,String.format(Locale.US,"ARC %.1f",fSpecialCd),12,Color.rgb(255,210,70),Paint.Align.RIGHT,false);button(c,fLeftRect,"◀",Color.rgb(47,101,180),fLeft);button(c,fRightRect,"▶",Color.rgb(47,101,180),fRight);button(c,fHitRect,pick(TXT_ATTACK),Color.rgb(184,62,50),false);button(c,fBlockRect,pick(TXT_BLOCK),Color.rgb(70,110,145),fBlock);button(c,fArcRect,pick(TXT_SPECIAL),Color.rgb(194,132,32),false);}

  // ---------------- The Inspection ----------------
  private void startInspection(){iHunger=78;iEnergy=74;iCoffee=62;iSafety=55;iHeat=24;iScore=0;iWorkTime=0;}
  private void updateInspection(float dt){iWorkTime+=dt;iHunger=Math.max(0,iHunger-dt*.55f);iEnergy=Math.max(0,iEnergy-dt*.42f);iCoffee=Math.max(0,iCoffee-dt*.70f);iSafety=Math.max(0,iSafety-dt*.08f);iHeat=Math.min(100,iHeat+dt*.30f);if(iHunger>25&&iEnergy>20&&iSafety>25&&iHeat<85)iScore+=(int)(dt*8f);}
  private void inspectionAction(int idx){if(idx==0)iHunger=Math.min(100,iHunger+38);else if(idx==1)iEnergy=Math.min(100,iEnergy+32);else if(idx==2){iCoffee=Math.min(100,iCoffee+48);iEnergy=Math.min(100,iEnergy+12);}else if(idx==3)iSafety=100;else iHeat=Math.max(0,iHeat-45);tone(ToneGenerator.TONE_PROP_ACK,45);invalidate();}
  private void drawInspection(Canvas c){p.setColor(Color.rgb(47,52,54));c.drawRect(0,0,960,600,p);p.setColor(Color.rgb(77,73,66));c.drawRect(0,355,960,500,p);p.setColor(Color.rgb(45,65,75));c.drawRect(60,110,380,360,p);p.setColor(Color.rgb(92,97,97));c.drawRect(80,315,360,350,p);text(c,28,38,"THE INSPECTION: TAMAGOTCHI SZAKI",22,Color.WHITE,Paint.Align.LEFT,true);drawWorker(c,220,255,true);text(c,455,110,"SHIFT "+String.format(Locale.US,"%02d:%02d",(int)iWorkTime/60,(int)iWorkTime%60),15,Color.rgb(255,210,70),Paint.Align.LEFT,true);statBar(c,455,140,"LÁNGOS",iHunger,Color.rgb(218,137,57));statBar(c,455,185,pick(TXT_ENERGY),iEnergy,Color.rgb(58,160,95));statBar(c,455,230,pick(TXT_COFFEE),iCoffee,Color.rgb(130,88,54));statBar(c,455,275,"PPE",iSafety,Color.rgb(52,120,180));statBar(c,455,320,"HEAT",100-iHeat,Color.rgb(214,75,50));String[] labels={pick(TXT_FOOD),pick(TXT_ENERGY),pick(TXT_COFFEE),pick(TXT_GLASSES),pick(TXT_SHADE)};int[] cols={Color.rgb(190,110,38),Color.rgb(47,145,81),Color.rgb(116,75,48),Color.rgb(46,108,166),Color.rgb(80,91,112)};for(int i=0;i<5;i++)button(c,inspectionButtons[i],labels[i],cols[i],false);text(c,455,370,pick(TXT_SCORE)+" "+iScore,18,Color.WHITE,Paint.Align.LEFT,true);if(iSafety<25)text(c,455,405,"⚠ PPE!",18,Color.rgb(255,80,60),Paint.Align.LEFT,true);if(iHeat>80)text(c,455,430,"☀ 40°C!",18,Color.rgb(255,160,45),Paint.Align.LEFT,true);}

  // ---------------- GateCraft Chronicles ----------------
  private void startChronicles(){cX=480;cY=310;cJoyX=cJoyY=0;cHp=100;cXp=0;cLevel=1;cLoot=0;cScore=0;cAttackCd=cSparkCd=cShield=cHammerCd=0;enemies.clear();loots.clear();for(int i=0;i<7;i++)enemies.add(new Enemy(190+rnd.nextInt(580),150+rnd.nextInt(250),i%3));}
  private void updateChronicles(float dt){float speed=175;cX=Math.max(85,Math.min(875,cX+cJoyX*speed*dt));cY=Math.max(115,Math.min(450,cY+cJoyY*speed*dt));cAttackCd=Math.max(0,cAttackCd-dt);cSparkCd=Math.max(0,cSparkCd-dt);cShield=Math.max(0,cShield-dt);cHammerCd=Math.max(0,cHammerCd-dt);for(Enemy e:enemies)if(e.hp>0){float dx=cX-e.x,dy=cY-e.y,d=(float)Math.sqrt(dx*dx+dy*dy);if(d>1&&d<300){e.x+=dx/d*(42+e.type*7)*dt;e.y+=dy/d*(42+e.type*7)*dt;}if(d<38&&cShield<=0){cHp=Math.max(0,cHp-(int)(dt*7));}}for(Loot l:loots)if(!l.got&&dist(cX,cY,l.x,l.y)<34){l.got=true;cLoot++;cScore+=75;tone(ToneGenerator.TONE_PROP_ACK,35);}if(cHp<=0){owner.reportGameOver(cScore);startChronicles();}}
  private void chronAttack(int kind){if(kind==0){if(cAttackCd>0)return;cAttackCd=.38f;hitChron(65,20);}else if(kind==1){if(cSparkCd>0)return;cSparkCd=3.2f;hitChron(165,18);}else if(kind==2){if(cShield>0)return;cShield=2.0f;tone(ToneGenerator.TONE_PROP_ACK,60);}else{if(cHammerCd>0)return;cHammerCd=2.6f;hitChron(95,34);}invalidate();}
  private void hitChron(float range,int damage){for(Enemy e:enemies)if(e.hp>0&&dist(cX,cY,e.x,e.y)<range){e.hp-=damage;if(e.hp<=0){cXp+=20+e.type*8;cScore+=120;loots.add(new Loot(e.x,e.y));if(cXp>=cLevel*100){cXp-=cLevel*100;cLevel++;cHp=100;}}}owner.reportScore(cScore);tone(ToneGenerator.TONE_PROP_BEEP,45);}
  private void drawChronicles(Canvas c){p.setColor(Color.rgb(22,35,38));c.drawRect(0,0,960,600,p);text(c,22,36,"GATECRAFT CHRONICLES: THE LAKATOS",21,Color.rgb(225,225,215),Paint.Align.LEFT,true);text(c,22,61,"HP "+cHp+"  XP "+cXp+"/"+(cLevel*100)+"  LV "+cLevel+"  LOOT "+cLoot,13,Color.rgb(255,210,70),Paint.Align.LEFT,false);drawIsoFloor(c);for(Loot l:loots)if(!l.got){p.setColor(Color.rgb(234,178,44));c.drawRect(l.x-7,l.y-5,l.x+7,l.y+5,p);}for(Enemy e:enemies)if(e.hp>0)drawIsoEnemy(c,e);drawIsoHero(c,cX,cY,cShield>0);drawJoystick(c);button(c,cAttackRect,"HIT",Color.rgb(162,58,47),false);button(c,cSparkRect,"SPARK",Color.rgb(206,137,30),false);button(c,cShieldRect,"SHIELD",Color.rgb(47,111,165),cShield>0);button(c,cHammerRect,"HAMMER",Color.rgb(105,75,53),false);}

  // ---------------- Touch handling ----------------
  @Override public boolean onTouchEvent(MotionEvent e) {
    float x=(e.getX(e.getActionIndex())-ox)/scale, y=(e.getY(e.getActionIndex())-oy)/scale;
    int a=e.getActionMasked();
    if(a==MotionEvent.ACTION_DOWN||a==MotionEvent.ACTION_POINTER_DOWN){if(exitRect.contains(x,y)){owner.requestExitFromView();return true;}if(active!=0&&backRect.contains(x,y)){active=0;paused=false;clearHeld();invalidate();return true;}handleDown(x,y);}
    updateHeld(e); invalidate(); return true;
  }
  private void handleDown(float x,float y){if(active==0){for(int i=0;i<5;i++){RectF r=new RectF(38,112+i*88,922,184+i*88);if(r.contains(x,y)){startMode(i+1);return;}}return;}if(active==2){if(heroPage==0){if(hUp.contains(x,y))moveHero(0,-1);else if(hDown.contains(x,y))moveHero(0,1);else if(hLeft.contains(x,y))moveHero(-1,0);else if(hRight.contains(x,y))moveHero(1,0);else if(hTown.contains(x,y))heroPage=1;else if(hCombat.contains(x,y))heroPage=2;}else if(heroPage==1){if(new RectF(130,420,350,468).contains(x,y)&&iron>=3&&cement>=2){iron-=3;cement-=2;shopLevel++;}else if(new RectF(390,420,580,468).contains(x,y)&&iron>=2&&tools>=1){iron-=2;tools--;weldLevel++;}else if(new RectF(620,420,810,468).contains(x,y)&&gold>=100){gold-=100;truckLevel++;}else if(hTown.contains(x,y))heroPage=0;else if(hCombat.contains(x,y))heroPage=2;}else{if(new RectF(700,490,840,545).contains(x,y))heroBattleAttack();else if(hTown.contains(x,y))heroPage=0;}invalidate();return;}if(active==3){if(fHitRect.contains(x,y))fighterHit(false);else if(fArcRect.contains(x,y))fighterHit(true);return;}if(active==4){for(int i=0;i<inspectionButtons.length;i++)if(inspectionButtons[i].contains(x,y)){inspectionAction(i);return;}}if(active==5){if(cAttackRect.contains(x,y))chronAttack(0);else if(cSparkRect.contains(x,y))chronAttack(1);else if(cShieldRect.contains(x,y))chronAttack(2);else if(cHammerRect.contains(x,y))chronAttack(3);}}
  private void updateHeld(MotionEvent e){clearHeld();int action=e.getActionMasked(),up=e.getActionIndex();if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)return;for(int i=0;i<e.getPointerCount();i++){if(action==MotionEvent.ACTION_POINTER_UP&&i==up)continue;float x=(e.getX(i)-ox)/scale,y=(e.getY(i)-oy)/scale;if(active==1){if(wLeftRect.contains(x,y))wLeft=true;if(wRightRect.contains(x,y))wRight=true;if(wJumpRect.contains(x,y))wJump=true;}else if(active==3){if(fLeftRect.contains(x,y))fLeft=true;if(fRightRect.contains(x,y))fRight=true;if(fBlockRect.contains(x,y))fBlock=true;}else if(active==5){float dx=x-CJOY_X,dy=y-CJOY_Y,d=(float)Math.sqrt(dx*dx+dy*dy);if(d<CJOY_R*1.45f){float den=Math.max(CJOY_R,d);cJoyX=dx/den;cJoyY=dy/den;}}}}
  private void clearHeld(){wLeft=wRight=wJump=fLeft=fRight=fBlock=false;cJoyX=cJoyY=0;}

  // ---------------- Drawing helpers ----------------
  private void text(Canvas c,float x,float y,String s,float size,int color,Paint.Align align,boolean bold){p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setFakeBoldText(bold);c.drawText(s,x,y,p);p.setFakeBoldText(false);}
  private void button(Canvas c,RectF r,String label,int color,boolean down){p.setColor(down?shade(color,-35):color);c.drawRoundRect(r,7,7,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);p.setColor(Color.argb(220,235,238,240));c.drawRoundRect(r,7,7,p);p.setStyle(Paint.Style.FILL);text(c,r.centerX(),r.centerY()+5,label,Math.min(14,Math.max(9,170f/Math.max(4,label.length()))),Color.WHITE,Paint.Align.CENTER,true);}
  private int shade(int color,int d){return Color.rgb(Math.max(0,Math.min(255,Color.red(color)+d)),Math.max(0,Math.min(255,Color.green(color)+d)),Math.max(0,Math.min(255,Color.blue(color)+d)));}
  private void drawWorker(Canvas c,float x,float y,boolean idle){p.setColor(Color.rgb(225,176,55));c.drawRect(x+5,y-6,x+25,y+2,p);p.setColor(Color.rgb(225,184,145));c.drawRect(x+9,y+2,x+22,y+15,p);p.setColor(Color.rgb(45,103,145));c.drawRect(x+5,y+15,x+27,y+38,p);p.setColor(Color.rgb(41,48,55));c.drawRect(x+6,y+38,x+14,y+48,p);c.drawRect(x+19,y+38,x+27,y+48,p);p.setColor(Color.rgb(205,110,40));c.drawRect(x+25,y+19,x+34,y+23,p);if(idle){p.setColor(Color.rgb(245,189,48));c.drawCircle(x+34,y+22,3,p);}}
  private void drawHero(Canvas c,float x,float y){p.setColor(Color.rgb(58,77,120));c.drawCircle(x,y-8,11,p);p.setColor(Color.rgb(202,155,75));c.drawRect(x-9,y+2,x+9,y+22,p);p.setColor(Color.rgb(80,50,35));c.drawRect(x+8,y+5,x+18,y+9,p);}
  private void drawRival(Canvas c,float x,float y){p.setColor(Color.rgb(145,55,50));c.drawCircle(x,y-8,10,p);p.setColor(Color.rgb(70,42,38));c.drawRect(x-9,y+2,x+9,y+20,p);text(c,x,y-22,"!",12,Color.YELLOW,Paint.Align.CENTER,true);}
  private void drawBuilding(Canvas c,float x,float y,float w,float h,int color,String label){p.setColor(color);c.drawRect(x,y,x+w,y+h,p);p.setColor(shade(color,-40));float[] roof={x-8,y,x+w/2,y-28,x+w+8,y};android.graphics.Path path=new android.graphics.Path();path.moveTo(roof[0],roof[1]);path.lineTo(roof[2],roof[3]);path.lineTo(roof[4],roof[5]);path.close();c.drawPath(path,p);text(c,x+w/2,y+h/2,label,10,Color.WHITE,Paint.Align.CENTER,true);}
  private void drawUnit(Canvas c,float x,float y,String name,int color){p.setColor(color);c.drawRect(x-16,y-20,x+16,y+18,p);p.setColor(Color.rgb(220,184,145));c.drawCircle(x,y-28,9,p);text(c,x,y+36,name,9,Color.WHITE,Paint.Align.CENTER,true);}
  private void drawEnemy(Canvas c,float x,float y,String name,int color){p.setColor(color);c.drawCircle(x,y-10,20,p);p.setColor(Color.rgb(35,25,20));c.drawRect(x-8,y-13,x-3,y-8,p);c.drawRect(x+3,y-13,x+8,y-8,p);text(c,x,y+28,name,9,Color.WHITE,Paint.Align.CENTER,true);}
  private void healthBar(Canvas c,float x,float y,float w,int hp,int color,String name){text(c,x,y-8,name,12,Color.WHITE,Paint.Align.LEFT,true);p.setColor(Color.rgb(55,55,55));c.drawRect(x,y,x+w,y+16,p);p.setColor(color);c.drawRect(x,y,x+w*Math.max(0,hp)/100f,y+16,p);text(c,x+w+8,y+14,String.valueOf(hp),11,Color.WHITE,Paint.Align.LEFT,true);}
  private void drawFighterSprite(Canvas c,float x,float ground,int color,boolean enemy){p.setColor(Color.argb(90,0,0,0));c.drawOval(new RectF(x-28,ground+34,x+28,ground+46),p);p.setColor(color);c.drawRect(x-20,ground-55,x+20,ground+15,p);p.setColor(Color.rgb(220,178,138));c.drawCircle(x,ground-69,15,p);p.setColor(Color.rgb(45,50,55));c.drawRect(x-22,ground+15,x-6,ground+43,p);c.drawRect(x+6,ground+15,x+22,ground+43,p);p.setColor(enemy?Color.rgb(185,75,55):Color.rgb(235,168,45));c.drawRect(enemy?x-45:x+20,ground-40,enemy?x-18:x+48,ground-31,p);}
  private void statBar(Canvas c,float x,float y,String label,float value,int color){text(c,x,y,label,12,Color.WHITE,Paint.Align.LEFT,true);p.setColor(Color.rgb(40,43,45));c.drawRect(x+120,y-12,x+390,y+3,p);p.setColor(color);c.drawRect(x+120,y-12,x+120+270*Math.max(0,Math.min(100,value))/100f,y+3,p);text(c,x+400,y,String.valueOf((int)value),11,Color.WHITE,Paint.Align.LEFT,false);}
  private void drawIsoFloor(Canvas c){for(int gy=-3;gy<10;gy++)for(int gx=-4;gx<11;gx++){float x=480+(gx-gy)*44,y=130+(gx+gy)*22;p.setColor(((gx+gy)&1)==0?Color.rgb(58,68,62):Color.rgb(52,62,57));android.graphics.Path q=new android.graphics.Path();q.moveTo(x,y-22);q.lineTo(x+44,y);q.lineTo(x,y+22);q.lineTo(x-44,y);q.close();c.drawPath(q,p);}p.setColor(Color.rgb(75,65,55));c.drawRect(95,100,110,430,p);c.drawRect(850,100,865,430,p);}
  private void drawIsoHero(Canvas c,float x,float y,boolean shield){p.setColor(Color.argb(90,0,0,0));c.drawOval(new RectF(x-20,y+15,x+20,y+28),p);if(shield){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(4);p.setColor(Color.rgb(65,165,230));c.drawCircle(x,y-5,30,p);p.setStyle(Paint.Style.FILL);}p.setColor(Color.rgb(43,111,156));c.drawRect(x-12,y-26,x+12,y+12,p);p.setColor(Color.rgb(232,180,65));c.drawRect(x-15,y-42,x+15,y-32,p);p.setColor(Color.rgb(210,170,132));c.drawCircle(x,y-28,10,p);p.setColor(Color.rgb(165,75,45));c.drawRect(x+10,y-15,x+30,y-10,p);}
  private void drawIsoEnemy(Canvas c,Enemy e){int col=e.type==0?Color.rgb(145,80,48):(e.type==1?Color.rgb(85,92,100):Color.rgb(125,55,50));p.setColor(Color.argb(80,0,0,0));c.drawOval(new RectF(e.x-18,e.y+12,e.x+18,e.y+24),p);p.setColor(col);c.drawRect(e.x-13,e.y-28,e.x+13,e.y+13,p);p.setColor(Color.rgb(235,80,45));c.drawRect(e.x-6,e.y-18,e.x-2,e.y-14,p);c.drawRect(e.x+2,e.y-18,e.x+6,e.y-14,p);p.setColor(Color.rgb(40,40,40));c.drawRect(e.x-16,e.y-42,e.x+16,e.y-35,p);}
  private void drawJoystick(Canvas c){p.setColor(Color.argb(130,80,90,100));c.drawCircle(CJOY_X,CJOY_Y,CJOY_R,p);p.setColor(Color.rgb(70,125,175));c.drawCircle(CJOY_X+cJoyX*35,CJOY_Y+cJoyY*35,25,p);}
  private float dist(float x1,float y1,float x2,float y2){float dx=x1-x2,dy=y1-y2;return(float)Math.sqrt(dx*dx+dy*dy);}

  private static final class Platform {final float x,y,w,h;Platform(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;}}
  private static final class Token {final float x,y;boolean got;Token(float x,float y){this.x=x;this.y=y;}}
  private static final class Hazard {final float x,y,w,h;final int type;Hazard(float x,float y,float w,float h,int type){this.x=x;this.y=y;this.w=w;this.h=h;this.type=type;}}
  private static final class ResourceNode {final int x,y,type;boolean got;ResourceNode(int x,int y,int type){this.x=x;this.y=y;this.type=type;}}
  private static final class Enemy {float x,y;int hp=55;final int type;Enemy(float x,float y,int type){this.x=x;this.y=y;this.type=type;this.hp=50+type*15;}}
  private static final class Loot {final float x,y;boolean got;Loot(float x,float y){this.x=x;this.y=y;}}
}
