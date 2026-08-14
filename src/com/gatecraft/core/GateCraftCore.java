package com.gatecraft.core;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Form;

@DesignerComponent(
    version = 1,
    description = "GateCraft shared calculation core.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true)
@SimpleObject(external = true)
public class GateCraftCore extends AndroidNonvisibleComponent {

  public GateCraftCore(Form form) {
    super(form);
  }

  @SimpleFunction(description = "Returns the GateCraftCore extension version.")
  public String Version() {
    return "0.1.0";
  }

  @SimpleFunction(description = "Simple build/runtime test. Returns PONG when the extension is working.")
  public String Ping() {
    return "PONG";
  }

  @SimpleFunction(description = "Rounds a number to two decimal places.")
  public double Round2(double value) {
    return Math.round(value * 100.0d) / 100.0d;
  }
}
