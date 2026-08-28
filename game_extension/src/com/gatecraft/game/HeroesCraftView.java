package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class HeroesCraftView extends BaseArcadeView {
  static final class Node { int x,y,type; boolean taken; Node(int x,int y,int t){this.x=x;this.y=y;type=t;} }
  private final List<Node> nodes=new ArrayList<Node>();
  private final Random rnd=new Random(721);
  private final ArcadeFx fx=new ArcadeFx();
  private int page=0,hx=2,hy=6,fromX=2,fromY=6,toX=2,toY=6,iron=3,cement=2,tools=1,gold=150,blueprints,contracts,score,shop=1,weld=0,truck=0;
  private int ownHp=190,enemyHp=175,turn,selectedArmy=0,enemyType=0,crew0=6,crew1=3,crew2=1;
  private float moveT=1,animClock,combatAnim,flash;
  private boolean moving;
  private final RectF up=new RectF(770,354,842,424),down=new RectF(770,430,842,500),left=new RectF(694,430,766,500),right=new RectF(846,430,918,500);
  private final RectF townBtn=new RectF(690,273,804,337),combatBtn=new RectF(812,273,944,337),mapBtn=new RectF(744,432,944,496);
  private final RectF build0=new RectF(112,390,302,456),build1=new RectF(386,390,576,456),build2=new RectF(660,390,850,456);
  private final RectF attackBtn=new RectF(714,332,944,390),defendBtn=new RectF(714,398,825,456),skillBtn=new RectF(833,398,944,456);

  HeroesCraftView(Context c,GateCraftGame o){super(c,o);generateNodes();}
  @Override public int score(){return score;} @Override public void shutdown(){super.shutdown();nodes.clear();fx.clear();}
  @Override public void restart(){page=0;ownHp=190;enemyHp=175;turn=0;combatAnim=0;flash=0;moving=false;moveT=1;invalidate();}

  private void generateNodes(){nodes.clear();boolean[][] used=new boolean[13][9];used[2][6]=true;int[][] fixed={{5,6,0},{7,5,1},{9,3,2},{3,3,3},{10,6,4},{6,2,0},{1,4,1},{8,7,2},{11,2,3},{4,7,4},{9,1,0},{6,5,2},{2,2,3},{11,7,1}};for(int[] a:fixed){nodes.add(new Node(a[0],a[1],a[2]));used[a[0]][a[1]]=true;}for(int i=0;i<5;i++){int x=1+rnd.nextInt(11),y=1+rnd.nextInt(7);if(!used[x][y]){nodes.add(new Node(x,y,rnd.nextInt(5)));used[x][y]=true;}}}

  @Override void updateGame(float dt){animClock+=dt;fx.update(dt);flash=Math.max(0,flash-dt);if(moving){moveT=Math.min(1,moveT+dt*5.2f);if(moveT>=1){moving=false;hx=toX;hy=toY;collectAt(hx,hy);}}if(combatAnim>0)combatAnim=Math.max(0,combatAnim-dt);}
  @Override void drawGame(Canvas c){if(page==0)drawMap(c);else if(page==1)drawTown(c);else drawCombat(c);fx.draw(c,p);}

  private String resLine(){return "Fe "+iron+"   Cem "+cement+"   Tool "+tools+"   Gold "+gold+"   BP "+blueprints;}
  private void topBar(Canvas c,String title){fill(c,Color.rgb(17,12,10),0,0,960,78);fill(c,Color.rgb(88,57,31),0,64,960,78);text(c,title,26,37,26,Color.rgb(248,218,145),Paint.Align.LEFT,true);text(c,resLine(),420,36,15,Color.WHITE,Paint.Align.LEFT,true);text(c,t("MEGRENDELÉS","CONTRACT","AUFTRAG","ENCARGO","CONTRAT","订单","COMMESSA","SERVIÇO","ZLECENIE","OPDRACHT","LUCRARE","ЗАКАЗ")+" "+contracts,420,59,12,Color.rgb(225,190,105),Paint.Align.LEFT,false);}
  private void ornateFrame(Canvas c,float l,float top,float r,float b){fill(c,Color.rgb(68,44,25),l,top,r,b);stroke(c,Color.rgb(227,193,116),4,l,top,r,b);stroke(c,Color.rgb(28,20,15),2,l+7,top+7,r-7,b-7);for(float x=l+14;x<r-10;x+=36)fill(c,Color.rgb(113,76,36),x,top+4,x+14,top+9);for(float x=l+14;x<r-10;x+=36)fill(c,Color.rgb(113,76,36),x,b-9,x+14,b-4);}

  private void drawMap(Canvas c){
    topBar(c,"HEROES OF CRAFT & GATES");fill(c,Color.rgb(9,9,10),0,78,960,540);ornateFrame(c,16,90,676,524);fill(c,Color.rgb(18,22,20),27,101,665,513);
    float oxm=322,oym=126,tw=48,th=25;
    for(int y=0;y<9;y++)for(int x=0;x<13;x++){int terrain=terrain(x,y);int col;if(terrain==0)col=((x+y)&1)==0?Color.rgb(58,120,58):Color.rgb(53,109,54);else if(terrain==1)col=Color.rgb(42,87,123);else if(terrain==2)col=Color.rgb(111,93,55);else col=Color.rgb(44,91,48);float cx=oxm+(x-y)*tw/2,cy=oym+(x+y)*th/2;diamond(c,cx,cy,tw,th,col,Color.rgb(27,52,30));if(terrain==3&&((x*13+y*5)%3==0))tree(c,cx,cy-12,0.8f);if(terrain==2)roadMark(c,cx,cy);}
    // structures
    building(c,tileX(9,2,oxm,tw),tileY(9,2,oym,th)-28,0);building(c,tileX(10,6,oxm,tw),tileY(10,6,oym,th)-25,1);building(c,tileX(3,1,oxm,tw),tileY(3,1,oym,th)-24,2);
    for(Node n:nodes)if(!n.taken){float cx=tileX(n.x,n.y,oxm,tw),cy=tileY(n.x,n.y,oym,th)-14;drawNode(c,cx,cy,n.type);}
    float a=moving?moveT:1f;float hxv=fromX+(toX-fromX)*a,hyv=fromY+(toY-fromY)*a;float heroX=oxm+(hxv-hyv)*tw/2,heroY=oym+(hxv+hyv)*th/2-17;drawHero(c,heroX,heroY);
    // right command panel inspired by 90s strategy UI
    ornateFrame(c,688,90,948,524);fill(c,Color.rgb(30,24,20),700,103,936,257);text(c,t("VÁLLALKOZÓ","CONTRACTOR","UNTERNEHMER","CONTRATISTA","ENTREPRENEUR","承包商","IMPRESARIO","EMPREITEIRO","WYKONAWCA","AANNEMER","ANTREPRENOR","ПОДРЯДЧИК"),818,128,16,Color.rgb(248,219,147),Paint.Align.CENTER,true);portrait(c,818,184);text(c,t("Lépés","Move","Zug","Paso","Mouvement","移动","Mossa","Movimento","Ruch","Zet","Mișcare","Ход")+": "+(3+truck),818,241,13,Color.LTGRAY,Paint.Align.CENTER,false);
    arcadeButton(c,townBtn,t("MŰHELY","TOWN","WERKSTATT","TALLER","ATELIER","工坊","OFFICINA","OFICINA","WARSZTAT","WERKPLAATS","ATELIER","МАСТЕРСКАЯ"),Color.rgb(120,77,34),false);arcadeButton(c,combatBtn,t("HARC","COMBAT","KAMPF","COMBATE","COMBAT","战斗","LOTTA","COMBATE","WALKA","GEVECHT","LUPTĂ","БОЙ"),Color.rgb(132,47,43),false);
    arcadeButton(c,up,"▲",Color.rgb(71,79,108),false);arcadeButton(c,down,"▼",Color.rgb(71,79,108),false);arcadeButton(c,left,"◀",Color.rgb(71,79,108),false);arcadeButton(c,right,"▶",Color.rgb(71,79,108),false);
  }

  private int terrain(int x,int y){if((y>=7&&x<=3)||(x==0&&y>=5))return 1;if((x+y)%7==0||((x==4||x==5)&&y>2&&y<8))return 2;if((x*5+y*3)%8<=1)return 3;return 0;}
  private float tileX(int x,int y,float o,float tw){return o+(x-y)*tw/2;}private float tileY(int x,int y,float o,float th){return o+(x+y)*th/2;}
  private void roadMark(Canvas c,float x,float y){line(c,Color.rgb(150,126,71),3,x-12,y,x+12,y);}
  private void tree(Canvas c,float x,float y,float s){fill(c,Color.rgb(72,44,25),x-3*s,y,x+4*s,y+18*s);fill(c,Color.rgb(25,68,35),x-12*s,y-14*s,x+12*s,y+4*s);fill(c,Color.rgb(46,104,48),x-8*s,y-21*s,x+7*s,y-5*s);fill(c,Color.rgb(89,133,57),x-2*s,y-23*s,x+9*s,y-10*s);}
  private void drawNode(Canvas c,float x,float y,int kind){float bob=(float)Math.sin(animClock*4+x*.02f)*2;if(kind<=3)SpriteKit.icon(c,p,kind,x,y+bob,0.75f);else{SpriteKit.icon(c,p,4,x,y+bob,0.75f);text(c,"!",x,y-18+bob,13,Color.rgb(255,221,66),Paint.Align.CENTER,true);}}
  private void drawHero(Canvas c,float x,float y){SpriteKit.worker(c,p,x,y+27,0.62f,1,moving?1:0,animClock,false);fill(c,Color.rgb(225,186,52),x-2,y-37,x+3,y-31);}
  private void portrait(Canvas c,float x,float y){fill(c,Color.rgb(18,18,22),x-44,y-44,x+44,y+44);stroke(c,Color.rgb(216,181,99),3,x-44,y-44,x+44,y+44);SpriteKit.worker(c,p,x,y+37,1.15f,1,0,animClock,false);}
  private void building(Canvas c,float x,float y,int kind){int body=kind==0?Color.rgb(146,142,129):kind==1?Color.rgb(130,82,54):Color.rgb(95,102,111);fill(c,Color.rgb(53,43,36),x-30,y-2,x+30,y+32);fill(c,body,x-27,y-30,x+27,y+28);for(int i=-22;i<=20;i+=14)fill(c,Color.rgb(74,78,81),x+i,y-39,x+i+9,y-25);fill(c,Color.rgb(45,40,37),x-6,y+3,x+7,y+28);fill(c,Color.rgb(177,121,41),x-19,y-17,x-9,y-8);fill(c,Color.rgb(177,121,41),x+9,y-17,x+19,y-8);if(kind==1)line(c,Color.rgb(59,59,60),5,x+22,y-31,x+22,y-50);if(kind==2)fill(c,Color.rgb(117,47,41),x-21,y-33,x+21,y-27);}

  private void collectAt(int x,int y){for(Node n:nodes)if(!n.taken&&n.x==x&&n.y==y){n.taken=true;if(n.type==0){iron+=2;score+=70;}else if(n.type==1){cement+=2;score+=70;}else if(n.type==2){tools+=1;score+=90;}else if(n.type==3){gold+=120;blueprints++;score+=150;}else{contracts++;gold+=70;score+=180;}fx.sparks(360,210,12,Color.rgb(255,215,74));owner.reportScore(score);}}
  private void move(int dx,int dy){if(moving)return;int nx=Math.max(0,Math.min(12,hx+dx)),ny=Math.max(0,Math.min(8,hy+dy));if(nx==hx&&ny==hy)return;if(terrain(nx,ny)==1&&truck<2)return;fromX=hx;fromY=hy;toX=nx;toY=ny;moveT=0;moving=true;}

  private void drawTown(Canvas c){
    topBar(c,t("MŰHELYFEJLESZTÉS","WORKSHOP DEVELOPMENT","WERKSTATTAUSBAU","DESARROLLO DEL TALLER","DÉVELOPPEMENT ATELIER","工坊发展","SVILUPPO OFFICINA","DESENVOLVIMENTO DA OFICINA","ROZWÓJ WARSZTATU","WERKPLAATS ONTWIKKELING","DEZVOLTARE ATELIER","РАЗВИТИЕ МАСТЕРСКОЙ"));fill(c,Color.rgb(31,47,35),0,78,960,540);for(int y=82;y<540;y+=28)for(int x=0;x<960;x+=52)diamond(c,x+(y%56),y,52,25,((x+y)/20)%2==0?Color.rgb(61,99,60):Color.rgb(55,89,55),Color.rgb(40,65,42));
    // palisade & road
    for(int x=24;x<936;x+=30){fill(c,Color.rgb(88,61,35),x,94,x+16,119);fill(c,Color.rgb(117,78,42),x+3,91,x+13,116);}fill(c,Color.rgb(120,102,69),420,116,540,520);for(int y=122;y<510;y+=34)fill(c,Color.rgb(142,123,82),425,y,535,y+18);
    townBuilding(c,207,275,0,shop);townBuilding(c,480,245,1,weld);townBuilding(c,753,278,2,truck);
    arcadeButton(c,build0,t("MŰHELY +","WORKSHOP +","WERKSTATT +","TALLER +","ATELIER +","工坊 +","OFFICINA +","OFICINA +","WARSZTAT +","WERKPLAATS +","ATELIER +","МАСТЕРСКАЯ +"),Color.rgb(113,73,32),false);
    arcadeButton(c,build1,t("HEGESZTŐ +","WELDING +","SCHWEISSEN +","SOLDADURA +","SOUDAGE +","焊接 +","SALDATURA +","SOLDA +","SPAWANIE +","LASSEN +","SUDURĂ +","СВАРКА +"),Color.rgb(82,91,103),false);
    arcadeButton(c,build2,t("TEHERAUTÓ +","TRUCK +","LKW +","CAMIÓN +","CAMION +","卡车 +","CAMION +","CAMINHÃO +","CIĘŻARÓWKA +","TRUCK +","CAMION +","ГРУЗОВИК +"),Color.rgb(102,72,47),false);
    arcadeButton(c,mapBtn,t("VISSZA A TÉRKÉPRE","BACK TO MAP","ZUR KARTE","VOLVER AL MAPA","RETOUR CARTE","返回地图","TORNA ALLA MAPPA","VOLTAR AO MAPA","WRÓĆ DO MAPY","TERUG NAAR KAART","ÎNAPOI LA HARTĂ","НАЗАД НА КАРТУ"),Color.rgb(63,79,99),false);
  }
  private void townBuilding(Canvas c,float x,float y,int kind,int lv){int body=kind==0?Color.rgb(151,130,98):kind==1?Color.rgb(101,112,123):Color.rgb(116,86,57);float w=75+lv*8,h=70+lv*10;fill(c,Color.argb(80,0,0,0),x-w/2+8,y+15,x+w/2+18,y+38);fill(c,body,x-w/2,y-h/2,x+w/2,y+h/2);stroke(c,Color.rgb(52,45,39),3,x-w/2,y-h/2,x+w/2,y+h/2);for(float xx=x-w/2+8;xx<x+w/2-4;xx+=22)fill(c,Color.rgb(58,63,67),xx,y-h/2-10,xx+14,y-h/2+4);fill(c,Color.rgb(51,43,37),x-10,y+10,x+11,y+h/2);for(int i=0;i<lv;i++)fill(c,Color.rgb(226,172,53),x-w/2+8+i*12,y-h/2+10,x-w/2+16+i*12,y-h/2+18);text(c,"LV "+lv,x,y+h/2+24,14,Color.WHITE,Paint.Align.CENTER,true);}
  private void upgrade(int which){int cost=100+(which==0?shop:which==1?weld:truck)*60;if(gold<cost)return;gold-=cost;if(which==0)shop++;else if(which==1)weld++;else truck++;score+=120;owner.reportScore(score);fx.sparks(480,250,18,Color.rgb(255,212,73));}

  private void drawCombat(Canvas c){
    topBar(c,t("MEGRENDELÉS / HARC","CONTRACT / COMBAT","AUFTRAG / KAMPF","ENCARGO / COMBATE","CHANTIER / COMBAT","订单 / 战斗","COMMESSA / LOTTA","SERVIÇO / COMBATE","ZLECENIE / WALKA","OPDRACHT / GEVECHT","LUCRARE / LUPTĂ","ЗАКАЗ / БОЙ"));fill(c,Color.rgb(39,54,42),0,78,960,540);ornateFrame(c,20,94,680,516);fill(c,Color.rgb(47,66,48),31,105,669,505);
    for(int gy=0;gy<6;gy++)for(int gx=0;gx<7;gx++){float cx=105+gx*83+(gy%2)*41,cy=145+gy*55;diamond(c,cx,cy,84,50,((gx+gy)&1)==0?Color.rgb(77,96,63):Color.rgb(68,87,58),Color.rgb(39,52,37));}
    int[] counts={crew0,crew1,crew2};for(int i=0;i<3;i++)unit(c,125+i*110,390-i*44,i,false,counts[i],selectedArmy==i);for(int i=0;i<3;i++)unit(c,590-i*100,180+i*54,i,true,2+i,false);
    bar(c,60,112,ownHp,190,Color.rgb(58,183,78));bar(c,390,112,enemyHp,175,Color.rgb(196,57,47));if(combatAnim>0){float t=1-combatAnim/.35f;float sx=220+selectedArmy*105,sy=350-selectedArmy*40,tx=510-enemyType*70,ty=210+enemyType*45;line(c,Color.rgb(255,211,75),5,sx,sy,sx+(tx-sx)*Math.min(1,t),sy+(ty-sy)*Math.min(1,t));}
    ornateFrame(c,694,94,948,516);text(c,t("CSAPAT","CREW","TEAM","EQUIPO","ÉQUIPE","队伍","SQUADRA","EQUIPE","EKIPA","TEAM","ECHIPĂ","БРИГАДА"),821,128,18,Color.rgb(248,218,145),Paint.Align.CENTER,true);text(c,t("Segéd","Helper","Helfer","Ayudante","Aide","助手","Aiutante","Ajudante","Pomocnik","Helper","Ajutor","Помощник")+" x"+crew0,715,163,13,Color.WHITE,Paint.Align.LEFT,false);text(c,t("Hegesztő","Welder","Schweißer","Soldador","Soudeur","焊工","Saldatore","Soldador","Spawacz","Lasser","Sudor","Сварщик")+" x"+crew1,715,186,13,Color.WHITE,Paint.Align.LEFT,false);text(c,t("Nehéz lakatos","Heavy smith","Schwerer Schlosser","Cerrajero pesado","Métallier lourd","重型金工","Fabbro pesante","Serralheiro pesado","Ciężki ślusarz","Zware smid","Lăcătuș greu","Тяжёлый слесарь")+" x"+crew2,715,209,13,Color.WHITE,Paint.Align.LEFT,false);
    arcadeButton(c,attackBtn,t("TÁMADÁS","ATTACK","ANGRIFF","ATACAR","ATTAQUER","攻击","ATTACCA","ATACAR","ATAK","AANVAL","ATAC","АТАКА"),Color.rgb(136,48,42),false);arcadeButton(c,defendBtn,t("VÉD","DEFEND","WEHR","DEFENSA","DÉFENSE","防御","DIFESA","DEFESA","OBRONA","VERDEDIG","APĂRĂ","ЗАЩИТА"),Color.rgb(59,91,126),false);arcadeButton(c,skillBtn,t("ÍV","ARC","LICHT","ARCO","ARC","电弧","ARCO","ARCO","ŁUK","BOOG","ARC","ДУГА"),Color.rgb(111,66,151),false);arcadeButton(c,mapBtn,t("TÉRKÉP","MAP","KARTE","MAPA","CARTE","地图","MAPPA","MAPA","MAPA","KAART","HARTĂ","КАРТА"),Color.rgb(66,78,96),false);
    if(flash>0)fill(c,Color.argb(80,255,226,130),31,105,669,505);
  }
  private void unit(Canvas c,float x,float y,int type,boolean enemy,int count,boolean selected){if(selected){fill(c,Color.argb(80,255,220,70),x-30,y-55,x+30,y+28);stroke(c,Color.rgb(255,221,72),2,x-30,y-55,x+30,y+28);}SpriteKit.worker(c,p,x,y,0.78f,enemy?-1:1,type==2?3:0,animClock,enemy);text(c,"x"+count,x,y+18,12,Color.WHITE,Paint.Align.CENTER,true);}
  private void bar(Canvas c,float x,float y,int v,int max,int col){fill(c,Color.rgb(29,29,32),x,y,x+250,y+17);fill(c,col,x+2,y+2,x+2+246*Math.max(0,v)/(float)max,y+15);stroke(c,Color.rgb(235,226,193),1,x,y,x+250,y+17);}
  private void combatAction(int kind){if(combatAnim>0)return;if(kind==0){int dmg=22+weld*5+selectedArmy*6;enemyHp-=dmg;score+=dmg*3;combatAnim=.35f;flash=.08f;fx.sparks(470,250,13,Color.rgb(255,195,70));if(enemyHp>0)ownHp-=15+turn/2;}else if(kind==1){ownHp=Math.min(190,ownHp+10);enemyHp-=8;}else{if(tools>0){tools--;enemyHp-=42+weld*4;combatAnim=.45f;fx.sparks(500,245,24,Color.rgb(97,201,255));}else return;}turn++;selectedArmy=(selectedArmy+1)%3;enemyType=(enemyType+1)%3;if(enemyHp<=0){gold+=190+contracts*20;score+=650;blueprints+=(turn%3==0?1:0);enemyHp=175+turn*6;ownHp=190;owner.reportScore(score);}if(ownHp<=0){ownHp=190;enemyHp=175;score=Math.max(0,score-120);}owner.reportScore(score);}

  @Override void onGameDown(float x,float y){
    if(page==0){if(in(up,x,y))move(0,-1);else if(in(down,x,y))move(0,1);else if(in(left,x,y))move(-1,0);else if(in(right,x,y))move(1,0);else if(in(townBtn,x,y))page=1;else if(in(combatBtn,x,y))page=2;}
    else if(page==1){if(in(build0,x,y))upgrade(0);else if(in(build1,x,y))upgrade(1);else if(in(build2,x,y))upgrade(2);else if(in(mapBtn,x,y))page=0;}
    else {if(in(attackBtn,x,y))combatAction(0);else if(in(defendBtn,x,y))combatAction(1);else if(in(skillBtn,x,y))combatAction(2);else if(in(mapBtn,x,y))page=0;}
    invalidate();
  }
  @Override void onGameMove(float x,float y){}
  @Override void onGameUp(float x,float y){}
}
