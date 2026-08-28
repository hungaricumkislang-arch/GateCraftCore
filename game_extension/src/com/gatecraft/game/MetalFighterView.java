package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.Random;

final class MetalFighterView extends BaseArcadeView {
  private final ArcadeFx fx=new ArcadeFx();
  private final Random rnd=new Random(33);
  private float px=210,ex=735,pvx,evx,py,ey,pvy,evy,aiTimer,attackTimer,eAttackTimer,specialCd,hitFlash,shake,roundClock=60,comboTimer;
  private int php=100,ehp=100,score,round=1,wins,losses,special=35,combo;
  private boolean left,right,block,punch,kick,arc,jump,jumpLatch,pGround=true,eGround=true;
  private int pAttack,eAttack,pFacing=1,eFacing=-1;
  private final RectF lBtn=new RectF(18,432,110,524),rBtn=new RectF(116,432,208,524),jBtn=new RectF(214,432,316,524);
  private final RectF punchBtn=new RectF(574,426,666,524),kickBtn=new RectF(671,426,763,524),blockBtn=new RectF(768,426,860,524),arcBtn=new RectF(865,420,958,524);

  MetalFighterView(Context c,GateCraftGame o){super(c,o);}
  @Override public int score(){return score;} @Override public int level(){return round;} @Override public int lives(){return Math.max(1,2-losses);}
  @Override public void shutdown(){super.shutdown();fx.clear();}
  @Override public void restart(){px=210;ex=735;py=ey=0;pvy=evy=0;php=100;ehp=100;pvx=evx=0;specialCd=0;hitFlash=shake=0;attackTimer=eAttackTimer=0;left=right=block=punch=kick=arc=jump=false;pGround=eGround=true;roundClock=60;combo=0;comboTimer=0;special=Math.max(20,special);invalidate();}

  @Override void updateGame(float dt){
    fx.update(dt);specialCd=Math.max(0,specialCd-dt);hitFlash=Math.max(0,hitFlash-dt);shake=Math.max(0,shake-dt);attackTimer=Math.max(0,attackTimer-dt);eAttackTimer=Math.max(0,eAttackTimer-dt);comboTimer=Math.max(0,comboTimer-dt);if(comboTimer<=0)combo=0;roundClock=Math.max(0,roundClock-dt);
    float sp=300;if(left)pvx=-sp;else if(right)pvx=sp;else pvx*=Math.pow(.0008,dt);
    if(jump&&!jumpLatch&&pGround){pvy=-565;pGround=false;jumpLatch=true;fx.dust(px,385,8,Color.rgb(92,88,82));}if(!jump)jumpLatch=false;
    if(!pGround){pvy+=1360*dt;py+=pvy*dt;if(py>=0){py=0;pvy=0;pGround=true;fx.dust(px,388,8,Color.rgb(98,93,84));}}
    px+=pvx*dt;px=Math.max(62,Math.min(898,px));
    pFacing=px<=ex?1:-1;eFacing=-pFacing;
    aiTimer-=dt;if(aiTimer<=0){aiTimer=.16f+rnd.nextFloat()*.24f;float d=px-ex;float ad=Math.abs(d);if(ad>155){evx=(d>0?1:-1)*(165+rnd.nextFloat()*80);}else{evx*=.3f;float r=rnd.nextFloat();if(r<.46f)enemyStrike(rnd.nextFloat()<.27f?2:1);else if(r<.58f&&eGround){evy=-500;eGround=false;}else if(r<.72f&&special>45){} }}
    if(!eGround){evy+=1360*dt;ey+=evy*dt;if(ey>=0){ey=0;evy=0;eGround=true;}}
    ex+=evx*dt;ex=Math.max(62,Math.min(898,ex));evx*=Math.pow(.05,dt);
    if(Math.abs(px-ex)<74&&Math.abs(py-ey)<65){float mid=(px+ex)/2;if(px<ex){px=Math.min(px,mid-38);ex=Math.max(ex,mid+38);}else{px=Math.max(px,mid+38);ex=Math.min(ex,mid-38);}}
    if(punch){punch=false;playerStrike(10,102,1,.24f);}if(kick){kick=false;playerStrike(16,126,2,.33f);}if(arc){arc=false;if(specialCd<=0&&special>=50){special-=50;specialCd=2.2f;playerStrike(28,245,3,.46f);fx.sparks(px+pFacing*76,318+py,28,Color.rgb(100,202,255));}}
    special=Math.min(100,special+(int)(dt*4));
    if(roundClock<=0){if(php>=ehp)ehp=0;else php=0;}
    if(ehp<=0)winRound();else if(php<=0)loseRound();
  }

