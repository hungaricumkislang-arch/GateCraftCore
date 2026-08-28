package com.gatecraft.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class ArcadeFx {
  static final class Particle {
    float x,y,vx,vy,life,maxLife,size; int color,kind;
    Particle(float x,float y,float vx,float vy,float life,float size,int color,int kind){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.life=this.maxLife=life;this.size=size;this.color=color;this.kind=kind;}
  }
  private final List<Particle> ps=new ArrayList<Particle>();
  private final Random rnd=new Random(1419L);
  void sparks(float x,float y,int n,int color){for(int i=0;i<n;i++){float a=(float)(rnd.nextDouble()*Math.PI*2),sp=70+rnd.nextFloat()*220;ps.add(new Particle(x,y,(float)Math.cos(a)*sp,(float)Math.sin(a)*sp,0.22f+rnd.nextFloat()*0.35f,2+rnd.nextFloat()*3,color,0));}}
  void dust(float x,float y,int n,int color){for(int i=0;i<n;i++){ps.add(new Particle(x+(rnd.nextFloat()-.5f)*20,y,-35+rnd.nextFloat()*70,-20-rnd.nextFloat()*80,.35f+rnd.nextFloat()*.45f,3+rnd.nextFloat()*5,color,1));}}
  void update(float dt){Iterator<Particle>it=ps.iterator();while(it.hasNext()){Particle q=it.next();q.life-=dt;if(q.life<=0){it.remove();continue;}q.x+=q.vx*dt;q.y+=q.vy*dt;if(q.kind==0)q.vy+=420*dt;else{q.vx*=.96f;q.vy*=.96f;}}}
  void draw(Canvas c,Paint p){for(Particle q:ps){float a=Math.max(0,q.life/q.maxLife);p.setStyle(Paint.Style.FILL);p.setColor(Color.argb((int)(255*a),Color.red(q.color),Color.green(q.color),Color.blue(q.color)));if(q.kind==0)c.drawRect(q.x-q.size,q.y-q.size*.5f,q.x+q.size*2,q.y+q.size*.5f,p);else c.drawCircle(q.x,q.y,q.size*(1.15f-a*.15f),p);}}
  void clear(){ps.clear();}

  static void beveledPanel(Canvas c,Paint p,float l,float t,float r,float b,int fill,int light,int dark){p.setStyle(Paint.Style.FILL);p.setColor(fill);c.drawRect(l,t,r,b,p);p.setColor(light);c.drawRect(l,t,r,t+3,p);c.drawRect(l,t,l+3,b,p);p.setColor(dark);c.drawRect(l,b-3,r,b,p);c.drawRect(r-3,t,r,b,p);}
  static void isoBox(Canvas c,Paint p,float cx,float cy,float w,float h,float d,int top,int left,int right){Path q=new Path();q.moveTo(cx,cy-h/2);q.lineTo(cx+w/2,cy);q.lineTo(cx,cy+h/2);q.lineTo(cx-w/2,cy);q.close();p.setColor(top);p.setStyle(Paint.Style.FILL);c.drawPath(q,p);Path a=new Path();a.moveTo(cx-w/2,cy);a.lineTo(cx,cy+h/2);a.lineTo(cx,cy+h/2+d);a.lineTo(cx-w/2,cy+d);a.close();p.setColor(left);c.drawPath(a,p);Path b=new Path();b.moveTo(cx+w/2,cy);b.lineTo(cx,cy+h/2);b.lineTo(cx,cy+h/2+d);b.lineTo(cx+w/2,cy+d);b.close();p.setColor(right);c.drawPath(b,p);}
  static int mix(int a,int b,float t){return Color.rgb((int)(Color.red(a)+(Color.red(b)-Color.red(a))*t),(int)(Color.green(a)+(Color.green(b)-Color.green(a))*t),(int)(Color.blue(a)+(Color.blue(b)-Color.blue(a))*t));}
}
