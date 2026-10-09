package dev.monolith.setting;
import com.google.gson.*;
/** GLFW key code; -1 = unbound. */
public class KeybindSetting extends Setting<Integer> {
    public KeybindSetting(String n, int key) { super(n, key); }
    public JsonElement save() { return new JsonPrimitive(value); }
    public void load(JsonElement e) { value = e.getAsInt(); }
}
