package dev.monolith.setting;

import com.google.gson.JsonElement;
import java.util.function.BooleanSupplier;

/** Base class for every configurable value. The Click GUI builds its controls from these automatically. */
public abstract class Setting<T> {
    public final String name;
    public String tooltip = "";
    protected T value;
    private final T def;
    private BooleanSupplier visible = () -> true;

    protected Setting(String name, T def) { this.name = name; this.value = def; this.def = def; }

    public T get() { return value; }
    public void set(T v) { this.value = v; }
    public void reset() { this.value = def; }
    public Setting<T> tip(String t) { this.tooltip = t; return this; }
    public Setting<T> visibleWhen(BooleanSupplier s) { this.visible = s; return this; }
    public boolean isVisible() { return visible.getAsBoolean(); }

    public abstract JsonElement save();
    public abstract void load(JsonElement e);
}
