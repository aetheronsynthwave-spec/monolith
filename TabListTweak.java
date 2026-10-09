package dev.monolith.module.impl;
public class TabListTweak extends ElementTweak {
    public TabListTweak() { super("Tab List", "Resize, move or hide the player list."); }
    @Override protected float pivotX(int w, int h) { return w / 2f; }
    @Override protected float pivotY(int w, int h) { return 0; }
}
