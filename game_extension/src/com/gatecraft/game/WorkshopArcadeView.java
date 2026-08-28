package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

final class WorkshopArcadeView extends BaseArcadeView {
  static final class Platform{float x,y,w,h;Platform(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;}}
  static final class Bolt{float x,y;boolean got;Bolt(float x,float y){this.x=x;this.y=y;}}
  static final class Hazard{float x,y,w;Hazard(float x,float y,float w){this.x=x;this.y=y;this.w=w;}}
  private final List<Platform> platforms=new ArrayList<Platform>();
  private final List<Bolt> bolts=new ArrayList<Bolt>();
  private final List<Hazard> hazards=new ArrayList<Hazard>();
  private float px,py,vx,vy,cameraX,worldW;
  private boolean left,right,jump,jumpLatch,ground;
  private int score,lives=3,level=1;
  private final RectF leftBtn=new RectF(24,432,132,520),rightBtn=new RectF(144,432,252,520),jumpBtn=new RectF(818,418,944,520);

  WorkshopArcadeView(Context c,GateCraftGame o){super(c,o);restart();}
  @Override public int score(){return score;}@Override public int level(){return level;}@Override public int lives(){return lives;}
  @Override public void restart(){platforms.clear();bolts.clear();hazards.clear();px=92;py=330;vx=vy=0;cameraX=0;ground=false;buildLevel();invalidate();}
  private void buildLevel(){worldW=2600+level*160;platforms.add(new Platform(0,390,worldW,60));platforms.add(new Platform(310,320,190,24));platforms.add(new Platform(620,280,210,24));platforms.add(new Platform(980,335,170,24));platforms.add(new Platform(1260,250,230,24));platforms.add(new Platform(1640,300,210,24));platforms.add(new Platform(2010,245,250,24));platforms.add(new Platform(worldW-330,315,250,24));for(int i=0;i<11;i++)bolts.add(new Bolt(250+i*205,245-(i%3)*28));hazards.add(new Hazard(760,372,78));hazards.add(new Hazard(1440,372,88));hazards.add(new Hazard(1880,372,104));}
  @Override void updateGame(float dt){float acc=1100f;if(left)vx-=acc*dt;if(right)vx+=acc*dt;if(!left&&!right)vx*=Math.pow(.001,dt);vx=Math.max(-300,Math.min(300,vx));if(jump&&!jumpLatch&&ground){vy=-510;ground=false;jumpLatch=true;}if(!jump)jumpLatch=false;vy+=1250*dt;float nx=px+vx*dt,ny=py+vy*dt;ground=false;for(Platform q:platforms){if(px+34>q.x&&px<q.x+q.w&&py+54<=q.y+8&&ny+54>=q.y&&vy>=0){ny=q.y-54;vy=0;ground=true;}}px=nx;py=ny;if(py>520){lives--;if(lives<=0){owner.reportGameOver(score);lives=3;score=Math.max(0,score-250);}px=Math.max(80,cameraX+140);py=220;vy=0;}for(Hazard h:hazards)if(px+34>h.x&&px<h.x+h.w&&py+54>h.y&&py<h.y+24){lives--;px=Math.max(80,cameraX+110);py=220;vy=-200;break;}for(Bolt b:bolts)if(!b.got&&Math.abs((px+17)-b.x)<30&&Math.abs((py+28)-b.y)<34){b.got=true;score+=100;owner.reportScore(score);}cameraX=Math.max(0,Math.min(worldW-820,px-250+Math.max(0,vx)*.35f));if(px>worldW-105){score+=1000;level++;owner.reportLevelComplete(level,score);restart();}}
  @Override void drawGame(Canvas c){fill(c,Color.rgb(17,21,31),0,0,LW,LH);for(int y=82;y<390;y+=28){int col=(y/28)%2==0?Color.rgb(33,37,47):Color.rgb(27,31,40);fill(c,col,0,y,LW,y+14);}for(int x=0;x<960;x+=120){fill(c,Color.rgb(11,14,20),x,82,x+8,390);fill(c,Color.rgb(59,64,70),x+8,82,x+14,390);}for(int i=0;i<6;i++){float dx=70+i*165-(cameraX*.15f%165);fill(c,Color.rgb(48,55,60),dx,230,dx+90,390);fill(c,Color.rgb(91,74,48),dx+12,250,dx+28,390);fill(c,Color.rgb(20,23,27),dx+42,280,dx+76,390);fill(c,Color.rgb(184,90,34),dx+50,292,dx+66,306);}float sx=-cameraX+64;for(Platform q:platforms){float x=sx+q.x;if(x>1000||x+q.w<0)continue;fill(c,Color.rgb(65,68,68),x,q.y,x+q.w,q.y+q.h);for(float xx=x;xx<x+q.w;xx+=32){fill(c,Color.rgb(112,115,111),xx,q.y,Math.min(xx+26,x+q.w),q.y+5);fill(c,Color.rgb(37,40,41),xx+26,q.y,Math.min(xx+32,x+q.w),q.y+q.h);}stroke(c,Color.rgb(16,18,20),3,x,q.y,x+q.w,q.y+q.h);}for(Hazard h:hazards){float x=sx+h.x;if(x<-100||x>1000)continue;for(int i=0;i<6;i++){float xx=x+i*h.w/6;fill(c,Color.rgb(210,72,34),xx,h.y,xx+h.w/12,h.y+18);line(c,Color.rgb(255,183,52),3,xx,h.y,xx+h.w/12,h.y+18);}}for(Bolt b:bolts)if(!b.got){float x=sx+b.x;if(x>-40&&x<1000){fill(c,Color.rgb(255,210,64),x-9,b.y-9,x+9,b.y+9);fill(c,Color.rgb(106,72,15),x-4,b.y-11,x+4,b.y+11);fill(c,Color.rgb(255,244,163),x-3,b.y-5,x+3,b.y+5);}}drawWorker(c,sx+px,py,(System.currentTimeMillis()/90)%4,(vx<0));fill(c,Color.rgb(8,10,14),0,0,960,78);fill(c,Color.rgb(73,42,24),12,14,385,64);stroke(c,Color.rgb(219,181,100),3,12,14,385,64);text(c,"WORKSHOP RUN",30,47,26,Color.rgb(255,225,145),Paint.Align.LEFT,true);text(c,t("PONT","SCORE","PUNKTE","PUNTOS","SCORE","得分","PUNTI","PONTOS","PUNKTY","SCORE","SCOR","СЧЁТ")+" "+score,430,44,20,Color.WHITE,Paint.Align.LEFT,true);text(c,"♥ "+lives,700,44,22,Color.rgb(236,74,74),Paint.Align.LEFT,true);text(c,t("SZINT","LEVEL","STUFE","NIVEL","NIVEAU","等级","LIVELLO","NÍVEL","POZIOM","NIVEAU","NIVEL","УРОВЕНЬ")+" "+level,790,44,18,Color.rgb(245,210,80),Paint.Align.LEFT,true);arcadeButton(c,leftBtn,"◀",Color.rgb(34,107,162),left);arcadeButton(c,rightBtn,"▶",Color.rgb(34,107,162),right);arcadeButton(c,jumpBtn,t("UGRÁS","JUMP","SPRUNG","SALTO","SAUT","跳","SALTO","PULO","SKOK","SPRONG","SALT","ПРЫЖОК"),Color.rgb(172,78,37),jump);}
  private void drawWorker(Canvas c,float x,float y,long frame,boolean flip){int skin=Color.rgb(221,166,105),shirt=Color.rgb(39,97,153),pants=Color.rgb(41,45,54),helmet=Color.rgb(238,184,43);float bob=(frame%2)*2;px(c,pants,x+8,y+31+bob,10,20);px(c,pants,x+22,y+31+bob,10,20);px(c,Color.rgb(25,27,31),x+5,y+49+bob,14,5);px(c,Color.rgb(25,27,31),x+20,y+49+bob,14,5);px(c,shirt,x+5,y+13+bob,29,23);px(c,skin,x+10,y+3+bob,19,13);px(c,helmet,x+7,y+bob,25,5);px(c,Color.rgb(70,43,27),x+12,y+10+bob,14,3);px(c,skin,x+1,y+17+bob,7,17);px(c,skin,x+33,y+17+bob,7,17);px(c,Color.rgb(226,226,220),x+34,y+28+bob,13,4);}
  @Override void onGameDown(float x,float y){left=in(leftBtn,x,y);right=in(rightBtn,x,y);jump=in(jumpBtn,x,y);} @Override void onGameMove(float x,float y){left=in(leftBtn,x,y);right=in(rightBtn,x,y);jump=in(jumpBtn,x,y);} @Override void onGameUp(float x,float y){left=right=jump=false;}
}
