package com.example.addon;

import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class JeraddonWelcomeService {
    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal("§c§l[Jeraddon] §r§f§l本模块免费（给群友使用），其他功能测试中，要加功能可以跟我说"), false);
    }
}
