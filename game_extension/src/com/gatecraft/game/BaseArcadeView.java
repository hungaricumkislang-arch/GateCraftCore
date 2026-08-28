package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

abstract class BaseArcadeView extends View implements GameMetrics {
  static final float LW = 960f, LH = 540f;
  final GateCraftGame owner;
  final Paint p = new Paint();
  float scale = 1f, ox, oy;
  int lang = 2;
  boolean paused;
  long lastFrame = SystemClock.uptimeMillis();
  private final RectF backRect = new RectF(14, 12, 112, 58);
  private final RectF exitRect = new RectF(838, 12, 946, 58);
  private final float[] touchX=new float[16], touchY=new float[16];
  private final boolean[] touchActive=new boolean[16];
  private ToneGenerator tones;

  BaseArcadeView(Context c, GateCraftGame owner) {
    super(c);
    this.owner = owner;
    p.setAntiAlias(false);
    p.setFilterBitmap(false);
    setFocusable(true);
    setBackgroundColor(Color.BLACK);
  }

  @Override public void setLanguage(int value) { lang = Math.max(1, Math.min(12, value)); invalidate(); }
  @Override public void setPaused(boolean value) { paused = value; lastFrame = SystemClock.uptimeMillis(); if (!value) invalidate(); }
  @Override public int level() { return 1; }
  @Override public int lives() { return 3; }
  @Override public void shutdown() { paused = true; clearTouches(); if(tones!=null){try{tones.release();}catch(Throwable ignored){}tones=null;} }

  @Override protected final void onDraw(Canvas c) {
    super.onDraw(c);
    if (getWidth() <= 0 || getHeight() <= 0) return;
    scale = Math.min(getWidth()/LW, getHeight()/LH);
    ox = (getWidth()-LW*scale)/2f;
    oy = (getHeight()-LH*scale)/2f;
    c.save(); c.translate(ox, oy); c.scale(scale, scale);
    long now=SystemClock.uptimeMillis();
    float dt=Math.min(.04f,Math.max(0f,(now-lastFrame)/1000f)); lastFrame=now;
    if (!paused) updateGame(dt);
    drawGame(c);
    drawChrome(c);
    c.restore();
    if (!paused) postInvalidateDelayed(16L);
  }

  abstract void updateGame(float dt);
  abstract void drawGame(Canvas c);
  abstract void onGameDown(float x,float y);
  abstract void onGameMove(float x,float y);
  abstract void onGameUp(float x,float y);

  private void drawChrome(Canvas c) {
    arcadeButton(c,backRect,t("Vissza","Back","Zurück","Atrás","Retour","返回","Indietro","Voltar","Wstecz","Terug","Înapoi","Назад"),Color.rgb(42,55,68),false);
    arcadeButton(c,exitRect,t("Kilép","Exit","Ende","Salir","Quitter","退出","Esci","Sair","Wyjdź","Afsluiten","Ieșire","Выход"),Color.rgb(135,42,42),false);
  }

  @Override public boolean onTouchEvent(MotionEvent e) {
    int action=e.getActionMasked(), ai=e.getActionIndex();
    for(int i=0;i<e.getPointerCount();i++){
      int id=e.getPointerId(i);if(id<0||id>=touchActive.length)continue;
      touchX[id]=(e.getX(i)-ox)/scale;touchY[id]=(e.getY(i)-oy)/scale;touchActive[id]=true;
    }
    float x=(e.getX(ai)-ox)/scale, y=(e.getY(ai)-oy)/scale;
    if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_POINTER_DOWN){
      if(backRect.contains(x,y)){owner.returnToLauncher();return true;}
      if(exitRect.contains(x,y)){owner.requestExitFromView();return true;}
      onGameDown(x,y);onGameMove(x,y);
    }else if(action==MotionEvent.ACTION_MOVE){onGameMove(x,y);
    }else if(action==MotionEvent.ACTION_POINTER_UP){int id=e.getPointerId(ai);if(id>=0&&id<touchActive.length)touchActive[id]=false;onGameUp(x,y);onGameMove(x,y);
    }else if(action==MotionEvent.ACTION_UP){clearTouches();onGameUp(x,y);onGameMove(x,y);
    }else if(action==MotionEvent.ACTION_CANCEL){clearTouches();onGameUp(x,y);}
    return true;
  }

  boolean touched(RectF r){for(int i=0;i<touchActive.length;i++)if(touchActive[i]&&r.contains(touchX[i],touchY[i]))return true;return false;}
  private void clearTouches(){for(int i=0;i<touchActive.length;i++)touchActive[i]=false;}
  void tone(int code,int ms){try{if(tones==null)tones=new ToneGenerator(AudioManager.STREAM_MUSIC,28);tones.startTone(code,ms);}catch(Throwable ignored){}}

  String t(String hu,String en,String de,String es,String fr,String zh,String it,String pt,String pl,String nl,String ro,String ru) {
    String[] a={hu,en,de,es,fr,zh,it,pt,pl,nl,ro,ru}; return a[Math.max(0,Math.min(11,lang-1))];
  }

  void fill(Canvas c,int color,float l,float t,float r,float b){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRect(l,t,r,b,p);}  
  void stroke(Canvas c,int color,float w,float l,float t,float r,float b){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);p.setColor(color);c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);}  
  void line(Canvas c,int color,float w,float x1,float y1,float x2,float y2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);p.setColor(color);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}  
  void text(Canvas c,String s,float x,float y,float size,int color,Paint.Align align,boolean bold){p.setStyle(Paint.Style.FILL);p.setColor(color);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.MONOSPACE);c.drawText(s,x,y,p);}  
  void arcadeButton(Canvas c,RectF r,String label,int color,boolean pressed){
    int top=pressed?shade(color,-28):shade(color,24), bottom=pressed?shade(color,-48):shade(color,-18);
    fill(c,Color.rgb(14,16,22),r.left-4,r.top-4,r.right+4,r.bottom+4);
    fill(c,top,r.left,r.top,r.right,r.bottom-7);
    fill(c,bottom,r.left,r.bottom-7,r.right,r.bottom);
    stroke(c,Color.rgb(230,230,220),2,r.left,r.top,r.right,r.bottom);
    line(c,Color.argb(120,255,255,255),2,r.left+3,r.top+3,r.right-3,r.top+3);
    text(c,label,(r.left+r.right)/2,r.centerY()+7,18,Color.WHITE,Paint.Align.CENTER,true);
  }
  int shade(int c,int d){return Color.rgb(cl(Color.red(c)+d),cl(Color.green(c)+d),cl(Color.blue(c)+d));}
  int cl(int v){return Math.max(0,Math.min(255,v));}
  boolean in(RectF r,float x,float y){return r.contains(x,y);}  
  void diamond(Canvas c,float cx,float cy,float w,float h,int color,int edge){Path q=new Path();q.moveTo(cx,cy-h/2);q.lineTo(cx+w/2,cy);q.lineTo(cx,cy+h/2);q.lineTo(cx-w/2,cy);q.close();p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawPath(q,p);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1);p.setColor(edge);c.drawPath(q,p);p.setStyle(Paint.Style.FILL);}  
  void px(Canvas c,int color,float x,float y,float w,float h){fill(c,color,(float)Math.floor(x),(float)Math.floor(y),(float)Math.ceil(x+w),(float)Math.ceil(y+h));}
}
