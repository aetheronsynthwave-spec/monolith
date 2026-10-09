package dev.monolith.gui;

import dev.monolith.Monolith;
import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.*;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/**
 * Compact Click GUI. Everything is drawn at S (85%) scale; mouse input is divided by S to match.
 * Animations: open/close pop, sliding tab indicator, staggered card entrance, hover fades, eased sliders/toggles.
 * Press the GUI key again (Right Shift by default) or Esc to close.
 */
public class ClickGuiScreen extends Screen {
    private static final float S = 0.85f, CARD = 32f, RAIL = 96f;
    private static final String PROFILES = "Profiles", HUDEDIT = "Edit HUD";
    private static final int[] PALETTE = {0xFFFFFFFF, 0xFFBDBDBD, 0xFF757575, 0xFFFF4D4D, 0xFFFF9F43, 0xFFFFD93D, 0xFF5EE08A, 0xFF4FD8FF, 0xFF5C7CFF, 0xFFB06CFF, 0xFFFF6FB5};

    private final List<String> tabs = new ArrayList<>();
    private String tab, search = "", profileInput = "", status = "", tooltip;
    private boolean searchFocus, profileFocus, closing, guiKeyDown = true;   // guiKeyDown starts true: the opening key must be released first
    private float openT, scroll, scrollTarget, tabAnim = 1, railY = -1, dt;
    private long last = System.nanoTime();
    private final Map<Module, float[]> anim = new HashMap<>();   // [enabled, expanded, hover]
    private final Set<Module> expanded = new HashSet<>();
    private final Map<Object, Float> smooth = new HashMap<>();
    private NumberSetting dragging; private float dragX, dragW;
    private KeybindSetting listening;
    private float px, py, pw, ph, vw, vh;

