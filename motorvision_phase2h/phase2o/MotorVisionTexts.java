package com.gatecraft.motorvision;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class MotorVisionTexts {
  static final String[] LANG={"hu","en","de","es","fr","zh","it","pt","pl","nl","ro","ru"};
  private static final Map<String,String[]> M=new LinkedHashMap<String,String[]>();

  private static void put(String key,String... values){
    if(values.length!=12)throw new RuntimeException("12-language contract: "+key);
    M.put(key,values);
  }

  static int langIndex(String code){
    if(code!=null){
      String c=code.trim().toLowerCase(Locale.ROOT);
      for(int i=0;i<LANG.length;i++)if(LANG[i].equals(c))return i;
    }
    return 1;
  }

  static String text(String key,int language){
    String[] a=M.get(key==null?"":key.trim().toLowerCase(Locale.ROOT));
    if(a==null)return "";
    if(language<0||language>=12)language=1;
    String s=a[language];
    return s==null||s.length()==0?a[1]:s;
  }

  static Set<String> keys(){return M.keySet();}

  static {
    put("title",
      "Vezérlés felismerése","Controller recognition","Steuerung erkennen","Reconocimiento de control",
      "Reconnaissance de commande","控制器识别","Riconoscimento centralina","Reconhecimento do controlador",
      "Rozpoznawanie sterownika","Besturing herkennen","Recunoaștere controler","Распознавание контроллера");
    put("identify",
      "Azonosítás","Identify","Erkennen","Identificar","Identifier","识别","Identifica","Identificar",
      "Identyfikuj","Identificeren","Identifică","Распознать");
    put("scan",
      "Kamera","Camera","Kamera","Cámara","Caméra","相机","Fotocamera","Câmara",
      "Kamera","Camera","Cameră","Камера");
    put("retry",
      "Újra","Retry","Erneut","Reintentar","Réessayer","重试","Riprova","Tentar novamente",
      "Ponów","Opnieuw","Reîncearcă","Повторить");
    put("analyzing",
      "Elemzés…","Analyzing…","Analyse…","Analizando…","Analyse…","正在分析…","Analisi…","A analisar…",
      "Analiza…","Analyseren…","Se analizează…","Анализ…");
    put("possible_matches",
      "Lehetséges találatok","Possible matches","Mögliche Treffer","Coincidencias posibles","Correspondances possibles",
      "可能的匹配","Possibili corrispondenze","Correspondências possíveis","Możliwe dopasowania",
      "Mogelijke matches","Potriviri posibile","Возможные совпадения");
    put("no_match",
      "Nincs megbízható találat","No reliable match","Kein zuverlässiger Treffer","Sin coincidencia fiable",
      "Aucune correspondance fiable","没有可靠的匹配","Nessuna corrispondenza affidabile","Sem correspondência fiável",
      "Brak wiarygodnego dopasowania","Geen betrouwbare match","Nicio potrivire sigură","Нет надёжного совпадения");
    put("select",
      "Kiválasztás","Select","Auswählen","Seleccionar","Sélectionner","选择","Seleziona","Selecionar",
      "Wybierz","Selecteren","Selectează","Выбрать");
    put("selected",
      "Kiválasztva","Selected","Ausgewählt","Seleccionado","Sélectionné","已选择","Selezionato","Selecionado",
      "Wybrano","Geselecteerd","Selectat","Выбрано");
    put("cancel",
      "Mégse","Cancel","Abbrechen","Cancelar","Annuler","取消","Annulla","Cancelar",
      "Anuluj","Annuleren","Anulează","Отмена");
    put("index_not_loaded",
      "A vezérléslista nincs betöltve.","Controller index is not loaded.","Steuerungsindex ist nicht geladen.",
      "El índice de controladores no está cargado.","L’index des contrôleurs n’est pas chargé.","控制器索引尚未加载。",
      "L’indice delle centraline non è caricato.","O índice de controladores não está carregado.",
      "Indeks sterowników nie jest załadowany.","De besturingsindex is niet geladen.",
      "Indexul controlerelor nu este încărcat.","Индекс контроллеров не загружен.");
    put("invalid_index",
      "Hibás vezérléslista.","Invalid controller index.","Ungültiger Steuerungsindex.","Índice de controladores no válido.",
      "Index des contrôleurs invalide.","控制器索引无效。","Indice delle centraline non valido.",
      "Índice de controladores inválido.","Nieprawidłowy indeks sterowników.","Ongeldige besturingsindex.",
      "Index de controlere invalid.","Недопустимый индекс контроллеров.");
    put("camera_unavailable",
      "A kamera nem érhető el.","Camera is unavailable.","Kamera ist nicht verfügbar.","La cámara no está disponible.",
      "La caméra n’est pas disponible.","相机不可用。","La fotocamera non è disponibile.","A câmara não está disponível.",
      "Kamera jest niedostępna.","Camera is niet beschikbaar.","Camera nu este disponibilă.","Камера недоступна.");
    put("score",
      "Egyezési pont","Match score","Trefferwert","Puntuación de coincidencia","Score de correspondance",
      "匹配分数","Punteggio corrispondenza","Pontuação de correspondência","Wynik dopasowania",
      "Matchscore","Scor potrivire","Оценка совпадения");
    put("ocr_failed",
      "A képfelismerés nem sikerült.","Image recognition failed.","Bilderkennung fehlgeschlagen.",
      "El reconocimiento de imagen ha fallado.","La reconnaissance d’image a échoué.","图像识别失败。",
      "Riconoscimento immagine non riuscito.","Falha no reconhecimento da imagem.","Rozpoznawanie obrazu nie powiodło się.",
      "Beeldherkenning mislukt.","Recunoașterea imaginii a eșuat.","Не удалось распознать изображение.");
    put("choose_one",
      "Válassz egy találatot.","Select one match.","Bitte einen Treffer auswählen.","Selecciona una coincidencia.",
      "Sélectionnez une correspondance.","请选择一个匹配项。","Seleziona una corrispondenza.",
      "Selecione uma correspondência.","Wybierz jedno dopasowanie.","Selecteer één resultaat.",
      "Selectează o potrivire.","Выберите одно совпадение.");
  }
}
