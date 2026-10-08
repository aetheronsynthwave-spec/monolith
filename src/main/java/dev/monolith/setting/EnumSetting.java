package dev.monolith.setting;
import com.google.gson.*;
/** Dropdown setting backed by a Java enum. */
public class EnumSetting<E extends Enum<E>> extends Setting<E> {
    public final E[] options;
    public EnumSetting(String n, E d) { super(n, d); this.options = d.getDeclaringClass().getEnumConstants(); }
    public void cycle() { set(options[(value.ordinal() + 1) % options.length]); }
    public JsonElement save() { return new JsonPrimitive(value.name()); }
    public void load(JsonElement e) { for (E o : options) if (o.name().equals(e.getAsString())) { value = o; return; } }
}
