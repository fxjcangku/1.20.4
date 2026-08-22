package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.JeraddonModule;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.common.KeepAliveS2CPacket;
import net.minecraft.util.Hand;

import java.util.ArrayDeque;

/**
 * 发包防踢
 *
 * 功能1：收到服务器"你还在吗"的消息，立刻回复多次，保证不被超时踢出。
 * 功能2：如果很久没收到服务器消息，主动补发一次回复，防止网络不稳定时掉线。
 * 功能3：定时模拟玩家在动（发移动/挥手/换手里物品的假消息），防止服务器因没动静把你踢出。
 * 功能4：对发出去的包数量进行限制，防止发太快被反作弊检测到。
 */
public class PacketThrottle extends JeraddonModule {

    // ── 设置组 ─────────────────────────────────────────────────────────────────
    private final SettingGroup sgKeepAlive  = settings.getDefaultGroup();
    private final SettingGroup sgWatchdog   = settings.createGroup("断线保险");
    private final SettingGroup sgHeartbeat  = settings.createGroup("防挂机踢人");
    private final SettingGroup sgMove       = settings.createGroup("移动包");
    private final SettingGroup sgInteract   = settings.createGroup("交互包");
    private final SettingGroup sgMine       = settings.createGroup("挖掘包");
    private final SettingGroup sgChat       = settings.createGroup("聊天包");

    // ── 防超时踢人 ────────────────────────────────────────────────────────────
    private final Setting<Boolean> keepAliveEnabled = sgKeepAlive.add(new BoolSetting.Builder()
        .name("防超时踢人")
        .description("服务器发来[你还在吗]时，立刻回复，防止30秒没反应被踢。")
        .defaultValue(true).build());

    private final Setting<Integer> keepAliveDuplex = sgKeepAlive.add(new IntSetting.Builder()
        .name("回复次数")
        .description("每次服务器问你在不在，回几次。1=正常，2=双保险，3=三保险。网络不好推荐2-3。")
        .defaultValue(2).min(1).sliderMax(5)
        .visible(keepAliveEnabled::get).build());

    private final Setting<Boolean> keepAlivePos = sgKeepAlive.add(new BoolSetting.Builder()
        .name("附带位置包")
        .description("回复时顺带告诉服务器你现在的坐标，双重确认你还在线。")
        .defaultValue(true)
        .visible(keepAliveEnabled::get).build());

    private final Setting<Boolean> keepAliveLog = sgKeepAlive.add(new BoolSetting.Builder()
        .name("显示回复记录")
        .description("在聊天栏显示每次服务器问你在不在的时间间隔。")
        .defaultValue(false)
        .visible(keepAliveEnabled::get).build());

    // ── 断线保险 ──────────────────────────────────────────────────────────────
    private final Setting<Boolean> watchdogEnabled = sgWatchdog.add(new BoolSetting.Builder()
        .name("启用断线保险")
        .description("很久没收到服务器消息时，主动补发一次回复，防止网络抖动突然掉线。")
        .defaultValue(true).build());

    private final Setting<Integer> watchdogTimeout = sgWatchdog.add(new IntSetting.Builder()
        .name("等多久触发（帧）")
        .description("多少帧没收到服务器消息就补发。20帧=1秒，默认300=15秒，建议200-400。")
        .defaultValue(300).min(40).sliderMax(600)
        .visible(watchdogEnabled::get).build());

    private final Setting<Integer> watchdogResendCount = sgWatchdog.add(new IntSetting.Builder()
        .name("补发次数")
        .description("触发后连发几次回复包，建议2-3次。")
        .defaultValue(3).min(1).sliderMax(6)
        .visible(watchdogEnabled::get).build());

    // ── 防挂机踢人 ────────────────────────────────────────────────────────────
    private final Setting<Boolean> heartbeatEnabled = sgHeartbeat.add(new BoolSetting.Builder()
        .name("启用防挂机踢人")
        .description("定时模拟你在操作（发移动/挥手/换手里物品的假消息），让服务器觉得你没挂机。")
        .defaultValue(true).build());

    private final Setting<Integer> heartbeatInterval = sgHeartbeat.add(new IntSetting.Builder()
        .name("模拟间隔（帧）")
        .description("每隔多少帧模拟一次操作。20=1秒，建议80-200。")
        .defaultValue(60).min(20).sliderMax(400)
        .visible(heartbeatEnabled::get).build());

    private final Setting<Integer> heartbeatBurst = sgHeartbeat.add(new IntSetting.Builder()
        .name("每次发几组")
        .description("每次模拟操作发几组假消息。1=轻量，3=标准，5=超强。网络差/容易被踢推荐3-5。")
        .defaultValue(3).min(1).sliderMax(8)
        .visible(heartbeatEnabled::get).build());