  private void playerStrike(int dmg,float range,int kind,float duration){if(attackTimer>0)return;pAttack=kind;attackTimer=duration;float dy=Math.abs(py-ey);if(Math.abs(px-ex)<=range&&dy<82){int dealt=dmg+(combo>=2?3:0);ehp-=dealt;ex+=pFacing*(kind==2?28:18);hitFlash=.10f;shake=kind==3?.20f:.09f;combo++;comboTimer=.9f;special=Math.min(100,special+9);score+=dealt*(5+combo);owner.reportScore(score);fx.sparks(ex-pFacing*22,316+ey,kind==3?26:11,kind==3?Color.rgb(105,205,255):Color.rgb(255,192,70));}}
  private void enemyStrike(int kind){if(eAttackTimer>0)return;eAttack=kind;eAttackTimer=kind==2?.38f:.27f;if(Math.abs(px-ex)<(kind==2?125:102)&&Math.abs(py-ey)<82){int d=kind==2?17:12;if(block){d=4;special=Math.min(100,special+4);fx.sparks(px-pFacing*28,320+py,8,Color.rgb(190,210,230));}php-=d;px-=pFacing*(block?5:18);hitFlash=.10f;shake=.08f;combo=0;fx.sparks(px+pFacing*22,317+py,9,Color.rgb(255,145,55));}}
  private void winRound(){score+=1200+Math.max(0,(int)roundClock)*10;wins++;round++;owner.reportGameComplete(score);fx.sparks(ex,280,40,Color.rgb(255,218,75));restart();}
  private void loseRound(){losses++;owner.reportGameOver(score);score=Math.max(0,score-250);restart();}

  @Override void drawGame(Canvas c){
    float shx=shake>0?((float)Math.sin(System.currentTimeMillis()*.08)*7):0;c.save();c.translate(shx,0);
    // arcade construction yard
    fill(c,Color.rgb(26,17,31),0,0,960,540);fill(c,Color.rgb(76,42,91),0,74,960,238);for(int y=82;y<238;y+=18)fill(c,(y/18)%2==0?Color.rgb(88,50,102):Color.rgb(66,38,82),0,y,960,y+8);
    // skyline and crane
    for(int x=-30;x<1000;x+=145){fill(c,Color.rgb(39,34,37),x,190,x+76,392);fill(c,Color.rgb(102,70,43),x+8,205,x+19,392);fill(c,Color.rgb(13,15,19),x+29,246,x+64,392);for(int yy=260;yy<370;yy+=31)fill(c,Color.rgb(205,128,42),x+37,yy,x+52,yy+8);}line(c,Color.rgb(205,148,57),8,30,137,575,137);line(c,Color.rgb(205,148,57),7,575,137,805,214);line(c,Color.rgb(205,148,57),5,300,137,300,218);line(c,Color.rgb(62,49,39),3,805,214,805,298);
    // welding station animated
    fill(c,Color.rgb(65,37,30),427,284,540,390);fill(c,Color.rgb(232,84,29),446,305,521,365);fill(c,Color.rgb(255,199,57),459,315,508,351);for(int i=0;i<17;i++){float sx=475+(i%6)*12,sy=297-(i*19%66)+(float)Math.sin(i+System.currentTimeMillis()*.01)*4;fill(c,i%2==0?Color.rgb(255,216,73):Color.rgb(239,104,35),sx,sy,sx+3,sy+6);}
    // floor perspective
    fill(c,Color.rgb(42,50,58),0,358,960,540);for(int y=360;y<540;y+=23)line(c,Color.rgb(25,31,37),2,0,y,960,y);for(int x=-120;x<1080;x+=56)line(c,Color.rgb(67,77,84),2,x,358,x+155,540);line(c,Color.rgb(182,135,49),4,0,394,960,394);
    drawFighter(c,px,389+py,false,pFacing,pAttack,attackTimer,block);drawFighter(c,ex,389+ey,true,eFacing,eAttack,eAttackTimer,false);fx.draw(c,p);
    if(hitFlash>0)fill(c,Color.argb(55,255,235,175),0,74,960,410);c.restore();
    // HUD always stable
    fill(c,Color.rgb(7,7,11),0,0,960,77);text(c,"METAL FIGHTER",480,30,27,Color.rgb(248,211,80),Paint.Align.CENTER,true);text(c,t("LAKATOS SZAKI","METALWORKER","METALLBAUER","HERRERO","MÉTALLIER","金属工","FABBRO","SERRALHEIRO","ŚLUSARZ","METAALBEWERKER","LĂCĂTUȘ","СЛЕСАРЬ"),18,61,13,Color.WHITE,Paint.Align.LEFT,true);text(c,t("KÓKLER KONTÁR","HACK RIVAL","PFUSCHER","CHAPUCERO","BRICOLEUR","劣质工","PASTICCIONE","GAMBIARRA","PARTACZ","BEUNHAAS","CÂRPACI","ХАЛТУРЩИК"),942,61,13,Color.WHITE,Paint.Align.RIGHT,true);hpBar(c,18,9,php,Color.rgb(57,194,75),false);hpBar(c,662,9,ehp,Color.rgb(211,59,46),true);text(c,"R"+round+"  "+Math.max(0,(int)roundClock),480,60,18,Color.WHITE,Paint.Align.CENTER,true);
    fill(c,Color.rgb(31,30,37),345,11,615,19);fill(c,Color.rgb(112,65,164),347,13,347+266*special/100f,17);text(c,"ARC "+special+"%",480,18,10,Color.rgb(214,190,255),Paint.Align.CENTER,true);if(combo>=2)text(c,"COMBO x"+combo,480,96,22,Color.rgb(255,201,63),Paint.Align.CENTER,true);
    arcadeButton(c,lBtn,"◀",Color.rgb(45,99,158),left);arcadeButton(c,rBtn,"▶",Color.rgb(45,99,158),right);arcadeButton(c,jBtn,"▲",Color.rgb(45,99,158),jump);arcadeButton(c,punchBtn,t("ÜT","HIT","HIEB","GOLPE","COUP","拳","COLPO","SOCO","CIOS","SLAG","LOV","УДАР"),Color.rgb(169,58,43),false);arcadeButton(c,kickBtn,t("RÚG","KICK","TRITT","PATADA","PIED","踢","CALCIO","CHUTE","KOP","TRAP","PICIOR","ПИНОК"),Color.rgb(178,91,35),false);arcadeButton(c,blockBtn,t("VÉD","BLOCK","BLOCK","BLOQ","BLOC","防御","PARA","DEF","BLOK","BLOK","BLOC","БЛОК"),Color.rgb(54,101,139),block);arcadeButton(c,arcBtn,(special>=50&&specialCd<=0)?t("ÍV","ARC","LICHT","ARCO","ARC","电弧","ARCO","ARCO","ŁUK","BOOG","ARC","ДУГА"):"…",Color.rgb(119,65,165),false);
  }

