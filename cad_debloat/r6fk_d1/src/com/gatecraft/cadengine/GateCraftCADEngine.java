package com.gatecraft.cadengine;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.Component;
import com.google.appinventor.components.runtime.ComponentContainer;
import org.json.JSONArray;
import org.json.JSONObject;

@DesignerComponent(
    version = 1,
    description = "GateCraft CAD compact calculation engine. R6FK D1.",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "")
@SimpleObject(external = true)
public final class GateCraftCADEngine extends AndroidNonvisibleComponent implements Component {

  public GateCraftCADEngine(ComponentContainer container) {
    super(container.$form());
  }

  @SimpleFunction(description = "R6AM exact profile endpoint/AABB gap calculation. Read-only.")
  public String ProfileContactGapR6AMJson(String idA, String idB, String sceneJson, String boundsJson) {
    try {
      if (idA == null || idB == null || idA.length() == 0 || idB.length() == 0 || idA.equals(idB)) {
        return error("GC_R6AM_BAD_IDS");
      }

      JSONObject scene = new JSONObject(sceneJson == null ? "{}" : sceneJson);
      JSONObject bounds = new JSONObject(boundsJson == null ? "{}" : boundsJson);
      JSONObject objectA = findById(scene.optJSONArray("objects"), idA);
      JSONObject objectB = findById(scene.optJSONArray("objects"), idB);
      JSONObject boundA = findById(bounds.optJSONArray("objects"), idA);
      JSONObject boundB = findById(bounds.optJSONArray("objects"), idB);
      if (objectA == null || objectB == null || boundA == null || boundB == null) {
        return error("GC_R6AM_OBJECT_OR_BOUNDS_MISSING");
      }

      JSONObject metaA = objectA.optJSONObject("metadata");
      JSONObject metaB = objectB.optJSONObject("metadata");
      JSONObject transformA = objectA.optJSONObject("transform");
      JSONObject transformB = objectB.optJSONObject("transform");
      if (!supportedExactProfile(metaA, transformA) || !supportedExactProfile(metaB, transformB)) {
        return error("GC_R6AM_EXACT_PROFILE_REQUIRED");
      }

      double[] minA = vec3(boundA.optJSONArray("min"));
      double[] maxA = vec3(boundA.optJSONArray("max"));
      double[] minB = vec3(boundB.optJSONArray("min"));
      double[] maxB = vec3(boundB.optJSONArray("max"));
      if (minA == null || maxA == null || minB == null || maxB == null) {
        return error("GC_R6AM_BAD_BOUNDS");
      }

      double[][] endA = endpoints(metaA, transformA);
      double[][] endB = endpoints(metaB, transformB);
      double gap = Double.POSITIVE_INFINITY;

      for (int a = 0; a < 2; a++) {
        for (int b = 0; b < 2; b++) {
          gap = Math.min(gap, distance(endA[a], endB[b]));
        }
      }
      for (int a = 0; a < 2; a++) {
        gap = Math.min(gap, pointAabbDistance(endA[a], minB, maxB));
      }
      for (int b = 0; b < 2; b++) {
        gap = Math.min(gap, pointAabbDistance(endB[b], minA, maxA));
      }

      JSONObject result = new JSONObject();
      result.put("ok", true);
      result.put("schema", "gatecraft.r6am.profile-gap.v1");
      result.put("idA", idA);
      result.put("idB", idB);
      result.put("gapMm", gap);
      return result.toString();
    } catch (Throwable t) {
      return error("GC_R6AM_ENGINE_FAILED");
    }
  }

  private static boolean supportedExactProfile(JSONObject metadata, JSONObject transform) {
    if (metadata == null || transform == null) return false;
    if (metadata.optDouble("cutLengthMm", 0.0) <= 0.0) return false;
    return "Z".equalsIgnoreCase(metadata.optString("lengthAxis", ""));
  }

  private static JSONObject findById(JSONArray objects, String id) {
    if (objects == null) return null;
    for (int i = 0; i < objects.length(); i++) {
      JSONObject object = objects.optJSONObject(i);
      if (object != null && id.equals(object.optString("id", ""))) return object;
    }
    return null;
  }

  private static double[] vec3(JSONArray array) {
    if (array == null || array.length() < 3) return null;
    return new double[] { array.optDouble(0), array.optDouble(1), array.optDouble(2) };
  }

  private static double[][] endpoints(JSONObject metadata, JSONObject transform) {
    double half = 0.5 * metadata.optDouble("cutLengthMm", 0.0);
    return new double[][] {
      world(transform, 0.0, 0.0, -half),
      world(transform, 0.0, 0.0, half)
    };
  }

  // Exact R6AM/runtime order: scale -> rotate X -> rotate Y -> rotate Z -> translate.
  private static double[] world(JSONObject t, double x, double y, double z) {
    x *= t.optDouble("scaleX", 1.0);
    y *= t.optDouble("scaleY", 1.0);
    z *= t.optDouble("scaleZ", 1.0);

    double angle = Math.toRadians(t.optDouble("rotateX", 0.0));
    double c = Math.cos(angle), s = Math.sin(angle);
    double q = y * c - z * s;
    z = y * s + z * c;
    y = q;

    angle = Math.toRadians(t.optDouble("rotateY", 0.0));
    c = Math.cos(angle); s = Math.sin(angle);
    q = x * c + z * s;
    z = -x * s + z * c;
    x = q;

    angle = Math.toRadians(t.optDouble("rotateZ", 0.0));
    c = Math.cos(angle); s = Math.sin(angle);
    q = x * c - y * s;
    y = x * s + y * c;
    x = q;

    return new double[] {
      x + t.optDouble("x", 0.0),
      y + t.optDouble("y", 0.0),
      z + t.optDouble("z", 0.0)
    };
  }

  private static double distance(double[] a, double[] b) {
    double dx = a[0] - b[0];
    double dy = a[1] - b[1];
    double dz = a[2] - b[2];
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static double pointAabbDistance(double[] p, double[] min, double[] max) {
    double dx = p[0] < min[0] ? min[0] - p[0] : (p[0] > max[0] ? p[0] - max[0] : 0.0);
    double dy = p[1] < min[1] ? min[1] - p[1] : (p[1] > max[1] ? p[1] - max[1] : 0.0);
    double dz = p[2] < min[2] ? min[2] - p[2] : (p[2] > max[2] ? p[2] - max[2] : 0.0);
    return Math.sqrt(dx * dx + dy * dy + dz * dz);
  }

  private static String error(String code) {
    try {
      JSONObject result = new JSONObject();
      result.put("ok", false);
      result.put("code", code);
      return result.toString();
    } catch (Throwable ignored) {
      return "{\"ok\":false,\"code\":\"GC_R6AM_ENGINE_FAILED\"}";
    }
  }
}
