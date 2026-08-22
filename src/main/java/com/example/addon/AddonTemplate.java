package com.example.addon;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import com.example.addon.modules.PacketThrottle;
import com.example.addon.modules.automation.AutoEnchantBook;
import com.example.addon.modules.automation.EnchantmentSelectSetting;
import com.example.addon.modules.automation.FumoCommand;
import com.example.addon.modules.automation.serverhelper.ServerHelperModule;
import meteordevelopment.meteorclient.commands.Commands;
import net.minecraft.item.Items;

public class AddonTemplate extends MeteorAddon {
    public static final Category BYPASS = new Category("§c§lJeraddon §a§l绕过", Items.ENDER_EYE.getDefaultStack());
    public static final Category AUTOMATION = new Category("§c§lJeraddon §a§l自动化", Items.HOPPER.getDefaultStack());

    @Override
    public void onInitialize() {
        EnchantmentSelectSetting.register();
        Config.get().customFont.set(false);
        meteordevelopment.meteorclient.MeteorClient.EVENT_BUS.subscribe(new JeraddonWelcomeService());
        Modules.get().add(new ServerHelperModule());
        Modules.get().add(new PacketThrottle());
        Modules.get().add(new AutoEnchantBook());
        Commands.add(new FumoCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(BYPASS);
        Modules.registerCategory(AUTOMATION);
    }

    @Override
    public String getPackage() {
        return "com.example.addon";
    }
}
