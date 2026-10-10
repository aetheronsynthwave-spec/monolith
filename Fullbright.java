public class Fullbright extends Module {
    public final NumberSetting gamma = add(new NumberSetting("Gamma", 12, 2, 16, 1));
    private double saved = Double.NaN;
    public Fullbright() { super("Fullbright", "See clearly in the dark.", Category.RENDER); }
    @Override public void onFrame(float d) {
        var opt = mc.options.getGamma();
        if (Double.isNaN(saved)) saved = opt.getValue();
        if (opt.value != gamma.get()) opt.value = gamma.get();   // bypasses the 0..1 clamp (needs the widener)
    }
    @Override public void onDisable() { if (!Double.isNaN(saved)) { mc.options.getGamma().value = saved; saved = Double.NaN; } }
}