    public ClickGuiScreen() {
        super(Text.literal("Monolith"));
        for (Category c : Category.values()) if (!Monolith.modules().byCategory(c).isEmpty()) tabs.add(c.label);
        tabs.add(HUDEDIT); tabs.add(PROFILES);
        tab = tabs.get(0);
    }

    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    private void click() { client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.6f)); }
    private void requestClose() { if (!closing) { closing = true; searchFocus = profileFocus = false; listening = null; } }
    private float sm(Object key, float target, float speed) {
        float v = Render2D.approach(smooth.getOrDefault(key, target), target, dt, speed);
        smooth.put(key, v); return v;
    }
    private static float ease(float t) { t = Math.max(0, Math.min(1, t)); return 1 - (float) Math.pow(1 - t, 3); }

    @Override public void tick() {
        if (closing && openT <= 0.001f) { Monolith.config().save(Monolith.config().active()); client.setScreen(null); }
    }

    // ===================================================== render
    @Override public void render(DrawContext c, int mxr, int myr, float partial) {
        long now = System.nanoTime(); dt = Math.min(0.05f, (now - last) / 1e9f); last = now;
        double mx = mxr / S, my = myr / S;

        // Close with the GUI key (polled, so it works even while a text box is focused).
        boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), Monolith.guiKey) == GLFW.GLFW_PRESS;
        if (down && !guiKeyDown && listening == null) requestClose();
        guiKeyDown = down;

        openT = closing ? Math.max(0, openT - dt * 6f) : Math.min(1, openT + dt * 5f);
        float e = ease(openT);
        if (closing && openT <= 0.01f) return;
        scroll = Render2D.approach(scroll, scrollTarget, dt, 14);
        tabAnim = Render2D.approach(tabAnim, 1, dt, 9);
        tooltip = null;

        Render2D.rect(c, 0, 0, width, height, ((int) (0x99 * e)) << 24);

        var m = c.getMatrices();
        m.pushMatrix(); m.scale(S, S);
        vw = width / S; vh = height / S;
        pw = Math.min(380, vw - 20); ph = Math.min(250, vh - 20);
        px = (vw - pw) / 2f; py = (vh - ph) / 2f + (1 - e) * 10;
        float ps = 0.94f + 0.06f * e, pcx = px + pw / 2, pcy = py + ph / 2;
        m.pushMatrix(); m.translate(pcx, pcy); m.scale(ps, ps); m.translate(-pcx, -pcy);

        Render2D.shadow(c, px, py, pw, ph, 10, 8, e);
        Render2D.roundedBordered(c, px, py, pw, ph, 10, Render2D.withAlpha(Theme.BG, e), Render2D.withAlpha(Theme.BORDER, e));

        if (e > 0.12f) {
            drawRail(c, mx, my);
            float cx = px + RAIL + 12, cw = pw - RAIL - 24, cy = py + 12;
            if (tab.equals(PROFILES)) drawProfiles(c, mx, my, cx, cy, cw);
            else if (tab.equals(HUDEDIT)) {
                Render2D.text(c, "Drag HUD elements anywhere on screen.", cx, cy + 4, Theme.TEXT_DIM);
                button(c, mx, my, cx, cy + 20, 100, 20, "Open editor");
            } else drawModules(c, mx, my, cx, cy, cw);
        }
        if (tooltip != null) {
            float w = Render2D.width(tooltip) + 12, tx = (float) Math.min(mx + 10, vw - w - 4);
            Render2D.roundedBordered(c, tx, (float) my + 12, w, 17, 5, 0xF2101010, Theme.BORDER_HI);
            Render2D.text(c, tooltip, tx + 6, (float) my + 16, Theme.TEXT);
        }
        m.popMatrix(); m.popMatrix();
    }

    private void drawRail(DrawContext c, double mx, double my) {
        Render2D.textShadow(c, "MONOLITH", px + 16, py + 14, Theme.TEXT);
        Render2D.text(c, "client", px + 16, py + 25, Theme.TEXT_DIM);
        float ty0 = py + 46, target = ty0 + tabs.indexOf(tab) * 22;
        if (railY < 0) railY = target;
        railY = Render2D.approach(railY, target, dt, 14);                       // sliding selection pill
        Render2D.rounded(c, px + 8, railY, RAIL - 16, 20, 6, 0xFF1F1F1F);
        Render2D.rounded(c, px + 8, railY + 5, 2, 10, 1, Theme.ACCENT);
        for (int i = 0; i < tabs.size(); i++) {
            String t = tabs.get(i); float ty = ty0 + i * 22;
            float h = sm(t, Render2D.hover(mx, my, px + 8, ty, RAIL - 16, 20) ? 1 : 0, 14);
            boolean sel = t.equals(tab);
            if (!sel && h > 0.02f) Render2D.rounded(c, px + 8, ty, RAIL - 16, 20, 6, Render2D.lerpColor(0x00181818, 0xFF181818, h));
            Render2D.text(c, t, px + 20 + h * 2, ty + 6, Render2D.lerpColor(Theme.TEXT_DIM, Theme.TEXT, sel ? 1 : h));
        }
        Render2D.rect(c, px + RAIL, py + 12, 1, ph - 24, Theme.BORDER);
    }

    // ===================================================== modules
    private List<Module> visibleModules() {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module mo : Monolith.modules().all()) {
            if (!q.isEmpty()) { if (mo.name.toLowerCase().contains(q)) out.add(mo); }
            else if (mo.category.label.equals(tab)) out.add(mo);
        }
        return out;
    }
    private List<Setting<?>> shown(Module m) { return m.settings().stream().filter(Setting::isVisible).toList(); }
    private float settingH(Setting<?> s) { return s instanceof NumberSetting ? 28 : 22; }
    private float cardHeight(Module m, float ex) {
        if (ex < 0.001f) return CARD;
        float inner = 0; for (Setting<?> s : shown(m)) inner += settingH(s) + 2;
        return CARD + 4 + inner * ex;
    }

    private void drawModules(DrawContext c, double mx, double my, float x, float y, float w) {
        float focus = sm("search", searchFocus ? 1 : 0, 14);
        Render2D.roundedBordered(c, x, y, w, 22, 7, Theme.CARD, Render2D.lerpColor(Theme.BORDER, Theme.BORDER_HI, focus));
        String shownText = search.isEmpty() && !searchFocus ? "Search modules..." : search + (searchFocus && (System.currentTimeMillis() / 500 % 2 == 0) ? "_" : "");
        Render2D.text(c, shownText, x + 9, y + 7, search.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT);

        float listTop = y + 30, listH = py + ph - listTop - 8;
        c.enableScissor((int) (x - 2), (int) listTop, (int) (x + w + 2), (int) (listTop + listH));
        float cy = listTop - scroll, total = 0; int idx = 0;
        for (Module mo : visibleModules()) {
            float[] a = anim.computeIfAbsent(mo, k -> new float[]{mo.isEnabled() ? 1 : 0, 0, 0});
            a[0] = Render2D.approach(a[0], mo.isEnabled() ? 1 : 0, dt, 14);
            a[1] = Render2D.approach(a[1], expanded.contains(mo) ? 1 : 0, dt, 12);
            float h = cardHeight(mo, a[1]);
            float appear = ease(tabAnim * 1.7f - idx * 0.09f);                      // staggered entrance
            float cyy = cy + (1 - appear) * 14; idx++;
            if (cy + h < listTop || cy > listTop + listH) { cy += h + 5; total += h + 5; continue; }
            boolean hov = Render2D.hover(mx, my, x, cyy, w, CARD) && my > listTop && my < listTop + listH;
            a[2] = Render2D.approach(a[2], hov ? 1 : 0, dt, 16);
            Render2D.roundedBordered(c, x, cyy, w, h, 8, Render2D.lerpColor(Theme.CARD, Theme.CARD_HOVER, a[2]), Render2D.lerpColor(Theme.BORDER, Theme.BORDER_HI, Math.max(a[0], a[2] * 0.6f)));
            if (a[0] > 0.02f) Render2D.rounded(c, x + 4, cyy + CARD / 2 - 8 * a[0], 2, 16 * a[0], 1, Theme.ACCENT);   // grows when enabled
            Render2D.text(c, mo.name, x + 12, cyy + 7, Render2D.lerpColor(0xFFCFCFCF, Theme.TEXT, Math.max(a[0], a[2])));
            Render2D.text(c, mo.description, x + 12, cyy + 18, Theme.TEXT_DIM);
            Render2D.text(c, a[1] > 0.5f ? "-" : "+", x + w - 56, cyy + 12, Render2D.lerpColor(0xFF555555, Theme.TEXT, a[2]));
            float tx = x + w - 38, tyy = cyy + 9;
            Render2D.rounded(c, tx, tyy, 28, 14, 7, Render2D.lerpColor(Theme.TRACK, 0xFFFFFFFF, a[0]));
            Render2D.rounded(c, tx + 2 + 14 * a[0], tyy + 2, 10, 10, 5, Render2D.lerpColor(0xFFFFFFFF, 0xFF000000, a[0]));
            if (hov) tooltip = "Left-click: toggle   Right-click: settings";

            if (a[1] > 0.02f) {
                c.enableScissor((int) x, (int) Math.max(listTop, cyy + CARD), (int) (x + w), (int) Math.min(listTop + listH, cyy + h));
                float sy = cyy + CARD + 2;
                for (Setting<?> s : shown(mo)) { drawSetting(c, mx, my, s, x + 10, sy - (1 - a[1]) * 6, w - 20); sy += settingH(s) + 2; }
                c.disableScissor();
            }
            cy += h + 5; total += h + 5;
        }
        c.disableScissor();
        scrollTarget = Math.max(0, Math.min(scrollTarget, Math.max(0, total - listH)));
        if (total > listH) { float bh = Math.max(14, listH * listH / total), by = listTop + (scroll / total) * listH; Render2D.rounded(c, x + w + 3, by, 2, bh, 1, Theme.BORDER_HI); }
    }

    private void drawSetting(DrawContext c, double mx, double my, Setting<?> s, float x, float y, float w) {
        if (Render2D.hover(mx, my, x, y, w, settingH(s)) && !s.tooltip.isEmpty()) tooltip = s.tooltip;
        Render2D.text(c, s.name, x + 2, y + 6, Theme.TEXT_DIM);
        if (s instanceof BoolSetting b) {
            float k = sm(s, b.get() ? 1 : 0, 16), tx = x + w - 26;
            Render2D.rounded(c, tx, y + 5, 22, 11, 5, Render2D.lerpColor(Theme.TRACK, 0xFFFFFFFF, k));
            Render2D.rounded(c, tx + 2 + 10 * k, y + 7, 7, 7, 3, Render2D.lerpColor(0xFFFFFFFF, 0xFF000000, k));
        } else if (s instanceof NumberSetting n) {
            float f = sm(s, (float) ((n.get() - n.min) / (n.max - n.min)), 18), sx = x + 2, sw = w - 4;
            String v = n.step >= 1 ? String.valueOf(n.asInt()) : String.format("%.2f", n.get());
            Render2D.text(c, v, x + w - 4 - Render2D.width(v), y + 6, Theme.TEXT);
            Render2D.rounded(c, sx, y + 19, sw, 3, 1, Theme.TRACK);
            Render2D.rounded(c, sx, y + 19, Math.max(2, sw * f), 3, 1, Theme.ACCENT);
            Render2D.rounded(c, sx + sw * f - 3, y + 17, 6, 7, 3, 0xFFFFFFFF);
        } else if (s instanceof EnumSetting<?> e) {
            String v = e.get().name().charAt(0) + e.get().name().substring(1).toLowerCase().replace('_', ' ');
            float bw = Math.max(70, Render2D.width(v) + 16);
            Render2D.roundedBordered(c, x + w - bw - 2, y + 2, bw, 17, 5, 0xFF1A1A1A, Theme.BORDER);
            Render2D.centered(c, v, x + w - bw / 2f - 2, y + 6, Theme.TEXT);
        } else if (s instanceof ColorSetting col) {
            float sx = x + w - PALETTE.length * 13;
            for (int i = 0; i < PALETTE.length; i++) {
                float bx = sx + i * 13; boolean sel = (col.get() & 0xFFFFFF) == (PALETTE[i] & 0xFFFFFF);
                float g = sm(PALETTE[i] + "" + s.name, sel || Render2D.hover(mx, my, bx, y + 4, 11, 13) ? 1 : 0, 18);
                Render2D.rounded(c, bx - g, y + 5 - g, 11 + g * 2, 11 + g * 2, 3, PALETTE[i]);
                if (sel) Render2D.outline(c, (int) bx - 2, (int) y + 3, 15, 15, 0xFFFFFFFF);
            }
        } else if (s instanceof KeybindSetting k) {
            String t = listening == k ? "..." : k.get() <= 0 ? "None" : GLFW.glfwGetKeyName(k.get(), 0) != null ? GLFW.glfwGetKeyName(k.get(), 0).toUpperCase() : "KEY " + k.get();
            Render2D.roundedBordered(c, x + w - 64, y + 2, 62, 17, 5, 0xFF1A1A1A, listening == k ? Theme.BORDER_HI : Theme.BORDER);
            Render2D.centered(c, t, x + w - 33, y + 6, Theme.TEXT);
        } else if (s instanceof StringSetting st) { Render2D.text(c, st.get(), x + w - 4 - Render2D.width(st.get()), y + 6, Theme.TEXT); }
    }

    // ===================================================== profiles
    private void drawProfiles(DrawContext c, double mx, double my, float x, float y, float w) {
        var cfg = Monolith.config();
        Render2D.text(c, "Active: " + cfg.active(), x, y + 2, Theme.TEXT);
        Render2D.text(c, status, x + w - Render2D.width(status), y + 2, Theme.TEXT_DIM);
        float f = sm("pin", profileFocus ? 1 : 0, 14);
        Render2D.roundedBordered(c, x, y + 14, w - 124, 20, 6, Theme.CARD, Render2D.lerpColor(Theme.BORDER, Theme.BORDER_HI, f));
        Render2D.text(c, profileInput.isEmpty() && !profileFocus ? "Profile name..." : profileInput + (profileFocus ? "_" : ""), x + 8, y + 20, profileInput.isEmpty() ? Theme.TEXT_DIM : Theme.TEXT);
        button(c, mx, my, x + w - 118, y + 14, 56, 20, "Create"); button(c, mx, my, x + w - 58, y + 14, 58, 20, "Rename");
        float ry = y + 42;
        for (String p : cfg.list()) {
            boolean act = p.equals(cfg.active());
            Render2D.roundedBordered(c, x, ry, w, 24, 7, Theme.CARD, act ? Theme.BORDER_HI : Theme.BORDER);
            Render2D.text(c, p, x + 10, ry + 8, Theme.TEXT);
            button(c, mx, my, x + w - 124, ry + 2, 38, 20, "Load"); button(c, mx, my, x + w - 82, ry + 2, 38, 20, "Save"); button(c, mx, my, x + w - 40, ry + 2, 36, 20, "Del");
            ry += 28;
        }
        button(c, mx, my, x, py + ph - 32, 112, 20, "Export > clipboard"); button(c, mx, my, x + 118, py + ph - 32, 120, 20, "Import < clipboard");
    }

    private void button(DrawContext c, double mx, double my, float x, float y, float w, float h, String t) {
        float k = sm("b" + x + ":" + y, Render2D.hover(mx, my, x, y, w, h) ? 1 : 0, 16);
        Render2D.roundedBordered(c, x, y, w, h, 6, Render2D.lerpColor(0xFF181818, 0xFF262626, k), Render2D.lerpColor(Theme.BORDER, Theme.BORDER_HI, k));
        Render2D.centered(c, t, x + w / 2f, y + (h - 8) / 2f, Theme.TEXT);
    }
    private boolean hit(double mx, double my, float x, float y, float w, float h) { return Render2D.hover(mx, my, x, y, w, h); }

    // ===================================================== input
    @Override public boolean mouseClicked(Click ev, boolean dbl) {
        if (closing) return true;
        double mx = ev.x() / S, my = ev.y() / S; int btn = ev.button();
        listening = null; searchFocus = false; profileFocus = false;
        float ty0 = py + 46;
        for (int i = 0; i < tabs.size(); i++) {
            if (hit(mx, my, px + 8, ty0 + i * 22, RAIL - 16, 20)) {
                String t = tabs.get(i);
                if (!t.equals(tab)) { tabAnim = 0; tab = t; scrollTarget = 0; scroll = 0; }
                click(); return true;
            }
        }
        float cx = px + RAIL + 12, cw = pw - RAIL - 24, cy = py + 12;
        if (tab.equals(HUDEDIT)) { if (hit(mx, my, cx, cy + 20, 100, 20)) { click(); client.setScreen(new HudEditorScreen(this)); } return true; }
        if (tab.equals(PROFILES)) { profileClick(mx, my, cx, cy, cw); return true; }
        if (hit(mx, my, cx, cy, cw, 22)) { searchFocus = true; return true; }
        float listTop = cy + 30, listH = py + ph - listTop - 8;
        if (my < listTop || my > listTop + listH) return true;
        float y = listTop - scroll;
        for (Module m : visibleModules()) {
            float[] a = anim.computeIfAbsent(m, k -> new float[]{0, 0, 0});
            float h = cardHeight(m, a[1]);
            if (hit(mx, my, cx, y, cw, CARD)) {
                boolean chevron = mx > cx + cw - 62 && mx < cx + cw - 44;
                if (btn == 1 || chevron) { if (!expanded.remove(m)) expanded.add(m); } else m.toggle();
                click(); return true;
            }
            if (expanded.contains(m) && hit(mx, my, cx, y + CARD, cw, h - CARD)) {
                float sy = y + CARD + 2;
                for (Setting<?> s : shown(m)) { if (hit(mx, my, cx + 10, sy, cw - 20, settingH(s))) { settingClick(s, mx, cx + 10, cw - 20, btn); return true; } sy += settingH(s) + 2; }
            }
            y += h + 5;
        }
        return true;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void settingClick(Setting<?> s, double mx, float x, float w, int btn) {
        click();
        if (btn == 1) { s.reset(); return; }                               // right-click resets a setting
        if (s instanceof BoolSetting b) b.set(!b.get());
        else if (s instanceof EnumSetting e) e.cycle();
        else if (s instanceof KeybindSetting k) listening = k;
        else if (s instanceof NumberSetting n) { dragging = n; dragX = x + 2; dragW = w - 4; setFromMouse(mx); }
        else if (s instanceof ColorSetting cs) {
            int i = (int) Math.floor((mx - (x + w - PALETTE.length * 13)) / 13);
            if (i >= 0 && i < PALETTE.length) cs.set(PALETTE[i]);
        }
    }
    private void setFromMouse(double mx) {
        if (dragging == null) return;
        float f = Math.max(0, Math.min(1, (float) ((mx - dragX) / dragW)));
        dragging.set(dragging.min + (dragging.max - dragging.min) * f);
    }
    @Override public boolean mouseDragged(Click ev, double dx, double dy) { if (dragging != null) { setFromMouse(ev.x() / S); return true; } return false; }
    @Override public boolean mouseReleased(Click ev) { dragging = null; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { scrollTarget -= (float) v * 28; if (scrollTarget < 0) scrollTarget = 0; return true; }

    private void profileClick(double mx, double my, float x, float y, float w) {
        var cfg = Monolith.config();
        if (hit(mx, my, x, y + 14, w - 124, 20)) { profileFocus = true; return; }
        if (hit(mx, my, x + w - 118, y + 14, 56, 20) && !profileInput.isBlank()) { cfg.create(profileInput); status = "Created " + profileInput; click(); profileInput = ""; return; }
        if (hit(mx, my, x + w - 58, y + 14, 58, 20) && !profileInput.isBlank()) { cfg.rename(cfg.active(), profileInput); status = "Renamed"; click(); profileInput = ""; return; }
        float ry = y + 42;
        for (String p : new ArrayList<>(cfg.list())) {
            if (hit(mx, my, x + w - 124, ry + 2, 38, 20)) { status = cfg.load(p) ? "Loaded " + p : "Load failed"; click(); }
            else if (hit(mx, my, x + w - 82, ry + 2, 38, 20)) { cfg.save(p); status = "Saved " + p; click(); }
            else if (hit(mx, my, x + w - 40, ry + 2, 36, 20)) { cfg.delete(p); status = "Deleted " + p; click(); }
            ry += 28;
        }
        if (hit(mx, my, x, py + ph - 32, 112, 20)) { client.keyboard.setClipboard(cfg.export()); status = "Copied to clipboard"; click(); }
        if (hit(mx, my, x + 118, py + ph - 32, 120, 20)) { status = cfg.importString(client.keyboard.getClipboard(), profileInput.isBlank() ? "imported" : profileInput) ? "Imported" : "Invalid clipboard data"; click(); }
    }

    @Override public boolean keyPressed(KeyInput in) {
        int key = in.key();
        if (listening != null) { listening.set(key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE ? -1 : key); listening = null; return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE) {
            if (searchFocus && !search.isEmpty()) search = search.substring(0, search.length() - 1);
            if (profileFocus && !profileInput.isEmpty()) profileInput = profileInput.substring(0, profileInput.length() - 1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) { if (searchFocus || profileFocus) { searchFocus = profileFocus = false; } else requestClose(); return true; }
        if (key == Monolith.guiKey) { requestClose(); return true; }
        return super.keyPressed(in);
    }
    @Override public boolean charTyped(CharInput in) {
        String s = new String(Character.toChars(in.codepoint()));
        if (searchFocus) { search += s; scrollTarget = 0; return true; }
        if (profileFocus && profileInput.length() < 24) { profileInput += s; return true; }
        return false;
    }
}
