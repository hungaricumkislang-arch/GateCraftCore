package com.gatecraft.motorvision;

import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesServices;
import com.google.appinventor.components.annotations.UsesApplicationMetadata;
import com.google.appinventor.components.annotations.androidmanifest.ServiceElement;
import com.google.appinventor.components.annotations.androidmanifest.MetaDataElement;
import com.google.appinventor.components.common.ComponentCategory;
import java.util.Locale;

@DesignerComponent(version=2, description="Offline GateCraft MotorDiag controller recognition core with bundled image OCR, 12-language and 4-theme contract.", category=ComponentCategory.EXTENSION, nonVisible=true, iconName="")
@SimpleObject(external=true)
@UsesLibraries(libraries="motorvision-ocr-runtime.aar")
@UsesServices(services={
  @ServiceElement(
    name="com.google.mlkit.common.internal.MlKitComponentDiscoveryService",
    directBootAware="true",
    exported="false",
    metaDataElements={
      @MetaDataElement(name="com.google.firebase.components:com.google.mlkit.common.internal.CommonComponentRegistrar", value="com.google.firebase.components.ComponentRegistrar"),
      @MetaDataElement(name="com.google.firebase.components:com.google.mlkit.vision.common.internal.VisionCommonRegistrar", value="com.google.firebase.components.ComponentRegistrar"),
      @MetaDataElement(name="com.google.firebase.components:com.google.mlkit.vision.text.internal.TextRegistrar", value="com.google.firebase.components.ComponentRegistrar")
    }
  )
})
@UsesApplicationMetadata(metaDataElements={
  @MetaDataElement(name="com.google.mlkit.vision.DEPENDENCIES", value="ocr")
})
public class GateCraftMotorVision extends AndroidNonvisibleComponent {
  private final MotorVisionMatcherCore matcher=new MotorVisionMatcherCore();
  private final android.content.Context context;
  private MotorVisionMlKitOcr ocr;
  private boolean ocrBusy=false;
  private int language=0; // HU default, GateCraft 1..12 maps to 0..11
  private String theme="default";
  private String lastError="";

  public GateCraftMotorVision(ComponentContainer container){
    super(container.$form());
    context=container.$context().getApplicationContext();
  }
  @SimpleFunction
  public String Version(){return "1.2.0-phase2k";}
  @SimpleFunction
  public String Schema(){return MotorVisionMatcherCore.SCHEMA;}

  @SimpleFunction
  public void SetLanguageIndex(double value){int n=(int)Math.round(value);language=(n>=1&&n<=12)?n-1:1;}
  @SimpleFunction
  public void SetLanguageCode(String code){language=MotorVisionTexts.langIndex(code);}
  @SimpleFunction
  public double LanguageIndex(){return language+1;}
  @SimpleFunction
  public String LanguageCode(){return MotorVisionTexts.LANG[language];}
  @SimpleFunction
  public String UiText(String key){return MotorVisionTexts.text(key,language);}

  @SimpleFunction
  public void SetTheme(String value){String v=value==null?"default":value.trim().toLowerCase(Locale.ROOT);theme=("marka1".equals(v)||"marka2".equals(v)||"marka3".equals(v))?v:"default";}
  @SimpleFunction
  public String Theme(){return theme;}
  @SimpleFunction
  public String ThemeBackgroundImage(){if("marka1".equals(theme))return "MAKITAA.jpg";if("marka2".equals(theme))return "Image-1(1).jpg";if("marka3".equals(theme))return "milwoukeee.jpg";return "ALAP.jpg";}
  @SimpleFunction
  public String ThemeButtonImage(){if("marka1".equals(theme))return "btn_makita.png";if("marka2".equals(theme))return "btn_marka2.png";if("marka3".equals(theme))return "btn_marka3.png";return "btn_alapd.jpg";}
  @SimpleFunction
  public String ThemeBackButtonImage(){if("marka1".equals(theme))return "back_marka1.png";if("marka2".equals(theme))return "back_marka2.png";if("marka3".equals(theme))return "back_marka3.png";return "back_ALAP.jpg";}
  @SimpleFunction
  public double ThemeTextColor(){return ("marka1".equals(theme)||"marka3".equals(theme))?-1.0:-16777216.0;}

  @SimpleFunction
  public double LoadControllerIndex(String json){try{lastError="";return matcher.loadIndex(json);}catch(Throwable t){lastError=messageOf(t);return 0;}}
  @SimpleFunction
  public boolean IndexLoaded(){return matcher.size()>0;}
  @SimpleFunction
  public double ControllerCount(){return matcher.size();}
  @SimpleFunction
  public String DatabaseVersion(){return matcher.dbVersion();}
  @SimpleFunction
  public String NormalizeText(String text){return MotorVisionMatcherCore.normalize(text);}
  @SimpleFunction
  public String CleanOcrText(String text){return MotorVisionOcrText.clean(text);}

  @SimpleFunction
  public String IdentifyFromOcrText(String text){return IdentifyFromText(MotorVisionOcrText.clean(text));}

  @SimpleFunction
  public String IdentifyFromText(String text){
    if(!IndexLoaded()){lastError=UiText("index_not_loaded");return emptyMatches();}
    try{lastError="";matcher.identify(text);return matcher.matchesJson();}catch(Throwable t){lastError=messageOf(t);return emptyMatches();}
  }
  @SimpleFunction
  public String TopMatchesJson(){return matcher.matchesJson();}
  @SimpleFunction
  public String BestControllerId(){return matcher.bestControllerId();}
  @SimpleFunction
  public double BestScore(){return matcher.bestScore();}
  @SimpleFunction
  public String LastError(){return lastError;}

  @SimpleFunction
  public boolean OcrBusy(){return ocrBusy;}

  @SimpleFunction
  public void RecognizeImage(String imagePath){
    if(ocrBusy){
      lastError="OCR busy";
      OcrFailed(lastError);
      return;
    }
    ocrBusy=true;
    lastError="";
    try{
      if(ocr==null)ocr=new MotorVisionMlKitOcr(context);
      ocr.recognize(imagePath,new MotorVisionMlKitOcr.Callback(){
        public void ok(String text){
          ocrBusy=false;
          String raw=text==null?"":text;
          String cleaned=MotorVisionOcrText.clean(raw);
          OcrRecognized(raw,cleaned);
        }
        public void fail(String message){
          ocrBusy=false;
          lastError=(message==null||message.length()==0)?"OCR failed":message;
          OcrFailed(lastError);
        }
      });
    }catch(Throwable t){
      ocrBusy=false;
      lastError=messageOf(t);
      OcrFailed(lastError);
    }
  }

  @SimpleFunction
  public void CloseOcr(){
    try{if(ocr!=null)ocr.close();}catch(Throwable ignored){}
    ocr=null;
    ocrBusy=false;
  }

  @SimpleEvent
  public void OcrRecognized(String rawText,String cleanedText){
    EventDispatcher.dispatchEvent(this,"OcrRecognized",rawText,cleanedText);
  }

  @SimpleEvent
  public void OcrFailed(String message){
    EventDispatcher.dispatchEvent(this,"OcrFailed",message);
  }

  private static String emptyMatches(){return "{\"schema\":\"gatecraft.motorvision.matches.v1\",\"matches\":[]}";}
  private static String messageOf(Throwable t){return t==null?"Unknown error":(t.getMessage()==null?t.getClass().getSimpleName():t.getMessage());}
}