    private final Setting<Boolean> heartbeatMicro = sgHeartbeat.add(new BoolSetting.Builder()
        .name("微位移")
        .description("发假消息时带极小的坐标偏移（0.0001格），让服务器觉得你在动，不触发挂机检测。")
        .defaultValue(true)
        .visible(heartbeatEnabled::get).build());

    private final Setting<Boolean> heartbeatSwing = sgHeartbeat.add(new BoolSetting.Builder()
        .name("模拟挥手")
        .description("同时发送挥手动作，让服务器觉得你在操作。")
        .defaultValue(true)
        .visible(heartbeatEnabled::get).build());

    private final Setting<Boolean> heartbeatSlot = sgHeartbeat.add(new BoolSetting.Builder()
        .name("模拟换手里的物品")
        .description("同时发送切换手持物品的消息，刷新服务器的活跃判定。")
        .defaultValue(true)
        .visible(heartbeatEnabled::get).build());

    private final Setting<Boolean> heartbeatLook = sgHeartbeat.add(new BoolSetting.Builder()
        .name("摇头包")
        .description("发送微小Yaw抖动的Look包，服务器认为你在转头，本地人物不动。用于对抗检测旋转的挂机踢人。")
        .defaultValue(true)
        .visible(heartbeatEnabled::get).build());

    // ── 移动限速 ──────────────────────────────────────────────────────────────
    private final Setting<Boolean> moveEnabled = sgMove.add(new BoolSetting.Builder()
        .name("限制移动消息频率")
        .description("限制每秒发出的移动消息数量，防止反作弊因发太快把你踢出。")
        .defaultValue(true).build());

    private final Setting<Integer> moveMax = sgMove.add(new IntSetting.Builder()
        .name("移动消息/秒上限")
        .description("每秒最多发几条移动消息。原版约20条/秒，建议10-30。")
        .defaultValue(20).min(1).sliderMax(100)
        .visible(moveEnabled::get).build());

    private final Setting<Boolean> moveQueue = sgMove.add(new BoolSetting.Builder()
        .name("排队发送（不丢失）")
        .description("超出上限时不丢弃，放进队列下一帧再发，保证移动不丢失。")
        .defaultValue(false)
        .visible(moveEnabled::get).build());

    // ── 交互限速 ──────────────────────────────────────────────────────────────
    private final Setting<Boolean> interactEnabled = sgInteract.add(new BoolSetting.Builder()
        .name("限制交互消息频率")
        .description("限制右键/点击方块/交互实体的消息频率，防反作弊。")
        .defaultValue(true).build());

    private final Setting<Integer> interactMax = sgInteract.add(new IntSetting.Builder()
        .name("交互消息/秒上限")
        .description("每秒最多发几条交互消息，建议5-20。")
        .defaultValue(10).min(1).sliderMax(60)
        .visible(interactEnabled::get).build());

    // ── 挖掘限速 ──────────────────────────────────────────────────────────────
    private final Setting<Boolean> mineEnabled = sgMine.add(new BoolSetting.Builder()
        .name("限制挖掘消息频率")
        .description("限制挖掘相关消息的频率，防反作弊。")
        .defaultValue(false).build());

    private final Setting<Integer> mineMax = sgMine.add(new IntSetting.Builder()
        .name("挖掘消息/秒上限")
        .description("每秒最多发几条挖掘消息，建议10-30。")
        .defaultValue(20).min(1).sliderMax(60)
        .visible(mineEnabled::get).build());

    // ── 静默开关 ──────────────────────────────────────────────────────────────
    private final Setting<Boolean> silentNotify = sgChat.add(new BoolSetting.Builder()
        .name("静默模式")
        .description("关闭所有断线保险/聊天限制等通知消息，不在聊天栏刷屏。")
        .defaultValue(false).build());

    // ── 聊天限速 ──────────────────────────────────────────────────────────────
    private final Setting<Boolean> chatEnabled = sgChat.add(new BoolSetting.Builder()
        .name("限制聊天消息频率")
        .description("防止聊天/发指令太频繁触发服务器冷却踢人。")
        .defaultValue(true).build());

    private final Setting<Integer> chatMax = sgChat.add(new IntSetting.Builder()
        .name("聊天消息/秒上限")
        .description("每秒最多发几条聊天或指令消息，建议1-5。")
        .defaultValue(3).min(1).sliderMax(20)
        .visible(chatEnabled::get).build());

