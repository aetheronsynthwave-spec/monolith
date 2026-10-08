package dev.monolith.gui;
import dev.monolith.module.Module;

import dev.monolith.Monolith;
import dev.monolith.hud.HudModule;
import dev.monolith.module.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import java.util.*;

/**
 * Single-window Click GUI: category rail (left), search + module cards (right).
 * Left click = toggle, right click = expand settings. A "Profiles" tab manages configs.
 * Input method signatures target 1.21.9+ (Click / KeyInput / CharInput wrappers).
 */
public class ClickGuiScreen extends Screen {
    private static final String PROFILES = "Profiles", HUDEDIT = "Edit HUD";
    private final List<String> tabs = new ArrayList<>();
    private String tab; private String search = "";
    private boolean searchFocus;
    private float open = 0, scroll = 0, scrollTarget = 0;
    private long last = System.nanoTime();
    private final Map<Module, float[]> anim = new HashMap<>();   // [toggle, expand]
    private final Set<Module> expanded = new HashSet<>();
    private NumberSetting dragging; private float dragX, dragW;
    private KeybindSetting listening; private String tooltip;
    private String profileInput = ""; private boolean profileFocus; private String status = "";

    // layout (recomputed every frame)
    private float px, py, pw, ph, railW = 120, top = 44;

