/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.CharFilter;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WMinus;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2519;
import net.minecraft.class_2520;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class StringListSetting extends Setting<List<String>>{
    public final Class<? extends WTextBox.Renderer> renderer;
    public final CharFilter filter;

    public StringListSetting(String name, String description, List<String> defaultValue, Consumer<List<String>> onChanged, Consumer<Setting<List<String>>> onModuleActivated, IVisible visible, Class<? extends WTextBox.Renderer> renderer, CharFilter filter) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.renderer = renderer;
        this.filter = filter;
    }

    @Override
    protected List<String> parseImpl(String str) {
        return Arrays.asList(str.split(","));
    }

    @Override
    protected boolean isValueValid(List<String> value) {
        return true;
    }

    @Override
    public class_2487 save(class_2487 tag) {
        class_2499 valueTag = new class_2499();
        for (int i = 0; i < this.value.size(); i++) {
            valueTag.method_10531(i, class_2519.method_23256(get().get(i)));
        }
        tag.method_10566("value", valueTag);

        return tag;
    }

    @Override
    public List<String> load(class_2487 tag) {
        get().clear();

        class_2499 valueTag = tag.method_10554("value", 8);
        for (class_2520 tagI : valueTag) {
            get().add(tagI.method_10714());
        }

        return get();
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    public static void fillTable(GuiTheme theme, WTable table, StringListSetting setting) {
        table.clear();

        ArrayList<String> strings = new ArrayList<>(setting.get());
        CharFilter filter = setting.filter == null ? (text, c) -> true : setting.filter;

        for (int i = 0; i < setting.get().size(); i++) {
            int msgI = i;
            String message = setting.get().get(i);

            WTextBox textBox = table.add(theme.textBox(message, filter, setting.renderer)).expandX().widget();
            textBox.action = () -> strings.set(msgI, textBox.get());
            textBox.actionOnUnfocused = () -> setting.set(strings);

            WMinus delete = table.add(theme.minus()).widget();
            delete.action = () -> {
                strings.remove(msgI);
                setting.set(strings);

                fillTable(theme, table, setting);
            };

            table.row();
        }

        if (!setting.get().isEmpty()) {
            table.add(theme.horizontalSeparator()).expandX();
            table.row();
        }

        WButton add = table.add(theme.button("Add")).expandX().widget();
        add.action = () -> {
            strings.add("");
            setting.set(strings);

            fillTable(theme, table, setting);
        };

        WButton reset = table.add(theme.button(GuiRenderer.RESET)).widget();
        reset.action = () -> {
            setting.reset();

            fillTable(theme, table, setting);
        };
    }

    public static class Builder extends SettingBuilder<Builder, List<String>, StringListSetting> {
        private Class<? extends WTextBox.Renderer> renderer;
        private CharFilter filter;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(String... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder renderer(Class<? extends WTextBox.Renderer> renderer) {
            this.renderer = renderer;
            return this;
        }

        public Builder filter(CharFilter filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public StringListSetting build() {
            return new StringListSetting(name, description, defaultValue, onChanged, onModuleActivated, visible, renderer, filter);
        }
    }
}
