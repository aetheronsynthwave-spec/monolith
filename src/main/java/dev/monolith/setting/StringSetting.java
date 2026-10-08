package dev.monolith.setting;
import com.google.gson.*;
public class StringSetting extends Setting<String> {
    public StringSetting(String n, String d) { super(n, d); }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { value = e.getAsString(); }
}
