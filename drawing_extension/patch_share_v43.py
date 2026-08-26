from pathlib import Path
import re, sys

p = Path(sys.argv[1])
s = p.read_text(encoding='utf-8')

start = s.find('@SimpleFunction(description = "Creates an A4 landscape vector PDF')
end = s.find('@SimpleFunction(description = "Returns the last GateCraftDrawing error message.")', start)
if start < 0 or end < 0:
    raise SystemExit('CreateAndShareDrawingPDF markers not found')

replacement = r'''@SimpleFunction(description = "Creates an A4 landscape vector PDF from a GateCraft drawing payload and opens Android sharing. Downloads save is attempted first; cache FileProvider is used as fallback.")
  public boolean CreateAndShareDrawingPDF(String payload, String chooserTitle) {
    lastError = "";
    lastUri = null;
    try {
      if (payload == null || payload.trim().length() == 0) {
        throw new IllegalArgumentException("Empty drawing payload");
      }
      File dir = new File(context.getCacheDir(), "gatecraft_draw_pdf");
      if (!dir.exists() && !dir.mkdirs()) {
        throw new IllegalStateException("PDF cache directory could not be created");
      }
      String type = field(payload, 0, "DRAWING");
      String project = tag(payload, "PROJECT", "");
      String base = "GateCraft_" + (project.length() > 0 ? safe(project) + "_" : "") + safe(type) + "_Drawing.pdf";
      File tmp = new File(dir, base);
      render(payload, tmp);
      if (!tmp.isFile() || tmp.length() < 100) {
        throw new IllegalStateException("Generated PDF is empty");
      }
      lastTemp = tmp;

      Uri shareUri = null;
      try {
        shareUri = publish(tmp);
      } catch (Throwable saveEx) {
        lastError = "SAVE_FALLBACK: " + shareMessage(saveEx);
      }
      if (shareUri == null) {
        shareUri = com.google.appinventor.components.runtime.util.NougatUtil.getPackageUri(form, tmp);
      }
      if (shareUri == null) {
        throw new IllegalStateException("Could not create share URI");
      }
      lastUri = shareUri;

      Intent send = new Intent(Intent.ACTION_SEND);
      send.setType("application/pdf");
      send.putExtra(Intent.EXTRA_STREAM, shareUri);
      send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      Intent chooser = Intent.createChooser(send, chooserTitle == null ? "" : chooserTitle);
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
      form.startActivity(chooser);
      android.widget.Toast.makeText(context, shareUi(payload, true), android.widget.Toast.LENGTH_SHORT).show();
      return true;
    } catch (Throwable ex) {
      lastError = ex.getClass().getSimpleName() + ": " + shareMessage(ex);
      try {
        android.widget.Toast.makeText(context, shareUi(payload, false) + "\n" + lastError, android.widget.Toast.LENGTH_LONG).show();
      } catch (Throwable ignored) { }
      return false;
    }
  }

  private static String shareMessage(Throwable ex) {
    if (ex == null) return "";
    String m = ex.getMessage();
    return m == null ? "" : m;
  }

  private static String shareUi(String payload, boolean ok) {
    int l = lang(payload);
    String[] yes = {"PDF elkészült – megosztás megnyitva.","PDF created – sharing opened.","PDF erstellt – Teilen geöffnet.","PDF creado – compartir abierto.","PDF créé – partage ouvert.","PDF 已生成 – 已打开分享。","PDF creato – condivisione aperta.","PDF criado – partilha aberta.","PDF utworzony – otwarto udostępnianie.","PDF gemaakt – delen geopend.","PDF creat – partajarea a fost deschisă.","PDF создан – открыта отправка."};
    String[] no = {"A PDF megosztása sikertelen:","PDF sharing failed:","PDF-Teilen fehlgeschlagen:","Error al compartir PDF:","Échec du partage PDF :","PDF 分享失败：","Condivisione PDF non riuscita:","Falha ao partilhar PDF:","Udostępnianie PDF nie powiodło się:","PDF delen mislukt:","Partajarea PDF a eșuat:","Не удалось отправить PDF:"};
    int i = l < 1 || l > 12 ? 1 : l - 1;
    return ok ? yes[i] : no[i];
  }

  '''
s = s[:start] + replacement + s[end:]
# Keep App Inventor editor metadata compatibility while allowing runtime patch builds.
s = s.replace('version = 2,', 'version = 2,', 1)
s = s.replace('return "2.0.0";', 'return "2.1.0";', 1)
p.write_text(s, encoding='utf-8')
print('patched', p)
