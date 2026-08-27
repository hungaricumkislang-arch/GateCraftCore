package com.gatecraft.drawing;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@DesignerComponent(
        version = 3,
        versionName = "2.3.0",
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
    private ViewGroup renderParent;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public GateCraftDrawing(ComponentContainer container) {
        super(container.$form());
        this.form = container.$form();
        this.context = container.$context();
    }

    @SimpleFunction(description = "Returns the GateCraft drawing runtime version.")
    public String Version() { return "2.3.0"; }

    @SimpleFunction(description = "Returns the last PDF/share error, or an empty string.")
    public String LastError() { return lastError == null ? "" : lastError; }

    @SimpleFunction(description = "Returns the URI of the most recently generated PDF.")
    public String LastPDFUri() { return lastPdfUri == null ? "" : lastPdfUri; }

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
                @Override public void run() { startHtmlPdf(payload, chooserTitle); }
            });
            return true;
        } catch (Throwable t) {
            pdfBusy = false;
            lastError = errorText(t);
            return false;
        }
    }

    private void startHtmlPdf(String payload, final String chooserTitle) {
        try {
            destroyRenderer();
            String html = loadDrawingHtml();
            if (html.length() == 0) throw new IllegalStateException("gatecraft_draw.html is empty");
            String injected = injectPayload(html, payload);

            Activity activity = (Activity) context;
            View rootView = activity.findViewById(android.R.id.content);
            if (!(rootView instanceof ViewGroup)) throw new IllegalStateException("Android content root is unavailable");
            renderParent = (ViewGroup) rootView;

            final WebView web = new WebView(context);
            renderWebView = web;
            web.setBackgroundColor(0xFFFFFFFF);
            web.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            web.setX(-10000f);
            web.setY(-10000f);
            renderParent.addView(web, new ViewGroup.LayoutParams(1200, 700));

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
                    waitForRenderedSvg(web, chooserTitle, 0);
                }
            });
            web.loadDataWithBaseURL("file:///android_asset/", injected, "text/html", "UTF-8", null);
        } catch (Throwable t) {
            failAsync(t);
        }
    }

    private void waitForRenderedSvg(final WebView web, final String chooserTitle, final int attempt) {
        if (web != renderWebView) {
            failAsync(new IllegalStateException("PDF renderer replaced"));
            return;
        }
        if (attempt > 30) {
            failAsync(new IllegalStateException("Drawing SVG did not become ready"));
            return;
        }
        try {
            web.evaluateJavascript(
                "(function(){try{var e=document.body&&document.body.getAttribute('data-gc-pdf-error');if(e)return 'ERR:'+e;var s=document.getElementById('svg');if(!s)return 'WAIT';var b=s.getBoundingClientRect();var n=s.querySelectorAll('*').length;return (n>0&&b.width>10&&b.height>10)?'READY':'WAIT';}catch(e){return 'ERR:'+String(e);}})()",
                new ValueCallback<String>() {
                    @Override public void onReceiveValue(String value) {
                        String v = value == null ? "" : value;
                        if (v.indexOf("READY") >= 0) {
                            mainHandler.postDelayed(new Runnable() {
                                @Override public void run() { renderAttachedWebViewToPdf(web, chooserTitle); }
                            }, 250L);
                        } else if (v.indexOf("ERR:") >= 0) {
                            failAsync(new IllegalStateException("Drawing render error: " + v));
                        } else {
                            mainHandler.postDelayed(new Runnable() {
                                @Override public void run() { waitForRenderedSvg(web, chooserTitle, attempt + 1); }
                            }, 100L);
                        }
                    }
                });
        } catch (Throwable t) {
            failAsync(t);
        }
    }

    private void renderAttachedWebViewToPdf(WebView web, String chooserTitle) {
        Bitmap bitmap = null;
        PdfDocument pdf = null;
        FileOutputStream out = null;
        try {
            if (web != renderWebView || web.getParent() == null) throw new IllegalStateException("PDF WebView is not attached");
            int wSpec = View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY);
            int hSpec = View.MeasureSpec.makeMeasureSpec(700, View.MeasureSpec.EXACTLY);
            web.measure(wSpec, hSpec);
            web.layout(0, 0, 1200, 700);
            web.invalidate();

            bitmap = Bitmap.createBitmap(1200, 700, Bitmap.Config.ARGB_8888);
            Canvas capture = new Canvas(bitmap);
            capture.drawColor(0xFFFFFFFF);
            web.draw(capture);
            validateBitmap(bitmap);

            pdf = new PdfDocument();
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(842, 595, 1).create();
            PdfDocument.Page page = pdf.startPage(info);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(0xFFFFFFFF);
            float margin = 22f;
            float scale = Math.min((842f - 2f * margin) / 1200f, (595f - 2f * margin) / 700f);
            float dw = 1200f * scale;
            float dh = 700f * scale;
            float left = (842f - dw) / 2f;
            float top = (595f - dh) / 2f;
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            canvas.drawBitmap(bitmap, null, new RectF(left, top, left + dw, top + dh), paint);
            pdf.finishPage(page);

            File file = createOutputFile();
            out = new FileOutputStream(file);
            pdf.writeTo(out);
            out.flush();
            out.close();
            out = null;
            pdf.close();
            pdf = null;
            validatePdf(file);
            sharePdf(file, chooserTitle);
        } catch (Throwable t) {
            if (out != null) try { out.close(); } catch (Throwable ignored) {}
            if (pdf != null) try { pdf.close(); } catch (Throwable ignored) {}
            failAsync(t);
        } finally {
            if (bitmap != null) try { bitmap.recycle(); } catch (Throwable ignored) {}
        }
    }

    private void validateBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() < 100 || bitmap.getHeight() < 100) {
            throw new IllegalStateException("Drawing bitmap is invalid");
        }
        int nonWhite = 0;
        for (int y = 0; y < bitmap.getHeight(); y += 4) {
            for (int x = 0; x < bitmap.getWidth(); x += 4) {
                int c = bitmap.getPixel(x, y);
                int a = (c >>> 24) & 255;
                int r = (c >>> 16) & 255;
                int g = (c >>> 8) & 255;
                int b = c & 255;
                if (a > 20 && (r < 245 || g < 245 || b < 245)) {
                    nonWhite++;
                    if (nonWhite >= 120) return;
                }
            }
        }
        throw new IllegalStateException("Drawing renderer produced a blank image");
    }

    private File createOutputFile() throws Exception {
        File dir = new File(context.getFilesDir(), "GateCraft");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create PDF cache directory");
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return new File(dir, "GateCraft_Drawing_" + stamp + ".pdf");
    }

    private void validatePdf(File file) throws Exception {
        if (file == null || !file.exists()) throw new IllegalStateException("PDF file was not created");
        if (file.length() < 4096L) throw new IllegalStateException("Generated PDF is unexpectedly empty (" + file.length() + " bytes)");
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            byte[] b = new byte[5];
            int n = in.read(b);
            if (n < 5 || b[0] != '%' || b[1] != 'P' || b[2] != 'D' || b[3] != 'F' || b[4] != '-') {
                throw new IllegalStateException("Generated file is not a PDF");
            }
        } finally {
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
        }
    }

    private void sharePdf(File file, String chooserTitle) throws Exception {
        Uri uri = NougatUtil.getPackageUri(form, file);
        lastPdfUri = uri.toString();
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        String title = chooserTitle == null || chooserTitle.trim().length() == 0
                ? "Share GateCraft PDF" : chooserTitle.trim();
        context.startActivity(Intent.createChooser(send, title));
        Toast.makeText(context, successText(), Toast.LENGTH_SHORT).show();
        pdfBusy = false;
        destroyRenderer();
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
            if (in != null) try { in.close(); } catch (Throwable ignored) {}
        }
    }

    private String injectPayload(String html, String payload) {
        String quoted = JSONObject.quote(payload);
        String script = "<style>@page{size:A4 landscape;margin:0}html,body{margin:0!important;padding:0!important;background:#fff!important;overflow:hidden!important}#svg{display:block!important;width:1200px!important;height:700px!important}</style>" +
                "<script>(function(){try{if(typeof bootTimer!=='undefined')clearInterval(bootTimer);parseStart(" + quoted + ");render();safeDraw();var s=document.getElementById('svg');if(!s)throw new Error('SVG missing');var c=s.cloneNode(true);c.setAttribute('width','1200');c.setAttribute('height','700');c.setAttribute('viewBox','0 0 1200 700');c.style.width='1200px';c.style.height='700px';c.style.display='block';document.body.innerHTML='';document.body.style.margin='0';document.body.style.padding='0';document.body.style.background='#fff';document.body.style.overflow='hidden';document.body.appendChild(c);}catch(e){document.body.setAttribute('data-gc-pdf-error',String(e));}})();</script>";
        String lower = html.toLowerCase(Locale.ROOT);
        int pos = lower.lastIndexOf("</body>");
        return pos >= 0 ? html.substring(0, pos) + script + html.substring(pos) : html + script;
    }

    private String successText() { return "GateCraft PDF ready"; }

    private void failAsync(final Throwable t) {
        mainHandler.post(new Runnable() {
            @Override public void run() {
                lastError = errorText(t);
                pdfBusy = false;
                destroyRenderer();
                try { Toast.makeText(context, "PDF: " + lastError, Toast.LENGTH_LONG).show(); } catch (Throwable ignored) {}
            }
        });
    }

    private String errorText(Throwable t) {
        if (t == null) return "Unknown PDF error";
        String m = t.getMessage();
        if (m != null && m.trim().length() > 0) return m.trim();
        return t.getClass().getSimpleName();
    }

    private void destroyRenderer() {
        WebView w = renderWebView;
        renderWebView = null;
        ViewGroup parent = renderParent;
        renderParent = null;
        if (w != null) {
            if (parent != null) try { parent.removeView(w); } catch (Throwable ignored) {}
            try { w.stopLoading(); } catch (Throwable ignored) {}
            try { w.loadUrl("about:blank"); } catch (Throwable ignored) {}
            try { w.clearHistory(); } catch (Throwable ignored) {}
            try { w.removeAllViews(); } catch (Throwable ignored) {}
            try { w.destroy(); } catch (Throwable ignored) {}
        }
    }
}
