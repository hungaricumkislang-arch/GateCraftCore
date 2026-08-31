package com.gatecraft.cad;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.AndroidViewComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

@DesignerComponent(
    version = 1,
    description = "GateCraft native CAD drawing surface. No MIT App Inventor Canvas component is used.",
    category = ComponentCategory.EXTENSION,
    nonVisible = false,
    iconName = ""
)
@SimpleObject(external = true)
public class GateCraftCAD extends AndroidViewComponent {

  private final CadView view;
  private String tool = "LINE";
  private double mmPerPx = 10.0;
  private boolean snapEnabled = true;

  public GateCraftCAD(ComponentContainer container) {
    super(container);
    view = new CadView(container.$context());
    container.$add(this);
    Width(-1);
    Height(-1);
  }

  @Override
  public View getView() {
    return view;
  }

  @SimpleFunction(description = "Set active CAD tool: SELECT, LINE, RECT, CIRCLE.")
  public void SetTool(String value) {
    if (value == null) return;
    String v = value.trim().toUpperCase();
    if (v.equals("SELECT") || v.equals("LINE") || v.equals("RECT") || v.equals("CIRCLE")) {
      tool = v;
      view.invalidate();
      ToolChanged(v);
    }
  }

  @SimpleProperty(description = "Current CAD tool.")
  public String Tool() {
    return tool;
  }

  @SimpleFunction(description = "Delete all CAD objects.")
  public void Clear() {
    view.objects.clear();
    view.selectedIndex = -1;
    view.invalidate();
    DrawingChanged(view.objects.size());
  }

  @SimpleFunction(description = "Delete the selected object.")
  public void DeleteSelected() {
    if (view.selectedIndex >= 0 && view.selectedIndex < view.objects.size()) {
      view.objects.remove(view.selectedIndex);
      view.selectedIndex = -1;
      view.invalidate();
      DrawingChanged(view.objects.size());
      SelectionChanged(0, "");
    }
  }

  @SimpleFunction(description = "Move selected object by millimetres.")
  public void MoveSelected(double dxMm, double dyMm) {
    if (view.selectedIndex < 0 || view.selectedIndex >= view.objects.size()) return;
    float dx = (float) (dxMm / mmPerPx);
    float dy = (float) (dyMm / mmPerPx);
    CadObject o = view.objects.get(view.selectedIndex);
    o.x1 += dx; o.y1 += dy; o.x2 += dx; o.y2 += dy;
    view.invalidate();
    DrawingChanged(view.objects.size());
  }

  @SimpleFunction(description = "Enable or disable endpoint snapping.")
  public void SetSnapEnabled(boolean enabled) {
    snapEnabled = enabled;
  }

  @SimpleProperty(description = "Millimetres represented by one screen pixel.")
  public double MillimetersPerPixel() {
    return mmPerPx;
  }

  @SimpleProperty
  public void MillimetersPerPixel(double value) {
    if (value > 0.0001) mmPerPx = value;
  }

  @SimpleProperty(description = "Number of CAD objects.")
  public int ObjectCount() {
    return view.objects.size();
  }

  @SimpleProperty(description = "ID of selected object, or zero.")
  public int SelectedId() {
    if (view.selectedIndex < 0 || view.selectedIndex >= view.objects.size()) return 0;
    return view.objects.get(view.selectedIndex).id;
  }

  @SimpleFunction(description = "Return drawing geometry as compact JSON.")
  public String ExportJson() {
    StringBuilder b = new StringBuilder();
    b.append("[");
    for (int i = 0; i < view.objects.size(); i++) {
      if (i > 0) b.append(',');
      CadObject o = view.objects.get(i);
      b.append("{\"id\":").append(o.id)
       .append(",\"type\":\"").append(o.type).append("\"")
       .append(",\"x1\":").append(o.x1 * mmPerPx)
       .append(",\"y1\":").append(o.y1 * mmPerPx)
       .append(",\"x2\":").append(o.x2 * mmPerPx)
       .append(",\"y2\":").append(o.y2 * mmPerPx)
       .append("}");
    }
    b.append("]");
    return b.toString();
  }

  @SimpleEvent
  public void ToolChanged(String activeTool) {
    EventDispatcher.dispatchEvent(this, "ToolChanged", activeTool);
  }

  @SimpleEvent
  public void SelectionChanged(int id, String type) {
    EventDispatcher.dispatchEvent(this, "SelectionChanged", id, type);
  }

  @SimpleEvent
  public void ObjectCreated(int id, String type) {
    EventDispatcher.dispatchEvent(this, "ObjectCreated", id, type);
  }

  @SimpleEvent
  public void DrawingChanged(int objectCount) {
    EventDispatcher.dispatchEvent(this, "DrawingChanged", objectCount);
  }

  private class CadView extends View {
    final Paint normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint previewPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final List<CadObject> objects = new ArrayList<CadObject>();
    int nextId = 1;
    int selectedIndex = -1;
    float sx, sy, ex, ey;
    boolean drawing = false;

    CadView(android.content.Context context) {
      super(context);
      setBackgroundColor(Color.WHITE);
      normalPaint.setColor(Color.BLACK);
      normalPaint.setStyle(Paint.Style.STROKE);
      normalPaint.setStrokeWidth(3f);
      selectedPaint.setColor(Color.rgb(255, 170, 0));
      selectedPaint.setStyle(Paint.Style.STROKE);
      selectedPaint.setStrokeWidth(6f);
      previewPaint.setColor(Color.GRAY);
      previewPaint.setStyle(Paint.Style.STROKE);
      previewPaint.setStrokeWidth(2f);
      setFocusable(true);
      setClickable(true);
    }

