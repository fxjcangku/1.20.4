package com.example.addon.modules.automation;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.SettingsWidgetFactory;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.StringListSetting;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public final class EnchantmentSelectSetting extends StringListSetting {
    public final List<String> options;
    private final List<WLabel> countLabels = new ArrayList<>();

    public EnchantmentSelectSetting(String name, List<String> options, List<meteordevelopment.meteorclient.settings.BoolSetting> backing) {
        super(name, "选择需要收集的附魔属性", selected(backing), values -> apply(values, backing), null, null, null, null);
        this.options = List.copyOf(options);
    }

    private static List<String> selected(List<meteordevelopment.meteorclient.settings.BoolSetting> backing) {
        List<String> values = new ArrayList<>();
        for (meteordevelopment.meteorclient.settings.BoolSetting setting : backing) if (Boolean.TRUE.equals(setting.get())) values.add(setting.name);
        return values;
    }

    private static void apply(List<String> values, List<meteordevelopment.meteorclient.settings.BoolSetting> backing) {
        for (meteordevelopment.meteorclient.settings.BoolSetting setting : backing) setting.set(values.contains(setting.name));
    }

    public static void register() {
        SettingsWidgetFactory.registerCustomFactory(EnchantmentSelectSetting.class, theme -> (table, setting) -> createWidget(theme, table, (EnchantmentSelectSetting) setting));
    }

    private static void createWidget(GuiTheme theme, WTable table, EnchantmentSelectSetting setting) {
        WHorizontalList list = table.add(theme.horizontalList()).expandCellX().widget();
        WButton select = list.add(theme.button("选择附魔")).expandCellX().widget();
        WLabel count = list.add(theme.label(setting.countText())).widget();
        setting.countLabels.add(count);
        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        select.action = () -> MinecraftClient.getInstance().setScreen(new EnchantmentSelectScreen(theme, setting));
        reset.action = () -> {
            setting.reset();
            setting.refreshCount();
        };
    }

    public void refreshCount() {
        String text = countText();
        countLabels.removeIf(label -> {
            label.set(text);
            return false;
        });
    }

    public int selectedCount() {
        return get().size();
    }

    private String countText() {
        return "已选择 " + get().size() + " 项";
    }
}
