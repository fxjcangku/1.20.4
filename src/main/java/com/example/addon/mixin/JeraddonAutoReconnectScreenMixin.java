package com.example.addon.mixin;

import com.example.addon.modules.automation.serverhelper.ReconnectHandler;
import com.example.addon.modules.automation.serverhelper.ServerHelperModule;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.DirectionalLayoutWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisconnectedScreen.class)
public abstract class JeraddonAutoReconnectScreenMixin extends Screen {

    @Shadow @Final private DirectionalLayoutWidget grid;

    @Unique private ButtonWidget jerinin$reconnectBtn;

    protected JeraddonAutoReconnectScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "init", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/widget/DirectionalLayoutWidget;refreshPositions()V",
        shift = At.Shift.BEFORE))
    private void jerinin$addButtons(CallbackInfo ci) {
        ServerHelperModule mod = Modules.get().get(ServerHelperModule.class);
        if (mod == null || !mod.isActive()) return;
        ReconnectHandler rh = mod.getReconnectHandler();
        if (rh == null || rh.getLastAddress() == null) return;
        if (mod.getCfg().hideReconnectButton.get()) return;

        jerinin$reconnectBtn = new ButtonWidget.Builder(
            Text.literal(jerinin$getText()), btn -> jerinin$tryConnect()
        ).build();
        grid.add(jerinin$reconnectBtn);

        grid.add(new ButtonWidget.Builder(Text.literal("切换自动重连"), btn -> {
            mod.getCfg().autoReconnect.set(!mod.getCfg().autoReconnect.get());
            if (!mod.getCfg().autoReconnect.get()) rh.cancelScheduled();
            jerinin$reconnectBtn.setMessage(Text.literal(jerinin$getText()));
        }).build());
    }

    @Override
    public void tick() {
        super.tick();
        ServerHelperModule mod = Modules.get().get(ServerHelperModule.class);
        if (mod == null || !mod.isActive()) return;
        ReconnectHandler rh = mod.getReconnectHandler();
        if (rh == null || rh.getLastAddress() == null) return;
        if (jerinin$reconnectBtn != null) {
            jerinin$reconnectBtn.setMessage(Text.literal(jerinin$getText()));
        }
    }

    @Unique
    private String jerinin$getText() {
        ServerHelperModule mod = Modules.get().get(ServerHelperModule.class);
        String text = "重新连接";
        if (mod != null && mod.getCfg().autoReconnect.get()) {
            ReconnectHandler rh = mod.getReconnectHandler();
            if (rh != null && rh.getTicksLeft() >= 0) {
                text += " (" + String.format("%.1f", rh.getTicksLeft() / 20.0) + ")";
            }
        }
        return text;
    }

    @Unique
    private void jerinin$tryConnect() {
        ServerHelperModule mod = Modules.get().get(ServerHelperModule.class);
        if (mod == null) return;
        ReconnectHandler rh = mod.getReconnectHandler();
        if (rh == null || rh.getLastAddress() == null || rh.getLastServerInfo() == null) return;
        rh.reconnectNow();
    }
}
