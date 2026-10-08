package dev.monolith.module;
public enum Category { COMBAT("Combat"), MOVEMENT("Movement"), RENDER("Render"), HUD("HUD"), WORLD("World"), MISC("Misc");
    public final String label; Category(String l) { label = l; } }
