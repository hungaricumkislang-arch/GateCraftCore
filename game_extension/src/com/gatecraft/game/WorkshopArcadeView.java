package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

final class WorkshopArcadeView extends BaseArcadeView {
  static final class Platform{float x,y,w,h,base,phase;boolean moving;Platform(float x,float y,float w,float h){this.x=x;this.y=this.base=y;this.w=w;this.h=h;}Platform moving(float p){moving=true;phase=p;return this;}}
  static final class Pickup{float x,y;int kind;boolean got;Pickup(float x,float y,int k){this.x=x;this.y=y;kind=k;}}
  static final class Hazard{float x,y,w;int type;Hazard(float x,float y,float w,int t){this.x=x;this.y=y;this.w=w;type=t;}}
  private final List<Platform> ps=new ArrayList<Platform>();
  private final List<Pickup> items=new ArrayList<Pickup>();
  private final List<Hazard> hazards=new ArrayList<Hazard>();
  private final ArcadeFx fx=new ArcadeFx();
  private float x,y,vx,vy,cam,world,clock,coyote,jumpBuf,dashCd,boost,invuln,checkpoint=92;
  private boolean left,right,jump,jumpPrev,dash,ground,wasGround;
  private int score,lives=3,level=1,tools,coffee;
  private final RectF bL=new RectF(20,424,140,522),bR=new RectF(150,424,270,522),bD=new RectF(646,424,790,522),bJ=new RectF(800,414,946,522);

  WorkshopArcadeView(Context c,GateCraftGame o){super(c,o);restart();}
  @Override public int score(){return score;}@Override public int level(){return level;}@Override public int lives(){return lives;}
  @Override public void shutdown(){super.shutdown();fx.clear();ps.clear();items.clear();hazards.clear();}
  @Override public void restart(){ps.clear();items.clear();hazards.clear();fx.clear();x=92;y=300;vx=vy=cam=0;ground=wasGround=jumpPrev=false;coyote=jumpBuf=dashCd=boost=invuln=0;checkpoint=92;build();invalidate();}

  private void build(){world=2850+level*180;ps.add(new Platform(0,390,world,70));ps.add(new Platform(310,315,190,24));ps.add(new Platform(620,270,220,24).moving(.3f));ps.add(new Platform(1000,330,190,24));ps.add(new Platform(1300,245,250,24).moving(1.5f));ps.add(new Platform(1690,295,220,24));ps.add(new Platform(2070,235,260,24).moving(2.4f));ps.add(new Platform(2470,315,210,24));ps.add(new Platform(world-330,295,260,24));for(int i=0;i<13;i++)items.add(new Pickup(235+i*205,235-(i%4)*24,i%5==4?1:0));items.add(new Pickup(880,342,2));items.add(new Pickup(1850,342,2));items.add(new Pickup(world-520,342,1));hazards.add(new Hazard(760,365,82,0));hazards.add(new Hazard(1460,362,92,1));hazards.add(new Hazard(1950,365,110,0));hazards.add(new Hazard(2360,362,90,1));}

