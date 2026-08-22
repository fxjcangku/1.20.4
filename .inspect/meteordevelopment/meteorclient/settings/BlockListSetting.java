/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.class_2248;
import net.minecraft.class_2487;
import net.minecraft.class_2499;
import net.minecraft.class_2519;
import net.minecraft.class_2520;
import net.minecraft.class_2960;
import net.minecraft.class_7923;

public class BlockListSetting extends Setting<List<class_2248>> {
    public final Predicate<class_2248> filter;

    public BlockListSetting(String name, String description, List<class_2248> defaultValue, Consumer<List<class_2248>> onChanged, Consumer<Setting<List<class_2248>>> onModuleActivated, Predicate<class_2248> filter, IVisible visible) {
        super(name, description, defaultValue, onChanged, onModuleActivated, visible);

        this.filter = filter;
    }

    @Override
    public void resetImpl() {
        value = new ArrayList<>(defaultValue);
    }

    @Override
    protected List<class_2248> parseImpl(String str) {
        String[] values = str.split(",");
        List<class_2248> blocks = new ArrayList<>(values.length);

        try {
            for (String value : values) {
                class_2248 block = parseId(class_7923.field_41175, value);
                if (block != null && (filter == null || filter.test(block))) blocks.add(block);
            }
        } catch (Exception ignored) {}

        return blocks;
    }

    @Override
    protected boolean isValueValid(List<class_2248> value) {
        return true;
    }

    @Override
    public Iterable<class_2960> getIdentifierSuggestions() {
        return class_7923.field_41175.method_10235();
    }

    @Override
    protected class_2487 save(class_2487 tag) {
        class_2499 valueTag = new class_2499();
        for (class_2248 block : get()) {
            valueTag.add(class_2519.method_23256(class_7923.field_41175.method_10221(block).toString()));
        }
        tag.method_10566("value", valueTag);

        return tag;
    }

    @Override
    protected List<class_2248> load(class_2487 tag) {
        get().clear();

        class_2499 valueTag = tag.method_10554("value", 8);
        for (class_2520 tagI : valueTag) {
            class_2248 block = class_7923.field_41175.method_10223(new class_2960(tagI.method_10714()));

            if (filter == null || filter.test(block)) get().add(block);
        }

        return get();
    }

    public static class Builder extends SettingBuilder<Builder, List<class_2248>, BlockListSetting> {
        private Predicate<class_2248> filter;

        public Builder() {
            super(new ArrayList<>(0));
        }

        public Builder defaultValue(class_2248... defaults) {
            return defaultValue(defaults != null ? Arrays.asList(defaults) : new ArrayList<>());
        }

        public Builder filter(Predicate<class_2248> filter) {
            this.filter = filter;
            return this;
        }

        @Override
        public BlockListSetting build() {
            return new BlockListSetting(name, description, defaultValue, onChanged, onModuleActivated, filter, visible);
        }
    }
}
