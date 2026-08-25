package com.gatecraft.drawing;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.Form;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Locale;

@DesignerComponent(
    version = 2,
    description = "GateCraft deterministic technical drawing PDF generator and share helper.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true)
@SimpleObject(external = true)
public class GateCraftDrawing extends AndroidNonvisibleComponent {
  private static final int PW = 842;
  private static final int PH = 595;
  private final Context context;
  private final Form form;
  private Uri lastUri;
  private File lastTemp;
  private String lastError = "";

  public GateCraftDrawing(ComponentContainer container) {
    super(container.$form());
    form = container.$form();
    context = container.$context();
  }

  @SimpleFunction(description = "Returns the GateCraftDrawing extension version.")
  public String Version() { return "2.0.0"; }

  @SimpleFunction(description = "Creates an A4 landscape vector PDF from a GateCraft drawing payload, saves it to Downloads/GateCraft on Android 10+, and opens the Android share sheet. Returns true on success.")
  public boolean CreateAndShareDrawingPDF(String payload, String chooserTitle) {
    lastError = "";
    try {
      File dir = new File(context.getCacheDir(), "gatecraft_draw_pdf");
      if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("PDF cache directory could not be created");
      String type = field(payload, 0, "DRAWING");
      String project = tag(payload, "PROJECT", "");
      String base = "GateCraft_" + (project.length() > 0 ? safe(project) + "_" : "") + safe(type) + "_Drawing.pdf";
      File tmp = new File(dir, base);
      render(payload, tmp);
      lastTemp = tmp;
      lastUri = publish(tmp);
      Intent send = new Intent(Intent.ACTION_SEND);
      send.setType("application/pdf");
      send.putExtra(Intent.EXTRA_STREAM, lastUri);
      send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      Intent chooser = Intent.createChooser(send, chooserTitle == null ? "" : chooserTitle);
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      form.startActivity(chooser);
      return true;
    } catch (Throwable ex) {
      lastError = ex.getClass().getSimpleName() + ": " + (ex.getMessage() == null ? "" : ex.getMessage());
      return false;
    }
  }

  @SimpleFunction(description = "Returns the last GateCraftDrawing error message.")
  public String LastError() { return lastError; }

  @SimpleFunction(description = "Returns the URI of the last drawing PDF.")
  public String LastPDFUri() { return lastUri == null ? "" : lastUri.toString(); }

  private Uri publish(File src) throws Exception {
    if (Build.VERSION.SDK_INT >= 29) {
      ContentResolver resolver = context.getContentResolver();
      ContentValues cv = new ContentValues();
      cv.put(MediaStore.MediaColumns.DISPLAY_NAME, src.getName());
      cv.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
      cv.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GateCraft");
      cv.put(MediaStore.MediaColumns.IS_PENDING, 1);
      Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
      if (uri == null) throw new IllegalStateException("MediaStore destination unavailable");
      OutputStream out = resolver.openOutputStream(uri, "w");
      if (out == null) throw new IllegalStateException("PDF output stream unavailable");
      FileInputStream in = new FileInputStream(src);
      byte[] buf = new byte[16384];
      int n;
      try { while ((n = in.read(buf)) > 0) out.write(buf, 0, n); }
      finally { try { in.close(); } catch (Throwable ignored) {} try { out.close(); } catch (Throwable ignored) {} }
      ContentValues done = new ContentValues();
      done.put(MediaStore.MediaColumns.IS_PENDING, 0);
      resolver.update(uri, done, null, null);
      return uri;
    }
    return com.google.appinventor.components.runtime.util.NougatUtil.getPackageUri(form, src);
  }

  private void render(String payload, File out) throws Exception {
    String[] a = payload == null ? new String[0] : payload.split("\\|", -1);
    String type = a.length > 0 ? a[0].toUpperCase(Locale.US) : "";
    int lang = lang(payload);
    int accent = parseColor(tag(payload, "COLOR", "#6B7280"));
    String colorName = tag(payload, "COLORNAME", "");
    String project = tag(payload, "PROJECT", "");
    PdfDocument pdf = new PdfDocument();
    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PW, PH, 1).create();
    PdfDocument.Page page = pdf.startPage(info);
    Canvas c = page.getCanvas();
    c.drawColor(Color.WHITE);
    Paint thin = stroke(1.4f, Color.BLACK);
    Paint frame = stroke(7.0f, 0xFF202020);
    Paint rail = stroke(7.0f, 0xFF555555);
    Paint fill = fill(accent);
    Paint txt = text(11.0f, false, Color.BLACK);
    Paint small = text(8.5f, false, 0xFF333333);
    Paint bold = text(15.0f, true, Color.BLACK);
    c.drawText("GateCraft – " + title(type, lang), 42, 42, bold);
    if (project.length() > 0) c.drawText(tr("PROJECT", lang) + ": " + project, 42, 60, small);
    else c.drawText(tr("VECTOR", lang), 42, 60, small);
    if (colorName.length() > 0) {
      c.drawRect(PW - 174, 34, PW - 154, 54, fill);
      c.drawText(colorName, PW - 147, 50, small);
    }
    if ("USZO".equals(type)) drawUszo(c,a,payload,lang,thin,frame,rail,fill,txt,small);
    else if ("TOLO".equals(type)) drawTolo(c,a,payload,lang,thin,frame,rail,fill,txt,small);
    else if ("SZARNYAS".equals(type)) drawSzarnyas(c,a,payload,lang,thin,frame,fill,txt,small);
    else if ("KISKAPU".equals(type)) drawKiskapu(c,a,payload,lang,thin,frame,fill,txt,small);
    else if ("KERITES".equals(type)) drawKerites(c,a,lang,thin,frame,fill,small);
    else if ("PANEL3D".equals(type)) drawPanel3D(c,a,lang,thin,frame,fill,small);
    else if ("TRAPEZ".equals(type)) drawTrapez(c,a,lang,thin,frame,fill,small);
    else if ("ZSALU".equals(type)) drawZsalu(c,a,lang,thin,frame,fill,small);
    else if ("TERKO".equals(type)) drawTerko(c,a,lang,thin,frame,fill,small);
    else if ("LEPCSO".equals(type)) drawLepcso(c,a,lang,thin,frame,fill,small);
    else throw new IllegalArgumentException("Unsupported drawing payload: " + type);
    c.drawText("GateCraft • " + tr("NOTE",lang), 42, PH-24, small);
    pdf.finishPage(page);
    FileOutputStream fos = new FileOutputStream(out);
    try { pdf.writeTo(fos); } finally { try { fos.close(); } catch(Throwable ignored){} pdf.close(); }
  }

  private void drawKiskapu(Canvas c,String[] a,String payload,int l,Paint thin,Paint frame,Paint fill,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), leave=d(a,3), hinge=d(a,4), lock=d(a,5), recv=d(a,6), F=Math.max(1,d(a,7));
    double fw=W-(hinge+lock+recv), fh=H-2*leave, stiff=fw-2*F;
    float sc=(float)Math.min(560.0/Math.max(fw,1),300.0/Math.max(fh,1));
    float x=145,y=430,w=(float)(fw*sc),h=(float)(fh*sc), pf=(float)(F*sc);
    gateRect(c,x,y-h,x+w,y,frame,fill,3f);
    float yy=y-h/2;
    c.drawLine(x+pf,yy,x+w-pf,yy,frame);
    hinge(c,x-4,y-h+Math.max(22,h*.20f),8,thin); hinge(c,x-4,y-Math.max(22,h*.20f),8,thin);
    if (boolTag(payload,"OSZLOP")) { post(c,x-32,y-h-12,22,h+24,frame,fill); post(c,x+w+10,y-h-12,22,h+24,frame,fill); }
    dimH(c,x,x+w,y+45,mm(tr("FRAME_W",l),fw),thin,small);
    dimV(c,x-48,y-h,y,mm(tr("FRAME_H",l),fh),thin,small);
    c.drawText(mm(tr("STIFF",l),stiff),x+10,y-h-18,small);
    c.drawText(mm(tr("OPENING",l),W),x+10,y+76,small);
  }

  private void drawSzarnyas(Canvas c,String[] a,String payload,int l,Paint thin,Paint frame,Paint fill,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), leave=d(a,3), hingeGap=d(a,4), lockGap=d(a,5), F=Math.max(1,d(a,6));
    double fh=Math.ceil(H-2*leave), leaf=Math.ceil((W-(2*hingeGap+lockGap))/2.0), stiff=Math.ceil(leaf-2*F);
    float visualGap=24f;
    float sc=(float)Math.min((560.0-visualGap)/Math.max(leaf*2,1),300.0/Math.max(fh,1));
    float x=140,y=430,h=(float)(fh*sc),lw=(float)(leaf*sc),gap=Math.max(visualGap,(float)(lockGap*sc)),pf=(float)(F*sc);
    gateRect(c,x,y-h,x+lw,y,frame,fill,3f); gateRect(c,x+lw+gap,y-h,x+2*lw+gap,y,frame,fill,3f);
    float yy=y-h/2;
    c.drawLine(x+pf,yy,x+lw-pf,yy,frame); c.drawLine(x+lw+gap+pf,yy,x+2*lw+gap-pf,yy,frame);
    hinge(c,x-4,y-h+Math.max(22,h*.20f),8,thin); hinge(c,x-4,y-Math.max(22,h*.20f),8,thin);
    hinge(c,x+2*lw+gap+4,y-h+Math.max(22,h*.20f),8,thin); hinge(c,x+2*lw+gap+4,y-Math.max(22,h*.20f),8,thin);
    if (boolTag(payload,"OSZLOP")) { post(c,x-34,y-h-12,22,h+24,frame,fill); post(c,x+2*lw+gap+12,y-h-12,22,h+24,frame,fill); }
    dimH(c,x,x+2*lw+gap,y+45,mm(tr("OPENING",l),W),thin,small);
    dimV(c,x-48,y-h,y,mm(tr("FRAME_H",l),fh),thin,small);
    c.drawText(mm(tr("LEAF",l),leaf),x+10,y-h-18,small); c.drawText(mm(tr("STIFF_EACH",l),stiff),x+250,y-h-18,small);
    c.drawText(mm(tr("CENTER_GAP",l),lockGap),(x+lw+gap/2)-25,y+75,small);
  }

  private void drawTolo(Canvas c,String[] a,String payload,int l,Paint thin,Paint frame,Paint rail,Paint fill,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), over=d(a,3), leave=d(a,4), railH=d(a,5), roller=d(a,6), F=Math.max(1,d(a,7));
    double fw=W+over, fh=H-(leave+roller+railH), v=Math.ceil(fh-2*F), seg=Math.ceil((fw-4*F)/3.0);
    float sc=(float)Math.min(590.0/Math.max(fw,1),280.0/Math.max(fh,1));
    float x=125,y=420,w=(float)(fw*sc),h=(float)(fh*sc),pf=(float)(F*sc);
    gateRect(c,x,y-h,x+w,y,frame,fill,3f);
    double vx1=F+seg+F/2.0, vx2=2*F+2*seg+F/2.0;
    float X1=x+(float)(vx1*sc), X2=x+(float)(vx2*sc);
    c.drawLine(X1,y-h+pf,X1,y-pf,frame); c.drawLine(X2,y-h+pf,X2,y-pf,frame);
    float yy=y-h/2;
    c.drawLine(x+pf,yy,X1-pf/2,yy,frame); c.drawLine(X1+pf/2,yy,X2-pf/2,yy,frame); c.drawLine(X2+pf/2,yy,x+w-pf,yy,frame);
    c.drawLine(x-18,y+17,x+w+18,y+17,rail);
    roller(c,x+w*.27f,y+15,10,thin); roller(c,x+w*.72f,y+15,10,thin);
    if (boolTag(payload,"OSZLOP")) {
      post(c,x+w+14,y-h-16,24,h+32,frame,fill);
      if (over>0) post(c,x-42,y-h-16,24,h+32,frame,fill);
    }
    dimH(c,x,x+w,y+58,mm(tr("FRAME_W",l),fw),thin,small); dimV(c,x-48,y-h,y,mm(tr("FRAME_H",l),fh),thin,small);
    c.drawText("2× "+mm(tr("V_STIFF",l),v)+"   3× "+mm(tr("H_STIFF",l),seg),x+10,y-h-18,small);
  }

  private void drawUszo(Canvas c,String[] a,String payload,int l,Paint thin,Paint frame,Paint rail,Paint fill,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), P=d(a,3), leave=d(a,4), railH=d(a,5), gap=d(a,6), F=Math.max(1,d(a,7));
    double top=W+P, bottom=W*1.5, front=H-(leave+gap+railH), rear=Math.max(F,front-F), v=H-(leave+gap+railH+2*F), seg=Math.ceil((top-4*F)/3.0), over=bottom-W;
    float sc=(float)Math.min(590.0/Math.max(bottom,1),275.0/Math.max(front,1));
    float x=125,y=420,xt=x+(float)(top*sc),xb=x+(float)(bottom*sc),xo=x+(float)(W*sc),ytop=y-(float)(front*sc),pf=(float)(F*sc);
    c.drawPath(path(new float[]{x,ytop, xt,ytop, xt,y-(float)((front-rear)*sc), xb,y, x,y}),frame);
    c.drawLine(x,y,xb,y,frame);
    // Rear vertical is a real calculated member, drawn at the end of the rectangular top section.
    float yRearTop=y-(float)(rear*sc);
    c.drawLine(xt,yRearTop,xt,y,frame);
    double vx1=F+seg+F/2.0, vx2=2*F+2*seg+F/2.0;
    float X1=x+(float)(vx1*sc), X2=x+(float)(vx2*sc);
    c.drawLine(X1,ytop+pf,X1,y-pf,frame); c.drawLine(X2,ytop+pf,X2,y-pf,frame);
    float yy=(ytop+y)/2;
    c.drawLine(x+pf,yy,X1-pf/2,yy,frame); c.drawLine(X1+pf/2,yy,X2-pf/2,yy,frame); c.drawLine(X2+pf/2,yy,xt-pf,yy,frame);
    c.drawLine(x,y+13,xb,y+13,rail);
    // Cantilever roller carriages: schematic only, no fabricated detail.
    rollerCarriage(c,x+(float)(over*sc*.28f),y+18,thin); rollerCarriage(c,x+(float)(over*sc*.75f),y+18,thin);
    if (boolTag(payload,"OSZLOP")) {
      post(c,xt+14,y-(float)(rear*sc)-16,24,(float)(rear*sc)+32,frame,fill);
      if (over>0) post(c,x-42,y-(float)(front*sc)-16,24,(float)(front*sc)+32,frame,fill);
    }
    Paint dash=stroke(1.0f,0xFF777777); c.drawLine(xo,ytop-25,xo,y+65,dash);
    dimH(c,x,xo,y+58,mm(tr("OPENING",l),W),thin,small); dimH(c,x,xb,y+92,mm(tr("BOTTOM",l),bottom),thin,small); dimH(c,xo,xb,y+126,mm(tr("OVER",l),over),thin,small); dimV(c,x-48,ytop,y,mm(tr("FRONT",l),front),thin,small);
    c.drawText("2× "+mm(tr("V_STIFF",l),v)+"   3× "+mm(tr("H_STIFF",l),seg),x+10,ytop-18,small); c.drawText(mm(tr("REAR",l),rear),xt-90,yRearTop-14,small);
  }

  private void drawKerites(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint fill,Paint small) {
    double len=d(a,1), slat=d(a,2), gap=d(a,3), h=d(a,4), dir=d(a,5), slatLen=d(a,6);
    if (h<=0) h=slatLen>0?slatLen:1800;
    float sc=(float)Math.min(650.0/Math.max(len,1),310.0/Math.max(h,1)); float x=95,y=430,w=(float)(len*sc),hh=(float)(h*sc);
    c.drawRect(x,y-hh,x+w,y,fill); c.drawRect(x,y-hh,x+w,y,thin);
    if ((int)Math.round(dir)==2) {
      double step=Math.max(20,slat+gap); for(double yy=0;yy<h;yy+=step){float Y=y-(float)(yy*sc); c.drawLine(x,Y,x+w,Y,frame);}
    } else {
      double step=Math.max(20,slat+gap); for(double xx=0;xx<len;xx+=step){float X=x+(float)(xx*sc); c.drawLine(X,y-hh,X,y,frame);}
    }
    dimH(c,x,x+w,y+48,mm(tr("LENGTH",l),len),thin,small); dimV(c,x-45,y-hh,y,mm(tr("HEIGHT",l),h),thin,small);
    c.drawText(mm(tr("SLAT",l),slat)+"  "+mm(tr("GAP",l),gap),x+8,y-hh-18,small);
  }

  private void drawPanel3D(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint fill,Paint small) {
    double len=d(a,1), h=d(a,2), pw=d(a,3); if (len<20) len*=1000; if(h<20)h*=1000; if(pw<20)pw*=1000;
    float sc=(float)Math.min(650.0/Math.max(len,1),310.0/Math.max(h,1)); float x=95,y=430,w=(float)(len*sc),hh=(float)(h*sc);
    c.drawRect(x,y-hh,x+w,y,fill); c.drawRect(x,y-hh,x+w,y,thin);
    double panel=Math.max(500,pw); for(double xx=0;xx<=len;xx+=panel){float X=x+(float)(xx*sc);c.drawLine(X,y-hh,X,y,frame);} for(int i=1;i<8;i++){float Y=y-hh+i*hh/8;c.drawLine(x,Y,x+w,Y,thin);}
    dimH(c,x,x+w,y+48,mm(tr("LENGTH",l),len),thin,small); dimV(c,x-45,y-hh,y,mm(tr("HEIGHT",l),h),thin,small); c.drawText(mm(tr("PANEL",l),panel),x+8,y-hh-18,small);
  }

  private void drawTrapez(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint fill,Paint small) {
    double len=d(a,1), sheet=d(a,2), overlap=d(a,3), h=d(a,4); if(len<20)len*=1000;if(sheet<20)sheet*=1000;if(overlap<5)overlap*=1000;if(h<20)h*=1000;
    float sc=(float)Math.min(650.0/Math.max(len,1),310.0/Math.max(h,1)); float x=95,y=430,w=(float)(len*sc),hh=(float)(h*sc);
    c.drawRect(x,y-hh,x+w,y,fill); c.drawRect(x,y-hh,x+w,y,thin);
    double eff=Math.max(100,sheet-overlap); for(double xx=0;xx<len;xx+=eff){float X=x+(float)(xx*sc);c.drawLine(X,y-hh,X,y,frame);} for(int i=0;i<36;i++){float X=x+i*w/36f;c.drawLine(X,y-hh,X+Math.max(2,w/100f),y,thin);}
    dimH(c,x,x+w,y+48,mm(tr("LENGTH",l),len),thin,small); dimV(c,x-45,y-hh,y,mm(tr("HEIGHT",l),h),thin,small); c.drawText(mm(tr("SHEET",l),sheet)+"  "+mm(tr("OVERLAP",l),overlap),x+8,y-hh-18,small);
  }

  private void drawZsalu(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint fill,Paint small) {
    int mode=(int)Math.round(d(a,1)), form=(int)Math.round(d(a,2));
    double L=4000,H=1200,W=250;
    if(mode==1){ if(form==2){L=d(a,5)+d(a,6);H=d(a,7);} else if(form==3){L=d(a,8)+d(a,9)+d(a,10);H=d(a,11);} else if(form==4){L=2*(d(a,12)+d(a,13));H=d(a,14);} else {L=d(a,3);H=d(a,4);} }
    else if(mode==2){L=Math.max(300,600*d(a,16));H=d(a,15);W=400;}
    else if(mode==3){L=d(a,17);W=d(a,18);H=d(a,19);}
    else {L=d(a,20);W=d(a,21);H=d(a,22);}
    if(L<20)L*=1000;if(H<20)H*=1000;if(W<20)W*=1000;
    float sc=(float)Math.min(640.0/Math.max(L,1),300.0/Math.max(H,1)); float x=105,y=430,w=(float)(L*sc),hh=(float)(H*sc);
    c.drawRect(x,y-hh,x+w,y,fill); c.drawRect(x,y-hh,x+w,y,frame);
    double blockL=500,blockH=200; for(double yy=0;yy<H;yy+=blockH){float Y=y-(float)(yy*sc);c.drawLine(x,Y,x+w,Y,thin);double shift=((int)Math.round(yy/blockH)%2)*blockL/2;for(double xx=-shift;xx<L;xx+=blockL){float X=x+(float)(xx*sc);c.drawLine(X,Math.max(y-hh,Y-(float)(blockH*sc)),X,Y,thin);}}
    dimH(c,x,x+w,y+48,mm(tr("LENGTH",l),L),thin,small); dimV(c,x-45,y-hh,y,mm(tr("HEIGHT",l),H),thin,small); c.drawText(tr("ZSALU_MODE",l)+" "+mode,x+8,y-hh-18,small);
  }

  private void drawTerko(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint fill,Paint small) {
    int shape=(int)Math.round(d(a,1)); double L=d(a,2),W=d(a,3),D=d(a,4), lL=d(a,5),lW=d(a,6),cutL=d(a,7),cutW=d(a,8);
    if(L<20)L*=1000;if(W<20)W*=1000;if(D<20)D*=1000;if(lL<20)lL*=1000;if(lW<20)lW*=1000;if(cutL<20)cutL*=1000;if(cutW<20)cutW*=1000;
    float x=145,y=410; Paint outline=frame;
    if(shape==2){double dia=D>0?D:3000;float sc=(float)(300.0/Math.max(dia,1)),r=(float)(dia*sc/2);c.drawCircle(x+260,y-160,r,fill);c.drawCircle(x+260,y-160,r,outline);dimH(c,x+260-r,x+260+r,y+45,mm(tr("DIAMETER",l),dia),thin,small);}
    else if(shape==3){double A=lL>0?lL:5000,B=lW>0?lW:4000,C=cutL>0?cutL:2200,E=cutW>0?cutW:1800;float sc=(float)Math.min(520.0/A,300.0/B);android.graphics.Path p=new android.graphics.Path();p.moveTo(x,y);p.lineTo(x+(float)(A*sc),y);p.lineTo(x+(float)(A*sc),y-(float)((B-E)*sc));p.lineTo(x+(float)((A-C)*sc),y-(float)((B-E)*sc));p.lineTo(x+(float)((A-C)*sc),y-(float)(B*sc));p.lineTo(x,y-(float)(B*sc));p.close();c.drawPath(p,fill);c.drawPath(p,outline);dimH(c,x,x+(float)(A*sc),y+45,mm(tr("LENGTH",l),A),thin,small);dimV(c,x-45,y-(float)(B*sc),y,mm(tr("WIDTH",l),B),thin,small);}
    else {double A=L>0?L:5000,B=W>0?W:3000;float sc=(float)Math.min(520.0/A,300.0/B);float ww=(float)(A*sc),hh=(float)(B*sc);c.drawRect(x,y-hh,x+ww,y,fill);c.drawRect(x,y-hh,x+ww,y,outline);dimH(c,x,x+ww,y+45,mm(tr("LENGTH",l),A),thin,small);dimV(c,x-45,y-hh,y,mm(tr("WIDTH",l),B),thin,small);}
  }

  private void drawLepcso(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint fill,Paint small) {
    double rise=d(a,1), count=Math.max(1,d(a,2)), going=d(a,3), width=d(a,4); if(rise<=0)rise=175;if(going<=0)going=280;
    double totalH=rise*count,totalL=going*count; float sc=(float)Math.min(600.0/Math.max(totalL,1),320.0/Math.max(totalH,1));float x=100,y=440;
    android.graphics.Path p=new android.graphics.Path();p.moveTo(x,y);for(int i=0;i<(int)Math.round(count);i++){float X=x+(float)(i*going*sc),Y=y-(float)(i*rise*sc);p.lineTo(X,Y-(float)(rise*sc));p.lineTo(X+(float)(going*sc),Y-(float)(rise*sc));}c.drawPath(p,frame);
    for(int i=0;i<(int)Math.round(count);i++){float X=x+(float)(i*going*sc),Y=y-(float)((i+1)*rise*sc);c.drawRect(X,Y,X+(float)(going*sc),Y+Math.max(3,(float)(rise*sc*.12)),fill);}
    dimH(c,x,x+(float)(totalL*sc),y+50,mm(tr("RUN",l),totalL),thin,small);dimV(c,x-50,y-(float)(totalH*sc),y,mm(tr("RISE_TOTAL",l),totalH),thin,small);c.drawText((int)Math.round(count)+"×  "+mm(tr("RISE",l),rise)+"  /  "+mm(tr("GOING",l),going),x+12,y-(float)(totalH*sc)-20,small);if(width>0)c.drawText(mm(tr("STAIR_WIDTH",l),width),x+350,y-(float)(totalH*sc)-20,small);
  }

  private static void gateRect(Canvas c,float l,float t,float r,float b,Paint outline,Paint fill,float inset){c.drawRect(l+inset,t+inset,r-inset,b-inset,fill);rect(c,l,t,r,b,outline);}
  private static void post(Canvas c,float x,float y,float w,float h,Paint outline,Paint fill){c.drawRect(x,y,x+w,y+h,fill);c.drawRect(x,y,x+w,y+h,outline);}
  private static void hinge(Canvas c,float x,float y,float r,Paint p){c.drawCircle(x,y,r,p);c.drawLine(x-r-6,y,x+r+6,y,p);}
  private static void roller(Canvas c,float x,float railY,float r,Paint p){c.drawCircle(x,railY+r,r,p);c.drawLine(x-r-10,railY+2*r+4,x+r+10,railY+2*r+4,p);}
  private static void rollerCarriage(Canvas c,float x,float y,Paint p){c.drawRect(x-18,y,x+18,y+8,p);c.drawCircle(x-8,y-6,7,p);c.drawCircle(x+8,y-6,7,p);}
  private static android.graphics.Path path(float[] q){android.graphics.Path p=new android.graphics.Path();if(q.length>=2){p.moveTo(q[0],q[1]);for(int i=2;i+1<q.length;i+=2)p.lineTo(q[i],q[i+1]);}return p;}
  private static void rect(Canvas c,float l,float t,float r,float b,Paint p){c.drawLine(l,t,r,t,p);c.drawLine(r,t,r,b,p);c.drawLine(r,b,l,b,p);c.drawLine(l,b,l,t,p);}
  private static void dimH(Canvas c,float x1,float x2,float y,String label,Paint line,Paint text){c.drawLine(x1,y,x2,y,line);c.drawLine(x1,y-5,x1,y+5,line);c.drawLine(x2,y-5,x2,y+5,line);float tw=text.measureText(label);c.drawText(label,(x1+x2-tw)/2,y-6,text);}
  private static void dimV(Canvas c,float x,float y1,float y2,String label,Paint line,Paint text){c.drawLine(x,y1,x,y2,line);c.drawLine(x-5,y1,x+5,y1,line);c.drawLine(x-5,y2,x+5,y2,line);c.save();c.rotate(-90,x-8,(y1+y2)/2);c.drawText(label,x-8,(y1+y2)/2,text);c.restore();}
  private static Paint stroke(float width,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStrokeWidth(width);p.setStyle(Paint.Style.STROKE);return p;}
  private static Paint fill(int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);return p;}
  private static Paint text(float size,boolean bold,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStyle(Paint.Style.FILL);p.setTextSize(size);p.setTypeface(Typeface.create(Typeface.SANS_SERIF,bold?Typeface.BOLD:Typeface.NORMAL));return p;}
  private static double d(String[] a,int i){try{return i<a.length?Double.parseDouble(a[i].replace(',','.')):0;}catch(Throwable e){return 0;}}
  private static String field(String s,int idx,String fallback){try{String[] a=s.split("\\|",-1);return idx<a.length&&a[idx].length()>0?a[idx]:fallback;}catch(Throwable e){return fallback;}}
  private static String tag(String s,String key,String fallback){try{String[] a=s.split("\\|",-1);for(int i=0;i<a.length-1;i++)if(key.equalsIgnoreCase(a[i]))return a[i+1];}catch(Throwable ignored){}return fallback;}
  private static boolean boolTag(String s,String key){String v=tag(s,key,"false");return "true".equalsIgnoreCase(v)||"1".equals(v)||"yes".equalsIgnoreCase(v);}
  private static int lang(String s){try{int v=(int)Math.round(Double.parseDouble(tag(s,"LANG","2")));return v>=1&&v<=12?v:2;}catch(Throwable e){return 2;}}
  private static int parseColor(String v){try{String s=v==null?"":v.trim();if(!s.startsWith("#"))s="#"+s;return Color.parseColor(s);}catch(Throwable e){return 0xFF6B7280;}}
  private static String safe(String s){String x=s==null?"":s.trim().replaceAll("[^A-Za-z0-9._-]+","_");if(x.length()==0)x="Drawing";return x.length()>64?x.substring(0,64):x;}
  private static String mm(String label,double v){return label+" "+Math.round(v)+" mm";}

  private static String title(String t,int l){
    if("KISKAPU".equals(t))return tr("KISKAPU",l);if("SZARNYAS".equals(t))return tr("SZARNYAS",l);if("TOLO".equals(t))return tr("TOLO",l);if("USZO".equals(t))return tr("USZO",l);if("KERITES".equals(t))return tr("KERITES",l);if("PANEL3D".equals(t))return tr("PANEL3D",l);if("TRAPEZ".equals(t))return tr("TRAPEZ",l);if("ZSALU".equals(t))return tr("ZSALU",l);if("TERKO".equals(t))return tr("TERKO",l);if("LEPCSO".equals(t))return tr("LEPCSO",l);return t;
  }

  private static String tr(String k,int l){
    int i=l<1||l>12?2:l;
    String[][] r={
      {"VECTOR","Vektoros műszaki rajz","Vector technical drawing","Vektor-Technikzeichnung","Dibujo técnico vectorial","Dessin technique vectoriel","矢量技术图","Disegno tecnico vettoriale","Desenho técnico vetorial","Wektorowy rysunek techniczny","Vector technische tekening","Desen tehnic vectorial","Векторный технический чертёж"},
      {"NOTE","determinált geometria – helyszíni ellenőrzés szükséges","deterministic geometry – verify on site","deterministische Geometrie – vor Ort prüfen","geometría determinista – verificar en obra","géométrie déterministe – vérifier sur site","确定性几何 – 现场核验","geometria deterministica – verificare in sito","geometria determinística – verificar no local","geometria deterministyczna – sprawdzić na miejscu","deterministische geometrie – ter plaatse controleren","geometrie deterministă – verificați la fața locului","детерминированная геометрия – проверить на месте"},
      {"PROJECT","Projekt","Project","Projekt","Proyecto","Projet","项目","Progetto","Projeto","Projekt","Project","Proiect","Проект"},
      {"KISKAPU","Kiskapu","Pedestrian gate","Gehtür","Puerta peatonal","Portillon","人行门","Cancello pedonale","Portão pedonal","Furtka","Looppoort","Poartă pietonală","Калитка"},
      {"SZARNYAS","Szárnyaskapu","Swing gate","Drehtor","Puerta batiente","Portail battant","平开门","Cancello a battente","Portão de batente","Brama skrzydłowa","Draaipoort","Poartă batantă","Распашные ворота"},
      {"TOLO","Tolókapu","Sliding gate","Schiebetor","Puerta corredera","Portail coulissant","平移门","Cancello scorrevole","Portão de correr","Brama przesuwna","Schuifpoort","Poartă culisantă","Откатные ворота"},
      {"USZO","Úszókapu","Cantilever gate","Freitragendes Schiebetor","Puerta voladiza","Portail autoportant","悬臂门","Cancello autoportante","Portão autoportante","Brama samonośna","Vrijdragende schuifpoort","Poartă autoportantă","Консольные ворота"},
      {"KERITES","Kerítésléc","Fence slats","Zaunlatten","Lamas de valla","Lames de clôture","围栏板条","Doghe recinzione","Réguas de vedação","Sztachety ogrodzeniowe","Schuttinglatten","Șipci gard","Штакетник"},
      {"PANEL3D","3D panel","3D panel","3D-Panel","Panel 3D","Panneau 3D","3D网片","Pannello 3D","Painel 3D","Panel 3D","3D-paneel","Panou 3D","3D-панель"},
      {"TRAPEZ","Trapézlemez","Trapezoidal sheet","Trapezblech","Chapa trapezoidal","Tôle trapézoïdale","梯形板","Lamiera grecata","Chapa trapezoidal","Blacha trapezowa","Trapeziumplaat","Tablă trapezoidală","Профнастил"},
      {"ZSALU","Zsalukő","Concrete block","Schalstein","Bloque de encofrado","Bloc à bancher","混凝土砌块","Blocco cassero","Bloco de cofragem","Pustak szalunkowy","Bekistingsblok","Bloc de cofraj","Опалубочный блок"},
      {"TERKO","Térkő","Paving","Pflaster","Pavimento","Pavage","铺装","Pavimentazione","Pavimento","Kostka brukowa","Bestrating","Pavaj","Брусчатка"},
      {"LEPCSO","Lépcső","Stair","Treppe","Escalera","Escalier","楼梯","Scala","Escada","Schody","Trap","Scară","Лестница"},
      {"FRAME_W","Keret szélesség","Frame width","Rahmenbreite","Ancho marco","Largeur cadre","框宽","Larghezza telaio","Largura quadro","Szerokość ramy","Framebreedte","Lățime cadru","Ширина рамы"},
      {"FRAME_H","Keret magasság","Frame height","Rahmenhöhe","Altura marco","Hauteur cadre","框高","Altezza telaio","Altura quadro","Wysokość ramy","Framehoogte","Înălțime cadru","Высота рамы"},
      {"OPENING","Nyílás","Opening","Öffnung","Hueco","Ouverture","开口","Apertura","Vão","Światło","Opening","Deschidere","Проём"},
      {"STIFF","Merevítő","Stiffener","Verstrebung","Refuerzo","Renfort","加强件","Irrigidimento","Reforço","Usztywnienie","Versteviging","Rigidizare","Усилитель"},
      {"LEAF","Szárny","Leaf","Flügel","Hoja","Vantail","门扇","Anta","Folha","Skrzydło","Vleugel","Canat","Створка"},
      {"STIFF_EACH","Merevítő/szárny","Stiffener/leaf","Verstrebung/Flügel","Refuerzo/hoja","Renfort/vantail","每扇加强件","Irrigidimento/anta","Reforço/folha","Usztywnienie/skrzydło","Versteviging/vleugel","Rigidizare/canat","Усилитель/створка"},
      {"CENTER_GAP","Középső hézag","Center gap","Mittelspalt","Holgura central","Jeu central","中缝","Gioco centrale","Folga central","Szczelina środkowa","Middenspleet","Rost central","Центральный зазор"},
      {"V_STIFF","Függőleges","Vertical","Vertikal","Vertical","Vertical","竖向","Verticale","Vertical","Pionowy","Verticaal","Vertical","Вертикальный"},
      {"H_STIFF","Vízszintes","Horizontal","Horizontal","Horizontal","Horizontal","水平","Orizzontale","Horizontal","Poziomy","Horizontaal","Orizontal","Горизонтальный"},
      {"BOTTOM","Alsó hossz","Bottom length","Unterlänge","Longitud inferior","Longueur basse","下部长度","Lunghezza inferiore","Comprimento inferior","Długość dolna","Onderlengte","Lungime inferioară","Нижняя длина"},
      {"OVER","Túlnyúlás","Overhang","Überstand","Voladizo","Porte-à-faux","悬伸","Sbalzo","Balanço","Przeciwwaga","Overstek","Contragreutate","Противовес"},
      {"FRONT","Első függőleges","Front vertical","Vordere Vertikale","Vertical delantera","Montant avant","前竖杆","Verticale anteriore","Vertical dianteira","Pion przedni","Voorste verticaal","Verticală față","Передняя вертикаль"},
      {"REAR","Hátsó függőleges","Rear vertical","Hintere Vertikale","Vertical trasera","Montant arrière","后竖杆","Verticale posteriore","Vertical traseira","Pion tylny","Achterste verticaal","Verticală spate","Задняя вертикаль"},
      {"LENGTH","Hossz","Length","Länge","Longitud","Longueur","长度","Lunghezza","Comprimento","Długość","Lengte","Lungime","Длина"},
      {"HEIGHT","Magasság","Height","Höhe","Altura","Hauteur","高度","Altezza","Altura","Wysokość","Hoogte","Înălțime","Высота"},
      {"WIDTH","Szélesség","Width","Breite","Ancho","Largeur","宽度","Larghezza","Largura","Szerokość","Breedte","Lățime","Ширина"},
      {"SLAT","Léc","Slat","Latte","Lama","Lame","板条","Doga","Régua","Sztacheta","Lat","Șipcă","Штакетина"},
      {"GAP","Hézag","Gap","Abstand","Separación","Jeu","间隙","Spazio","Folga","Szczelina","Tussenruimte","Rost","Зазор"},
      {"PANEL","Panel","Panel","Panel","Panel","Panneau","面板","Pannello","Painel","Panel","Paneel","Panou","Панель"},
      {"SHEET","Lemez","Sheet","Blech","Chapa","Tôle","板材","Lamiera","Chapa","Arkusz","Plaat","Tablă","Лист"},
      {"OVERLAP","Ráfedés","Overlap","Überdeckung","Solape","Recouvrement","搭接","Sovrapposizione","Sobreposição","Zakład","Overlap","Suprapunere","Нахлёст"},
      {"ZSALU_MODE","Mód","Mode","Modus","Modo","Mode","模式","Modalità","Modo","Tryb","Modus","Mod","Режим"},
      {"DIAMETER","Átmérő","Diameter","Durchmesser","Diámetro","Diamètre","直径","Diametro","Diâmetro","Średnica","Diameter","Diametru","Диаметр"},
      {"RUN","Teljes kinyúlás","Total run","Gesamtlauf","Desarrollo total","Giron total","总水平长度","Sviluppo totale","Desenvolvimento total","Całkowity bieg","Totale uitloop","Desfășurare totală","Общий вылет"},
      {"RISE_TOTAL","Teljes emelkedés","Total rise","Gesamthöhe","Altura total","Hauteur totale","总高度","Alzata totale","Elevação total","Całkowite wzniesienie","Totale stijging","Ridicare totală","Общий подъём"},
      {"RISE","Fellépő","Rise","Steigung","Contrahuella","Hauteur de marche","踏步高","Alzata","Espelho","Wysokość stopnia","Optrede","Contratreaptă","Подступенок"},
      {"GOING","Belépő","Going","Auftritt","Huella","Giron","踏面深度","Pedata","Piso","Głębokość stopnia","Aantrede","Treaptă","Проступь"},
      {"STAIR_WIDTH","Lépcső szélesség","Stair width","Treppenbreite","Ancho escalera","Largeur escalier","楼梯宽度","Larghezza scala","Largura escada","Szerokość schodów","Trapbreedte","Lățime scară","Ширина лестницы"}
    };
    for(String[] x:r)if(x[0].equals(k))return x[i];return k;
  }
}
