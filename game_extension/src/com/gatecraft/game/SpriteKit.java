package com.gatecraft.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

final class SpriteKit {
  private SpriteKit() {}

  static void worker(Canvas c, Paint p, float x, float groundY, float scale, int facing, int state, float phase, boolean enemy) {
    float s=scale, bob=(state==1?(float)Math.sin(phase*12f)*2f:0f)*s;
    int skin=enemy?Color.rgb(195,142,98):Color.rgb(224,174,113);
    int shirt=enemy?Color.rgb(126,55,42):Color.rgb(43,104,157);
    int dark=enemy?Color.rgb(72,46,40):Color.rgb(40,49,61);
    int helmet=enemy?Color.rgb(121,40,34):Color.rgb(234,181,47);
    float x0=x-18*s, y0=groundY-58*s+bob;
    rect(c,p,Color.argb(70,0,0,0),x-22*s,groundY-4*s,x+22*s,groundY+3*s);
    // legs and boots
    float stride=(state==1?(float)Math.sin(phase*12f)*5f:0f)*s;
    rect(c,p,dark,x0+6*s,y0+37*s,x0+15*s+stride*.35f,y0+55*s);
    rect(c,p,dark,x0+21*s,y0+37*s,x0+30*s-stride*.35f,y0+55*s);
    rect(c,p,Color.rgb(20,22,27),x0+3*s+stride*.2f,y0+53*s,x0+16*s+stride*.2f,y0+59*s);
    rect(c,p,Color.rgb(20,22,27),x0+19*s-stride*.2f,y0+53*s,x0+33*s-stride*.2f,y0+59*s);
    // torso / belt
    rect(c,p,shirt,x0+4*s,y0+18*s,x0+32*s,y0+40*s);
    rect(c,p,Color.rgb(56,46,35),x0+4*s,y0+34*s,x0+32*s,y0+39*s);
    rect(c,p,Color.rgb(202,154,52),x0+16*s,y0+34*s,x0+21*s,y0+39*s);
    // head / hardhat
    rect(c,p,skin,x0+10*s,y0+5*s,x0+27*s,y0+20*s);
    rect(c,p,helmet,x0+7*s,y0+1*s,x0+30*s,y0+7*s);
    rect(c,p,helmet,x0+11*s,y0-2*s,x0+26*s,y0+3*s);
    rect(c,p,Color.rgb(62,43,30),x0+12*s,y0+15*s,x0+25*s,y0+18*s);
    // arms + tool poses
    if(state==2){ // attack
      float dir=facing>=0?1:-1;
      rect(c,p,skin,x+dir*10*s,y0+21*s,x+dir*29*s,y0+28*s);
      line(c,p,Color.rgb(204,208,211),4*s,x+dir*27*s,y0+24*s,x+dir*48*s,y0+12*s);
      rect(c,p,Color.rgb(69,54,41),x+dir*46*s,y0+7*s,x+dir*50*s,y0+17*s);
      rect(c,p,skin,x-dir*15*s,y0+21*s,x-dir*5*s,y0+34*s);
    } else if(state==3){ // block
      rect(c,p,skin,x-15*s,y0+22*s,x-5*s,y0+35*s);
      rect(c,p,skin,x+5*s,y0+22*s,x+15*s,y0+35*s);
      rect(c,p,Color.rgb(67,74,82),x-22*s,y0+15*s,x-5*s,y0+43*s);
      stroke(c,p,Color.rgb(212,212,205),2*s,x-22*s,y0+15*s,x-5*s,y0+43*s);
    } else {
      float arm=(state==1?(float)Math.sin(phase*12f)*4f:0f)*s;
      rect(c,p,skin,x0-2*s,y0+20*s+arm,x0+6*s,y0+35*s+arm);
      rect(c,p,skin,x0+31*s,y0+20*s-arm,x0+39*s,y0+35*s-arm);
      line(c,p,Color.rgb(205,208,210),3*s,x0+37*s,y0+32*s-arm,x0+48*s,y0+37*s-arm);
    }
    // face pixel
    rect(c,p,Color.rgb(37,32,30), facing>=0?x0+22*s:x0+12*s, y0+11*s, facing>=0?x0+24*s:x0+14*s, y0+13*s);
  }

  static void icon(Canvas c, Paint p, int kind, float cx, float cy, float s) {
    if(kind==0){ // steel bundle
      for(int i=0;i<3;i++){rect(c,p,Color.rgb(113,122,132),cx-13*s,cy+(-8+i*7)*s,cx+13*s,cy+(-4+i*7)*s);rect(c,p,Color.rgb(205,211,216),cx-11*s,cy+(-7+i*7)*s,cx+8*s,cy+(-6+i*7)*s);}rect(c,p,Color.rgb(148,80,42),cx-2*s,cy-11*s,cx+2*s,cy+12*s);
    } else if(kind==1){ // cement sack
      rect(c,p,Color.rgb(211,201,168),cx-12*s,cy-13*s,cx+12*s,cy+13*s);rect(c,p,Color.rgb(86,72,52),cx-8*s,cy-3*s,cx+8*s,cy+4*s);rect(c,p,Color.rgb(237,229,204),cx-8*s,cy-10*s,cx+7*s,cy-7*s);
    } else if(kind==2){ // tool box
      rect(c,p,Color.rgb(119,61,34),cx-14*s,cy-9*s,cx+14*s,cy+11*s);rect(c,p,Color.rgb(198,143,48),cx-14*s,cy-8*s,cx+14*s,cy-3*s);stroke(c,p,Color.rgb(54,40,31),2*s,cx-14*s,cy-9*s,cx+14*s,cy+11*s);rect(c,p,Color.rgb(55,55,54),cx-6*s,cy-14*s,cx+6*s,cy-9*s);
    } else if(kind==3){ // chest
      rect(c,p,Color.rgb(91,57,31),cx-14*s,cy-10*s,cx+14*s,cy+12*s);rect(c,p,Color.rgb(164,102,34),cx-13*s,cy-9*s,cx+13*s,cy-1*s);rect(c,p,Color.rgb(221,177,52),cx-3*s,cy-3*s,cx+3*s,cy+6*s);
    } else { // coffee/energy pickup
      rect(c,p,Color.rgb(205,60,40),cx-8*s,cy-14*s,cx+8*s,cy+12*s);rect(c,p,Color.rgb(248,232,184),cx-6*s,cy-8*s,cx+6*s,cy+5*s);rect(c,p,Color.rgb(95,52,31),cx-4*s,cy-4*s,cx+4*s,cy+3*s);
    }
  }

  static void rect(Canvas c,Paint p,int color,float l,float t,float r,float b){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRect(l,t,r,b,p);}  
  static void line(Canvas c,Paint p,int color,float w,float x1,float y1,float x2,float y2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);p.setColor(color);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}  
  static void stroke(Canvas c,Paint p,int color,float w,float l,float t,float r,float b){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(w);p.setColor(color);c.drawRect(l,t,r,b,p);p.setStyle(Paint.Style.FILL);}  
}