  @Override void updateGame(float dt){clock+=dt;fx.update(dt);dashCd=Math.max(0,dashCd-dt);boost=Math.max(0,boost-dt);invuln=Math.max(0,invuln-dt);jumpBuf=Math.max(0,jumpBuf-dt);coyote=Math.max(0,coyote-dt);for(Platform q:ps)if(q.moving)q.y=q.base+(float)Math.sin(clock*1.6f+q.phase)*28;
    float vmax=boost>0?390:315,acc=ground?1350:880;if(left)vx-=acc*dt;if(right)vx+=acc*dt;if(!left&&!right)vx*=Math.pow(ground?.0006:.04,dt);vx=Math.max(-vmax,Math.min(vmax,vx));if(dash&&dashCd<=0){dash=false;dashCd=.85f;float d=right?1:left?-1:vx<0?-1:1;vx=d*(boost>0?560:480);fx.dust(x+19,y+56,10,Color.rgb(162,133,89));tone(24,35);}
    if(jump&&!jumpPrev)jumpBuf=.12f;jumpPrev=jump;if(jumpBuf>0&&(ground||coyote>0)){vy=-535;ground=false;coyote=jumpBuf=0;fx.dust(x+19,y+56,8,Color.rgb(151,145,132));tone(24,28);}vy+=1320*dt;float nx=x+vx*dt,ny=y+vy*dt;wasGround=ground;ground=false;for(Platform q:ps)if(x+38>q.x&&x<q.x+q.w&&y+58<=q.y+9&&ny+58>=q.y&&vy>=0){ny=q.y-58;vy=0;ground=true;if(!wasGround)fx.dust(x+19,q.y,6,Color.rgb(126,121,109));}if(wasGround&&!ground)coyote=.10f;x=Math.max(0,Math.min(world-40,nx));y=ny;if(y>535)hurt();for(Hazard h:hazards)if(invuln<=0&&x+38>h.x&&x<h.x+h.w&&y+58>h.y&&y<h.y+28){fx.sparks(x+19,y+42,18,Color.rgb(255,176,54));hurt();break;}
    for(Pickup q:items)if(!q.got&&Math.abs(x+19-q.x)<34&&Math.abs(y+29-q.y)<39){q.got=true;if(q.kind==0)score+=100;else if(q.kind==1){tools++;score+=250;}else{coffee++;boost=4.5f;score+=150;}fx.sparks(q.x,q.y,10,q.kind==2?Color.rgb(255,93,53):Color.rgb(255,211,67));tone(25,45);owner.reportScore(score);}if(x>world*.36f&&checkpoint<world*.3f)checkpoint=world*.36f;if(x>world*.68f&&checkpoint<world*.6f)checkpoint=world*.68f;float target=x-(vx>0?310:vx<0?170:250);cam+=(target-cam)*Math.min(1,dt*7.5f);cam=Math.max(0,Math.min(world-830,cam));if(x>world-100){score+=1200+tools*100;level++;tone(27,100);owner.reportLevelComplete(level,score);restart();}}
  private void hurt(){if(invuln>0)return;lives--;invuln=1.2f;tone(26,90);fx.sparks(x+19,y+30,16,Color.rgb(255,80,50));if(lives<=0){owner.reportGameOver(score);lives=3;score=Math.max(0,score-350);tools=coffee=0;checkpoint=92;}x=checkpoint;y=250;vx=0;vy=-120;cam=Math.max(0,checkpoint-230);}

