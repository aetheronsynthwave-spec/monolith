package dev.monolith.module.impl;
/** Note: chat mouse clicks/hover still use the unscaled position, so keep scale near 1 if you click chat links. */
public class ChatTweak extends ElementTweak {
    public ChatTweak() { super("Chat", "Resize or move the chat window."); }
    @Override protected float pivotX(int w, int h) { return 0; }
    @Override protected float pivotY(int w, int h) { return h - 40; }
}
