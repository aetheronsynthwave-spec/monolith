package dev.monolith.config;

import com.google.gson.*;
import dev.monolith.Monolith;
import dev.monolith.module.Module;
import dev.monolith.setting.Setting;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

/**
 * Profiles = one JSON file each in .minecraft/config/monolith/profiles.
 * A profile stores module on/off state, keybinds, every setting, and HUD layout (HUD x/y/scale are ordinary settings).
 */
public class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dir = FabricLoader.getInstance().getConfigDir().resolve("monolith").resolve("profiles");
    private final Path activeFile = dir.getParent().resolve("active.txt");
    private String active = "default";

    public ConfigManager() { try { Files.createDirectories(dir); } catch (IOException ignored) {} }

    public String active() { return active; }

    public List<String> list() {
        try (Stream<Path> s = Files.list(dir)) {
            return s.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".json"))
                .map(n -> n.substring(0, n.length() - 5)).sorted().toList();
        } catch (IOException e) { return List.of(); }
    }

    private static String safe(String n) { return n.replaceAll("[^A-Za-z0-9 _-]", "").trim(); }

    public JsonObject snapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("_schema", 2);
        for (Module m : Monolith.modules().all()) {
            JsonObject o = new JsonObject();
            o.addProperty("enabled", m.isEnabled());
            JsonObject s = new JsonObject();
            for (Setting<?> st : m.settings()) s.add(st.name, st.save());
            o.add("settings", s);
            root.add(m.name, o);
        }
        return root;
    }

    /** Profiles saved by older versions are skipped once so the new default colours/layout apply. */
    public boolean apply(JsonObject root) {
        if (!root.has("_schema") || root.get("_schema").getAsInt() < 2) return false;
        for (Module m : Monolith.modules().all()) {
            if (!root.has(m.name)) continue;
            try {
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("settings")) { JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> st : m.settings()) if (s.has(st.name)) st.load(s.get(st.name)); }
                m.setEnabled(o.has("enabled") && o.get("enabled").getAsBoolean());
            } catch (RuntimeException ignored) { /* corrupt entry: keep defaults */ }
        }
        return true;
    }

    public void save(String name) {
        name = safe(name); if (name.isEmpty()) return;
        try { Files.writeString(dir.resolve(name + ".json"), GSON.toJson(snapshot())); active = name; Files.writeString(activeFile, active); }
        catch (IOException e) { Monolith.LOGGER.error("Save failed", e); }
    }

    public boolean load(String name) {
        try {
            if (!apply(JsonParser.parseString(Files.readString(dir.resolve(safe(name) + ".json"))).getAsJsonObject())) return false;
            active = safe(name); Files.writeString(activeFile, active); return true;
        } catch (IOException | RuntimeException e) { return false; }
    }

    public void loadActive() {
        try { if (Files.exists(activeFile)) active = Files.readString(activeFile).trim(); } catch (IOException ignored) {}
        if (!load(active)) save(active);
    }

    public void create(String name) { save(name); }
    public void delete(String name) { try { Files.deleteIfExists(dir.resolve(safe(name) + ".json")); } catch (IOException ignored) {} }
    public void rename(String from, String to) {
        try { Files.move(dir.resolve(safe(from) + ".json"), dir.resolve(safe(to) + ".json"), StandardCopyOption.REPLACE_EXISTING); if (active.equals(from)) active = safe(to); }
        catch (IOException ignored) {}
    }

    /** Portable string (base64 JSON) for sharing via clipboard. */
    public String export() { return Base64.getEncoder().encodeToString(GSON.toJson(snapshot()).getBytes(StandardCharsets.UTF_8)); }
    public boolean importString(String b64, String asName) {
        try {
            JsonObject o = JsonParser.parseString(new String(Base64.getDecoder().decode(b64.trim()), StandardCharsets.UTF_8)).getAsJsonObject();
            if (!apply(o)) return false;
            save(asName); return true;
        } catch (RuntimeException e) { return false; }
    }
}
