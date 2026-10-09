package dev.monolith.module.impl;
public class ScoreboardTweak extends ElementTweak {
    public ScoreboardTweak() { super("Scoreboard", "Resize, move or hide the sidebar scoreboard."); }
    @Override protected float pivotX(int w, int h) { return w; }
    @Override protected float pivotY(int w, int h) { return h / 2f; }
}
