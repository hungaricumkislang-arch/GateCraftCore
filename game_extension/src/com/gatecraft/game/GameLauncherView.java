package com.gatecraft.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

final class GameLauncherView extends View {
  private static final float LW=960f,LH=540f;
  private final GateCraftGame owner;
  private final Paint p=new Paint();
  private float scale=1,ox,oy;
  private final RectF[] cards={new RectF(42,126,918,226),new RectF(42,244,918,344),new RectF(42,362,918,462)};
  private final RectF exit=new RectF(806,18,940,66);
  GameLauncherView(Context c,GateCraftGame owner){super(c);this.owner=owner;p.setAntiAlias(false);setBackgroundColor(Color.BLACK);}
  private String t(String hu,String en,String de,String es,String fr,String zh,String it,String pt,String pl,String nl,String ro,String ru){String[]a={hu,en,de,es,fr,zh,it,pt,pl,nl,ro,ru};return a[Math.max(0,Math.min(11,owner.getLanguage()-1))];}
  private void fill(Canvas c,int color,float l,float t,float r,float b){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRect(l,t,r,b,p);} 
  private void text(Canvas c,String s,float x,float y,float size,int col,Paint.Align align,boolean bold){p.setColor(col);p.setTextSize(size);p.setTextAlign(align);p.setTypeface(bold?android.graphics.Typeface.DEFAULT_BOLD:android.graphics.Typeface.MONOSPACE);c.drawText(s,x,y,p);} 
  @Override protected void onDraw(Canvas c){super.onDraw(c);scale=Math.min(getWidth()/LW,getHeight()/LH);ox=(getWidth()-LW*scale)/2;oy=(getHeight()-LH*scale)/2;c.save();c.translate(ox,oy);c.scale(scale,scale);
    fill(c,Color.rgb(8,12,18),0,0,LW,LH);for(int y=0;y<540;y+=32)fill(c,(y/32)%2==0?Color.rgb(13,20,27):Color.rgb(11,17,24),0,y,LW,y+16);
    for(int x=0;x<960;x+=64){fill(c,Color.rgb(26,31,35),x,82,x+3,512);fill(c,Color.rgb(5,8,12),x+3,82,x+5,512);} 
    text(c,"GATECRAFT ARCADE",42,58,32,Color.rgb(244,209,77),Paint.Align.LEFT,true);text(c,t("Különálló retro minijátékok","Standalone retro mini-games","Separate Retro-Minispiele","Minijuegos retro separados","Mini-jeux rétro séparés","独立复古小游戏","Mini-giochi rétro separati","Minijogos retro separados","Oddzielne gry retro","Losse retro-minigames","Mini-jocuri retro separate","Отдельные ретро-игры"),42,88,15,Color.LTGRAY,Paint.Align.LEFT,false);
    String[] names={"WORKSHOP RUN","HEROES OF CRAFT & GATES","METAL FIGHTER"};String[] subs={t("90-es évekbeli platform arcade","90s platform arcade","90er Plattform-Arcade","Arcade de plataformas 90s","Arcade plateforme années 90","90年代平台街机","Platform arcade anni 90","Arcade plataforma anos 90","Platformówka arcade lat 90","90s platform-arcade","Arcade platformă anii 90","Платформер-аркада 90-х"),t("Térkép • műhely • taktikai harc","Map • workshop • tactical combat","Karte • Werkstatt • taktischer Kampf","Mapa • taller • combate táctico","Carte • atelier • combat tactique","地图 • 工坊 • 战术战斗","Mappa • officina • combattimento tattico","Mapa • oficina • combate tático","Mapa • warsztat • walka taktyczna","Kaart • werkplaats • tactisch gevecht","Hartă • atelier • luptă tactică","Карта • мастерская • тактический бой"),t("1v1 építkezési arcade fighter","1v1 construction-site arcade fighter","1v1 Baustellen-Arcade-Fighter","Lucha arcade 1v1 en obra","Combat arcade 1v1 de chantier","1v1工地街机格斗","Fighter arcade 1v1 in cantiere","Luta arcade 1v1 em obra","Arcade 1v1 na budowie","1v1 bouwplaats-fighter","Fighter arcade 1v1 pe șantier","Аркадный файтинг 1v1 на стройке")};
    int[] cols={Color.rgb(35,126,92),Color.rgb(141,93,35),Color.rgb(156,45,45)};
    for(int i=0;i<3;i++){RectF r=cards[i];boolean u=owner.modeUnlocked(i+1);fill(c,Color.rgb(4,6,9),r.left-5,r.top-5,r.right+5,r.bottom+5);fill(c,u?cols[i]:Color.rgb(50,54,58),r.left,r.top,r.right,r.bottom);fill(c,Color.argb(45,255,255,255),r.left+7,r.top+7,r.right-7,r.top+15);text(c,names[i],r.left+24,r.top+41,25,Color.WHITE,Paint.Align.LEFT,true);text(c,subs[i],r.left+24,r.top+71,15,Color.rgb(230,230,220),Paint.Align.LEFT,false);if(u)text(c,"▶",r.right-35,r.centerY()+10,34,Color.WHITE,Paint.Align.CENTER,true);else text(c,t("ZÁR","LOCK","SPERRE","BLOQ","VERROU","锁定","BLOCCO","BLOQ","BLOK","SLOT","BLOC","ЗАМОК")+" "+owner.requiredFor(i+1),r.right-28,r.centerY()+7,17,Color.rgb(255,206,94),Paint.Align.RIGHT,true);}
    fill(c,Color.rgb(120,37,37),exit.left,exit.top,exit.right,exit.bottom);text(c,t("KILÉP","EXIT","ENDE","SALIR","QUITTER","退出","ESCI","SAIR","WYJDŹ","AFSLUITEN","IEȘIRE","ВЫХОД"),exit.centerX(),exit.centerY()+7,17,Color.WHITE,Paint.Align.CENTER,true);
    text(c,t("50 számolás: GRIND & WELD külön végső bónusz","50 calculations: GRIND & WELD remains a separate final bonus","50 Berechnungen: GRIND & WELD bleibt separater Endbonus","50 cálculos: GRIND & WELD sigue como bonus final separado","50 calculs : GRIND & WELD reste un bonus final séparé","50次计算：GRIND & WELD为独立最终奖励","50 calcoli: GRIND & WELD resta bonus finale separato","50 cálculos: GRIND & WELD fica como bônus final separado","50 obliczeń: GRIND & WELD pozostaje osobnym finałowym bonusem","50 berekeningen: GRIND & WELD blijft aparte eindbonus","50 calcule: GRIND & WELD rămâne bonus final separat","50 расчётов: GRIND & WELD остаётся отдельным финальным бонусом"),42,505,14,Color.rgb(174,180,188),Paint.Align.LEFT,false);
    c.restore();}
  @Override public boolean onTouchEvent(MotionEvent e){if(e.getActionMasked()!=MotionEvent.ACTION_DOWN)return true;float x=(e.getX()-ox)/scale,y=(e.getY()-oy)/scale;if(exit.contains(x,y)){owner.requestExitFromView();return true;}for(int i=0;i<3;i++)if(cards[i].contains(x,y)&&owner.modeUnlocked(i+1)){owner.launchMode(i+1);return true;}return true;}
}
