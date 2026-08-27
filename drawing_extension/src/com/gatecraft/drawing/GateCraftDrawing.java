package com.gatecraft.drawing;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
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
    private PrintDocumentAdapter printAdapter;
    private ParcelFileDescriptor printPfd;
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
        if (Build.VERSION.SDK_INT < 19) {
            lastError = "Android print PDF requires Android 4.4 or newer";
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
            final WebView web = new WebView(context);
            renderWebView = web;
            web.setBackgroundColor(0xFFFFFFFF);
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
                            printWebViewToPdf(web, chooserTitle);
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

    private void printWebViewToPdf(final WebView web, final String chooserTitle) {
        try {
            if (web != renderWebView) throw new IllegalStateException("PDF renderer replaced");
            final File outFile = createOutputFile();
            final PrintAttributes attrs = new PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4.asLandscape())
                    .setResolution(new PrintAttributes.Resolution("gc_pdf", "GateCraft PDF", 600, 600))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                    .build();
            final PrintDocumentAdapter adapter = Build.VERSION.SDK_INT >= 21
                    ? web.createPrintDocumentAdapter("GateCraft Drawing")
                    : web.createPrintDocumentAdapter();
            printAdapter = adapter;
            adapter.onLayout(attrs, attrs, new CancellationSignal(), new PrintDocumentAdapter.LayoutResultCallback() {
                @Override public void onLayoutFinished(PrintDocumentInfo info, boolean changed) {
                    try {
                        printPfd = ParcelFileDescriptor.open(outFile,
                                ParcelFileDescriptor.MODE_CREATE |
                                ParcelFileDescriptor.MODE_TRUNCATE |
                                ParcelFileDescriptor.MODE_READ_WRITE);
                        final ParcelFileDescriptor pfd = printPfd;
                        adapter.onWrite(new PageRange[]{PageRange.ALL_PAGES}, pfd, new CancellationSignal(),
                                new PrintDocumentAdapter.WriteResultCallback() {
                                    @Override public void onWriteFinished(PageRange[] pages) {
                                        closePrintPfd();
                                        mainHandler.post(new Runnable() {
                                            @Override public void run() {
                                                try {
                                                    validatePdf(outFile);
                                                    sharePdf(outFile, chooserTitle);
                                                } catch (Throwable t) {
                                                    failAsync(t);
                                                }
                                            }
                                        });
                                    }
                                    @Override public void onWriteFailed(CharSequence error) {
                                        closePrintPfd();
                                        failAsync(new IllegalStateException("PDF write failed: " + String.valueOf(error)));
                                    }
                                    @Override public void onWriteCancelled() {
                                        closePrintPfd();
                                        failAsync(new IllegalStateException("PDF write cancelled"));
                                    }
                                });
                    } catch (Throwable t) {
                        closePrintPfd();
                        failAsync(t);
                    }
                }
                @Override public void onLayoutFailed(CharSequence error) {
                    failAsync(new IllegalStateException("PDF layout failed: " + String.valueOf(error)));
                }
                @Override public void onLayoutCancelled() {
                    failAsync(new IllegalStateException("PDF layout cancelled"));
                }
            }, new Bundle());
        } catch (Throwable t) {
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
        if (file.length() < 2048L) throw new IllegalStateException("Generated PDF is unexpectedly empty (" + file.length() + " bytes)");
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

    private void closePrintPfd() {
        ParcelFileDescriptor p = printPfd;
        printPfd = null;
        if (p != null) try { p.close(); } catch (Throwable ignored) {}
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
        closePrintPfd();
        printAdapter = null;
        WebView w = renderWebView;
        renderWebView = null;
        if (w != null) {
            try { w.stopLoading(); } catch (Throwable ignored) {}
            try { w.loadUrl("about:blank"); } catch (Throwable ignored) {}
            try { w.clearHistory(); } catch (Throwable ignored) {}
            try { w.removeAllViews(); } catch (Throwable ignored) {}
            try { w.destroy(); } catch (Throwable ignored) {}
        }
    }
}
