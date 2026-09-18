package com.gatecraft.motorvision;
import android.net.Uri;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;
public final class MotorVisionMlKitOcr {
 public interface Callback { void ok(String text); void fail(String message); }
 private final android.content.Context context;
 private final TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
 public MotorVisionMlKitOcr(android.content.Context context){this.context=context;}
 public void recognize(String imagePath, final Callback cb){
  try{
   String raw=imagePath==null?"":imagePath.trim();
   if(raw.length()==0){cb.fail("Empty image path");return;}
   Uri uri=(raw.startsWith("content://")||raw.startsWith("file://"))?Uri.parse(raw):Uri.fromFile(new java.io.File(raw));
   InputImage image=InputImage.fromFilePath(context,uri);
   recognizer.process(image).addOnSuccessListener(new OnSuccessListener<Text>(){public void onSuccess(Text r){cb.ok(r==null?"":r.getText());}})
    .addOnFailureListener(new OnFailureListener(){public void onFailure(Exception e){cb.fail(e==null?"OCR failed":String.valueOf(e.getMessage()));}});
  }catch(IOException e){cb.fail(e.getMessage()==null?"Image read failed":e.getMessage());}
 }
 public void close(){recognizer.close();}
}