    // ── 内部状态 ───────────────────────────────────────────────────────────────
    private final ArrayDeque<Long>               moveWindow     = new ArrayDeque<>();
    private final ArrayDeque<Long>               interactWindow = new ArrayDeque<>();
    private final ArrayDeque<Long>               mineWindow     = new ArrayDeque<>();
    private final ArrayDeque<Long>               chatWindow     = new ArrayDeque<>();
    private final java.util.concurrent.ConcurrentLinkedDeque<PlayerMoveC2SPacket> moveQueue_buf = new java.util.concurrent.ConcurrentLinkedDeque<>();

    // 内部发包标志（防止自己发出的模拟消息被自己拦截，形成死循环）
    private volatile boolean internalSending = false;

    // 统计
    private int  droppedMove     = 0;
    private int  droppedInteract = 0;
    private int  droppedMine     = 0;
    private int  droppedChat     = 0;
    private int  keepAliveCount  = 0;
    private long lastKeepAliveMs = 0;
    private int  heartbeatTick   = 0;

    // 断线保险计时
    private long lastKaId          = -1;
    private int  ticksSinceLastKa  = 0;
    private int  watchdogFireCount = 0;

    public PacketThrottle() {
        super(AddonTemplate.BYPASS, "发包防踢",
            "防止超时踢出，模拟操作防挂机，限制消息频率防反作弊。详细参考下面使用说明。");
    }

    @Override
    public void onActivate() {
        // 单人世界不允许开启
        if (mc.isInSingleplayer()) {
            notify("§c本模块仅在服务器中生效，单人世界已自动关闭。");
            toggle();
            return;
        }
        moveWindow.clear(); interactWindow.clear();
        mineWindow.clear(); chatWindow.clear();
        moveQueue_buf.clear();
        droppedMove = 0; droppedInteract = 0; droppedMine = 0; droppedChat = 0;
        keepAliveCount = 0; lastKeepAliveMs = 0; heartbeatTick = 0;
        lastKaId = -1; ticksSinceLastKa = 0; watchdogFireCount = 0;
        notify("§a§l已启动 §8│ §f§l回复×" + keepAliveDuplex.get()
            + " §8│ §f§l断线保险§a✔ §8│ §f§l模拟操作×" + heartbeatBurst.get() + "组"
            + " §8│ §f§l移动上限" + moveMax.get() + "§7/s");
    }

    @Override
    public void onDeactivate() {
        moveWindow.clear(); interactWindow.clear();
        mineWindow.clear(); chatWindow.clear();
        moveQueue_buf.clear();
    }

    // 切子服时重置断线保险计时，防止因 handler 切换导致 KA 间隔虚高误触发
    @EventHandler
    private void onGameJoined(GameJoinedEvent event) {
        if (!isActive() || mc.isInSingleplayer()) return;
        ticksSinceLastKa = 0;
        lastKaId = -1;
    }

