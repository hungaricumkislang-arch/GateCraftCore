package com.gatecraft.core;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Form;
import com.google.appinventor.components.runtime.util.YailList;

@DesignerComponent(
    version = 2,
    description = "GateCraft shared calculation core.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true)
@SimpleObject(external = true)
public class GateCraftCore extends AndroidNonvisibleComponent {

  private static final double PI = 3.14159d;

  public GateCraftCore(Form form) {
    super(form);
  }

  @SimpleFunction(description = "Returns the GateCraftCore extension version.")
  public String Version() {
    return "0.2.0";
  }

  @SimpleFunction(description = "Simple build/runtime test. Returns PONG when the extension is working.")
  public String Ping() {
    return "PONG";
  }

  @SimpleFunction(description = "Rounds a number to two decimal places.")
  public double Round2(double value) {
    return Math.round(value * 100.0d) / 100.0d;
  }

  private static YailList list(Object... values) {
    return YailList.makeList(values);
  }

  private static double tanDegrees(double degrees) {
    return Math.tan(Math.toRadians(degrees));
  }

  @SimpleFunction(description = "GateCraft GEO 2D calculation. Returns area, perimeter, diagonal and derived values as a list.")
  public YailList GeoCalculate2D(String shape, double a, double b, double c, double d,
      double height, double radius, double diameter, double outerDiameter,
      double innerDiameter, double sideCount, double quantity) {

    double area = 0.0d;
    double perimeter = 0.0d;
    double diagonal = 0.0d;
    double outerRadius = 0.0d;
    double innerRadius = 0.0d;
    String formula = "";

    if ("square".equals(shape)) {
      area = a * a;
      perimeter = 4.0d * a;
      diagonal = a * Math.sqrt(4.0d);
      formula = "T = a'2, K = 4a";
    } else if ("rectangle".equals(shape)) {
      area = a * b;
      perimeter = 2.0d * (a + b);
      diagonal = Math.sqrt((a * a) + (b * b));
      formula = "T = a x b, K = 2(a+b)";
    } else if ("triangle".equals(shape)) {
      area = (a * height) / 2.0d;
      perimeter = a + b + c;
      formula = "T = a x m / 2";
    } else if ("right_triangle".equals(shape)) {
      c = Math.sqrt((a * a) + (b * b));
      area = (a * b) / 2.0d;
      perimeter = a + b + c;
      diagonal = c;
      formula = "c'2 = a'2 + b'2";
    } else if ("circle".equals(shape)) {
      if (diameter > 0.0d) {
        radius = diameter / 2.0d;
      }
      area = PI * radius * radius;
      perimeter = 2.0d * PI * radius;
      formula = "T = PI x r'2, K = 2 x PI X r";
    } else if ("annulus".equals(shape)) {
      outerRadius = outerDiameter / 2.0d;
      innerRadius = innerDiameter / 2.0d;
      area = PI * ((outerRadius * outerRadius) - (innerRadius * innerRadius));
      perimeter = (2.0d * PI * outerRadius) + (2.0d * PI * innerRadius);
      formula = "T = PI x (R'2 x r'2)";
    } else if ("trapezoid".equals(shape)) {
      area = ((a + b) / 2.0d) * height;
      perimeter = a + b + c + d;
      formula = "T = (A + B)/2 X M";
    } else if ("parallelogram".equals(shape)) {
      area = a * height;
      perimeter = 2.0d * (a + b);
      formula = "T = a x m";
    } else if ("rhombus".equals(shape)) {
      area = a * height;
      perimeter = 4.0d * a;
      formula = "T = a x m";
    } else if ("ellipse".equals(shape)) {
      area = PI * b * a;
      perimeter = (PI * (3.0d * (a + b)))
          - Math.sqrt((3.0d * (a + b)) * (a + (b * 3.0d)));
      formula = "T = PI x A x B";
    } else if ("regular_polygon".equals(shape)) {
      if (sideCount > 2.0d) {
        area = (sideCount * a * a) / (4.0d * tanDegrees(PI / sideCount));
        perimeter = sideCount * a;
        formula = "T = PI x A x B";
      }
    }

    area = area * quantity;
    perimeter = perimeter * quantity;

    return list(area, perimeter, diagonal, c, radius, outerRadius, innerRadius, formula);
  }

  @SimpleFunction(description = "GateCraft GEO 3D calculation. Returns volume, surfaces, diagonal and derived values as a list.")
  public YailList GeoCalculate3D(String shape, double a, double b, double length,
      double width, double height, double thickness, double radius, double diameter,
      double outerDiameter, double wall, double quantity) {

    double volume = 0.0d;
    double surface = 0.0d;
    double diagonal = 0.0d;
    double lateralSurface = 0.0d;
    double capacityLiter = 0.0d;
    double innerRadius = 0.0d;
    double slantHeight = 0.0d;
    String formula = "";

    if ("cube".equals(shape)) {
      volume = a * a * a;
      surface = 6.0d * a * a;
      diagonal = a * Math.sqrt(3.0d);
      formula = "V = a'3, F = 6xa'2";
    } else if ("box".equals(shape)) {
      volume = length * height * width;
      surface = 2.0d * ((length * width) + (height * width) + (length * height));
      diagonal = Math.sqrt((length * width) + (height * width) + (length * height));
      formula = "V = h x sz x m";
    } else if ("cylinder".equals(shape)) {
      radius = diameter / 2.0d;
      volume = PI * radius * radius * height;
      lateralSurface = 2.0d * PI * radius * height;
      surface = lateralSurface * (2.0d * PI * radius * height);
      formula = "V = PI x r'2 x m";
    } else if ("pipe".equals(shape)) {
      double outerRadius = outerDiameter / 2.0d;
      innerRadius = outerRadius - wall;
      volume = (PI * (outerRadius * outerRadius)) - ((innerRadius * innerRadius) * length);
      lateralSurface = 2.0d * PI * outerRadius * length;
      surface = lateralSurface;
      formula = "V = PI x (R'2 - r'2) x L";
    } else if ("cone".equals(shape)) {
      radius = diameter / 2.0d;
      volume = (PI * (radius * radius) * height) / 3.0d;
      slantHeight = Math.sqrt((radius * radius) + (height * height));
      lateralSurface = PI * radius * slantHeight;
      surface = lateralSurface + (PI * radius * radius);
      formula = "V = PI x r'2 x m /3";
    } else if ("sphere".equals(shape)) {
      radius = diameter / 2.0d;
      volume = (4.0d / 3.0d) * PI * radius * radius * radius;
      slantHeight = Math.sqrt((radius * radius) + (height * height));
      surface = 4.0d * PI * radius * radius;
      formula = "V = 4/3xPI x r'3";
    } else if ("prism".equals(shape)) {
      volume = (a * b) * height;
      formula = "V = m2 x H";
    } else if ("pyramid".equals(shape)) {
      volume = ((a * b) * height) / 3.0d;
      formula = "V = m2 x H / 3";
    } else if ("plate".equals(shape)) {
      volume = length * width * thickness;
      surface = 2.0d * ((length * width) + (length * thickness) + (width * thickness));
      formula = "V = L x W x T";
    } else if ("tank".equals(shape)) {
      radius = diameter / 2.0d;
      volume = PI * radius * radius * height;
      capacityLiter = volume * 1000.0d;
      formula = "L = m3 x 1000";
    }

    volume = volume * quantity;
    surface = surface * quantity;
    lateralSurface = lateralSurface * quantity;
    capacityLiter = volume * 1000.0d;

    return list(volume, surface, diagonal, lateralSurface, capacityLiter,
        radius, innerRadius, slantHeight, formula);
  }

  @SimpleFunction(description = "GateCraft GEO missing 2D dimension calculation using a stable target code.")
  public YailList GeoCalculateMissing2D(String shape, String target, double knownArea,
      double knownPerimeter, double a, double b, double quantity) {

    double missingResult = 0.0d;
    double area = 0.0d;
    double perimeter = 0.0d;
    double diagonal = 0.0d;
    double radius = 0.0d;
    double diameter = 0.0d;
    String formula = "";

    if ("square".equals(shape) && "square_side_from_area".equals(target)) {
      a = Math.sqrt(knownArea);
      missingResult = a;
      area = knownArea;
      perimeter = 4.0d * a;
      diagonal = a * Math.sqrt(2.0d);
      formula = "a = √ T";
    } else if ("square".equals(shape) && "square_side_from_perimeter".equals(target)) {
      a = knownPerimeter / 4.0d;
      missingResult = a;
      area = a * a;
      perimeter = a;
      diagonal = a * Math.sqrt(2.0d);
      formula = "a = K / 4";
    } else if ("rectangle".equals(shape) && "rectangle_b_from_area".equals(target)) {
      b = knownArea / a;
      missingResult = b;
      area = knownArea;
      perimeter = 2.0d * (a + b);
      diagonal = Math.sqrt((a * a) + (b * b));
      formula = "B = T / A";
    } else if ("rectangle".equals(shape) && "rectangle_a_from_area".equals(target)) {
      a = knownArea / b;
      missingResult = a;
      area = knownArea;
      perimeter = 2.0d * (a + b);
      diagonal = Math.sqrt((a * a) + (b * b));
      formula = "A = T / B";
    } else if ("rectangle".equals(shape) && "rectangle_a_from_perimeter".equals(target)) {
      a = (knownPerimeter / 2.0d) - b;
      missingResult = a;
      area = a * b;
      perimeter = knownPerimeter;
      diagonal = Math.sqrt((a * a) + (b * b));
      formula = "A = K / 2 - B";
    } else if ("rectangle".equals(shape) && "rectangle_b_from_perimeter".equals(target)) {
      b = (knownPerimeter / 2.0d) - a;
      missingResult = b;
      area = a * b;
      perimeter = knownPerimeter;
      diagonal = Math.sqrt((a * a) + (b * b));
      formula = "B = K / 2 - A";
    } else if ("circle".equals(shape) && "circle_radius_from_area".equals(target)) {
      radius = Math.sqrt(knownArea / PI);
      diameter = radius * 2.0d;
      missingResult = radius;
      area = knownArea;
      perimeter = 2.0d * PI * radius;
      formula = "r = √ (T / PI)";
    } else if ("circle".equals(shape) && "circle_diameter_from_area".equals(target)) {
      radius = knownArea / PI;
      diameter = radius * 2.0d;
      missingResult = diameter;
      area = knownArea;
      perimeter = 2.0d * PI * radius;
      formula = "d = 2 x √ (T / PI)";
    } else if ("circle".equals(shape) && "circle_radius_from_perimeter".equals(target)) {
      radius = knownPerimeter / (2.0d * PI);
      diameter = radius * 2.0d;
      missingResult = radius;
      area = PI * radius * radius;
      perimeter = knownPerimeter;
      formula = "r = K / (2 X PI)";
    } else if ("circle".equals(shape) && "circle_diameter_from_perimeter".equals(target)) {
      diameter = knownPerimeter / PI;
      radius = diameter / 2.0d;
      missingResult = diameter;
      area = knownPerimeter;
      perimeter = PI * radius * radius;
      formula = "d = K / PI";
    }

    area = area * quantity;
    perimeter = perimeter * quantity;

    return list(missingResult, area, perimeter, diagonal, a, b, radius, diameter, formula);
  }

  @SimpleFunction(description = "GateCraft GEO missing 3D dimension calculation using a stable target code.")
  public YailList GeoCalculateMissing3D(String shape, String target, double knownVolume,
      double length, double width, double height, double radius, double diameter,
      double quantity) {

    double missingResult = 0.0d;
    double volume = 0.0d;
    double surface = 0.0d;
    double diagonal = 0.0d;
    double lateralSurface = 0.0d;
    double capacityLiter = 0.0d;
    String formula = "";

    if ("box".equals(shape) && "box_length_from_volume".equals(target)) {
      length = knownVolume / (height * height);
      missingResult = length;
      volume = knownVolume;
      surface = 2.0d * ((length * width) + (length * height) + (width * height));
      diagonal = Math.sqrt((width * width) + (height * height) + (length * length));
      capacityLiter = volume * 1000.0d;
      formula = "L = V / (W x H)";
    } else if ("box".equals(shape) && "box_width_from_volume".equals(target)) {
      width = knownVolume / (length * height);
      missingResult = width;
      volume = knownVolume;
      surface = 2.0d * ((length * width) + (length * height) + (width * height));
      diagonal = Math.sqrt((width * width) + (height * height) + (length * length));
      capacityLiter = volume * 1000.0d;
      formula = "W = V / (L x H)";
    } else if ("box".equals(shape) && "box_height_from_volume".equals(target)) {
      height = knownVolume / (length * width);
      missingResult = height;
      volume = knownVolume;
      surface = 2.0d * ((length * width) + (length * height) + (width * height));
      diagonal = Math.sqrt((width * width) + (height * height) + (length * length));
      capacityLiter = volume * 1000.0d;
      formula = "H = V / (L x W)";
    } else if (("cylinder".equals(shape) || "tank".equals(shape))
        && ("cylinder_height_from_volume".equals(target) || "tank_height_from_volume".equals(target))) {
      if (diameter > 0.0d) {
        radius = diameter / 2.0d;
      }
      height = knownVolume / (PI * radius * radius);
      missingResult = height;
      volume = knownVolume;
      lateralSurface = 2.0d * PI * radius * height;
      surface = lateralSurface + (2.0d * PI * radius * radius);
      capacityLiter = volume * 1000.0d;
      formula = "H = V / (PI x r2)";
    } else if (("cylinder".equals(shape) || "tank".equals(shape))
        && ("cylinder_radius_from_volume".equals(target) || "tank_radius_from_volume".equals(target))) {
      radius = Math.sqrt(knownVolume / (PI * height));
      diameter = diameter * 2.0d;
      missingResult = radius;
      volume = knownVolume;
      lateralSurface = 2.0d * PI * radius * height;
      surface = lateralSurface + (2.0d * PI * radius * radius);
      capacityLiter = volume * 1000.0d;
      formula = "√ (V / (PI x H)";
    } else if (("cylinder".equals(shape) || "tank".equals(shape))
        && ("cylinder_diameter_from_volume".equals(target) || "tank_diameter_from_volume".equals(target))) {
      radius = Math.sqrt(knownVolume / (PI * height));
      diameter = radius * 2.0d;
      missingResult = diameter;
      volume = knownVolume;
      lateralSurface = 2.0d * PI * radius * height;
      surface = lateralSurface + (2.0d * PI * radius * radius);
      capacityLiter = volume * 1000.0d;
      formula = "2 X √ (V / (PI x H)";
    }

    volume = volume * quantity;
    surface = surface * quantity;
    lateralSurface = lateralSurface * quantity;
    capacityLiter = volume * 1000.0d;

    return list(missingResult, volume, surface, diagonal, lateralSurface,
        capacityLiter, length, width, height, radius, diameter, formula);
  }
}
