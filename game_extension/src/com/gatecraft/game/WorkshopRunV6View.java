package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class WorkshopRunV6View extends BaseArcadeView {
  static final class Platform { float x,y,w,h,base,phase; boolean moving,conveyor; int dir=1; Platform(float x,float y,float w,float h){this.x=x;this.y=this.base=y;this.w=w;this.h=h;} Platform moving(float ph){moving=true;phase=ph;return this;} Platform belt(int d){conveyor=true;dir=d;return this;} }
  static final class Pickup { float x,y; int kind; boolean got; Pickup(float x,float y,int k){this.x=x;this.y=y;kind=k;} }
  static final class Hazard { float x,y,w; int type; Hazard(float x,float y,float w,int t){this.x=x;this.y=y;this.w=w;type=t;} }
  static final class Crate { float x,y; int hp=2,drop; boolean broken; Crate(float x,float y,int d){this.x=x;this.y=y;drop=d;} }
  static final class Enemy { float x,y,baseX,vx,phase; int type,hp,maxHp; boolean dead; Enemy(float x,float y,int type,int hp){this.x=this.baseX=x;this.y=y;this.type=type;this.hp=this.maxHp=hp;} }

  private final List<Platform> ps=new ArrayList<Platform>();
  private final List<Pickup> items=new ArrayList<Pickup>();
  private final List<Hazard> hazards=new ArrayList<Hazard>();
  private final List<Crate> crates=new ArrayList<Crate>();
  private final List<Enemy> enemies=new ArrayList<Enemy>();
  private final ArcadeFx fx=new ArcadeFx();
  private float x,y,vx,vy,cam,world,clock,coyote,jumpBuf,dashCd,boost,invuln,checkpoint=92,attackCd,attackAnim,comboTimer,stageClear;
  private boolean left,right,jump,jumpPrev,dash,attack,ground,wasGround;
  private int score,lives=3,level=1,tools,coffee,bolts,combo,sector,stageBossHp;
  private final RectF bL=new RectF(20,424,132,522),bR=new RectF(140,424,252,522),bD=new RectF(548,430,670,522),bA=new RectF(680,420,812,522),bJ=new RectF(822,410,950,522);

  WorkshopRunV6View(Context c,GateCraftGame o){super(c,o);restart();}
  @Override public int score(){return score;} @Override public int level(){return level;} @Override public int lives(){return lives;}
  @Override public void shutdown(){super.shutdown();fx.clear();ps.clear();items.clear();hazards.clear();crates.clear();enemies.clear();}
  @Override public void restart(){ps.clear();items.clear();hazards.clear();crates.clear();enemies.clear();fx.clear();x=92;y=300;vx=vy=cam=0;ground=wasGround=jumpPrev=false;coyote=jumpBuf=dashCd=boost=invuln=attackCd=attackAnim=comboTimer=stageClear=0;checkpoint=92;sector=0;combo=0;build();invalidate();}

  private void build(){
    world=3200+level*220;
    ps.add(new Platform(0,390,world,70));
    ps.add(new Platform(300,315,180,24));ps.add(new Platform(590,270,220,24).moving(.4f));
    ps.add(new Platform(900,330,190,24).belt(1));ps.add(new Platform(1210,250,240,24).moving(1.2f));
    ps.add(new Platform(1580,300,220,24).belt(-1));ps.add(new Platform(1910,235,250,24).moving(2.1f));
    ps.add(new Platform(2280,315,200,24));ps.add(new Platform(2560,245,230,24).moving(.7f));ps.add(new Platform(world-420,300,300,24));
    for(int i=0;i<15;i++)items.add(new Pickup(240+i*195,230-(i%4)*22,i%6==5?1:0));
    items.add(new Pickup(820,342,2));items.add(new Pickup(1780,342,2));items.add(new Pickup(world-560,342,3));
    hazards.add(new Hazard(735,365,78,0));hazards.add(new Hazard(1390,362,92,1));hazards.add(new Hazard(2050,365,105,2));hazards.add(new Hazard(2700,362,94,1));
    crates.add(new Crate(520,350,0));crates.add(new Crate(1110,350,2));crates.add(new Crate(1710,350,1));crates.add(new Crate(2410,350,3));crates.add(new Crate(world-720,350,2));
    enemies.add(new Enemy(430,389,0,3));enemies.add(new Enemy(1010,389,0,3));enemies.add(new Enemy(1330,246,1,2));enemies.add(new Enemy(1670,389,0,4));enemies.add(new Enemy(2130,231,1,3));enemies.add(new Enemy(2520,389,0,4));
    Enemy boss=new Enemy(world-245,389,2,10+level*2);enemies.add(boss);stageBossHp=boss.hp;
  }

  @Override void updateGame(float dt){
    clock+=dt;fx.update(dt);dashCd=Math.max(0,dashCd-dt);boost=Math.max(0,boost-dt);invuln=Math.max(0,invuln-dt);jumpBuf=Math.max(0,jumpBuf-dt);coyote=Math.max(0,coyote-dt);attackCd=Math.max(0,attackCd-dt);attackAnim=Math.max(0,attackAnim-dt);comboTimer=Math.max(0,comboTimer-dt);stageClear=Math.max(0,stageClear-dt);if(comboTimer<=0)combo=0;
    for(Platform q:ps)if(q.moving)q.y=q.base+(float)Math.sin(clock*1.55f+q.phase)*30;
    float vmax=boost>0?405:325,acc=ground?1400:900;if(left)vx-=acc*dt;if(right)vx+=acc*dt;if(!left&&!right)vx*=Math.pow(ground?.0006:.04,dt);vx=Math.max(-vmax,Math.min(vmax,vx));
    if(dash&&dashCd<=0){dash=false;dashCd=.8f;float d=right?1:left?-1:vx<0?-1:1;vx=d*(boost>0?590:500);invuln=Math.max(invuln,.14f);fx.dust(x+19,y+56,12,Color.rgb(160,134,91));tone(24,35);}
    if(jump&&!jumpPrev)jumpBuf=.13f;jumpPrev=jump;if(jumpBuf>0&&(ground||coyote>0)){vy=-545;ground=false;coyote=jumpBuf=0;fx.dust(x+19,y+56,8,Color.rgb(151,145,132));tone(24,28);}vy+=1320*dt;
    float nx=x+vx*dt,ny=y+vy*dt;wasGround=ground;ground=false;Platform landed=null;
    for(Platform q:ps)if(x+38>q.x&&x<q.x+q.w&&y+58<=q.y+10&&ny+58>=q.y&&vy>=0){ny=q.y-58;vy=0;ground=true;landed=q;if(!wasGround)fx.dust(x+19,q.y,6,Color.rgb(126,121,109));}
    if(landed!=null&&landed.conveyor)nx+=landed.dir*70*dt;if(wasGround&&!ground)coyote=.11f;x=Math.max(0,Math.min(world-40,nx));y=ny;if(y>535)hurt();
    for(Hazard h:hazards)if(invuln<=0&&x+38>h.x&&x<h.x+h.w&&y+58>h.y&&y<h.y+30){fx.sparks(x+19,y+42,18,h.type==2?Color.rgb(100,205,255):Color.rgb(255,176,54));hurt();break;}
    if(attack){attack=false;doAttack();}
    updateEnemies(dt);
    for(Pickup q:items)if(!q.got&&Math.abs(x+19-q.x)<34&&Math.abs(y+29-q.y)<40){q.got=true;collect(q.kind,q.x,q.y);}
    if(x>world*.34f&&sector<1){sector=1;checkpoint=world*.34f;}if(x>world*.66f&&sector<2){sector=2;checkpoint=world*.66f;}
    float target=x-(vx>0?315:vx<0?165:245);cam+=(target-cam)*Math.min(1,dt*7.5f);cam=Math.max(0,Math.min(world-830,cam));
    if(x>world-95&&bossDead()){stageClear=.8f;score+=1500+tools*100+combo*35;level++;tone(27,100);owner.reportLevelComplete(level,score);restart();}
  }

  private void updateEnemies(float dt){
    for(Enemy e:enemies){if(e.dead)continue;e.phase+=dt;float dx=(x+19)-e.x,ad=Math.abs(dx);if(e.type==0){if(ad<300)e.vx+=(dx>0?1:-1)*300*dt;else e.vx+=(e.x>e.baseX+75?-1:e.x<e.baseX-75?1:0)*170*dt;e.vx=Math.max(-95,Math.min(95,e.vx));e.x+=e.vx*dt;e.vx*=Math.pow(.14,dt);}else if(e.type==1){e.y=260+(float)Math.sin(clock*2.2+e.baseX*.01)*34;if(ad<260)e.x+=Math.signum(dx)*58*dt;}else{if(ad<420)e.x+=Math.signum(dx)*(65+level*3)*dt;if(((int)(clock*2.4))%4==0&&ad<120&&invuln<=0)hurt();}
      if(ad<43&&Math.abs((y+50)-e.y)<54&&invuln<=0){hurt();e.x-=Math.signum(dx)*22;}
    }
  }

  private void doAttack(){if(attackCd>0)return;attackCd=.24f;attackAnim=.19f;float dir=vx<0?-1:1;int hits=0;for(Enemy e:enemies)if(!e.dead&&Math.abs((x+19+dir*34)-e.x)<60&&Math.abs((y+35)-e.y)<72){e.hp-=1+(tools>2?1:0);e.x+=dir*24;hits++;fx.sparks(e.x,e.y-30,10,Color.rgb(255,194,68));if(e.hp<=0){e.dead=true;score+=e.type==2?900:180+e.type*80;combo++;comboTimer=1.2f;bolts+=e.type==2?3:(combo%3==0?1:0);owner.reportScore(score);fx.sparks(e.x,e.y-20,e.type==2?32:16,Color.rgb(255,216,79));}}
    for(Crate q:crates)if(!q.broken&&Math.abs((x+19+dir*34)-q.x)<58&&Math.abs((y+38)-q.y)<70){q.hp--;hits++;fx.sparks(q.x,q.y-15,8,Color.rgb(211,157,74));if(q.hp<=0){q.broken=true;score+=80;items.add(new Pickup(q.x,q.y-35,q.drop));}}
    if(hits==0&&bolts>0&&combo>=3){bolts--;for(Enemy e:enemies)if(!e.dead&&Math.signum(e.x-x)==dir&&Math.abs(e.x-x)<300){e.hp-=2;fx.sparks(e.x,e.y-25,12,Color.rgb(106,207,255));break;}}tone(25,32);
  }

  private void collect(int kind,float px,float py){if(kind==0)score+=110;else if(kind==1){tools++;score+=260;}else if(kind==2){coffee++;boost=4.8f;score+=170;}else{lives=Math.min(5,lives+1);score+=300;}fx.sparks(px,py,12,kind==2?Color.rgb(255,93,53):Color.rgb(255,211,67));tone(25,45);owner.reportScore(score);}
  private boolean bossDead(){for(Enemy e:enemies)if(e.type==2)return e.dead;return true;}
  private void hurt(){if(invuln>0)return;lives--;invuln=1.15f;combo=0;tone(26,90);fx.sparks(x+19,y+30,16,Color.rgb(255,80,50));if(lives<=0){owner.reportGameOver(score);lives=3;score=Math.max(0,score-350);tools=coffee=bolts=0;checkpoint=92;}x=checkpoint;y=250;vx=0;vy=-120;cam=Math.max(0,checkpoint-230);}

  @Override void drawGame(Canvas c){
    int theme=(level-1)%3;int sky=theme==0?Color.rgb(13,17,26):theme==1?Color.rgb(21,14,20):Color.rgb(9,22,24);fill(c,sky,0,0,LW,LH);
    for(int yy=78;yy<390;yy+=26)fill(c,theme==1?(((yy/26)&1)==0?Color.rgb(48,30,34):Color.rgb(35,24,29)):(((yy/26)&1)==0?Color.rgb(36,39,48):Color.rgb(27,31,40)),0,yy,LW,yy+13);
    for(int bx=-80;bx<1050;bx+=150){float xx=bx-(cam*.10f%150);fill(c,Color.rgb(10,13,18),xx,90,xx+10,390);fill(c,Color.rgb(69,72,75),xx+10,90,xx+18,390);line(c,theme==2?Color.rgb(40,105,113):Color.rgb(116,75,39),7,xx+35,120,xx+118,120);line(c,Color.rgb(116,75,39),5,xx+80,120,xx+80,188);}
    float sx=64-cam;
    for(float cp:new float[]{world*.34f,world*.66f}){float xx=sx+cp;if(xx>-60&&xx<1020){line(c,Color.rgb(77,62,45),6,xx,320,xx,389);fill(c,Color.rgb(202,149,42),xx-20,310,xx+25,337);text(c,"CP",xx+2,330,13,Color.rgb(38,31,24),Paint.Align.CENTER,true);}}
    for(Platform q:ps){float xx=sx+q.x;if(xx>1010||xx+q.w<-40)continue;fill(c,q.conveyor?Color.rgb(74,72,67):Color.rgb(60,65,67),xx,q.y,xx+q.w,q.y+q.h);for(float k=xx;k<xx+q.w;k+=32){fill(c,Color.rgb(128,130,125),k,q.y,Math.min(k+24,xx+q.w),q.y+6);if(q.conveyor)line(c,Color.rgb(218,166,48),2,k+6,q.y+13,k+22,q.y+13);}stroke(c,Color.rgb(14,17,19),3,xx,q.y,xx+q.w,q.y+q.h);}
    for(Hazard h:hazards){float xx=sx+h.x;if(xx<-120||xx>1030)continue;if(h.type==0){for(int i=0;i<7;i++){float k=xx+i*h.w/7;fill(c,Color.rgb(211,72,34),k,h.y,k+h.w/14,h.y+22);}}else if(h.type==1){float cx=xx+h.w/2,cy=h.y+11,r=h.w*.30f;for(int i=0;i<12;i++){double a=i*Math.PI*2/12;line(c,Color.rgb(194,199,201),5,cx,cy,cx+(float)Math.cos(a)*r,cy+(float)Math.sin(a)*r);}fill(c,Color.rgb(80,83,85),cx-8,cy-8,cx+8,cy+8);}else{for(int i=0;i<5;i++)line(c,i%2==0?Color.rgb(90,206,255):Color.WHITE,3,xx+i*18,h.y+24,xx+10+i*18,h.y-2);}}
    for(Crate q:crates)if(!q.broken){float xx=sx+q.x;if(xx>-60&&xx<1020){fill(c,Color.rgb(105,68,38),xx-18,q.y-34,xx+18,q.y);stroke(c,Color.rgb(190,134,60),2,xx-18,q.y-34,xx+18,q.y);line(c,Color.rgb(58,42,30),3,xx-15,q.y-30,xx+15,q.y-4);line(c,Color.rgb(58,42,30),3,xx+15,q.y-30,xx-15,q.y-4);}}
    for(Pickup q:items)if(!q.got){float xx=sx+q.x;if(xx>-50&&xx<1010){float pulse=1+(float)Math.sin(clock*5+q.x)*.08f;if(q.kind==0){fill(c,Color.rgb(255,208,58),xx-9*pulse,q.y-9*pulse,xx+9*pulse,q.y+9*pulse);}else SpriteKit.icon(c,p,q.kind==1?2:q.kind==2?4:3,xx,q.y,pulse);}}
    for(Enemy e:enemies)if(!e.dead){float xx=sx+e.x;if(xx>-80&&xx<1040){if(e.type==0)SpriteKit.worker(c,p,xx,e.y,0.88f,x<e.x?-1:1,1,e.phase,true);else if(e.type==1){fill(c,Color.rgb(70,79,91),xx-18,e.y-22,xx+18,e.y+12);fill(c,Color.rgb(216,68,50),xx-6,e.y-15,xx+6,e.y-5);line(c,Color.rgb(155,166,177),4,xx-28,e.y-8,xx+28,e.y-8);}else{SpriteKit.worker(c,p,xx,e.y,1.45f,x<e.x?-1:1,2,e.phase,true);fill(c,Color.rgb(28,27,31),xx-45,e.y-88,xx+45,e.y-79);fill(c,Color.rgb(198,54,45),xx-43,e.y-86,xx-43+86*Math.max(0,e.hp)/(float)e.maxHp,e.y-81);text(c,t("MŰVEZETŐ","FOREMAN","MEISTER","CAPATAZ","CHEF","工头","CAPO","ENCARREGADO","BRYGADZISTA","VOORMAN","ȘEF","ПРОРАБ"),xx,e.y-96,12,Color.rgb(255,213,94),Paint.Align.CENTER,true);}}}
    int state=attackAnim>0?2:!ground?4:Math.abs(vx)>35?1:0;if(invuln<=0||((int)(clock*12)%2==0))SpriteKit.worker(c,p,sx+x+19,y+58,1.08f,vx<0?-1:1,state,clock,false);fx.draw(c,p);
    fill(c,Color.rgb(7,9,13),0,0,960,78);ArcadeFx.beveledPanel(c,p,12,12,335,65,Color.rgb(74,43,25),Color.rgb(205,157,78),Color.rgb(33,20,15));text(c,"WORKSHOP RUN '96",28,47,24,Color.rgb(255,226,150),Paint.Align.LEFT,true);text(c,t("PONT","SCORE","PUNKTE","PUNTOS","SCORE","得分","PUNTI","PONTOS","PUNKTY","SCORE","SCOR","СЧЁТ")+" "+score,360,42,16,Color.WHITE,Paint.Align.LEFT,true);text(c,"LIFE "+lives+"  TOOL "+tools+"  BOLT "+bolts,560,42,14,Color.rgb(177,220,255),Paint.Align.LEFT,true);text(c,t("SZINT","LEVEL","STUFE","NIVEL","NIVEAU","等级","LIVELLO","NÍVEL","POZIOM","NIVEAU","NIVEL","УРОВЕНЬ")+" "+level,835,42,14,Color.rgb(245,211,85),Paint.Align.LEFT,true);if(combo>=2)text(c,"COMBO x"+combo,470,66,14,Color.rgb(255,190,64),Paint.Align.CENTER,true);
    arcadeButton(c,bL,"◀",Color.rgb(37,105,163),left);arcadeButton(c,bR,"▶",Color.rgb(37,105,163),right);arcadeButton(c,bD,dashCd<=0?t("ROHAM","DASH","SPRINT","IMPULSO","RUÉE","冲刺","SCATTO","ARRANCADA","ZRYW","DASH","AVÂNT","РЫВОК"):"…",Color.rgb(114,77,154),touched(bD));arcadeButton(c,bA,t("ÜT","HIT","HIEB","GOLPE","COUP","拳","COLPO","SOCO","CIOS","SLAG","LOV","УДАР"),Color.rgb(170,61,43),touched(bA));arcadeButton(c,bJ,t("UGRÁS","JUMP","SPRUNG","SALTO","SAUT","跳跃","SALTO","PULO","SKOK","SPRONG","SALT","ПРЫЖОК"),Color.rgb(181,79,35),jump);
  }

  private void sync(){left=touched(bL);right=touched(bR);jump=touched(bJ);}
  @Override void onGameDown(float tx,float ty){sync();if(in(bD,tx,ty)&&dashCd<=0)dash=true;if(in(bA,tx,ty))attack=true;}
  @Override void onGameMove(float tx,float ty){sync();}
  @Override void onGameUp(float tx,float ty){sync();}
}
