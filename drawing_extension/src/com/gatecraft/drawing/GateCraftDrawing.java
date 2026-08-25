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
    version = 1,
    description = "GateCraft deterministic gate drawing PDF generator and share helper.",
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
  public String Version() { return "1.0.0"; }

  @SimpleFunction(description = "Creates an A4 landscape vector PDF from a GateCraft drawing payload, saves it to Downloads/GateCraft on Android 10+, and opens the Android share sheet. Returns true on success.")
  public boolean CreateAndShareDrawingPDF(String payload, String chooserTitle) {
    lastError = "";
    try {
      File dir = new File(context.getCacheDir(), "gatecraft_draw_pdf");
      if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("PDF cache directory could not be created");
      String type = field(payload, 0, "GATE");
      File tmp = new File(dir, "GateCraft_" + safe(type) + "_Drawing.pdf");
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
    PdfDocument pdf = new PdfDocument();
    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PW, PH, 1).create();
    PdfDocument.Page page = pdf.startPage(info);
    Canvas c = page.getCanvas();
    c.drawColor(Color.WHITE);
    Paint thin = p(1.4f, false, Color.BLACK);
    Paint frame = p(7.0f, false, 0xFF202020);
    Paint rail = p(9.0f, false, 0xFF555555);
    Paint txt = p(11.0f, false, Color.BLACK);
    Paint small = p(8.5f, false, 0xFF333333);
    Paint bold = p(15.0f, true, Color.BLACK);
    c.drawText("GateCraft – " + title(type, lang), 42, 42, bold);
    c.drawText(tr("VECTOR", lang), 42, 61, small);
    if ("USZO".equals(type)) drawUszo(c,a,lang,thin,frame,rail,txt,small);
    else if ("TOLO".equals(type)) drawTolo(c,a,lang,thin,frame,rail,txt,small);
    else if ("SZARNYAS".equals(type)) drawSzarnyas(c,a,lang,thin,frame,txt,small);
    else if ("KISKAPU".equals(type)) drawKiskapu(c,a,lang,thin,frame,txt,small);
    else throw new IllegalArgumentException("Unsupported drawing payload: " + type);
    c.drawText("GateCraft • " + tr("NOTE",lang), 42, PH-24, small);
    pdf.finishPage(page);
    FileOutputStream fos = new FileOutputStream(out);
    try { pdf.writeTo(fos); } finally { try { fos.close(); } catch(Throwable ignored){} pdf.close(); }
  }

  private void drawKiskapu(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), leave=d(a,3), hinge=d(a,4), lock=d(a,5), recv=d(a,6), F=Math.max(1,d(a,7));
    double fw=W-(hinge+lock+recv), fh=H-2*leave, stiff=fw-2*F;
    float sc=(float)Math.min(610.0/Math.max(fw,1),300.0/Math.max(fh,1));
    float x=120,y=440,w=(float)(fw*sc),h=(float)(fh*sc);
    rect(c,x,y-h,x+w,y,frame);
    float yy=y-h/2;
    c.drawLine(x+(float)(F*sc),yy,x+w-(float)(F*sc),yy,frame);
    dimH(c,x,x+w,y+45,mm(tr("FRAME_W",l),fw),thin,small);
    dimV(c,x-48,y-h,y,mm(tr("FRAME_H",l),fh),thin,small);
    c.drawText(mm(tr("STIFF",l),stiff),x+10,y-h-18,small);
    c.drawText(mm(tr("OPENING",l),W),x+10,y+76,small);
  }

  private void drawSzarnyas(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), leave=d(a,3), hinge=d(a,4), lockGap=d(a,5), F=Math.max(1,d(a,6));
    double fh=Math.ceil(H-2*leave), leaf=Math.ceil((W-(2*hinge+lockGap))/2.0), stiff=Math.ceil(leaf-2*F), total=leaf*2+lockGap;
    float sc=(float)Math.min(610.0/Math.max(total,1),300.0/Math.max(fh,1));
    float x=115,y=440,h=(float)(fh*sc),lw=(float)(leaf*sc),gap=(float)Math.max(8,lockGap*sc);
    rect(c,x,y-h,x+lw,y,frame); rect(c,x+lw+gap,y-h,x+2*lw+gap,y,frame);
    float yy=y-h/2, inset=(float)(F*sc);
    c.drawLine(x+inset,yy,x+lw-inset,yy,frame); c.drawLine(x+lw+gap+inset,yy,x+2*lw+gap-inset,yy,frame);
    dimH(c,x,x+2*lw+gap,y+45,mm(tr("OPENING",l),W),thin,small);
    dimV(c,x-48,y-h,y,mm(tr("FRAME_H",l),fh),thin,small);
    c.drawText(mm(tr("LEAF",l),leaf),x+10,y-h-18,small); c.drawText(mm(tr("STIFF_EACH",l),stiff),x+230,y-h-18,small);
  }

  private void drawTolo(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint rail,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), over=d(a,3), leave=d(a,4), railH=d(a,5), roller=d(a,6), F=Math.max(1,d(a,7));
    double fw=W+over, fh=H-(leave+roller+railH), v=Math.ceil(fh-2*F), seg=Math.ceil((fw-4*F)/3.0);
    float sc=(float)Math.min(640.0/Math.max(fw,1),290.0/Math.max(fh,1));
    float x=105,y=430,w=(float)(fw*sc),h=(float)(fh*sc),pf=(float)(F*sc);
    rect(c,x,y-h,x+w,y,frame);
    double vx1=F+seg+F/2.0, vx2=2*F+2*seg+F/2.0;
    float X1=x+(float)(vx1*sc), X2=x+(float)(vx2*sc);
    c.drawLine(X1,y-h+pf,X1,y-pf,frame); c.drawLine(X2,y-h+pf,X2,y-pf,frame);
    float yy=y-h/2;
    c.drawLine(x+pf,yy,X1-pf/2,yy,frame); c.drawLine(X1+pf/2,yy,X2-pf/2,yy,frame); c.drawLine(X2+pf/2,yy,x+w-pf,yy,frame);
    c.drawLine(x-10,y+13,x+w+10,y+13,rail);
    dimH(c,x,x+w,y+58,mm(tr("FRAME_W",l),fw),thin,small); dimV(c,x-48,y-h,y,mm(tr("FRAME_H",l),fh),thin,small);
    c.drawText("2× "+mm(tr("V_STIFF",l),v)+"   3× "+mm(tr("H_STIFF",l),seg),x+10,y-h-18,small);
  }

  private void drawUszo(Canvas c,String[] a,int l,Paint thin,Paint frame,Paint rail,Paint txt,Paint small) {
    double W=d(a,1), H=d(a,2), P=d(a,3), leave=d(a,4), railH=d(a,5), gap=d(a,6), F=Math.max(1,d(a,7));
    double top=W+P, bottom=W*1.5, front=H-(leave+gap+railH), v=H-(leave+gap+railH+2*F), seg=Math.ceil((top-4*F)/3.0), over=bottom-W;
    float sc=(float)Math.min(640.0/Math.max(bottom,1),285.0/Math.max(front,1));
    float x=105,y=430,xt=x+(float)(top*sc),xb=x+(float)(bottom*sc),xo=x+(float)(W*sc),ytop=y-(float)(front*sc),pf=(float)(F*sc);
    c.drawLine(x,ytop,xt,ytop,frame); c.drawLine(x,y,xb,y,frame); c.drawLine(x,ytop,x,y,frame); c.drawLine(xt,ytop,xb,y,frame);
    double vx1=F+seg+F/2.0, vx2=2*F+2*seg+F/2.0;
    float X1=x+(float)(vx1*sc), X2=x+(float)(vx2*sc);
    c.drawLine(X1,ytop+pf,X1,y-pf,frame); c.drawLine(X2,ytop+pf,X2,y-pf,frame);
    float yy=(ytop+y)/2;
    c.drawLine(x+pf,yy,X1-pf/2,yy,frame); c.drawLine(X1+pf/2,yy,X2-pf/2,yy,frame); c.drawLine(X2+pf/2,yy,xt-pf,yy,frame);
    c.drawLine(x,y+13,xb,y+13,rail);
    Paint dash=p(1.0f,false,0xFF777777); c.drawLine(xo,ytop-25,xo,y+65,dash);
    dimH(c,x,xo,y+58,mm(tr("OPENING",l),W),thin,small); dimH(c,x,xb,y+92,mm(tr("BOTTOM",l),bottom),thin,small); dimH(c,xo,xb,y+126,mm(tr("OVER",l),over),thin,small); dimV(c,x-48,ytop,y,mm(tr("FRONT",l),front),thin,small);
    c.drawText("2× "+mm(tr("V_STIFF",l),v)+"   3× "+mm(tr("H_STIFF",l),seg),x+10,ytop-18,small); c.drawText(mm(tr("TOP",l),top),x+385,ytop-18,small);
  }

  private static void rect(Canvas c,float l,float t,float r,float b,Paint p){c.drawLine(l,t,r,t,p);c.drawLine(r,t,r,b,p);c.drawLine(r,b,l,b,p);c.drawLine(l,b,l,t,p);}
  private static void dimH(Canvas c,float x1,float x2,float y,String label,Paint line,Paint text){c.drawLine(x1,y,x2,y,line);c.drawLine(x1,y-5,x1,y+5,line);c.drawLine(x2,y-5,x2,y+5,line);float tw=text.measureText(label);c.drawText(label,(x1+x2-tw)/2,y-6,text);}
  private static void dimV(Canvas c,float x,float y1,float y2,String label,Paint line,Paint text){c.drawLine(x,y1,x,y2,line);c.drawLine(x-5,y1,x+5,y1,line);c.drawLine(x-5,y2,x+5,y2,line);c.save();c.rotate(-90,x-8,(y1+y2)/2);c.drawText(label,x-8,(y1+y2)/2,text);c.restore();}
  private static Paint p(float size,boolean bold,int color){Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStrokeWidth(size);p.setStyle(Paint.Style.STROKE);p.setTypeface(Typeface.create(Typeface.SANS_SERIF,bold?Typeface.BOLD:Typeface.NORMAL));if(size>=8){p.setStyle(Paint.Style.FILL);p.setTextSize(size);}return p;}
  private static double d(String[] a,int i){try{return i<a.length?Double.parseDouble(a[i].replace(',','.')):0;}catch(Throwable e){return 0;}}
  private static String field(String s,int idx,String fallback){try{String[] a=s.split("\\|",-1);return idx<a.length&&a[idx].length()>0?a[idx]:fallback;}catch(Throwable e){return fallback;}}
  private static int lang(String s){try{String[] a=s.split("\\|",-1);for(int i=0;i<a.length-1;i++)if("LANG".equalsIgnoreCase(a[i])){int v=(int)Math.round(Double.parseDouble(a[i+1]));return v>=1&&v<=12?v:2;}}catch(Throwable ignored){}return 2;}
  private static String safe(String s){return s==null?"DRAW":s.replaceAll("[^A-Za-z0-9_-]","_");}
  private static String mm(String label,double v){return label+" = "+Math.round(v)+" mm";}
  private static String title(String type,int l){if("KISKAPU".equals(type))return tr("KISKAPU",l);if("SZARNYAS".equals(type))return tr("SZARNYAS",l);if("TOLO".equals(type))return tr("TOLO",l);if("USZO".equals(type))return tr("USZO",l);return "Gate drawing";}
  private static String tr(String k,int l){
    String[][] names={
      {"VECTOR","Vektoros műszaki rajz – GateCraft","NOTE","GateCraft számítási adatok alapján","KISKAPU","Kiskapu szakrajz","SZARNYAS","Szárnyaskapu szakrajz","TOLO","Tolókapu szakrajz","USZO","Úszókapu szakrajz","OPENING","Nyílás","FRAME_W","Keretszélesség","FRAME_H","Keretmagasság","STIFF","Merevítő","STIFF_EACH","Merevítő / szárny","LEAF","Szárnyszélesség","V_STIFF","Függőleges merevítő","H_STIFF","Vízszintes merevítő","BOTTOM","Alsó váz","OVER","Túlnyúlás","FRONT","Első függő","TOP","Felső váz"},
      {"VECTOR","Vector technical drawing – GateCraft","NOTE","Generated from GateCraft calculation data","KISKAPU","Pedestrian gate drawing","SZARNYAS","Swing gate drawing","TOLO","Sliding gate drawing","USZO","Cantilever gate drawing","OPENING","Opening","FRAME_W","Frame width","FRAME_H","Frame height","STIFF","Middle stiffener","STIFF_EACH","Stiffener / leaf","LEAF","Leaf width","V_STIFF","Vertical stiffener","H_STIFF","Horizontal stiffener","BOTTOM","Bottom frame","OVER","Counterbalance","FRONT","Front vertical","TOP","Top frame"},
      {"VECTOR","Vektor-Technische Zeichnung – GateCraft","NOTE","Aus GateCraft-Berechnungsdaten erzeugt","KISKAPU","Gehtür-Zeichnung","SZARNYAS","Drehtor-Zeichnung","TOLO","Schiebetor-Zeichnung","USZO","Freitragendes Tor – Zeichnung","OPENING","Öffnung","FRAME_W","Rahmenbreite","FRAME_H","Rahmenhöhe","STIFF","Mittelstrebe","STIFF_EACH","Strebe / Flügel","LEAF","Flügelbreite","V_STIFF","Vertikalstrebe","H_STIFF","Horizontalstrebe","BOTTOM","Unterrahmen","OVER","Gegengewicht","FRONT","Vorderer Pfosten","TOP","Oberrahmen"},
      {"VECTOR","Plano técnico vectorial – GateCraft","NOTE","Generado con datos de cálculo GateCraft","KISKAPU","Plano puerta peatonal","SZARNYAS","Plano puerta batiente","TOLO","Plano puerta corredera","USZO","Plano puerta autoportante","OPENING","Apertura","FRAME_W","Ancho del marco","FRAME_H","Altura del marco","STIFF","Refuerzo central","STIFF_EACH","Refuerzo / hoja","LEAF","Ancho de hoja","V_STIFF","Refuerzo vertical","H_STIFF","Refuerzo horizontal","BOTTOM","Marco inferior","OVER","Contrapeso","FRONT","Vertical delantero","TOP","Marco superior"},
      {"VECTOR","Dessin technique vectoriel – GateCraft","NOTE","Généré à partir des calculs GateCraft","KISKAPU","Plan portillon","SZARNYAS","Plan portail battant","TOLO","Plan portail coulissant","USZO","Plan portail autoportant","OPENING","Ouverture","FRAME_W","Largeur cadre","FRAME_H","Hauteur cadre","STIFF","Renfort central","STIFF_EACH","Renfort / vantail","LEAF","Largeur vantail","V_STIFF","Renfort vertical","H_STIFF","Renfort horizontal","BOTTOM","Cadre inférieur","OVER","Contrepoids","FRONT","Montant avant","TOP","Cadre supérieur"},
      {"VECTOR","GateCraft 矢量技术图","NOTE","根据 GateCraft 计算数据生成","KISKAPU","人行门技术图","SZARNYAS","平开门技术图","TOLO","滑动门技术图","USZO","悬臂门技术图","OPENING","净开口","FRAME_W","框架宽度","FRAME_H","框架高度","STIFF","中间加强件","STIFF_EACH","每扇加强件","LEAF","门扇宽度","V_STIFF","竖向加强件","H_STIFF","横向加强件","BOTTOM","下框","OVER","配重段","FRONT","前立边","TOP","上框"},
      {"VECTOR","Disegno tecnico vettoriale – GateCraft","NOTE","Generato dai dati di calcolo GateCraft","KISKAPU","Disegno cancelletto","SZARNYAS","Disegno cancello a battente","TOLO","Disegno cancello scorrevole","USZO","Disegno cancello autoportante","OPENING","Apertura","FRAME_W","Larghezza telaio","FRAME_H","Altezza telaio","STIFF","Rinforzo centrale","STIFF_EACH","Rinforzo / anta","LEAF","Larghezza anta","V_STIFF","Rinforzo verticale","H_STIFF","Rinforzo orizzontale","BOTTOM","Telaio inferiore","OVER","Contrappeso","FRONT","Montante anteriore","TOP","Telaio superiore"},
      {"VECTOR","Desenho técnico vetorial – GateCraft","NOTE","Gerado a partir dos cálculos GateCraft","KISKAPU","Desenho portão pedonal","SZARNYAS","Desenho portão de abrir","TOLO","Desenho portão deslizante","USZO","Desenho portão autoportante","OPENING","Vão","FRAME_W","Largura da moldura","FRAME_H","Altura da moldura","STIFF","Reforço central","STIFF_EACH","Reforço / folha","LEAF","Largura da folha","V_STIFF","Reforço vertical","H_STIFF","Reforço horizontal","BOTTOM","Moldura inferior","OVER","Contrapeso","FRONT","Vertical frontal","TOP","Moldura superior"},
      {"VECTOR","Wektorowy rysunek techniczny – GateCraft","NOTE","Wygenerowano z obliczeń GateCraft","KISKAPU","Rysunek furtki","SZARNYAS","Rysunek bramy skrzydłowej","TOLO","Rysunek bramy przesuwnej","USZO","Rysunek bramy samonośnej","OPENING","Światło","FRAME_W","Szerokość ramy","FRAME_H","Wysokość ramy","STIFF","Wzmocnienie środkowe","STIFF_EACH","Wzmocnienie / skrzydło","LEAF","Szerokość skrzydła","V_STIFF","Wzmocnienie pionowe","H_STIFF","Wzmocnienie poziome","BOTTOM","Dolna rama","OVER","Przeciwwaga","FRONT","Przedni pion","TOP","Górna rama"},
      {"VECTOR","Vector technische tekening – GateCraft","NOTE","Gegenereerd uit GateCraft-berekeningen","KISKAPU","Tekening looppoort","SZARNYAS","Tekening draaipoort","TOLO","Tekening schuifpoort","USZO","Tekening vrijdragende poort","OPENING","Doorgang","FRAME_W","Framebreedte","FRAME_H","Framehoogte","STIFF","Middenverstijver","STIFF_EACH","Verstijver / vleugel","LEAF","Vleugelbreedte","V_STIFF","Verticale verstijver","H_STIFF","Horizontale verstijver","BOTTOM","Onderframe","OVER","Tegengewicht","FRONT","Voorste staander","TOP","Bovenframe"},
      {"VECTOR","Desen tehnic vectorial – GateCraft","NOTE","Generat din calculele GateCraft","KISKAPU","Desen poartă pietonală","SZARNYAS","Desen poartă batantă","TOLO","Desen poartă culisantă","USZO","Desen poartă autoportantă","OPENING","Deschidere","FRAME_W","Lățime cadru","FRAME_H","Înălțime cadru","STIFF","Rigidizare centrală","STIFF_EACH","Rigidizare / canat","LEAF","Lățime canat","V_STIFF","Rigidizare verticală","H_STIFF","Rigidizare orizontală","BOTTOM","Cadru inferior","OVER","Contragreutate","FRONT","Montant frontal","TOP","Cadru superior"},
      {"VECTOR","Векторный технический чертёж – GateCraft","NOTE","Создано по расчётным данным GateCraft","KISKAPU","Чертёж калитки","SZARNYAS","Чертёж распашных ворот","TOLO","Чертёж откатных ворот","USZO","Чертёж консольных ворот","OPENING","Проём","FRAME_W","Ширина рамы","FRAME_H","Высота рамы","STIFF","Средняя перемычка","STIFF_EACH","Перемычка / створка","LEAF","Ширина створки","V_STIFF","Вертикальная стойка","H_STIFF","Горизонтальная перемычка","BOTTOM","Нижняя рама","OVER","Противовес","FRONT","Передняя стойка","TOP","Верхняя рама"}
    };
    String[] row=names[Math.max(1,Math.min(12,l))-1];
    for(int i=0;i<row.length-1;i+=2)if(row[i].equals(k))return row[i+1];
    return k;
  }
}
