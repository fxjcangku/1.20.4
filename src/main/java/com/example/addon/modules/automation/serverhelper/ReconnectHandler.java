package com.example.addon.modules.automation.serverhelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

/**
 * 自动重连处理器。
 *
 * 与 Meteor AutoReconnect 完全相同的实现方式：
 *   - tick 倒计时（不依赖 sleep 线程）
 *   - 断线时在主线程通过 tick() 驱动，不依赖 mc.world / mc.player
 *   - 调用方（ServerHelperModule）必须在 player==null 检查之前先调用 tick()
 */
public final class ReconnectHandler {

    private final MinecraftClient mc;
    private final ServerHelperSettings settings;
    private final Runnable onReconnectStart;

    private ServerAddress lastAddress;
    private ServerInfo    lastServerInfo;

    private int  reconnectAttempts;
    private int  ticksLeft = -1;   // -1 = 未调度

    public ReconnectHandler(MinecraftClient mc, ServerHelperSettings settings, Runnable onReconnectStart) {
        this.mc = mc;
        this.settings = settings;
        this.onReconnectStart = onReconnectStart;
    }

    /** 每个 tick 调用一次（断线后也要调用，不受 player==null 拦截）。 */
    public void tick() {
        if (ticksLeft < 0) return;
        if (ticksLeft == 0) {
            ticksLeft = -1;
            doReconnect();
            return;
        }
        ticksLeft--;
    }

    /** 进入服务器时调用，记录连接信息，重置稳定计数。 */
    public void recordServer(ServerAddress address, ServerInfo info) {
        this.lastAddress    = address;
        this.lastServerInfo = info != null ? info
            : new ServerInfo("自动重连", address.getAddress(), ServerInfo.ServerType.OTHER);
    }

    /**
     * 断线时调用，启动 tick 倒计时重连。
     * @return true = 已调度；false = 已达上限或未启用
     */
    public boolean startReconnect() {
        return startReconnect(settings.reconnectDelay.get(), settings.maxReconnectAttempts.get());
    }

    public boolean startReconnect(int delayTicks, int maxAttempts) {
        if (!settings.autoReconnect.get()) return false;
        if (lastAddress == null || lastServerInfo == null) return false;

        if (!settings.alwaysReconnect.get() && reconnectAttempts >= maxAttempts) return false;

        reconnectAttempts++;
        ticksLeft = Math.max(0, delayTicks);
        onReconnectStart.run();
        return true;
    }

    /** 取消待执行的重连并清空全部状态（模块关闭时调用）。 */
    public void reset() {
        ticksLeft          = -1;
        lastAddress        = null;
        lastServerInfo     = null;
        reconnectAttempts  = 0;
    }

    /** 连接稳定后由模块调用，将重试计数归零。 */
    public void markConnectionStable() {
        reconnectAttempts = 0;
    }

    public int  getReconnectAttempts() { return reconnectAttempts; }
    public boolean isScheduled()       { return ticksLeft >= 0; }
    /** 剩余 tick 数，-1 表示未调度。 */
    public int  getTicksLeft()         { return ticksLeft; }
    public ServerAddress getLastAddress()   { return lastAddress; }
    public ServerInfo    getLastServerInfo(){ return lastServerInfo; }

    /** 立即执行重连（跳过倒计时）。 */
    public void reconnectNow() {
        if (ticksLeft < 0) return;
        ticksLeft = -1;
        doReconnect();
    }

    public void cancelScheduled() {
        ticksLeft = -1;
    }

    // ── 私有 ────────────────────────────────────────────────────────────────

    private void doReconnect() {
        if (lastAddress == null || lastServerInfo == null) return;

        ConnectScreen.connect(
            new MultiplayerScreen(new TitleScreen()),
            mc,
            lastAddress,
            lastServerInfo,
            false
        );
    }
}
