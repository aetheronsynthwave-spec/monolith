package dev.monolith.setting;
import com.google.gson.*;
/** ARGB colour. Palette stays monochrome by default; the GUI offers a grayscale picker. */
public class ColorSetting extends Setting<Integer> {
    public ColorSetting(String n, int argb) { super(n, argb); }
    public void setGray(int g, int a) { value = (a << 24) | (g << 16) | (g << 8) | g; }
    public int gray() { return value & 0xFF; }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { value = e.getAsInt(); }
}