  @Override void drawGame(Canvas c){fill(c,Color.rgb(13,17,26),0,0,LW,LH);for(int yy=80;yy<390;yy+=26)fill(c,(yy/26)%2==0?Color.rgb(36,39,48):Color.rgb(27,31,40),0,yy,LW,yy+13);for(int bx=-80;bx<1050;bx+=150){float xx=bx-(cam*.10f%150);fill(c,Color.rgb(10,13,18),xx,90,xx+10,390);fill(c,Color.rgb(69,72,75),xx+10,90,xx+18,390);line(c,Color.rgb(116,75,39),7,xx+35,120,xx+118,120);line(c,Color.rgb(116,75,39),5,xx+80,120,xx+80,188);}for(int i=0;i<7;i++){float xx=50+i*170-(cam*.18f%170);fill(c,Color.rgb(50,55,59),xx,230,xx+104,390);fill(c,Color.rgb(94,74,48),xx+11,246,xx+28,390);fill(c,Color.rgb(17,20,24),xx+44,274,xx+87,390);fill(c,Color.rgb(188,80,30),xx+55,287,xx+75,304);}float sx=64-cam;
    for(float cp:new float[]{world*.36f,world*.68f}){float xx=sx+cp;if(xx>-60&&xx<1020){line(c,Color.rgb(77,62,45),6,xx,320,xx,389);fill(c,Color.rgb(202,149,42),xx-20,310,xx+25,337);text(c,"CP",xx+2,330,13,Color.rgb(38,31,24),Paint.Align.CENTER,true);}}
    for(Platform q:ps){float xx=sx+q.x;if(xx>1010||xx+q.w<-40)continue;fill(c,Color.rgb(60,65,67),xx,q.y,xx+q.w,q.y+q.h);for(float k=xx;k<xx+q.w;k+=32){fill(c,Color.rgb(128,130,125),k,q.y,Math.min(k+24,xx+q.w),q.y+6);fill(c,Color.rgb(33,37,39),k+24,q.y,Math.min(k+32,xx+q.w),q.y+q.h);}stroke(c,Color.rgb(14,17,19),3,xx,q.y,xx+q.w,q.y+q.h);}
    for(Hazard h:hazards){float xx=sx+h.x;if(xx<-120||xx>1030)continue;if(h.type==0){for(int i=0;i<7;i++){float k=xx+i*h.w/7;fill(c,Color.rgb(211,72,34),k,h.y,k+h.w/14,h.y+22);line(c,Color.rgb(255,190,55),3,k,h.y,k+h.w/14,h.y+22);}}else{float cx=xx+h.w/2,cy=h.y+11,r=h.w*.32f;for(int i=0;i<12;i++){double a=i*Math.PI*2/12;line(c,Color.rgb(194,199,201),5,cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r);}fill(c,Color.rgb(80,83,85),cx-8,cy-8,cx+8,cy+8);}}
    for(Pickup q:items)if(!q.got){float xx=sx+q.x;if(xx>-50&&xx<1010){float pulse=1+(float)Math.sin(clock*5+q.x)*.08f;if(q.kind==0){fill(c,Color.rgb(255,208,58),xx-9*pulse,q.y-9*pulse,xx+9*pulse,q.y+9*pulse);fill(c,Color.rgb(93,67,20),xx-3,q.y-12,xx+3,q.y+12);}else SpriteKit.icon(c,p,q.kind==1?2:4,xx,q.y,pulse);}}
    int state=!ground?4:Math.abs(vx)>35?1:0;if(invuln<=0||((int)(clock*12)%2==0))SpriteKit.worker(c,p,sx+x+19,y+58,1.05f,vx<0?-1:1,state,clock,false);fx.draw(c,p);fill(c,Color.rgb(7,9,13),0,0,960,78);ArcadeFx.beveledPanel(c,p,12,12,368,65,Color.rgb(74,43,25),Color.rgb(205,157,78),Color.rgb(33,20,15));text(c,"WORKSHOP RUN",30,47,26,Color.rgb(255,226,150),Paint.Align.LEFT,true);text(c,t("PONT","SCORE","PUNKTE","PUNTOS","SCORE","得分","PUNTI","PONTOS","PUNKTY","SCORE","SCOR","СЧЁТ")+" "+score,400,43,18,Color.WHITE,Paint.Align.LEFT,true);text(c,"LIFE "+lives,610,43,17,Color.rgb(239,78,68),Paint.Align.LEFT,true);text(c,"TOOL "+tools,700,43,15,Color.rgb(139,211,255),Paint.Align.LEFT,true);text(c,t("SZINT","LEVEL","STUFE","NIVEL","NIVEAU","等级","LIVELLO","NÍVEL","POZIOM","NIVEAU","NIVEL","УРОВЕНЬ")+" "+level,805,43,15,Color.rgb(245,211,85),Paint.Align.LEFT,true);if(boost>0){fill(c,Color.rgb(41,34,28),400,57,620,69);fill(c,Color.rgb(232,87,43),402,59,402+216*(boost/4.5f),67);}
    arcadeButton(c,bL,"◀",Color.rgb(37,105,163),left);arcadeButton(c,bR,"▶",Color.rgb(37,105,163),right);arcadeButton(c,bD,dashCd<=0?t("ROHAM","DASH","SPRINT","IMPULSO","RUÉE","冲刺","SCATTO","ARRANCADA","ZRYW","DASH","AVÂNT","РЫВОК"):"…",Color.rgb(114,77,154),touched(bD));arcadeButton(c,bJ,t("UGRÁS","JUMP","SPRUNG","SALTO","SAUT","跳跃","SALTO","PULO","SKOK","SPRONG","SALT","ПРЫЖОК"),Color.rgb(181,79,35),jump);}

  private void sync(){left=touched(bL);right=touched(bR);jump=touched(bJ);}
  @Override void onGameDown(float tx,float ty){sync();if(in(bD,tx,ty)&&dashCd<=0)dash=true;}
  @Override void onGameMove(float tx,float ty){sync();}
  @Override void onGameUp(float tx,float ty){sync();}
}
