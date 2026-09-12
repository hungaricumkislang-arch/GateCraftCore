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

  @SimpleFunction(description = "R6AM exact profile endpoint/AABB gap in mm. Read-only.")
  public double ProfileContactGapR6AM(String idA, String idB, String sceneJson, String boundsJson) {
    String json = ProfileContactGapR6AMJson(idA, idB, sceneJson, boundsJson);
    try {
      JSONObject result = new JSONObject(json);
      if (!result.optBoolean("ok", false) || !result.has("gapMm")) {
        throw new IllegalStateException(result.optString("code", "GC_R6AM_ENGINE_FAILED"));
      }
      return result.getDouble("gapMm");
    } catch (RuntimeException ex) {
      throw ex;
    } catch (Throwable ex) {
      throw new IllegalStateException("GC_R6AM_ENGINE_FAILED", ex);
    }
  }

  @SimpleFunction(description = "R6AM exact profile endpoint/AABB gap diagnostic JSON. Read-only.")
  public String ProfileContactGapR6AMJson(String idA, String idB, String sceneJson, String boundsJson) {
    try {
      return profileGapResult(idA, idB, sceneJson, boundsJson).toString();
    } catch (Throwable t) {
      return error("GC_R6AM_ENGINE_FAILED");
    }
  }

  @SimpleFunction(description = "R6AM full profile contact audit from current CAD JSON snapshots. Read-only.")
  public String ProfileContactAuditR6AMJson(String multiSelectionJson, String sceneJson,
                                             String boundsJson, String collisionJson,
                                             double toleranceMm) {
    try {
      JSONObject multi = new JSONObject(multiSelectionJson == null ? "{}" : multiSelectionJson);
      JSONArray ids = multi.optJSONArray("ids");
      if (ids == null || ids.length() != 2) return error("GC_R6AM_SELECT_EXACTLY_2");
      String idA = ids.optString(0, "");
      String idB = ids.optString(1, "");
      if (idA.length() == 0 || idB.length() == 0 || idA.equals(idB)) return error("GC_R6AM_SELECT_EXACTLY_2");

      JSONObject gapResult = profileGapResult(idA, idB, sceneJson, boundsJson);
      if (!gapResult.optBoolean("ok", false)) return gapResult.toString();

      JSONObject collisionsDoc = new JSONObject(collisionJson == null ? "{}" : collisionJson);
      JSONArray collisions = collisionsDoc.optJSONArray("collisions");
      boolean intersect = false;
      if (collisions != null) {
        for (int i = 0; i < collisions.length(); i++) {
          JSONObject c = collisions.optJSONObject(i);
          if (c == null) continue;
          String a = c.optString("a", "");
          String b = c.optString("b", "");
          if ((idA.equals(a) && idB.equals(b)) || (idA.equals(b) && idB.equals(a))) {
            intersect = true;
            break;
          }
        }
      }

      double gap = gapResult.getDouble("gapMm");
      String cls = intersect ? "INTERSECT" : (gap <= toleranceMm ? "CONTACT" : "GAP");
      gapResult.put("schema", "gatecraft.r6am.profile-contact.v1");
      gapResult.put("class", cls);
      gapResult.put("intersect", intersect);
      gapResult.put("toleranceMm", toleranceMm);
      return gapResult.toString();
    } catch (Throwable t) {
      return error("GC_R6AM_ENGINE_FAILED");
    }
  }

  private static JSONObject profileGapResult(String idA, String idB, String sceneJson, String boundsJson) throws Exception {
    if (idA == null || idB == null || idA.length() == 0 || idB.length() == 0 || idA.equals(idB)) {
      return errorObject("GC_R6AM_BAD_IDS");
    }

    JSONObject scene = new JSONObject(sceneJson == null ? "{}" : sceneJson);
    JSONObject bounds = new JSONObject(boundsJson == null ? "{}" : boundsJson);
    JSONObject objectA = findById(scene.optJSONArray("objects"), idA);
    JSONObject objectB = findById(scene.optJSONArray("objects"), idB);
    JSONObject boundA = findById(bounds.optJSONArray("objects"), idA);
    JSONObject boundB = findById(bounds.optJSONArray("objects"), idB);
    if (objectA == null || objectB == null || boundA == null || boundB == null) {
      return errorObject("GC_R6AM_OBJECT_OR_BOUNDS_MISSING");
    }

    JSONObject metaA = objectA.optJSONObject("metadata");
    JSONObject metaB = objectB.optJSONObject("metadata");
    JSONObject transformA = objectA.optJSONObject("transform");
    JSONObject transformB = objectB.optJSONObject("transform");
    if (!supportedExactProfile(metaA, transformA) || !supportedExactProfile(metaB, transformB)) {
      return errorObject("GC_R6AM_EXACT_PROFILE_REQUIRED");
    }

    double[] minA = vec3(boundA.optJSONArray("min"));
    double[] maxA = vec3(boundA.optJSONArray("max"));
    double[] minB = vec3(boundB.optJSONArray("min"));
    double[] maxB = vec3(boundB.optJSONArray("max"));
    if (minA == null || maxA == null || minB == null || maxB == null) {
      return errorObject("GC_R6AM_OBJECT_OR_BOUNDS_MISSING");
    }

    double[][] endA = endpoints(metaA, transformA);
    double[][] endB = endpoints(metaB, transformB);
    double gap = Double.POSITIVE_INFINITY;
    for (int a = 0; a < 2; a++) {
      for (int b = 0; b < 2; b++) gap = Math.min(gap, distance(endA[a], endB[b]));
    }
    for (int a = 0; a < 2; a++) gap = Math.min(gap, pointAabbDistance(endA[a], minB, maxB));
    for (int b = 0; b < 2; b++) gap = Math.min(gap, pointAabbDistance(endB[b], minA, maxA));

    JSONObject result = new JSONObject();
    result.put("ok", true);
    result.put("schema", "gatecraft.r6am.profile-gap.v1");
    result.put("idA", idA);
    result.put("idB", idB);
    result.put("gapMm", gap);
    result.put("metadataA", metaA);
    result.put("metadataB", metaB);
    result.put("transformA", transformA);
    result.put("transformB", transformB);
    return result;
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
    return new double[][] { world(transform, 0.0, 0.0, -half), world(transform, 0.0, 0.0, half) };
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

  private static JSONObject errorObject(String code) throws Exception {
    JSONObject result = new JSONObject();
    result.put("ok", false);
    result.put("code", code);
    return result;
  }

  private static String error(String code) {
    try { return errorObject(code).toString(); }
    catch (Throwable ignored) { return "{\"ok\":false,\"code\":\"GC_R6AM_ENGINE_FAILED\"}"; }
  }
}
