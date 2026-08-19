package com.gatecraft.pdf;

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
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.Form;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@DesignerComponent(
    version = 1,
    description = "GateCraft native PDF generator for offers, summaries and reports.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true)
@SimpleObject(external = true)
public class GateCraftPDF extends AndroidNonvisibleComponent {

  private static final int PAGE_W = 595;
  private static final int PAGE_H = 842;
  private static final float MARGIN_L = 40f;
  private static final float MARGIN_R = 40f;
  private static final float MARGIN_B = 42f;
  private static final float CONTENT_W = PAGE_W - MARGIN_L - MARGIN_R;

  private final Context context;
  private final Form form;
  private String appName = "GateCraft";
  private File lastTempFile;
  private Uri lastSavedUri;
  private String lastFileName = "";
  private int lastPageCount = 0;
  private String lastErrorCode = "";
  private String lastErrorMessage = "";

  private static final class RenderState {
    PdfDocument pdf;
    PdfDocument.Page page;
    Canvas canvas;
    Paint normal;
    Paint bold;
    Paint small;
    Paint line;
    int pageNo;
    float y;
    JSONObject root;
    String title;
  }

  public GateCraftPDF(ComponentContainer container) {
    super(container.$form());
    this.form = container.$form();
    this.context = container.$context();
  }

  @SimpleFunction(description = "Initializes the GateCraft PDF engine. Does not request storage permission.")
  public void Initialize(String applicationName) {
    if (applicationName != null && applicationName.trim().length() > 0) {
      appName = applicationName.trim();
    }
    clearError();
  }

  @SimpleFunction(description = "Returns the GateCraftPDF extension version.")
  public String Version() {
    return "1.0.0";
  }

  @SimpleFunction(description = "Creates a PDF from GateCraft document JSON in app cache. Returns the temporary file path or empty string on error.")
  public String CreatePDF(String documentJson) {
    clearError();
    try {
      JSONObject root = new JSONObject(documentJson == null ? "{}" : documentJson);
      File dir = new File(context.getCacheDir(), "gatecraft_pdf");
      if (!dir.exists() && !dir.mkdirs()) {
        return fail("CACHE_DIR", "Could not create GateCraft PDF cache directory.");
      }
      String baseName = sanitizeFileName(text(opt(root, "fileName"), "GateCraft_Document"));
      if (!baseName.toLowerCase(Locale.US).endsWith(".pdf")) baseName += ".pdf";
      File out = new File(dir, baseName);
      renderDocument(root, out);
      lastTempFile = out;
      lastSavedUri = null;
      lastFileName = out.getName();
      PDFCreated(out.getAbsolutePath(), lastFileName, lastPageCount);
      return out.getAbsolutePath();
    } catch (Throwable ex) {
      return fail("CREATE_FAILED", message(ex));
    }
  }

  @SimpleFunction(description = "Creates a multi-page Hungarian sample offer for runtime testing. Returns the temporary file path.")
  public String CreateSampleOfferPDF() {
    try {
      JSONObject root = new JSONObject();
      root.put("fileName", "GateCraftPDF_Test.pdf");
      JSONObject company = new JSONObject();
      company.put("name", "GateCraft Teszt Kft.");
      company.put("address", "8156 Kisláng, Teszt utca 1.");
      company.put("phone", "+36 30 123 4567");
      company.put("email", "teszt@gatecraft.app");
      company.put("taxNumber", "12345678-1-07");
      root.put("company", company);
      JSONObject customer = new JSONObject();
      customer.put("name", "Minta Ügyfél");
      customer.put("address", "8000 Székesfehérvár, Példa utca 10.");
      customer.put("phone", "+36 30 000 0000");
      customer.put("email", "ugyfel@example.com");
      root.put("customer", customer);
      JSONObject document = new JSONObject();
      document.put("title", "AJÁNLAT");
      document.put("number", "GC-TEST-001");
      document.put("date", "2026.08.19.");
      document.put("validUntil", "2026.09.02.");
      document.put("currency", "Ft");
      root.put("document", document);
      JSONArray items = new JSONArray();
      for (int i = 1; i <= 32; i++) {
        JSONObject item = new JSONObject();
        item.put("name", i + ". próbatétel – hosszabb magyar szöveg, ékezetek: őűáéíóöü");
        item.put("quantity", (i % 4) + 1);
        item.put("unit", "db");
        item.put("unitPrice", (12000 + i * 750) + " Ft");
        item.put("total", ((i % 4) + 1) * (12000 + i * 750) + " Ft");
        items.put(item);
      }
      root.put("items", items);
      JSONObject totals = new JSONObject();
      totals.put("net", "755 000 Ft");
      totals.put("vat", "203 850 Ft");
      totals.put("gross", "958 850 Ft");
      root.put("totals", totals);
      root.put("notes", "Ez egy GateCraftPDF tesztdokumentum.\nA cél a magyar ékezetek, a többoldalas tördelés, a Downloads mentés, a megnyitás és a megosztás ellenőrzése.");
      JSONObject labels = new JSONObject();
      labels.put("customer", "Ügyfél");
      labels.put("documentNumber", "Ajánlatszám");
      labels.put("date", "Dátum");
      labels.put("validUntil", "Érvényes");
      labels.put("description", "Megnevezés");
      labels.put("quantity", "Menny.");
      labels.put("unit", "Egység");
      labels.put("unitPrice", "Egységár");
      labels.put("total", "Összeg");
      labels.put("net", "Nettó");
      labels.put("vat", "ÁFA");
      labels.put("gross", "Bruttó végösszeg");
      labels.put("notes", "Megjegyzés");
      labels.put("page", "Oldal");
      root.put("labels", labels);
      return CreatePDF(root.toString());
    } catch (Throwable ex) {
      return fail("SAMPLE_FAILED", message(ex));
    }
  }

  @SimpleFunction(description = "Saves the last generated PDF to Downloads/GateCraft on Android 10 or newer. Returns a content URI.")
  public String SaveToDownloads(String fileName) {
    clearError();
    if (lastTempFile == null || !lastTempFile.isFile()) {
      return fail("NO_PDF", "Create a PDF before saving.");
    }
    if (Build.VERSION.SDK_INT < 29) {
      return fail("ANDROID_VERSION", "Downloads saving without legacy storage permission requires Android 10 or newer.");
    }
    Uri uri = null;
    try {
      String name = sanitizeFileName(fileName == null || fileName.trim().length() == 0 ? lastTempFile.getName() : fileName.trim());
      if (!name.toLowerCase(Locale.US).endsWith(".pdf")) name += ".pdf";
      ContentResolver resolver = context.getContentResolver();
      ContentValues values = new ContentValues();
      values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
      values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
      values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/GateCraft");
      values.put(MediaStore.MediaColumns.IS_PENDING, 1);
      uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
      if (uri == null) return fail("MEDIASTORE_INSERT", "Android MediaStore did not return a destination URI.");
      OutputStream out = resolver.openOutputStream(uri, "w");
      if (out == null) throw new IllegalStateException("Could not open destination output stream.");
      copy(lastTempFile, out);
      out.close();
      ContentValues done = new ContentValues();
      done.put(MediaStore.MediaColumns.IS_PENDING, 0);
      resolver.update(uri, done, null, null);
      lastSavedUri = uri;
      lastFileName = name;
      PDFSaved(uri.toString(), name);
      return uri.toString();
    } catch (Throwable ex) {
      if (uri != null) {
        try { context.getContentResolver().delete(uri, null, null); } catch (Throwable ignored) { }
      }
      return fail("SAVE_FAILED", message(ex));
    }
  }

  @SimpleFunction(description = "Opens the last PDF saved to Downloads using an installed PDF viewer.")
  public boolean OpenLastPDF() {
    clearError();
    if (lastSavedUri == null) {
      fail("NO_SAVED_PDF", "Save the PDF before opening it.");
      return false;
    }
    try {
      Intent i = new Intent(Intent.ACTION_VIEW);
      i.setDataAndType(lastSavedUri, "application/pdf");
      i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
      form.startActivity(i);
      PDFOpened(lastSavedUri.toString());
      return true;
    } catch (Throwable ex) {
      fail("OPEN_FAILED", message(ex));
      return false;
    }
  }

  @SimpleFunction(description = "Shares the last PDF saved to Downloads through the Android share sheet.")
  public boolean ShareLastPDF(String chooserTitle, String message) {
    clearError();
    if (lastSavedUri == null) {
      fail("NO_SAVED_PDF", "Save the PDF before sharing it.");
      return false;
    }
    try {
      Intent send = new Intent(Intent.ACTION_SEND);
      send.setType("application/pdf");
      send.putExtra(Intent.EXTRA_STREAM, lastSavedUri);
      if (message != null && message.length() > 0) send.putExtra(Intent.EXTRA_TEXT, message);
      send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      Intent chooser = Intent.createChooser(send, chooserTitle == null ? "" : chooserTitle);
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      form.startActivity(chooser);
      PDFShared(lastSavedUri.toString());
      return true;
    } catch (Throwable ex) {
      fail("SHARE_FAILED", message(ex));
      return false;
    }
  }

  @SimpleFunction(description = "Deletes the temporary cached PDF. A copy already saved to Downloads is not deleted.")
  public boolean DeleteTemporaryPDF() {
    clearError();
    if (lastTempFile == null) return true;
    try {
      boolean ok = !lastTempFile.exists() || lastTempFile.delete();
      if (ok) lastTempFile = null;
      return ok;
    } catch (Throwable ex) {
      fail("DELETE_FAILED", message(ex));
      return false;
    }
  }

  @SimpleFunction(description = "Returns the content URI of the last PDF saved to Downloads.")
  public String LastPDFUri() { return lastSavedUri == null ? "" : lastSavedUri.toString(); }

  @SimpleFunction(description = "Returns the private temporary path of the last generated PDF.")
  public String LastTempPath() { return lastTempFile == null ? "" : lastTempFile.getAbsolutePath(); }

  @SimpleFunction(description = "Returns the last generated or saved file name.")
  public String LastFileName() { return lastFileName; }

  @SimpleFunction(description = "Returns the page count of the last generated PDF.")
  public double LastPageCount() { return lastPageCount; }

  @SimpleFunction(description = "Returns the last error code, or empty string.")
  public String LastErrorCode() { return lastErrorCode; }

  @SimpleFunction(description = "Returns the last error message, or empty string.")
  public String LastError() { return lastErrorMessage; }

  @SimpleEvent(description = "Raised after a PDF is successfully generated in app cache.")
  public void PDFCreated(String tempPath, String fileName, int pageCount) {
    EventDispatcher.dispatchEvent(this, "PDFCreated", tempPath, fileName, pageCount);
  }

  @SimpleEvent(description = "Raised after a PDF is successfully saved to Downloads/GateCraft.")
  public void PDFSaved(String uri, String fileName) {
    EventDispatcher.dispatchEvent(this, "PDFSaved", uri, fileName);
  }

  @SimpleEvent(description = "Raised after the saved PDF is opened.")
  public void PDFOpened(String uri) {
    EventDispatcher.dispatchEvent(this, "PDFOpened", uri);
  }

  @SimpleEvent(description = "Raised after the Android share sheet is opened for the saved PDF.")
  public void PDFShared(String uri) {
    EventDispatcher.dispatchEvent(this, "PDFShared", uri);
  }

  @SimpleEvent(description = "Raised when an operation fails.")
  public void Error(String code, String message) {
    EventDispatcher.dispatchEvent(this, "Error", code, message);
  }

  private void renderDocument(JSONObject root, File out) throws Exception {
    RenderState s = new RenderState();
    s.root = root;
    s.title = text(opt(object(root, "document"), "title"), text(opt(root, "title"), "GateCraft PDF"));
    s.pdf = new PdfDocument();
    s.normal = paint(10f, false, Color.BLACK);
    s.bold = paint(10f, true, Color.BLACK);
    s.small = paint(8f, false, Color.DKGRAY);
    s.line = paint(1f, false, 0xFFBDBDBD);
    startPage(s, true);
    drawDocumentInfo(s);
    drawItems(s);
    drawTotals(s);
    drawNotes(s);
    finishPage(s);
    lastPageCount = s.pageNo;
    FileOutputStream fos = new FileOutputStream(out);
    try { s.pdf.writeTo(fos); } finally { try { fos.close(); } catch (Throwable ignored) {} s.pdf.close(); }
  }

  private void startPage(RenderState s, boolean first) {
    s.pageNo++;
    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, s.pageNo).create();
    s.page = s.pdf.startPage(info);
    s.canvas = s.page.getCanvas();
    drawHeader(s);
    s.y = 102f;
    if (!first) drawTableHeader(s);
  }

  private void finishPage(RenderState s) {
    if (s.page == null) return;
    String pageLabel = label(s.root, "page", "Page") + " " + s.pageNo;
    s.canvas.drawText(pageLabel, PAGE_W - MARGIN_R - s.small.measureText(pageLabel), PAGE_H - 20f, s.small);
    s.pdf.finishPage(s.page);
    s.page = null;
    s.canvas = null;
  }

  private void newPage(RenderState s, boolean withTableHeader) {
    finishPage(s);
    s.pageNo++;
    PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, s.pageNo).create();
    s.page = s.pdf.startPage(info);
    s.canvas = s.page.getCanvas();
    drawHeader(s);
    s.y = 102f;
    if (withTableHeader) drawTableHeader(s);
  }

  private void drawHeader(RenderState s) {
    JSONObject company = object(s.root, "company");
    Paint titlePaint = paint(19f, true, Color.BLACK);
    Paint companyPaint = paint(12f, true, Color.BLACK);
    String companyName = text(opt(company, "name"), appName);
    s.canvas.drawText(companyName, MARGIN_L, 44f, companyPaint);
    String title = s.title == null ? "" : s.title;
    s.canvas.drawText(title, PAGE_W - MARGIN_R - titlePaint.measureText(title), 44f, titlePaint);
    float y = 60f;
    for (String key : new String[]{"address", "phone", "email", "taxNumber"}) {
      String v = text(opt(company, key), "");
      if (v.length() > 0) { s.canvas.drawText(v, MARGIN_L, y, s.small); y += 11f; }
    }
    s.canvas.drawLine(MARGIN_L, 90f, PAGE_W - MARGIN_R, 90f, s.line);
  }

  private void drawDocumentInfo(RenderState s) {
    JSONObject customer = object(s.root, "customer");
    JSONObject doc = object(s.root, "document");
    float leftX = MARGIN_L;
    float rightX = 340f;
    s.canvas.drawText(label(s.root, "customer", "Customer") + ":", leftX, s.y, s.bold);
    s.y += 16f;
    for (String key : new String[]{"name", "address", "phone", "email"}) {
      String v = text(opt(customer, key), "");
      if (v.length() > 0) { s.canvas.drawText(v, leftX, s.y, s.normal); s.y += 14f; }
    }
    float rightY = 110f;
    rightY = drawMetaLine(s, rightX, rightY, label(s.root, "documentNumber", "Document no."), text(opt(doc, "number"), ""));
    rightY = drawMetaLine(s, rightX, rightY, label(s.root, "date", "Date"), text(opt(doc, "date"), ""));
    rightY = drawMetaLine(s, rightX, rightY, label(s.root, "validUntil", "Valid until"), text(opt(doc, "validUntil"), ""));
    s.y = Math.max(s.y + 8f, rightY + 8f);
    ensure(s, 40f, false);
    drawTableHeader(s);
  }

  private float drawMetaLine(RenderState s, float x, float y, String k, String v) {
    if (v == null || v.length() == 0) return y;
    s.canvas.drawText(k + ":", x, y, s.bold);
    s.canvas.drawText(v, x, y + 13f, s.normal);
    return y + 30f;
  }

  private void drawTableHeader(RenderState s) {
    float top = s.y;
    s.canvas.drawRect(MARGIN_L, top - 12f, PAGE_W - MARGIN_R, top + 8f, paint(1f, false, 0xFFEFEFEF));
    s.canvas.drawText(label(s.root, "description", "Description"), MARGIN_L + 4f, top + 2f, s.bold);
    s.canvas.drawText(label(s.root, "quantity", "Qty"), 286f, top + 2f, s.bold);
    s.canvas.drawText(label(s.root, "unit", "Unit"), 334f, top + 2f, s.bold);
    s.canvas.drawText(label(s.root, "unitPrice", "Unit price"), 382f, top + 2f, s.bold);
    s.canvas.drawText(label(s.root, "total", "Total"), 475f, top + 2f, s.bold);
    s.y = top + 18f;
  }

  private void drawItems(RenderState s) {
    JSONArray items = array(s.root, "items");
    for (int i = 0; i < items.length(); i++) {
      JSONObject item = items.optJSONObject(i);
      if (item == null) continue;
      String name = text(opt(item, "name"), "");
      List<String> nameLines = wrap(name, s.normal, 238f);
      float rowH = Math.max(20f, 13f * nameLines.size() + 8f);
      ensure(s, rowH, true);
      float top = s.y;
      for (int k = 0; k < nameLines.size(); k++) s.canvas.drawText(nameLines.get(k), MARGIN_L + 4f, top + 11f + k * 13f, s.normal);
      s.canvas.drawText(text(opt(item, "quantity"), ""), 286f, top + 11f, s.normal);
      s.canvas.drawText(text(opt(item, "unit"), ""), 334f, top + 11f, s.normal);
      drawRight(s, text(opt(item, "unitPrice"), ""), 466f, top + 11f, s.normal);
      drawRight(s, text(opt(item, "total"), ""), PAGE_W - MARGIN_R - 4f, top + 11f, s.normal);
      s.canvas.drawLine(MARGIN_L, top + rowH - 2f, PAGE_W - MARGIN_R, top + rowH - 2f, s.line);
      s.y = top + rowH;
    }
  }

  private void drawTotals(RenderState s) {
    JSONObject totals = object(s.root, "totals");
    ensure(s, 80f, false);
    s.y += 8f;
    float labelX = 350f;
    for (String[] row : new String[][]{{"net", "net", "Net"}, {"vat", "vat", "VAT"}, {"gross", "gross", "Gross total"}}) {
      String value = text(opt(totals, row[0]), "");
      if (value.length() == 0) continue;
      Paint p = "gross".equals(row[0]) ? paint(11f, true, Color.BLACK) : s.normal;
      s.canvas.drawText(label(s.root, row[1], row[2]) + ":", labelX, s.y + 12f, p);
      drawRight(s, value, PAGE_W - MARGIN_R, s.y + 12f, p);
      s.y += 20f;
    }
  }

  private void drawNotes(RenderState s) {
    String notes = text(opt(s.root, "notes"), "");
    if (notes.length() == 0) return;
    ensure(s, 42f, false);
    s.y += 12f;
    s.canvas.drawText(label(s.root, "notes", "Notes") + ":", MARGIN_L, s.y, s.bold);
    s.y += 16f;
    String[] paras = notes.split("\\n", -1);
    for (String para : paras) {
      List<String> lines = wrap(para, s.normal, CONTENT_W);
      if (lines.size() == 0) lines.add("");
      for (String line : lines) {
        ensure(s, 16f, false);
        s.canvas.drawText(line, MARGIN_L, s.y, s.normal);
        s.y += 14f;
      }
    }
  }

  private void ensure(RenderState s, float needed, boolean tableContinuation) {
    if (s.y + needed <= PAGE_H - MARGIN_B) return;
    newPage(s, tableContinuation);
  }

  private static Paint paint(float size, boolean bold, int color) {
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    p.setColor(color);
    p.setTextSize(size);
    p.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
    return p;
  }

  private static void drawRight(RenderState s, String value, float x, float y, Paint p) {
    s.canvas.drawText(value == null ? "" : value, x - p.measureText(value == null ? "" : value), y, p);
  }

  private static List<String> wrap(String text, Paint p, float maxWidth) {
    ArrayList<String> out = new ArrayList<String>();
    if (text == null || text.length() == 0) { out.add(""); return out; }
    String[] words = text.trim().split("\\s+");
    String line = "";
    for (String word : words) {
      String candidate = line.length() == 0 ? word : line + " " + word;
      if (p.measureText(candidate) <= maxWidth) {
        line = candidate;
      } else {
        if (line.length() > 0) out.add(line);
        if (p.measureText(word) <= maxWidth) {
          line = word;
        } else {
          String rest = word;
          while (rest.length() > 0) {
            int cut = rest.length();
            while (cut > 1 && p.measureText(rest.substring(0, cut)) > maxWidth) cut--;
            out.add(rest.substring(0, cut));
            rest = rest.substring(cut);
          }
          line = "";
        }
      }
    }
    if (line.length() > 0) out.add(line);
    return out;
  }

  private static void copy(File src, OutputStream out) throws Exception {
    InputStream in = new FileInputStream(src);
    try {
      byte[] b = new byte[16384];
      int n;
      while ((n = in.read(b)) >= 0) if (n > 0) out.write(b, 0, n);
      out.flush();
    } finally { try { in.close(); } catch (Throwable ignored) {} }
  }

  private static JSONObject object(JSONObject o, String key) {
    JSONObject x = o == null ? null : o.optJSONObject(key);
    return x == null ? new JSONObject() : x;
  }

  private static JSONArray array(JSONObject o, String key) {
    JSONArray x = o == null ? null : o.optJSONArray(key);
    return x == null ? new JSONArray() : x;
  }

  private static Object opt(JSONObject o, String key) {
    return o == null ? null : o.opt(key);
  }

  private static String text(Object v, String fallback) {
    if (v == null || v == JSONObject.NULL) return fallback == null ? "" : fallback;
    String s = String.valueOf(v);
    return s.length() == 0 ? (fallback == null ? "" : fallback) : s;
  }

  private static String label(JSONObject root, String key, String fallback) {
    JSONObject labels = object(root, "labels");
    return text(opt(labels, key), fallback);
  }

  private static String sanitizeFileName(String name) {
    String n = name == null ? "GateCraft_Document.pdf" : name.trim();
    n = n.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_");
    while (n.contains("__")) n = n.replace("__", "_");
    if (n.length() == 0) n = "GateCraft_Document.pdf";
    if (n.length() > 120) n = n.substring(0, 120);
    return n;
  }

  private void clearError() { lastErrorCode = ""; lastErrorMessage = ""; }

  private String fail(String code, String msg) {
    lastErrorCode = code == null ? "ERROR" : code;
    lastErrorMessage = msg == null ? "" : msg;
    Error(lastErrorCode, lastErrorMessage);
    return "";
  }

  private static String message(Throwable ex) {
    if (ex == null) return "Unknown error";
    String m = ex.getMessage();
    return ex.getClass().getSimpleName() + (m == null || m.length() == 0 ? "" : ": " + m);
  }
}