    public ClickGuiScreen() { super(Text.literal("Monolith")); for (Category c : Category.values()) tabs.add(c.label); tabs.add(HUDEDIT); tabs.add(PROFILES); tab = tabs.get(2); }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    private void click() { client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.6f)); }

    @Override public void render(DrawContext c, int mx, int my, float partial) {
        long now = System.nanoTime(); float dt = (now - last) / 1e9f; last = now;
        open = Render2D.approach(open, 1, dt, 10);
        scroll = Render2D.approach(scroll, scrollTarget, dt, 14);
        tooltip = null;

        // responsive: panel is 62% x 72% of the screen, clamped.
        pw = Math.max(380, Math.min(width - 20, width * 0.62f)); ph = Math.max(260, Math.min(height - 20, height * 0.72f));
        float e = 1 - (float) Math.pow(1 - open, 3);
        px = (width - pw) / 2f; py = (height - ph) / 2f + (1 - e) * 14;

        Render2D.rect(c, 0, 0, width, height, ((int) (0xA0 * e) << 24));
        var m = c.getMatrices();
        Render2D.shadow(c, px, py, pw, ph, 10, 14, e);
        Render2D.roundedBordered(c, px, py, pw, ph, 10, Render2D.withAlpha(Theme.BG, e), Render2D.withAlpha(Theme.BORDER, e));

        // Rail
        Render2D.textShadow(c, "MONOLITH", px + 18, py + 16, Theme.TEXT);
        Render2D.text(c, "client", px + 18, py + 27, Theme.TEXT_DIM);
        float ty = py + top + 8;
        for (String t : tabs) {
            boolean sel = t.equals(tab), hov = Render2D.hover(mx, my, px + 8, ty, railW - 16, 22);
            if (sel) Render2D.rounded(c, px + 8, ty, railW - 16, 22, 6, 0xFF1F1F1F);
            else if (hov) Render2D.rounded(c, px + 8, ty, railW - 16, 22, 6, 0xFF141414);
            if (sel) Render2D.rounded(c, px + 8, ty + 5, 2, 12, 1, Theme.ACCENT);
            Render2D.text(c, t, px + 20, ty + 7, sel ? Theme.TEXT : Theme.TEXT_DIM);
            ty += 26;
        }
        Render2D.rect(c, px + railW, py + 12, 1, ph - 24, Theme.BORDER);

        float cx = px + railW + 14, cw = pw - railW - 28, cy = py + 14;
        if (tab.equals(PROFILES)) { drawProfiles(c, mx, my, cx, cy, cw); }
        else if (tab.equals(HUDEDIT)) { Render2D.text(c, "Drag HUD elements anywhere on screen.", cx, cy + 4, Theme.TEXT_DIM);
            Render2D.roundedBordered(c, cx, cy + 20, 110, 22, 6, Theme.CARD, Theme.BORDER); Render2D.centered(c, "Open editor", cx + 55, cy + 27, Theme.TEXT); }
        else drawModules(c, mx, my, cx, cy, cw, dt);

        if (tooltip != null) { int w = Render2D.width(tooltip) + 12; float tx = Math.min(mx + 10, width - w - 4);
            Render2D.roundedBordered(c, tx, my + 12, w, 18, 5, 0xF0101010, Theme.BORDER_HI); Render2D.text(c, tooltip, tx + 6, my + 17, Theme.TEXT); }
    }

    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        for (Module mo : Monolith.modules().all()) {
            if (!search.isEmpty()) { if (mo.name.toLowerCase().contains(search.toLowerCase())) out.add(mo); }
            else if (mo.category.label.equals(tab)) out.add(mo);
        }
        return out;
    }

    private List<Setting<?>> shown(Module m) { return m.settings().stream().filter(Setting::isVisible).toList(); }
    private float settingH(Setting<?> s) { return s instanceof NumberSetting || s instanceof ColorSetting ? 30 : 22; }

    private float cardHeight(Module m, float ex) {
        float h = 34; float inner = 0; for (Setting<?> s : shown(m)) inner += settingH(s) + 2;
        return h + inner * ex;
    }

    private void drawModules(DrawContext c, int mx, int my, float x, float y, float w, float dt) {
        // Search bar
        Render2D.roundedBordered(c, x, y, w, 24, 8, Theme.CARD, searchFocus ? Theme.BORDER_HI : Theme.BORDER);
        Render2D.text(c, search.isEmpty() && !searchFocus ? "Search modules..." : search + (searchFocus && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : ""), x + 10, y + 8, search.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT);

        float listTop = y + 34, listH = py + ph - listTop - 10;
        c.enableScissor((int) x - 2, (int) listTop, (int) (x + w + 2), (int) (listTop + listH));
        float cy = listTop - scroll, total = 0;
        for (Module mo : visibleModules()) {
            float[] a = anim.computeIfAbsent(mo, k -> new float[]{mo.isEnabled() ? 1 : 0, 0});
            a[0] = Render2D.approach(a[0], mo.isEnabled() ? 1 : 0, dt, 14);
            a[1] = Render2D.approach(a[1], expanded.contains(mo) ? 1 : 0, dt, 12);
            float h = cardHeight(mo, a[1]);
            boolean hov = Render2D.hover(mx, my, x, cy, w, 34) && my > listTop && my < listTop + listH;
            Render2D.roundedBordered(c, x, cy, w, h, 8, hov ? Theme.CARD_HOVER : Theme.CARD, Render2D.lerpColor(Theme.BORDER, Theme.BORDER_HI, a[0]));
            Render2D.text(c, mo.name, x + 12, cy + 8, Theme.TEXT);
            Render2D.text(c, mo.category.label, x + 12 + Render2D.width(mo.name) + 8, cy + 8, 0xFF555555);
            Render2D.text(c, mo.description, x + 12, cy + 20, Theme.TEXT_DIM);
            // toggle pill
            float tx = x + w - 40, tyy = cy + 10;
            Render2D.rounded(c, tx, tyy, 28, 14, 7, Render2D.lerpColor(Theme.TRACK, 0xFFFFFFFF, a[0]));
            Render2D.rounded(c, tx + 2 + 14 * a[0], tyy + 2, 10, 10, 5, Render2D.lerpColor(0xFFFFFFFF, 0xFF000000, a[0]));
            if (hov) tooltip = "Left-click: toggle   Right-click: settings";

            if (a[1] > 0.02f) {
                float sy = cy + 34; c.enableScissor((int) x, (int) Math.max(listTop, cy + 34), (int) (x + w), (int) Math.min(listTop + listH, cy + h));
                for (Setting<?> s : shown(mo)) { drawSetting(c, mx, my, s, x + 10, sy - (1 - a[1]) * 6, w - 20); sy += settingH(s) + 2; }
                c.disableScissor();
            }
            cy += h + 6; total += h + 6;
        }
        c.disableScissor();
        scrollTarget = Math.max(0, Math.min(scrollTarget, Math.max(0, total - listH)));
        // scrollbar
        if (total > listH) { float bh = listH * listH / total, by = listTop + (scroll / total) * listH; Render2D.rounded(c, x + w + 4, by, 2, bh, 1, Theme.BORDER_HI); }
    }

    private void drawSetting(DrawContext c, int mx, int my, Setting<?> s, float x, float y, float w) {
        boolean hov = Render2D.hover(mx, my, x, y, w, settingH(s));
        if (hov && !s.tooltip.isEmpty()) tooltip = s.tooltip;
        Render2D.text(c, s.name, x + 2, y + 6, Theme.TEXT_DIM);
        if (s instanceof BoolSetting b) {
            float tx = x + w - 26; Render2D.rounded(c, tx, y + 5, 22, 11, 5, b.get() ? 0xFFFFFFFF : Theme.TRACK);
            Render2D.rounded(c, tx + (b.get() ? 12 : 2), y + 7, 7, 7, 3, b.get() ? 0xFF000000 : 0xFFFFFFFF);
        } else if (s instanceof NumberSetting n) {
            float sx = x + 2, sw = w - 4, f = (float) ((n.get() - n.min) / (n.max - n.min));
            Render2D.text(c, n.step >= 1 ? String.valueOf(n.asInt()) : String.format("%.2f", n.get()), x + w - 4 - Render2D.width(n.step >= 1 ? String.valueOf(n.asInt()) : String.format("%.2f", n.get())), y + 6, Theme.TEXT);
            Render2D.rounded(c, sx, y + 21, sw, 3, 1, Theme.TRACK); Render2D.rounded(c, sx, y + 21, Math.max(2, sw * f), 3, 1, Theme.ACCENT);
            Render2D.rounded(c, sx + sw * f - 3, y + 19, 6, 7, 3, 0xFFFFFFFF);
        } else if (s instanceof EnumSetting<?> e) {
            String v = e.get().name().charAt(0) + e.get().name().substring(1).toLowerCase().replace('_', ' ');
            Render2D.roundedBordered(c, x + w - 84, y + 2, 82, 17, 5, 0xFF1A1A1A, Theme.BORDER); Render2D.centered(c, v, x + w - 43, y + 6, Theme.TEXT);
        } else if (s instanceof ColorSetting col) {
            Render2D.roundedBordered(c, x + w - 20, y + 4, 16, 12, 3, col.get() | 0xFF000000, Theme.BORDER_HI);
            float sx = x + 2, sw = w - 4, f = col.gray() / 255f;      // grayscale slider keeps the palette monochrome
            Render2D.rounded(c, sx, y + 21, sw, 3, 1, Theme.TRACK); Render2D.rounded(c, sx, y + 21, Math.max(2, sw * f), 3, 1, 0xFFFFFFFF);
        } else if (s instanceof KeybindSetting k) {
            String t = listening == k ? "..." : k.get() <= 0 ? "None" : org.lwjgl.glfw.GLFW.glfwGetKeyName(k.get(), 0) != null ? org.lwjgl.glfw.GLFW.glfwGetKeyName(k.get(), 0).toUpperCase() : "KEY " + k.get();
            Render2D.roundedBordered(c, x + w - 64, y + 2, 62, 17, 5, 0xFF1A1A1A, listening == k ? Theme.BORDER_HI : Theme.BORDER); Render2D.centered(c, t, x + w - 33, y + 6, Theme.TEXT);
        } else if (s instanceof StringSetting st) { Render2D.text(c, st.get(), x + w - 4 - Render2D.width(st.get()), y + 6, Theme.TEXT); }
    }

    private void drawProfiles(DrawContext c, int mx, int my, float x, float y, float w) {
        var cfg = Monolith.config();
        Render2D.text(c, "Active: " + cfg.active(), x, y + 2, Theme.TEXT);
        Render2D.text(c, status, x + w - Render2D.width(status), y + 2, Theme.TEXT_DIM);
        Render2D.roundedBordered(c, x, y + 16, w - 130, 22, 6, Theme.CARD, profileFocus ? Theme.BORDER_HI : Theme.BORDER);
        Render2D.text(c, profileInput.isEmpty() && !profileFocus ? "Profile name..." : profileInput + (profileFocus ? "_" : ""), x + 8, y + 23, profileInput.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT);
        button(c, mx, my, x + w - 124, y + 16, 60, 22, "Create"); button(c, mx, my, x + w - 60, y + 16, 60, 22, "Rename");
        float ry = y + 48;
        for (String p : cfg.list()) {
            boolean act = p.equals(cfg.active());
            Render2D.roundedBordered(c, x, ry, w, 26, 7, Theme.CARD, act ? Theme.BORDER_HI : Theme.BORDER);
            Render2D.text(c, p, x + 12, ry + 9, Theme.TEXT);
            button(c, mx, my, x + w - 130, ry + 3, 40, 20, "Load"); button(c, mx, my, x + w - 86, ry + 3, 40, 20, "Save"); button(c, mx, my, x + w - 42, ry + 3, 38, 20, "Del");
            ry += 30;
        }
        button(c, mx, my, x, py + ph - 36, 110, 22, "Export > clipboard"); button(c, mx, my, x + 118, py + ph - 36, 120, 22, "Import < clipboard");
    }

    private void button(DrawContext c, int mx, int my, float x, float y, float w, float h, String t) {
        boolean hov = Render2D.hover(mx, my, x, y, w, h);
        Render2D.roundedBordered(c, x, y, w, h, 6, hov ? 0xFF222222 : 0xFF181818, hov ? Theme.BORDER_HI : Theme.BORDER); Render2D.centered(c, t, x + w / 2f, y + (h - 8) / 2f, Theme.TEXT);
    }
    private boolean hit(double mx, double my, float x, float y, float w, float h) { return Render2D.hover(mx, my, x, y, w, h); }

    // ---------------- input ----------------
    @Override public boolean mouseClicked(Click ev, boolean dbl) {
        double mx = ev.x(), my = ev.y(); int btn = ev.button();
        listening = null; searchFocus = false; profileFocus = false;
        float ty = py + top + 8;
        for (String t : tabs) { if (hit(mx, my, px + 8, ty, railW - 16, 22)) { tab = t; scrollTarget = 0; click(); if (t.equals(HUDEDIT)) { } return true; } ty += 26; }
        float cx = px + railW + 14, cw = pw - railW - 28, cy = py + 14;
        if (tab.equals(HUDEDIT)) { if (hit(mx, my, cx, cy + 20, 110, 22)) { click(); client.setScreen(new HudEditorScreen(this)); } return true; }
        if (tab.equals(PROFILES)) { profileClick(mx, my, cx, cy, cw); return true; }
        if (hit(mx, my, cx, cy, cw, 24)) { searchFocus = true; return true; }
        float listTop = cy + 34, listH = py + ph - listTop - 10;
        if (my < listTop || my > listTop + listH) return true;
        float y = listTop - scroll;
        for (Module m : visibleModules()) {
            float[] a = anim.computeIfAbsent(m, k -> new float[]{0, 0});
            float h = cardHeight(m, a[1]);
            if (hit(mx, my, cx, y, cw, 34)) { if (btn == 0) m.toggle(); else if (!expanded.remove(m)) expanded.add(m); click(); return true; }
            if (expanded.contains(m) && hit(mx, my, cx, y + 34, cw, h - 34)) {
                float sy = y + 34;
                for (Setting<?> s : shown(m)) { if (hit(mx, my, cx + 10, sy, cw - 20, settingH(s))) { settingClick(s, mx, cx + 10, cw - 20, btn); return true; } sy += settingH(s) + 2; }
            }
            y += h + 6;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void settingClick(Setting<?> s, double mx, float x, float w, int btn) {
        click();
        if (s instanceof BoolSetting b) b.set(!b.get());
        else if (s instanceof EnumSetting e) e.cycle();
        else if (s instanceof KeybindSetting k) listening = k;
        else if (s instanceof NumberSetting n) { dragging = n; dragX = x + 2; dragW = w - 4; setFromMouse(mx); }
        else if (s instanceof ColorSetting cs) { colorDrag = cs; dragX = x + 2; dragW = w - 4; setFromMouse(mx); }
        if (btn == 1) s.reset();
    }
    private ColorSetting colorDrag;
    private void setFromMouse(double mx) {
        float f = Math.max(0, Math.min(1, (float) ((mx - dragX) / dragW)));
        if (dragging != null) dragging.set(dragging.min + (dragging.max - dragging.min) * f);
        if (colorDrag != null) colorDrag.setGray((int) (f * 255), colorDrag.get() >>> 24);
    }
    @Override public boolean mouseDragged(Click ev, double dx, double dy) { if (dragging != null || colorDrag != null) { setFromMouse(ev.x()); return true; } return false; }
    @Override public boolean mouseReleased(Click ev) { dragging = null; colorDrag = null; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { scrollTarget -= (float) v * 30; if (scrollTarget < 0) scrollTarget = 0; return true; }

    private void profileClick(double mx, double my, float x, float y, float w) {
        var cfg = Monolith.config();
        if (hit(mx, my, x, y + 16, w - 130, 22)) { profileFocus = true; return; }
        if (hit(mx, my, x + w - 124, y + 16, 60, 22) && !profileInput.isBlank()) { cfg.create(profileInput); status = "Created " + profileInput; click(); profileInput = ""; return; }
        if (hit(mx, my, x + w - 60, y + 16, 60, 22) && !profileInput.isBlank()) { cfg.rename(cfg.active(), profileInput); status = "Renamed"; click(); profileInput = ""; return; }
        float ry = y + 48;
        for (String p : new ArrayList<>(cfg.list())) {
            if (hit(mx, my, x + w - 130, ry + 3, 40, 20)) { status = cfg.load(p) ? "Loaded " + p : "Load failed"; click(); }
            else if (hit(mx, my, x + w - 86, ry + 3, 40, 20)) { cfg.save(p); status = "Saved " + p; click(); }
            else if (hit(mx, my, x + w - 42, ry + 3, 38, 20)) { cfg.delete(p); status = "Deleted " + p; click(); }
            ry += 30;
        }
        if (hit(mx, my, x, py + ph - 36, 110, 22)) { client.keyboard.setClipboard(cfg.export()); status = "Copied to clipboard"; click(); }
        if (hit(mx, my, x + 118, py + ph - 36, 120, 22)) { status = cfg.importString(client.keyboard.getClipboard(), profileInput.isBlank() ? "imported" : profileInput) ? "Imported" : "Invalid clipboard data"; click(); }
    }

    @Override public boolean keyPressed(KeyInput in) {
        int key = in.key();
        if (listening != null) { listening.set(key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE ? -1 : key); listening = null; return true; }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
            if (searchFocus && !search.isEmpty()) search = search.substring(0, search.length() - 1);
            if (profileFocus && !profileInput.isEmpty()) profileInput = profileInput.substring(0, profileInput.length() - 1);
            return true;
        }
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || key == Monolith.guiKey) { if (searchFocus || profileFocus) { searchFocus = profileFocus = false; return true; } Monolith.config().save(Monolith.config().active()); close(); return true; }
        return super.keyPressed(in);
    }
    @Override public boolean charTyped(CharInput in) {
        String s = new String(Character.toChars(in.codepoint()));
        if (searchFocus) { search += s; scrollTarget = 0; return true; }
        if (profileFocus && profileInput.length() < 24) { profileInput += s; return true; }
        return false;
    }
}
