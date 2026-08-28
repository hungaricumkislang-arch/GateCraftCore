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
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
        version = 4,
        versionName = "2.4.0",
        description = "GateCraft deterministic technical drawing PDF and share runtime.",
        category = ComponentCategory.EXTENSION,
        nonVisible = true,
        iconName = "images/extension.png")
@SimpleObject(external = true)
public class GateCraftDrawing extends AndroidNonvisibleComponent {
    private static final int RENDER_W = 1200;
    private static final int RENDER_H = 700;
    private static final int MAX_READY_ATTEMPTS = 50;
    private static final int MAX_STABILITY_ATTEMPTS = 6;

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
    public String Version() { return "2.4.0"; }

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
            web.setHorizontalScrollBarEnabled(false);
            web.setVerticalScrollBarEnabled(false);
            web.setX(-10000f);
            web.setY(-10000f);
            renderParent.addView(web, new ViewGroup.LayoutParams(RENDER_W, RENDER_H));

            WebSettings settings = web.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setAllowFileAccess(true);
            settings.setAllowContentAccess(true);
            settings.setDomStorageEnabled(false);
            settings.setLoadsImagesAutomatically(true);
            settings.setLoadWithOverviewMode(false);
            settings.setUseWideViewPort(false);

            web.setWebViewClient(new WebViewClient() {
                private boolean completed = false;
                @Override public void onPageFinished(WebView view, String url) {
                    if (completed) return;
                    completed = true;
                    waitForCanvasReady(web, chooserTitle, 0);
                }
            });
            web.loadDataWithBaseURL("file:///android_asset/", injected, "text/html", "UTF-8", null);
        } catch (Throwable t) {
            failAsync(t);
        }
    }

    private void waitForCanvasReady(final WebView web, final String chooserTitle, final int attempt) {
        if (web != renderWebView) {
            failAsync(new IllegalStateException("PDF renderer replaced"));
            return;
        }
        if (attempt > MAX_READY_ATTEMPTS) {
            failAsync(new IllegalStateException("Drawing canvas did not become ready"));
            return;
        }
        try {
            web.evaluateJavascript(
                "(function(){try{var b=document.body;if(!b)return 'WAIT';var e=b.getAttribute('data-gc-pdf-error');if(e)return 'ERR:'+e;var c=document.getElementById('gcPdfCanvas');var r=b.getAttribute('data-gc-pdf-painted');if(c&&r==='1'&&c.width===1200&&c.height===700)return 'READY';return 'WAIT';}catch(e){return 'ERR:'+String(e);}})()",
                new ValueCallback<String>() {
                    @Override public void onReceiveValue(String value) {
                        String v = value == null ? "" : value;
                        if (v.indexOf("READY") >= 0) {
                            waitForChromiumVisualCommit(web, chooserTitle);
                        } else if (v.indexOf("ERR:") >= 0) {
                            failAsync(new IllegalStateException("Drawing render error: " + cleanJsValue(v)));
                        } else {
                            mainHandler.postDelayed(new Runnable() {
                                @Override public void run() { waitForCanvasReady(web, chooserTitle, attempt + 1); }
                            }, 100L);
                        }
                    }
                });
        } catch (Throwable t) {
            failAsync(t);
        }
    }

    private void waitForChromiumVisualCommit(final WebView web, final String chooserTitle) {
        if (web != renderWebView) return;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                web.postVisualStateCallback(SystemClock.uptimeMillis(), new WebView.VisualStateCallback() {
                    @Override public void onComplete(long requestId) {
                        mainHandler.postDelayed(new Runnable() {
                            @Override public void run() { captureStablePair(web, chooserTitle, 0, null, null); }
                        }, 250L);
                    }
                });
                return;
            } catch (Throwable ignored) { }
        }
        mainHandler.postDelayed(new Runnable() {
            @Override public void run() { captureStablePair(web, chooserTitle, 0, null, null); }
        }, 900L);
    }

    private void captureStablePair(final WebView web, final String chooserTitle, final int attempt,
                                   final Bitmap previous, final BitmapStats previousStats) {
        if (web != renderWebView || web.getParent() == null) {
            recycle(previous);
            failAsync(new IllegalStateException("PDF WebView is not attached"));
            return;
        }
        if (attempt > MAX_STABILITY_ATTEMPTS) {
            recycle(previous);
            failAsync(new IllegalStateException("Drawing did not reach a stable rendered frame"));
            return;
        }

        Bitmap current = null;
        try {
            current = captureBitmap(web);
            BitmapStats currentStats = analyzeBitmap(current);
            validateBitmapStats(currentStats);

            if (previous != null && previousStats != null && framesStable(previousStats, currentStats)) {
                recycle(previous);
                writeBitmapPdfAndShare(current, chooserTitle);
                return;
            }

            recycle(previous);
            final Bitmap keep = current;
            final BitmapStats keepStats = currentStats;
            mainHandler.postDelayed(new Runnable() {
                @Override public void run() {
                    captureStablePair(web, chooserTitle, attempt + 1, keep, keepStats);
                }
            }, 350L);
        } catch (Throwable t) {
            recycle(current);
            recycle(previous);
            failAsync(t);
        }
    }

    private Bitmap captureBitmap(WebView web) {
        int wSpec = View.MeasureSpec.makeMeasureSpec(RENDER_W, View.MeasureSpec.EXACTLY);
        int hSpec = View.MeasureSpec.makeMeasureSpec(RENDER_H, View.MeasureSpec.EXACTLY);
        web.measure(wSpec, hSpec);
        web.layout(0, 0, RENDER_W, RENDER_H);
        web.invalidate();
        Bitmap bitmap = Bitmap.createBitmap(RENDER_W, RENDER_H, Bitmap.Config.ARGB_8888);
        Canvas capture = new Canvas(bitmap);
        capture.drawColor(0xFFFFFFFF);
        web.draw(capture);
        return bitmap;
    }

    private BitmapStats analyzeBitmap(Bitmap bitmap) {
        long nonWhite = 0L;
        long darkSum = 0L;
        long hash = 1469598103934665603L;
        int samples = 0;
        for (int y = 0; y < bitmap.getHeight(); y += 4) {
            for (int x = 0; x < bitmap.getWidth(); x += 4) {
                int c = bitmap.getPixel(x, y);
                int a = (c >>> 24) & 255;
                int r = (c >>> 16) & 255;
                int g = (c >>> 8) & 255;
                int b = c & 255;
                int darkness = 765 - (r + g + b);
                if (a > 20 && darkness > 30) {
                    nonWhite++;
                    darkSum += darkness;
                }
                hash ^= (long)c & 0xffffffffL;
                hash *= 1099511628211L;
                samples++;
            }
        }
        return new BitmapStats(nonWhite, darkSum, hash, samples);
    }

    private void validateBitmapStats(BitmapStats stats) {
        if (stats == null || stats.samples < 1000) throw new IllegalStateException("Drawing bitmap is invalid");
        if (stats.nonWhite < 350L || stats.darkSum < 50000L) {
            throw new IllegalStateException("Drawing renderer produced an incomplete or blank image");
        }
    }

    private boolean framesStable(BitmapStats a, BitmapStats b) {
        long countDiff = Math.abs(a.nonWhite - b.nonWhite);
        long darkDiff = Math.abs(a.darkSum - b.darkSum);
        long countTol = Math.max(4L, a.nonWhite / 200L);
        long darkTol = Math.max(1000L, a.darkSum / 200L);
        return countDiff <= countTol && darkDiff <= darkTol && a.hash == b.hash;
    }

    private void writeBitmapPdfAndShare(Bitmap bitmap, String chooserTitle) {
        PdfDocument pdf = null;
        FileOutputStream out = null;
        try {
            pdf = new PdfDocument();
            PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(842, 595, 1).create();
            PdfDocument.Page page = pdf.startPage(info);
            Canvas canvas = page.getCanvas();
            canvas.drawColor(0xFFFFFFFF);
            float margin = 22f;
            float scale = Math.min((842f - 2f * margin) / RENDER_W, (595f - 2f * margin) / RENDER_H);
            float dw = RENDER_W * scale;
            float dh = RENDER_H * scale;
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
            recycle(bitmap);
            sharePdf(file, chooserTitle);
        } catch (Throwable t) {
            if (out != null) try { out.close(); } catch (Throwable ignored) {}
            if (pdf != null) try { pdf.close(); } catch (Throwable ignored) {}
            recycle(bitmap);
            failAsync(t);
        }
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
        Toast.makeText(context, "GateCraft PDF ready", Toast.LENGTH_SHORT).show();
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
        String script =
            "<style>html,body{margin:0!important;padding:0!important;background:#fff!important;overflow:hidden!important}</style>" +
            "<script>(function(){" +
            "function gcErr(e){try{document.body.setAttribute('data-gc-pdf-error',String(e&&e.message?e.message:e));}catch(x){}}" +
            "try{" +
            "if(typeof bootTimer!=='undefined')clearInterval(bootTimer);" +
            "parseStart(" + quoted + ");render();if(!safeDraw())throw new Error('GateCraft safeDraw failed');" +
            "var s=document.getElementById('svg');if(!s)throw new Error('SVG missing');" +
            "var c=s.cloneNode(true);c.setAttribute('xmlns','http://www.w3.org/2000/svg');c.setAttribute('width','1200');c.setAttribute('height','700');c.setAttribute('viewBox','0 0 1200 700');" +
            "var css='';try{for(var i=0;i<document.styleSheets.length;i++){var rr=document.styleSheets[i].cssRules||[];for(var j=0;j<rr.length;j++)css+=rr[j].cssText+'\\n';}}catch(ignore){}" +
            "if(css){var d=document.createElementNS('http://www.w3.org/2000/svg','defs');var st=document.createElementNS('http://www.w3.org/2000/svg','style');st.setAttribute('type','text/css');st.textContent=css;d.appendChild(st);c.insertBefore(d,c.firstChild);}" +
            "var xml=new XMLSerializer().serializeToString(c);var img=new Image();" +
            "img.onload=function(){try{var cv=document.createElement('canvas');cv.id='gcPdfCanvas';cv.width=1200;cv.height=700;cv.style.width='1200px';cv.style.height='700px';cv.style.display='block';var cx=cv.getContext('2d');cx.fillStyle='#fff';cx.fillRect(0,0,1200,700);cx.drawImage(img,0,0,1200,700);document.body.innerHTML='';document.body.style.margin='0';document.body.style.padding='0';document.body.style.background='#fff';document.body.appendChild(cv);document.body.setAttribute('data-gc-pdf-ready','1');requestAnimationFrame(function(){requestAnimationFrame(function(){document.body.setAttribute('data-gc-pdf-painted','1');});});}catch(e){gcErr(e);}};" +
            "img.onerror=function(){gcErr('SVG rasterization failed');};" +
            "img.src='data:image/svg+xml;charset=utf-8,'+encodeURIComponent(xml);" +
            "}catch(e){gcErr(e);}" +
            "})();</script>";
        String lower = html.toLowerCase(Locale.ROOT);
        int pos = lower.lastIndexOf("</body>");
        return pos >= 0 ? html.substring(0, pos) + script + html.substring(pos) : html + script;
    }

    private String cleanJsValue(String v) {
        if (v == null) return "";
        String s = v;
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') s = s.substring(1, s.length() - 1);
        return s.replace("\\\"", "\"").replace("\\n", " ");
    }

    private void recycle(Bitmap bitmap) {
        if (bitmap != null && !bitmap.isRecycled()) {
            try { bitmap.recycle(); } catch (Throwable ignored) {}
        }
    }

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

    private static final class BitmapStats {
        final long nonWhite;
        final long darkSum;
        final long hash;
        final int samples;
        BitmapStats(long nonWhite, long darkSum, long hash, int samples) {
            this.nonWhite = nonWhite;
            this.darkSum = darkSum;
            this.hash = hash;
            this.samples = samples;
        }
    }
}
