package com.gatecraft.drawing;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.MediaUtil;
import com.google.appinventor.components.runtime.util.NougatUtil;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@DesignerComponent(
    version = 2,
    versionName = "2.2.0",
    description = "GateCraft deterministic technical drawing PDF and share runtime.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "images/extension.png")
@SimpleObject(external = true)
public class GateCraftDrawing extends AndroidNonvisibleComponent {
  private final Form form;
  private final Context context;
  private volatile String lastError = "";
  private volatile String lastPdfUri = "";
  private volatile boolean pdfBusy = false;
  private WebView renderWebView;

  public GateCraftDrawing(ComponentContainer container) {
    super(container.$form());
    form = container.$form();
    context = container.$context();
  }

  @SimpleFunction(description = "Returns the GateCraft drawing runtime version.")
  public String Version() {
    return "2.2.0";
  }

  @SimpleFunction(description = "Returns the last PDF/share error, or an empty string.")
  public String LastError() {
    return lastError == null ? "" : lastError;
  }

  @SimpleFunction(description = "Returns the URI of the most recently generated PDF.")
  public String LastPDFUri() {
    return lastPdfUri == null ? "" : lastPdfUri;
  }

  @SimpleFunction(description = "Creates and shares the current GateCraft drawing as a PDF.")
  public boolean CreateAndShareDrawingPDF(final String payload, final String chooserTitle) {
    lastError = "";
    if (payload == null || payload.trim().length() == 0) {
      lastError = "Empty drawing payload";
      return false;
    }
    if (pdfBusy) {
      lastError = "PDF generation is already running";
      return false;
    }
    pdfBusy = true;
    try {
      form.runOnUiThread(new Runnable() {
        @Override public void run() {
          startHtmlPdf(payload, chooserTitle == null ? "" : chooserTitle);
        }
      });
      return true;
    } catch (Throwable ex) {
      pdfBusy = false;
      lastError = errorText(ex);
      return false;
    }
  }

  private void startHtmlPdf(final String payload, final String chooserTitle) {
    try {
      destroyRenderer();
      final String html = loadDrawingHtml();
      if (html.length() == 0) throw new IllegalStateException("gatecraft_draw.html is empty");
      final String prepared = injectPayload(html, payload);

      final WebView web = new WebView(context);
      renderWebView = web;
      web.setBackgroundColor(Color.WHITE);
      web.setVerticalScrollBarEnabled(false);
      web.setHorizontalScrollBarEnabled(false);
      WebSettings settings = web.getSettings();
      settings.setJavaScriptEnabled(true);
      settings.setAllowFileAccess(true);
      settings.setAllowContentAccess(true);
      settings.setDomStorageEnabled(false);
      settings.setLoadsImagesAutomatically(true);
      web.setWebViewClient(new WebViewClient() {
        private boolean completed = false;
        @Override public void onPageFinished(WebView view, String url) {
          if (completed) return;
          completed = true;
          new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override public void run() {
              finishPdfFromWebView(web, chooserTitle);
            }
          }, 350L);
        }
      });
      web.loadDataWithBaseURL("file:///android_asset/", prepared, "text/html", "UTF-8", null);
    } catch (Throwable ex) {
      failAsync(ex);
    }
  }

  private String loadDrawingHtml() throws Exception {
    InputStream in = null;
    try {
      in = MediaUtil.openMedia(form, "gatecraft_draw.html");
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buf = new byte[8192];
      int n;
      while ((n = in.read(buf)) >= 0) if (n > 0) out.write(buf, 0, n);
      return new String(out.toByteArray(), "UTF-8");
    } finally {
      try { if (in != null) in.close(); } catch (Throwable ignored) { }
    }
  }

  private String injectPayload(String html, String payload) {
    String quoted = JSONObject.quote(payload);
    String script = "<script>(function(){try{"
        + "if(typeof bootTimer!=='undefined')clearInterval(bootTimer);"
        + "parseStart(" + quoted + ");render();safeDraw();"
        + "var s=document.getElementById('svg');if(!s)throw new Error('SVG missing');"
        + "var c=s.cloneNode(true);c.setAttribute('width','1200');c.setAttribute('height','700');"
        + "c.style.width='1200px';c.style.height='700px';c.style.display='block';"
        + "document.body.innerHTML='';document.body.style.margin='0';document.body.style.padding='0';"
        + "document.body.style.background='#fff';document.body.style.overflow='hidden';document.body.appendChild(c);"
        + "}catch(e){document.body.setAttribute('data-gc-pdf-error',String(e));}})();</script>";
    int at = html.toLowerCase(Locale.ROOT).lastIndexOf("</body>");
    return at >= 0 ? html.substring(0, at) + script + html.substring(at) : html + script;
  }

  private void finishPdfFromWebView(WebView web, String chooserTitle) {
    PdfDocument doc = null;
    FileOutputStream fos = null;
    try {
      if (web != renderWebView) throw new IllegalStateException("PDF renderer replaced");
      final int sourceW = 1200;
      final int sourceH = 700;
      int ws = View.MeasureSpec.makeMeasureSpec(sourceW, View.MeasureSpec.EXACTLY);
      int hs = View.MeasureSpec.makeMeasureSpec(sourceH, View.MeasureSpec.EXACTLY);
      web.measure(ws, hs);
      web.layout(0, 0, sourceW, sourceH);
      web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

      doc = new PdfDocument();
      PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(842, 595, 1).create();
      PdfDocument.Page page = doc.startPage(info);
      Canvas canvas = page.getCanvas();
      canvas.drawColor(Color.WHITE);
      float margin = 22f;
      float scale = Math.min((842f - 2f * margin) / sourceW, (595f - 2f * margin) / sourceH);
      float dx = (842f - sourceW * scale) / 2f;
      float dy = (595f - sourceH * scale) / 2f;
      canvas.save();
      canvas.translate(dx, dy);
      canvas.scale(scale, scale);
      web.draw(canvas);
      canvas.restore();
      doc.finishPage(page);

      File dir = new File(context.getCacheDir(), "GateCraft");
      if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create PDF cache directory");
      String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
      File pdf = new File(dir, "GateCraft_Drawing_" + stamp + ".pdf");
      fos = new FileOutputStream(pdf);
      doc.writeTo(fos);
      fos.flush();
      try { fos.close(); } catch (Throwable ignored) { }
      fos = null;
      try { doc.close(); } catch (Throwable ignored) { }
      doc = null;

      Uri uri = NougatUtil.getPackageUri(form, pdf);
      lastPdfUri = uri.toString();
      Intent send = new Intent(Intent.ACTION_SEND);
      send.setType("application/pdf");
      send.putExtra(Intent.EXTRA_STREAM, uri);
      send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      String title = chooserTitle == null || chooserTitle.trim().length() == 0 ? "Share GateCraft PDF" : chooserTitle.trim();
      context.startActivity(Intent.createChooser(send, title));
      Toast.makeText(context, successText(), Toast.LENGTH_SHORT).show();
      pdfBusy = false;
      destroyRenderer();
    } catch (Throwable ex) {
      try { if (fos != null) fos.close(); } catch (Throwable ignored) { }
      try { if (doc != null) doc.close(); } catch (Throwable ignored) { }
      failAsync(ex);
    }
  }

  private String successText() {
    return "GateCraft PDF ready";
  }

  private void failAsync(Throwable ex) {
    lastError = errorText(ex);
    pdfBusy = false;
    destroyRenderer();
    try { Toast.makeText(context, "PDF: " + lastError, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) { }
  }

  private String errorText(Throwable ex) {
    if (ex == null) return "Unknown PDF error";
    String m = ex.getMessage();
    return ex.getClass().getSimpleName() + (m == null || m.length() == 0 ? "" : ": " + m);
  }

  private void destroyRenderer() {
    WebView w = renderWebView;
    renderWebView = null;
    if (w != null) {
      try { w.stopLoading(); } catch (Throwable ignored) { }
      try { w.loadUrl("about:blank"); } catch (Throwable ignored) { }
      try { w.clearHistory(); } catch (Throwable ignored) { }
      try { w.removeAllViews(); } catch (Throwable ignored) { }
      try { w.destroy(); } catch (Throwable ignored) { }
    }
  }
}