    // ── 收到服务器"你还在吗"时，立刻回复N次 ──────────────────────────────────
    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!isActive() || !keepAliveEnabled.get()) return;
        if (!(event.packet instanceof KeepAliveS2CPacket ka)) return;

        long now = System.currentTimeMillis();
        long interval = lastKeepAliveMs == 0 ? 0 : now - lastKeepAliveMs;
        lastKeepAliveMs = now;
        lastKaId = ka.getId();
        ticksSinceLastKa = 0; // 重置断线保险计时

        // 取消原始回复，由我们自己控制发几次（不取消会多发1次，Grim会检测到重复）
        event.cancel();

        int times = keepAliveDuplex.get();
        internalSending = true;
        try {
            for (int i = 0; i < times; i++) {
                mc.getNetworkHandler().sendPacket(new KeepAliveC2SPacket(ka.getId()));
            }
            // 顺带告诉服务器当前坐标，双重确认在线
            if (keepAlivePos.get() && mc.player != null) {
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                    mc.player.isOnGround()
                ));
            }
        } finally {
            internalSending = false;
        }
        keepAliveCount++;

        if (keepAliveLog.get()) {
            notify("§e♥ 在线确认§8│§fid=" + ka.getId() + " §7间隔" + interval
                + "ms §8│§f回复" + times + "次 §8│§f累计" + keepAliveCount + "次");
        }
    }

    // ── 每帧：断线保险 + 模拟操作 + 排队发送 ─────────────────────────────────
    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!isActive() || mc.player == null || mc.getNetworkHandler() == null) return;

        // 断线保险：很久没收到服务器消息，补发位置包保持连接
        // 注意：不重复发旧 KeepAlive id，服务器已处理过的 id 重复发会触发异常检测
        if (watchdogEnabled.get()) {
            ticksSinceLastKa++;
            if (ticksSinceLastKa >= watchdogTimeout.get()) {
                ticksSinceLastKa = 0;
                watchdogFireCount++;
                int count = watchdogResendCount.get();
                internalSending = true;
                try {
                    for (int i = 0; i < count; i++) {
                        // 只发位置包，不重发旧 KA id
                        mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(
                            mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                            mc.player.isOnGround()
                        ));
                    }
                } finally {
                    internalSending = false;
                }
                if (!silentNotify.get()) notify("§c§l⚠ 断线保险触发 §8│ §7补发×" + count
                    + " §8│ §7累计" + watchdogFireCount + "次");
            }
        }

        // 模拟操作：定时发假消息，让服务器觉得你没挂机
        if (heartbeatEnabled.get()) {
            if (++heartbeatTick >= heartbeatInterval.get()) {
                heartbeatTick = 0;
                sendHeartbeatBurst();
            }
        }

        // 排队发送：每帧按上限放行积压的移动消息
        if (moveQueue.get() && !moveQueue_buf.isEmpty()) {
            int perTick = Math.max(1, moveMax.get() / 20);
            int released = 0;
            internalSending = true;
            try {
                while (!moveQueue_buf.isEmpty() && released < perTick) {
                    mc.getNetworkHandler().sendPacket(moveQueue_buf.pollFirst());
                    released++;
                }
            } finally {
                internalSending = false;
            }
        }
    }

    /** 模拟操作：连发 burst 组假消息（位置+挥手+换物品）。 */
    private void sendHeartbeatBurst() {
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        boolean onGround = mc.player.isOnGround();
        // 用当前手持槽，避免服务器认为换槽
        int slot = mc.player.getInventory().selectedSlot;
        int burst = heartbeatBurst.get();

        // 标记为内部消息，跳过自己的限速检查
        internalSending = true;
        try {
            for (int i = 0; i < burst; i++) {
                // 位置：奇数组偏移0.0001格，偶数组还原，让服务器觉得在动
                if (heartbeatMicro.get()) {
                    double offset = (i % 2 == 0) ? 0.0001 : 0.0;
                    mc.getNetworkHandler().sendPacket(
                        new PlayerMoveC2SPacket.PositionAndOnGround(x + offset, y, z, onGround)
                    );
                } else {
                    mc.getNetworkHandler().sendPacket(
                        new PlayerMoveC2SPacket.OnGroundOnly(onGround)
                    );
                }

                // 挥手
                if (heartbeatSwing.get()) {
                    mc.getNetworkHandler().sendPacket(
                        new HandSwingC2SPacket(Hand.MAIN_HAND)
                    );
                }

                // 换手里的物品
                if (heartbeatSlot.get()) {
                    mc.getNetworkHandler().sendPacket(
                        new UpdateSelectedSlotC2SPacket(slot)
                    );
                }

                // 摇头包：只在第一组发一次，避免同帧多次旋转触发反作弊
                if (heartbeatLook.get() && i == 0) {
                    float currentYaw = mc.player.getYaw();
                    float currentPitch = mc.player.getPitch();
                    // 奇偶心跳次数交替 +0.5f / -0.5f，来回抖动
                    float deltaYaw = (heartbeatTick % 2 == 0) ? 0.5f : -0.5f;
                    mc.getNetworkHandler().sendPacket(
                        new PlayerMoveC2SPacket.LookAndOnGround(
                            currentYaw + deltaYaw, currentPitch,
                            mc.player.isOnGround()
                        )
                    );
                }
            }
        } finally {
            internalSending = false;
        }
    }

    // ── 发出去的消息限速 ─────────────────────────────────────────────────────
    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (!isActive()) return;
        // 跳过自己发出的模拟消息，防止死循环
        if (internalSending) return;

        // 移动包
        if (event.packet instanceof PlayerMoveC2SPacket pkt) {
            if (moveEnabled.get() && throttle(moveWindow, moveMax.get())) {
                event.cancel();
                droppedMove++;
                if (moveQueue.get()) moveQueue_buf.addLast(pkt);
            }
            return;
        }

        // 交互包
        if (event.packet instanceof PlayerInteractEntityC2SPacket
         || event.packet instanceof PlayerInteractBlockC2SPacket
         || event.packet instanceof PlayerInteractItemC2SPacket) {
            if (interactEnabled.get() && throttle(interactWindow, interactMax.get())) {
                event.cancel();
                droppedInteract++;
            }
            return;
        }

        // 挖掘包
        if (event.packet instanceof PlayerActionC2SPacket action) {
            PlayerActionC2SPacket.Action act = action.getAction();
            if ((act == PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
              || act == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
              || act == PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK)
             && mineEnabled.get() && throttle(mineWindow, mineMax.get())) {
                event.cancel();
                droppedMine++;
            }
            return;
        }

        // 聊天/指令包
        if (event.packet instanceof ChatMessageC2SPacket
         || event.packet instanceof CommandExecutionC2SPacket) {
            if (chatEnabled.get() && throttle(chatWindow, chatMax.get())) {
                event.cancel();
                droppedChat++;
                if (!silentNotify.get()) notify("§e⚠ 聊天太频繁被限制 §8│ §7已拦截 " + droppedChat + " 条");
            }
        }
    }

    /**
     * 供其他模块查询：当前1秒窗口内还能发多少个交互包。
     * 若 PacketThrottle 未启用或交互限速未开，返回 Integer.MAX_VALUE。
     */
    public static int availableInteractSlots() {
        PacketThrottle inst = meteordevelopment.meteorclient.systems.modules.Modules.get()
            .get(PacketThrottle.class);
        if (inst == null || !inst.isActive() || !inst.interactEnabled.get())
            return Integer.MAX_VALUE;
        long now = System.nanoTime();
        while (!inst.interactWindow.isEmpty()
               && (now - inst.interactWindow.peekFirst()) > 1_000_000_000L)
            inst.interactWindow.pollFirst();
        return Math.max(0, inst.interactMax.get() - inst.interactWindow.size());
    }

    /** 滑动窗口限速：1秒窗口，超过max返回true（应丢弃）*/
    private boolean throttle(ArrayDeque<Long> window, int max) {
        long now = System.nanoTime();
        while (!window.isEmpty() && (now - window.peekFirst()) > 1_000_000_000L)
            window.pollFirst();
        if (window.size() >= max) return true;
        window.addLast(now);
        return false;
    }

    @Override
    public String getInfoString() {
        int total = droppedMove + droppedInteract + droppedMine + droppedChat;
        String ka = keepAliveEnabled.get()
            ? "§a§l回复×" + keepAliveCount + (watchdogFireCount > 0 ? "§c§l⚠断线保险×" + watchdogFireCount : "")
            : "§8§l回复关";
        return total == 0 ? ka : ka + " §8│ §c拦截" + total;
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WTable t = (WTable) buildInfoWidget(theme,
            new String[]{ "§l发包防踢 · 使用说明" },
            new String[]{
                "§e§l▌ 防超时踢人【最重要】",
                "§f  服务器问你在不在时，立刻回复×" + keepAliveDuplex.get() + "次 + 顺带发坐标。",
                "§f  彻底防止30秒超时被踢。"
            },
            new String[]{
                "§a§l▌ 断线保险",
                "§f  超" + watchdogTimeout.get() + "帧没收到服务器消息，主动补发×" + watchdogResendCount.get() + "次。",
                "§f  对抗网络抖动/延迟高，保持在线不掉线。"
            },
            new String[]{
                "§a§l▌ 模拟操作防挂机（位置+挥手+换物品）",
                "§f  每" + heartbeatInterval.get() + "帧连发" + heartbeatBurst.get() + "组假操作消息，让服务器觉得你没挂机。",
                "§f  微位移模式：每组偏移0.0001格，服务器认为你在移动。"
            },
            new String[]{
                "§b§l▌ 消息频率限制（移动/交互/挖掘/聊天）",
                "§f  精确控制每秒发出的各类消息数，防止被反作弊检测到。"
            }
        );
        // 动态实时数据
        t.add(theme.label(" ")).expandX(); t.row();
        t.add(theme.label("§b§l▌ 实时数据")).expandX(); t.row();
        t.add(theme.label("§f  回复次数：§f" + keepAliveCount)).expandX(); t.row();
        t.add(theme.label("§f  断线保险触发：§c" + watchdogFireCount + "次")).expandX(); t.row();
        t.add(theme.label("§f  上次回复：§f" + (lastKeepAliveMs == 0 ? "等待中" :
            (System.currentTimeMillis() - lastKeepAliveMs) + "ms前"))).expandX(); t.row();
        t.add(theme.label("§f  拦截统计：§c移动" + droppedMove + " §e交互" + droppedInteract
            + " §7挖掘" + droppedMine + " §b聊天" + droppedChat)).expandX(); t.row();
        if (moveQueue.get()) {
            t.add(theme.label("§f  排队待发：§f" + moveQueue_buf.size() + "条")).expandX(); t.row();
        }
        return t;
    }
}