    @Override
    protected void onDraw(android.graphics.Canvas c) {
      super.onDraw(c);
      for (int i = 0; i < objects.size(); i++) {
        drawObject(c, objects.get(i), i == selectedIndex ? selectedPaint : normalPaint);
      }
      if (drawing && !tool.equals("SELECT")) {
        CadObject p = new CadObject(-1, tool, sx, sy, ex, ey);
        drawObject(c, p, previewPaint);
      }
    }

    private void drawObject(android.graphics.Canvas c, CadObject o, Paint p) {
      if (o.type.equals("LINE")) {
        c.drawLine(o.x1, o.y1, o.x2, o.y2, p);
      } else if (o.type.equals("RECT")) {
        RectF r = new RectF(Math.min(o.x1, o.x2), Math.min(o.y1, o.y2), Math.max(o.x1, o.x2), Math.max(o.y1, o.y2));
        c.drawRect(r, p);
      } else if (o.type.equals("CIRCLE")) {
        float dx = o.x2 - o.x1;
        float dy = o.y2 - o.y1;
        float radius = (float) Math.sqrt(dx * dx + dy * dy);
        c.drawCircle(o.x1, o.y1, radius, p);
      }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
      float x = e.getX();
      float y = e.getY();
      if (e.getAction() == MotionEvent.ACTION_DOWN) {
        if (tool.equals("SELECT")) {
          selectedIndex = hitTest(x, y);
          invalidate();
          if (selectedIndex >= 0) {
            CadObject o = objects.get(selectedIndex);
            SelectionChanged(o.id, o.type);
          } else {
            SelectionChanged(0, "");
          }
          return true;
        }
        float[] s = snapPoint(x, y);
        sx = s[0]; sy = s[1]; ex = sx; ey = sy;
        drawing = true;
        invalidate();
        return true;
      }
      if (e.getAction() == MotionEvent.ACTION_MOVE && drawing) {
        float[] s = snapPoint(x, y);
        ex = s[0]; ey = s[1];
        invalidate();
        return true;
      }
      if (e.getAction() == MotionEvent.ACTION_UP && drawing) {
        float[] s = snapPoint(x, y);
        ex = s[0]; ey = s[1];
        CadObject o = new CadObject(nextId++, tool, sx, sy, ex, ey);
        objects.add(o);
        drawing = false;
        selectedIndex = objects.size() - 1;
        invalidate();
        ObjectCreated(o.id, o.type);
        SelectionChanged(o.id, o.type);
        DrawingChanged(objects.size());
        return true;
      }
      return true;
    }

    private float[] snapPoint(float x, float y) {
      if (!snapEnabled || objects.isEmpty()) return new float[] {x, y};
      float bestX = x, bestY = y;
      float best = 18f;
      for (int i = 0; i < objects.size(); i++) {
        CadObject o = objects.get(i);
        float d1 = distance(x, y, o.x1, o.y1);
        if (d1 < best) { best = d1; bestX = o.x1; bestY = o.y1; }
        float d2 = distance(x, y, o.x2, o.y2);
        if (d2 < best) { best = d2; bestX = o.x2; bestY = o.y2; }
      }
      return new float[] {bestX, bestY};
    }

    private int hitTest(float x, float y) {
      final float tol = 22f;
      for (int i = objects.size() - 1; i >= 0; i--) {
        CadObject o = objects.get(i);
        if (o.type.equals("LINE")) {
          if (distanceToSegment(x, y, o.x1, o.y1, o.x2, o.y2) <= tol) return i;
        } else if (o.type.equals("RECT")) {
          float l = Math.min(o.x1, o.x2), r = Math.max(o.x1, o.x2);
          float t = Math.min(o.y1, o.y2), b = Math.max(o.y1, o.y2);
          if (distanceToSegment(x,y,l,t,r,t) <= tol || distanceToSegment(x,y,r,t,r,b) <= tol ||
              distanceToSegment(x,y,r,b,l,b) <= tol || distanceToSegment(x,y,l,b,l,t) <= tol) return i;
        } else if (o.type.equals("CIRCLE")) {
          float radius = distance(o.x1, o.y1, o.x2, o.y2);
          float radial = distance(o.x1, o.y1, x, y);
          if (Math.abs(radial - radius) <= tol) return i;
        }
      }
      return -1;
    }

    private float distance(float ax, float ay, float bx, float by) {
      float dx = bx - ax, dy = by - ay;
      return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private float distanceToSegment(float px, float py, float ax, float ay, float bx, float by) {
      float vx = bx - ax, vy = by - ay;
      float wx = px - ax, wy = py - ay;
      float len2 = vx * vx + vy * vy;
      if (len2 <= 0.0001f) return distance(px, py, ax, ay);
      float q = (wx * vx + wy * vy) / len2;
      if (q < 0f) q = 0f;
      if (q > 1f) q = 1f;
      return distance(px, py, ax + q * vx, ay + q * vy);
    }
  }

  private static class CadObject {
    final int id;
    final String type;
    float x1, y1, x2, y2;
    CadObject(int id, String type, float x1, float y1, float x2, float y2) {
      this.id = id;
      this.type = type;
      this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
    }
  }
}
