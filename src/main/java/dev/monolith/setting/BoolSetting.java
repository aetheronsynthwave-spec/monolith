package dev.monolith.setting;
import com.google.gson.*;
public class BoolSetting extends Setting<Boolean> {
    public BoolSetting(String n, boolean d) { super(n, d); }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { value = e.getAsBoolean(); }
}
