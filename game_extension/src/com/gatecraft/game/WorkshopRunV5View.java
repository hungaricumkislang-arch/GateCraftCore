package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class WorkshopRunV5View extends BaseArcadeView {
  static final class Platform { float x,y,w,h,base,phase; boolean moving; Platform(float x,float y,float w,float h){this.x=x;this.y=this.base=y;this.w=w;this.h=h;} Platform moving(float ph){moving=true;phase=ph;return this;} }
  static final class Pickup { float x,y; int kind; boolean got; Pickup(float x,float y,int kind){this.x=x;this.y=y;this.kind=kind;} }
  static final class Hazard { float x,y,w; int type; Hazard(float x,float y,float w,int type){this.x=x;this.y=y;this.w=w;this.type=type;} }
  static final class Enemy { float x,y,vx,base,home,phase; int type,hp,maxHp; boolean dead,boss; Enemy(float x,float y,int type,int hp){this.x=this.home=x;this.y=this.base=y;this.type=type;this.hp=this.maxHp=hp;} }
  static final class Shot { float x,y,vx,life; boolean enemy; int power; Shot(float x,float y,float vx,boolean enemy,int power){this.x=x;this.y=y;this.vx=vx;this.enemy=enemy;this.power=power;life=2.2f;} }
  static final class Barrier { float x; int hp,maxHp; Barrier(float x,int hp){this.x=x;this.hp=this.maxHp=hp;} }

  private final List<Platform> ps=new ArrayList<Platform>();
  private final List<Pickup> items=new ArrayList<Pickup>();
  private final List<Hazard> hazards=new ArrayList<Hazard>();
  private final List<Enemy> enemies=new ArrayList<Enemy>();
  private final List<Shot> shots=new ArrayList<Shot>();
  private final List<Barrier> gates=new ArrayList<Barrier>();
  private final ArcadeFx fx=new ArcadeFx();
  private final Random rnd=new Random(6060L);

  private float x,y,vx,vy,cam,world,clock,coyote,jumpBuf,dashCd,boost,invuln,checkpoint=92,attackCd,comboTimer,stageBanner=1.7f;
  private boolean left,right,jump,jumpPrev,dash,attack,ground,wasGround;
  private int score,lives=3,level=1,tools,coffee,weapon,ammo,combo,kills;
  private final RectF bL=new RectF(18,421,132,523),bR=new RectF(138,421,252,523),bD=new RectF(602,421,712,523),bA=new RectF(718,421,834,523),bJ=new RectF(840,413,958,523);

  WorkshopRunV5View(Context c,GateCraftGame o){super(c,o);restart();}
  @Override public int score(){return score;} @Override public int level(){return level;} @Override public int lives(){return lives;}
  @Override public void shutdown(){super.shutdown();fx.clear();ps.clear();items.clear();hazards.clear();enemies.clear();shots.clear();gates.clear();}
  @Override public void restart(){ps.clear();items.clear();hazards.clear();enemies.clear();shots.clear();gates.clear();fx.clear();x=92;y=300;vx=vy=cam=0;ground=wasGround=jumpPrev=false;coyote=jumpBuf=dashCd=boost=invuln=attackCd=comboTimer=0;checkpoint=92;combo=0;kills=0;stageBanner=1.7f;build();invalidate();}

  private int theme(){return (level-1)%3;}
  private void build(){
    world=3600+level*210;
    ps.add(new Platform(0,390,world,78));
    ps.add(new Platform(280,314,200,24));ps.add(new Platform(610,266,210,24).moving(.2f));ps.add(new Platform(930,325,180,24));
    ps.add(new Platform(1210,244,245,24).moving(1.3f));ps.add(new Platform(1580,296,220,24));ps.add(new Platform(1880,220,260,24));
    ps.add(new Platform(2240,300,210,24).moving(2.5f));ps.add(new Platform(2530,250,235,24));ps.add(new Platform(2870,320,205,24));
    ps.add(new Platform(3190,235,260,24).moving(3.1f));ps.add(new Platform(world-430,292,340,24));
    ps.add(new Platform(720,175,150,20));ps.add(new Platform(1040,145,160,20));ps.add(new Platform(1500,165,170,20));ps.add(new Platform(2700,150,180,20));
    for(int i=0;i<17;i++)items.add(new Pickup(210+i*190,238-(i%4)*23,i%6==5?1:0));
    items.add(new Pickup(780,142,3));items.add(new Pickup(1110,112,2));items.add(new Pickup(1570,132,4));items.add(new Pickup(2760,117,3));items.add(new Pickup(world-620,342,2));
    hazards.add(new Hazard(720,363,84,0));hazards.add(new Hazard(1380,360,92,1));hazards.add(new Hazard(2050,362,105,2));hazards.add(new Hazard(2440,360,88,1));hazards.add(new Hazard(3050,363,110,0));
    gates.add(new Barrier(1160,5));gates.add(new Barrier(2380,7));
    for(int i=0;i<13;i++){float ex=440+i*225;int type=i%3;float ey=type==1?235:332;enemies.add(new Enemy(ex,ey,type,type==2?4:3));}
    enemies.add(new Enemy(world-260,305,3,22+level*2));enemies.get(enemies.size()-1).boss=true;
  }

  @Override void updateGame(float dt){
    clock+=dt;fx.update(dt);stageBanner=Math.max(0,stageBanner-dt);dashCd=Math.max(0,dashCd-dt);boost=Math.max(0,boost-dt);invuln=Math.max(0,invuln-dt);attackCd=Math.max(0,attackCd-dt);comboTimer=Math.max(0,comboTimer-dt);if(comboTimer<=0)combo=0;
    jumpBuf=Math.max(0,jumpBuf-dt);coyote=Math.max(0,coyote-dt);for(Platform q:ps)if(q.moving)q.y=q.base+(float)Math.sin(clock*1.7f+q.phase)*27;
    float vmax=boost>0?410:325,acc=ground?1450:900;if(left)vx-=acc*dt;if(right)vx+=acc*dt;if(!left&&!right)vx*=Math.pow(ground?.0005:.04,dt);vx=Math.max(-vmax,Math.min(vmax,vx));
    if(dash&&dashCd<=0){dash=false;dashCd=.78f;float d=right?1:left?-1:vx<0?-1:1;vx=d*(boost>0?610:520);invuln=Math.max(invuln,.16f);fx.dust(x+19,y+56,12,Color.rgb(164,134,88));tone(24,34);}
    if(jump&&!jumpPrev)jumpBuf=.13f;jumpPrev=jump;if(jumpBuf>0&&(ground||coyote>0)){vy=-548;ground=false;coyote=jumpBuf=0;fx.dust(x+19,y+56,9,Color.rgb(151,145,132));tone(24,28);}vy+=1330*dt;
    float nx=x+vx*dt,ny=y+vy*dt;wasGround=ground;ground=false;for(Platform q:ps)if(x+38>q.x&&x<q.x+q.w&&y+58<=q.y+10&&ny+58>=q.y&&vy>=0){ny=q.y-58;vy=0;ground=true;if(!wasGround)fx.dust(x+19,q.y,6,Color.rgb(126,121,109));}if(wasGround&&!ground)coyote=.10f;
    for(Barrier g:gates)if(g.hp>0&&x+38<=g.x+6&&nx+38>g.x&&vx>0){nx=g.x-38;vx=0;}
    x=Math.max(0,Math.min(world-40,nx));y=ny;if(y>535)hurt();
    for(Hazard h:hazards)if(invuln<=0&&x+38>h.x&&x<h.x+h.w&&y+58>h.y&&y<h.y+30){fx.sparks(x+19,y+42,18,Color.rgb(255,176,54));hurt();break;}
    if(attack&&attackCd<=0){attack=false;doAttack();}
    updateShots(dt);updateEnemies(dt);
    for(Pickup q:items)if(!q.got&&Math.abs(x+19-q.x)<35&&Math.abs(y+29-q.y)<40){q.got=true;if(q.kind==0)score+=100;else if(q.kind==1){tools++;score+=260;}else if(q.kind==2){coffee++;boost=5.0f;score+=180;}else if(q.kind==3){weapon=1;ammo+=18;score+=220;}else{lives=Math.min(5,lives+1);score+=300;}fx.sparks(q.x,q.y,12,q.kind==2?Color.rgb(255,93,53):Color.rgb(255,211,67));tone(25,45);owner.reportScore(score);}
    if(x>world*.31f&&checkpoint<world*.25f)checkpoint=world*.31f;if(x>world*.62f&&checkpoint<world*.55f)checkpoint=world*.62f;
    float target=x-(vx>0?335:vx<0?180:260);cam+=(target-cam)*Math.min(1,dt*7.5f);cam=Math.max(0,Math.min(world-830,cam));
    Enemy boss=boss();if(boss!=null&&boss.dead&&x>world-105){score+=1500+tools*120+kills*30;level++;tone(27,110);owner.reportLevelComplete(level,score);restart();}
  }

  private void doAttack(){attackCd=weapon==1?.24f:.34f;int dir=vx<0?-1:right?1:left?-1:1;if(weapon==1&&ammo>0){ammo--;shots.add(new Shot(x+19+dir*28,y+28,dir*640,false,2));fx.sparks(x+19+dir*22,y+29,4,Color.rgb(255,210,90));tone(23,24);if(ammo<=0)weapon=0;}else{weapon=0;float hitX=x+19+dir*52;fx.sparks(hitX,y+30,7,Color.rgb(230,188,95));for(Enemy e:enemies)if(!e.dead&&Math.abs((e.x)-hitX)<52&&Math.abs(e.y-(y+35))<70)damageEnemy(e,2);for(Barrier g:gates)if(g.hp>0&&Math.abs(g.x-hitX)<58){g.hp-=2;fx.sparks(g.x,330,10,Color.rgb(255,176,60));}}
  }
  private void updateShots(float dt){Iterator<Shot>it=shots.iterator();while(it.hasNext()){Shot s=it.next();s.life-=dt;s.x+=s.vx*dt;if(s.life<=0||s.x<0||s.x>world){it.remove();continue;}if(!s.enemy){boolean hit=false;for(Enemy e:enemies)if(!e.dead&&Math.abs(e.x-s.x)<30&&Math.abs(e.y-s.y)<55){damageEnemy(e,s.power);hit=true;break;}if(!hit)for(Barrier g:gates)if(g.hp>0&&Math.abs(g.x-s.x)<20){g.hp-=s.power;hit=true;fx.sparks(g.x,330,8,Color.rgb(255,181,70));break;}if(hit)it.remove();}else if(invuln<=0&&Math.abs((x+19)-s.x)<24&&Math.abs((y+30)-s.y)<45){it.remove();hurt();}}
  }
  private void updateEnemies(float dt){for(Enemy e:enemies){if(e.dead)continue;e.phase+=dt;if(e.boss){float dx=(x+19)-e.x;if(Math.abs(dx)<600)e.vx=Math.signum(dx)*(72+level*2);else e.vx*=.8f;e.x+=e.vx*dt;if(((int)(e.phase*2.2f))%5==0&&rnd.nextFloat()<dt*.9f)shots.add(new Shot(e.x+(dx<0?-35:35),e.y-35,dx<0?-360:360,true,1));if(invuln<=0&&Math.abs((x+19)-e.x)<58&&Math.abs((y+30)-e.y)<78)hurt();}
      else if(e.type==0){e.x+=Math.sin(e.phase*1.7f+e.base*.01f)*42*dt;if(invuln<=0&&Math.abs((x+19)-e.x)<34&&Math.abs((y+30)-e.y)<55)hurt();}
      else if(e.type==1){e.y=e.base+(float)Math.sin(e.phase*2.5f)*36;if(Math.abs((x+19)-e.x)<280&&rnd.nextFloat()<dt*.42f)shots.add(new Shot(e.x,e.y,((x+19)<e.x?-1:1)*300,true,1));}
      else {e.x=e.home+(float)Math.sin(e.phase*1.2f)*44;if(invuln<=0&&Math.abs((x+19)-e.x)<39&&Math.abs((y+30)-e.y)<56)hurt();}}
  }
  private void damageEnemy(Enemy e,int dmg){if(e.dead)return;e.hp-=dmg;combo++;comboTimer=1.1f;score+=dmg*(30+combo*4);fx.sparks(e.x,e.y-20,e.boss?18:10,e.boss?Color.rgb(255,108,48):Color.rgb(255,193,72));if(e.hp<=0){e.dead=true;kills++;score+=e.boss?1200:180+combo*20;tone(e.boss?27:25,e.boss?110:45);}owner.reportScore(score);}
  private Enemy boss(){for(Enemy e:enemies)if(e.boss)return e;return null;}
  private void hurt(){if(invuln>0)return;lives--;invuln=1.25f;tone(26,90);fx.sparks(x+19,y+30,18,Color.rgb(255,80,50));combo=0;if(lives<=0){owner.reportGameOver(score);lives=3;score=Math.max(0,score-420);tools=coffee=0;weapon=ammo=0;checkpoint=92;}x=checkpoint;y=250;vx=0;vy=-130;cam=Math.max(0,checkpoint-230);}

  @Override void drawGame(Canvas c){
    drawBackdrop(c);float sx=64-cam;
    for(float cp:new float[]{world*.31f,world*.62f}){float xx=sx+cp;if(xx>-60&&xx<1020){line(c,Color.rgb(77,62,45),6,xx,319,xx,389);fill(c,Color.rgb(202,149,42),xx-20,309,xx+27,338);text(c,"CP",xx+3,330,13,Color.rgb(38,31,24),Paint.Align.CENTER,true);}}
    for(Platform q:ps){float xx=sx+q.x;if(xx>1010||xx+q.w<-40)continue;fill(c,Color.rgb(59,64,68),xx,q.y,xx+q.w,q.y+q.h);for(float k=xx;k<xx+q.w;k+=32){fill(c,Color.rgb(130,132,128),k,q.y,Math.min(k+23,xx+q.w),q.y+6);fill(c,Color.rgb(32,36,39),k+23,q.y,Math.min(k+32,xx+q.w),q.y+q.h);}stroke(c,Color.rgb(12,15,18),3,xx,q.y,xx+q.w,q.y+q.h);}
    for(Barrier g:gates)if(g.hp>0){float xx=sx+g.x;if(xx>-50&&xx<1010){fill(c,Color.rgb(74,70,66),xx-11,208,xx+12,390);for(int yy=220;yy<380;yy+=28)fill(c,Color.rgb(179,117,38),xx-8,yy,xx+9,yy+8);barSmall(c,xx-30,193,g.hp,g.maxHp,Color.rgb(232,143,44));}}
    for(Hazard h:hazards){float xx=sx+h.x;if(xx<-130||xx>1040)continue;if(h.type==0){for(int i=0;i<7;i++){float k=xx+i*h.w/7;fill(c,Color.rgb(211,72,34),k,h.y,k+h.w/14,h.y+23);line(c,Color.rgb(255,190,55),3,k,h.y,k+h.w/14,h.y+23);}}else if(h.type==1){float cx=xx+h.w/2,cy=h.y+12,r=h.w*.30f;for(int i=0;i<12;i++){double a=i*Math.PI*2/12;line(c,Color.rgb(194,199,201),5,cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r);}fill(c,Color.rgb(80,83,85),cx-8,cy-8,cx+8,cy+8);}else{for(int i=0;i<6;i++){float k=xx+i*h.w/6;line(c,Color.rgb(89,190,255),4,k,h.y,k+h.w/12,h.y+26);}}}
    for(Pickup q:items)if(!q.got){float xx=sx+q.x;if(xx>-50&&xx<1010){float pulse=1+(float)Math.sin(clock*5+q.x)*.08f;if(q.kind==0){fill(c,Color.rgb(255,208,58),xx-9*pulse,q.y-9*pulse,xx+9*pulse,q.y+9*pulse);fill(c,Color.rgb(93,67,20),xx-3,q.y-12,xx+3,q.y+12);}else if(q.kind==3){fill(c,Color.rgb(82,112,145),xx-13,q.y-10,xx+13,q.y+10);text(c,"R",xx,q.y+5,13,Color.WHITE,Paint.Align.CENTER,true);}else if(q.kind==4){fill(c,Color.rgb(191,51,48),xx-11,q.y-11,xx+11,q.y+11);text(c,"+",xx,q.y+6,17,Color.WHITE,Paint.Align.CENTER,true);}else SpriteKit.icon(c,p,q.kind==1?2:4,xx,q.y,pulse);}}
    for(Enemy e:enemies)if(!e.dead){float xx=sx+e.x;if(xx<-100||xx>1060)continue;drawEnemy(c,e,xx,e.y);}
    for(Shot s:shots){float xx=sx+s.x;if(xx>-30&&xx<990){fill(c,s.enemy?Color.rgb(255,83,60):Color.rgb(255,214,91),xx-7,s.y-3,xx+8,s.y+3);}}
    int state=!ground?4:Math.abs(vx)>35?1:attackCd>0&&weapon==0?2:0;if(invuln<=0||((int)(clock*12)%2==0))SpriteKit.worker(c,p,sx+x+19,y+58,1.12f,vx<0?-1:1,state,clock,false);fx.draw(c,p);
    drawHud(c);if(stageBanner>0){fill(c,Color.argb(190,6,8,12),250,160,710,270);stroke(c,Color.rgb(208,158,70),3,250,160,710,270);text(c,t("MŰSZAK","SHIFT","SCHICHT","TURNO","ÉQUIPE","班次","TURNO","TURNO","ZMIANA","PLOEG","SCHIMB","СМЕНА")+" "+level,480,205,30,Color.rgb(255,221,145),Paint.Align.CENTER,true);text(c,theme()==0?"MACHINE HALL":theme()==1?"NIGHT YARD":"FURNACE LINE",480,239,17,Color.LTGRAY,Paint.Align.CENTER,true);}
    arcadeButton(c,bL,"◀",Color.rgb(37,105,163),left);arcadeButton(c,bR,"▶",Color.rgb(37,105,163),right);arcadeButton(c,bD,dashCd<=0?t("ROHAM","DASH","SPRINT","IMPULSO","RUÉE","冲刺","SCATTO","ARRANCADA","ZRYW","DASH","AVÂNT","РЫВОК"):"…",Color.rgb(104,73,153),touched(bD));arcadeButton(c,bA,weapon==1?"RIVET":"HAMMER",Color.rgb(164,68,39),touched(bA));arcadeButton(c,bJ,t("UGRÁS","JUMP","SPRUNG","SALTO","SAUT","跳跃","SALTO","PULO","SKOK","SPRONG","SALT","ПРЫЖОК"),Color.rgb(181,79,35),jump);
  }
  private void drawBackdrop(Canvas c){int th=theme(),sky=th==0?Color.rgb(14,18,25):th==1?Color.rgb(7,13,25):Color.rgb(29,15,12);fill(c,sky,0,0,LW,LH);for(int yy=80;yy<390;yy+=26)fill(c,th==2?Color.rgb(47,29,25):((yy/26)%2==0?Color.rgb(35,39,46):Color.rgb(25,30,38)),0,yy,LW,yy+13);for(int bx=-80;bx<1050;bx+=150){float xx=bx-(cam*.10f%150);fill(c,Color.rgb(10,13,18),xx,90,xx+10,390);fill(c,Color.rgb(69,72,75),xx+10,90,xx+18,390);line(c,Color.rgb(116,75,39),7,xx+35,120,xx+118,120);line(c,Color.rgb(116,75,39),5,xx+80,120,xx+80,188);}for(int i=0;i<7;i++){float xx=50+i*170-(cam*.18f%170);fill(c,Color.rgb(50,55,59),xx,230,xx+104,390);fill(c,Color.rgb(94,74,48),xx+11,246,xx+28,390);fill(c,Color.rgb(17,20,24),xx+44,274,xx+87,390);fill(c,th==2?Color.rgb(230,82,28):Color.rgb(188,80,30),xx+55,287,xx+75,304);}if(th==1)for(int i=0;i<9;i++){float xx=(i*137-(cam*.05f%137));fill(c,Color.rgb(194,208,223),xx,112,xx+3,116);} }
  private void drawEnemy(Canvas c,Enemy e,float xx,float yy){if(e.boss){SpriteKit.worker(c,p,xx,yy+58,1.72f,(x+19)<e.x?-1:1,e.hp%2==0?2:0,clock,true);fill(c,Color.rgb(120,36,32),xx-31,yy-86,xx+31,yy-73);text(c,"FOREMAN",xx,yy-92,13,Color.rgb(255,208,96),Paint.Align.CENTER,true);barSmall(c,xx-43,yy-106,e.hp,e.maxHp,Color.rgb(210,52,42));}else if(e.type==0){SpriteKit.worker(c,p,xx,yy+58,.92f,(x+19)<e.x?-1:1,1,clock,true);}else if(e.type==1){fill(c,Color.rgb(70,76,84),xx-20,yy-11,xx+20,yy+11);line(c,Color.rgb(177,184,190),4,xx-31,yy-17,xx+31,yy-17);fill(c,Color.rgb(218,68,48),xx-5,yy-4,xx+6,yy+5);}else{fill(c,Color.rgb(79,67,56),xx-20,yy-18,xx+20,yy+18);for(int i=0;i<10;i++){double a=i*Math.PI*2/10;line(c,Color.rgb(188,193,196),4,xx,yy,xx+(float)Math.cos(a)*28,yy+(float)Math.sin(a)*28);}fill(c,Color.rgb(207,73,42),xx-5,yy-5,xx+5,yy+5);}if(!e.boss)barSmall(c,xx-20,yy-36,e.hp,e.maxHp,Color.rgb(208,79,53));}
  private void drawHud(Canvas c){fill(c,Color.rgb(7,9,13),0,0,960,78);ArcadeFx.beveledPanel(c,p,12,12,310,65,Color.rgb(74,43,25),Color.rgb(205,157,78),Color.rgb(33,20,15));text(c,"WORKSHOP RUN '95",28,47,25,Color.rgb(255,226,150),Paint.Align.LEFT,true);text(c,t("PONT","SCORE","PUNKTE","PUNTOS","SCORE","得分","PUNTI","PONTOS","PUNKTY","SCORE","SCOR","СЧЁТ")+" "+score,330,39,16,Color.WHITE,Paint.Align.LEFT,true);text(c,"LIFE "+lives+"  TOOL "+tools,330,61,13,Color.rgb(149,211,255),Paint.Align.LEFT,true);text(c,weapon==1?"RIVET "+ammo:"HAMMER",520,39,15,weapon==1?Color.rgb(125,198,255):Color.rgb(239,182,81),Paint.Align.LEFT,true);if(combo>=2)text(c,"COMBO x"+combo,520,61,14,Color.rgb(255,211,71),Paint.Align.LEFT,true);Enemy b=boss();if(b!=null&&!b.dead&&x>world-800){text(c,"BOSS",710,31,12,Color.rgb(255,190,96),Paint.Align.LEFT,true);barSmall(c,760,20,b.hp,b.maxHp,Color.rgb(218,61,42));}if(boost>0){fill(c,Color.rgb(41,34,28),710,50,930,64);fill(c,Color.rgb(232,87,43),712,52,712+216*(boost/5f),62);}}
  private void barSmall(Canvas c,float bx,float by,int v,int max,int col){float w=110;fill(c,Color.rgb(34,31,31),bx,by,bx+w,by+10);fill(c,col,bx+1,by+1,bx+1+(w-2)*Math.max(0,v)/(float)Math.max(1,max),by+9);stroke(c,Color.rgb(215,205,181),1,bx,by,bx+w,by+10);}

  private void sync(){left=touched(bL);right=touched(bR);jump=touched(bJ);}
  @Override void onGameDown(float tx,float ty){sync();if(in(bD,tx,ty)&&dashCd<=0)dash=true;if(in(bA,tx,ty))attack=true;}
  @Override void onGameMove(float tx,float ty){sync();}
  @Override void onGameUp(float tx,float ty){sync();}
}