  private void hpBar(Canvas c,float x,float y,int v,int col,boolean reverse){float w=280;fill(c,Color.rgb(40,35,31),x,y,x+w,y+19);float f=Math.max(0,v)/100f;if(reverse)fill(c,col,x+w-w*f+2,y+2,x+w-2,y+17);else fill(c,col,x+2,y+2,x+2+(w-4)*f,y+17);stroke(c,Color.rgb(234,220,170),2,x,y,x+w,y+19);}

  private void drawFighter(Canvas c,float x,float ground,boolean enemy,int facing,int attack,float timer,boolean defending){
    int state=defending?3:(timer>0?2:(Math.abs(enemy?evx:pvx)>40?1:0));float phase=(System.currentTimeMillis()%1000)/1000f;SpriteKit.worker(c,p,x,ground,1.85f,facing,state,phase,enemy);
    if(timer>0){float dir=facing>=0?1:-1;if(attack==1){fill(c,enemy?Color.rgb(196,145,101):Color.rgb(224,174,113),x+dir*18,ground-85,x+dir*54,ground-69);}else if(attack==2){line(c,Color.rgb(47,49,55),12,x+dir*8,ground-36,x+dir*58,ground-53);}else if(attack==3){line(c,Color.rgb(94,194,255),12,x+dir*32,ground-88,x+dir*142,ground-111);line(c,Color.WHITE,3,x+dir*36,ground-88,x+dir*142,ground-111);}}
  }

  @Override void onGameDown(float x,float y){left=in(lBtn,x,y);right=in(rBtn,x,y);jump=in(jBtn,x,y);if(in(blockBtn,x,y))block=true;if(in(punchBtn,x,y))punch=true;if(in(kickBtn,x,y))kick=true;if(in(arcBtn,x,y))arc=true;}
  @Override void onGameMove(float x,float y){left=in(lBtn,x,y);right=in(rBtn,x,y);jump=in(jBtn,x,y);block=in(blockBtn,x,y);}
  @Override void onGameUp(float x,float y){left=right=block=jump=false;}
}
