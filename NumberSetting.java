package dev.monolith.setting;
import com.google.gson.*;
/** Slider / number setting. */
public class NumberSetting extends Setting<Double> {
    public final double min, max, step;
    public NumberSetting(String n, double d, double min, double max, double step) { super(n, d); this.min = min; this.max = max; this.step = step; }
    @Override public void set(Double v) {
        double s = step > 0 ? Math.round(v / step) * step : v;
        value = Math.max(min, Math.min(max, s));
    }
    public int asInt() { return (int) Math.round(value); }
    public float asFloat() { return value.floatValue(); }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { set(e.getAsDouble()); }
}
