package com.example.addon;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.text.Text;
import java.util.function.Consumer;

/**
 * 所有 Jeraddon 模块的基类。
 * 项目统一提示规范：
 * · [Jeraddon] 使用红色加粗
 * · [插件名] 使用白色加粗
 * · 普通正文使用白色加粗，成功使用绿色加粗，失败使用红色加粗
 * · 所有模块消息统一由本类输出，不允许出现 [Meteor] 前缀
 * · 模块开关、按键绑定、普通信息、警告和错误均遵循此格式
 */
public abstract class JeraddonModule extends Module {

    protected JeraddonModule(Category category, String name, String description) {
        super(category, name, description);
    }

    @Override
    public void toggle() {
        super.toggle();
        // 覆盖 GUI 点击路径：Meteor 的 toggle() 不会调用 sendToggledMsg()，只有按键绑定才会
        // 所以在这里统一输出提示，并将 sendToggledMsg() 置为空方法防止按键绑定双重提示
        if (mc.player != null && chatFeedback) {
            String status = isActive() ? "§a§l已启动" : "§c§l已关闭";
            mc.player.sendMessage(Text.literal(formatMessage(title, status)), false);
        }
    }

    @Override
    public void sendToggledMsg() {
        // 空实现：提示已在 toggle() 中输出，此处留空防止按键绑定路径产生双重提示
    }

    @Override
    public void info(String message, Object... args) {
        if ("Removed bind.".equals(message)) {
            notify("已移除按键绑定");
            return;
        }
        // "Bound to (highlight)%s(default)." 按键绑定成功消息
        if (message != null && message.startsWith("Bound to")) {
            // 先展开 %s 等格式参数，再提取按键名
            String expanded = formatArgs(message, args);
            String clean = expanded.replaceAll("\\(highlight\\)|\\(default\\)", "").trim();
            // "Bound to Q." → "已绑定按键：Q"
            String key = clean.replace("Bound to", "").replace(".", "").trim();
            notify("已绑定按键：" + key);
            return;
        }
        notify(formatArgs(message, args));
    }

    @Override
    public void info(Text message) {
        notify(message.getString());
    }

    @Override
    public void warning(String message, Object... args) {
        notify("§e§l" + formatArgs(message, args));
    }

    @Override
    public void error(String message, Object... args) {
        notify("§c§l" + formatArgs(message, args));
    }

    /**
     * 向玩家发送统一格式的公屏通知。
     * 格式：§c§l[Jeraddon] §r§f§l[插件名] §r§f§l消息
     *
     * @param message 消息内容（不含前缀）
     */
    protected void notify(String message) {
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal(formatMessage(title, "§f" + message)), false);
    }

    /**
     * 向玩家发送橙色加粗错误通知，与红色 [Jeraddon] 前缀形成视觉区分。
     * 格式：§c§l[Jeraddon] §r§f§l[插件名] §r§6§l错误消息
     *
     * @param message 错误内容（不含前缀）
     */
    protected void notifyError(String message) {
        if (mc.player == null) return;
        mc.player.sendMessage(Text.literal(formatMessage(title, "§6§l" + message)), false);
    }

    protected String highlightText(String text) {
        return "§a§l" + text + "§r§f§l";
    }

    protected String highlightServer(String text) {
        return "§6§l" + text + "§r§f§l";
    }

    protected String highlightLocation(String text) {
        return "§d§l" + text + "§r§f§l";
    }

    protected String highlightCommand(String text) {
        return "§e§l" + text + "§r§f§l";
    }

    public static String formatMessage(String moduleName, String message) {
        // 去掉模块名中的颜色代码，保证 [ 和 ] 与模块名同色（§f§l 白色粗体）
        String cleanModuleName = stripColorCodes(stripPrefix(moduleName));
        String cleanMessage = stripPrefix(message);
        return "§c§l[Jeraddon] §r§f§l[" + cleanModuleName + "] §r" + cleanMessage;
    }

    private static String stripPrefix(String value) {
        // 去除可能重复出现的 [Jeraddon] 前缀；其余模块名括号由调用方保证不重复传入
        return value
            .replace("[Jeraddon]", "")
            .trim();
    }

    /** 去除 Minecraft §x 格式化代码 */
    private static String stripColorCodes(String value) {
        return value.replaceAll("§[0-9a-fk-orA-FK-OR]", "");
    }

    private static String formatArgs(String message, Object... args) {
        if (args == null || args.length == 0) return message;
        try {
            return String.format(message, args);
        } catch (Exception ignored) {
            return message;
        }
    }

    // ══════════════════════════════════════════════════════════════════
    //  ▌ 个人习惯规范（所有模块必须遵守）
    // ══════════════════════════════════════════════════════════════════
    //
    //  【1】构造函数描述格式
    //  ─────────────────────────────────────────────────────────────────
    //  super(AddonTemplate.XXX, "模块名",
    //      "[一句话功能摘要，逗号分隔各能力]。详细参考下面使用说明。");
    //
    //  · 末尾必须以"。详细参考下面使用说明。"结尾（固定话术，不得更改）
    //  · 摘要尽量控制在 25 汉字以内，用顿号或逗号并列
    //  · 不写"支持 XXX"或"提供 XXX"等多余前缀，直接写功能
    //
    //  示例：
    //    "自动催熟农作物，补骨粉，ESP高亮，视角静默同步。详细参考下面使用说明。"
    //    "创造模式飞行手感，隐藏飞行能力，定时假落地包绕过飞行检测。详细参考下面使用说明。"
    //
    // ──────────────────────────────────────────────────────────────────
    //
    //  【2】getWidget 说明面板结构
    //  ─────────────────────────────────────────────────────────────────
    //  · 必须调用 buildInfoWidget(theme, sections...)，不允许手写 WTable
    //  · 第一个 String[] 是大标题，固定格式："§l模块名 · 使用说明"
    //  · 后续每个 String[] 是一个色块段落，第一行是段落标题，后续是正文
    //
    //  段落标题颜色约定（固定色，不得随意更改）：
    //    §e§l▌  准备 / 使用方法（黄色）
    //    §a§l▌  功能 / 原理 / 流程（绿色）
    //    §b§l▌  参数建议 / 说明（青色）
    //    §d§l▌  模式说明 / 提示（粉色）
    //    §c§l▌  注意 / 警告（红色）
    //
    //  正文行缩进约定：
    //    有序步骤  → "§f  1. 内容"（两空格 + 数字点）
    //    无序条目  → "§f  · 内容"（两空格 + 中间点）
    //    续行缩进  → "§f    内容"（四空格，与上一行对齐）
    //
    //  目标子服 → highlightText("目标子服")（亮绿色粗体）
    //  登录服/服务器名 → highlightServer("服务器")（金色粗体）
    //  主城/地点 → highlightLocation("地点")（紫粉色粗体）
    //  指令 → highlightCommand("/指令")（黄色粗体）
    //
    //  【3】仅服务器可用的模块（绕过类、防踢类）
    //  ─────────────────────────────────────────────────────────────────
    //  在 onActivate() 最开头加单人世界守卫：
    //
    //    if (mc.isInSingleplayer()) {
    //        notify("§c本模块仅在服务器中生效，单人世界已自动关闭。");
    //        toggle();
    //        return;
    //    }
    //
    // ──────────────────────────────────────────────────────────────────
    //
    //  【4】发布 / 上传仓库约定
    //  ─────────────────────────────────────────────────────────────────
    //
    //  私人仓库  https://github.com/fxjcangku/jeraddon
    //  · 构建任务：gradlew buildPersonal  →  输出未混淆 Personal JAR
    //  · 上传内容：源码 + Personal JAR（不加密，保留完整符号，方便自查调试）
    //  · 用途：自用存档、断点还原、跨设备同步
    //
    //  公开仓库  https://github.com/fxjcangku/jerinin-addon
    //  · 构建任务：gradlew buildOfficial  →  输出混淆 Official JAR
    //  · 上传内容：仅 Official JAR（已加密混淆，不含源码，对外发布）
    //  · Release 标签命名：v<版本>-beta<n>  例：v1.3-beta3
    //  · 每次公开发版前必须先完成私人仓库提交，Official 构建基于同一源码快照
    //
    // ──────────────────────────────────────────────────────────────────
    //
    //  【5】Release 发布文案模板（参考历史版本风格，带 emoji）
    //  ─────────────────────────────────────────────────────────────────
    //
    //  ## Jeraddon v<版本>
    //
    //  > 🧪 Beta 测试版，欢迎反馈 bug            ← beta 版加此行，正式版删掉
    //  > Minecraft 1.21.11 | Meteor Client 1.21.11-SNAPSHOT
    //
    //  ---
    //
    //  ### 📦 安装说明
    //
    //  1. 下载 `Jerinin-addon-Official-<版本>.jar` 放入 `.minecraft/mods/` 文件夹
    //  2. 启动游戏，按 Right Shift 打开 Meteor 菜单，找到 Jeraddon 分类即可使用
    //  3. **使用自动导路功能需额外安装 Baritone**，下载 `bariton1.21.11.jar` 放入同一 mods 文件夹
    //  4. 不使用自动导路功能只装第一个即可
    //
    //  ---
    //
    //  ### 🔥 新增：<模块名> — <一句话简介>
    //
    //  <两行功能摘要>
    //
    //  **核心功能**
    //
    //  - 🔄 **特性一**：说明
    //  - ⚡ **特性二**：说明
    //  - 🔧 **特性三**：说明
    //
    //  **指令**（如有）
    //
    //  ```
    //  .<指令名> <子命令>   说明
    //  ```
    //
    //  ---
    //
    //  ### 🔧 底层修复（如有）
    //
    //  - 修复内容一
    //  - 修复内容二
    //
    //  ---
    //
    //  ## 环境
    //
    //  | 依赖 | 版本 |
    //  |------|------|
    //  | Minecraft | 1.21.11 |
    //  | Fabric Loader | 0.16.5 |
    //  | Meteor Client | 1.21.11-SNAPSHOT |
    //  | Java | 21 |
    //
    //  💬 Discord：https://discord.gg/vwrRCtET
    //  🔗 GitHub：https://github.com/fxjcangku/jerinin-addon
    //
    //  ─────────────────────────────────────────────────────────────────
    //  注：emoji 选色约定
    //    🌾/🌱/🟫/🚿  → 农业/浇水类
    //    ⚔️/💎/🔴/💥  → 战斗/PVP 类
    //    🎒/📦/🗃️      → 背包/物品类
    //    🔍/📌/🗺️      → 辅助/嗅探/导航类
    //    🔧/⚙️          → 底层修复/工具类
    //
    //  附件上传顺序（用户看到的顺序）：
    //    1. Jerinin-addon-Official-<版本>.jar  ← 主插件，必传
    //    2. bariton1.21.11.jar                ← 寻路依赖，有导路功能时必传
    //
    // ══════════════════════════════════════════════════════════════════
    //  buildInfoWidget 示例（新模块直接复制并修改内容）
    //  ─────────────────────────────────────────────────────────────────
    //  @Override
    //  public WWidget getWidget(GuiTheme theme) {
    //      return buildInfoWidget(theme,
    //          new String[]{ "§l模块名 · 使用说明" },
    //          new String[]{
    //              "§e§l▌ 准备",
    //              "§f  1. 第一步",
    //              "§f  2. 第二步"
    //          },
    //          new String[]{
    //              "§a§l▌ 功能原理",
    //              "§f  · 特性一",
    //              "§f  · 特性二"
    //          },
    //          new String[]{
    //              "§b§l▌ 参数建议",
    //              "§f  服务器A — 建议值 X",
    //              "§f  服务器B — 建议值 Y"
    //          },
    //          new String[]{
    //              "§c§l▌ 注意",
    //              "§f  · 注意事项"
    //          }
    //      );
    //  }
    // ══════════════════════════════════════════════════════════════════
    /**
     * 与 buildInfoWidget 相同，但在说明文字前先执行 headerWidgets（用于放置按钮等交互控件）。
     */
    protected WWidget buildInfoWidget(GuiTheme theme, Consumer<WTable> headerWidgets, String[]... sections) {
        WTable t = theme.table();
        headerWidgets.accept(t);
        t.add(theme.label(" ")).expandX(); t.row();
        boolean firstSection = true;
        for (String[] section : sections) {
            if (section == null || section.length == 0) continue;
            if (!firstSection) {
                t.add(theme.label(" ")).expandX(); t.row();
            }
            for (String line : section) {
                t.add(theme.label(line)).expandX(); t.row();
            }
            firstSection = false;
        }
        return t;
    }

    protected WWidget buildInfoWidget(GuiTheme theme, String[]... sections) {
        WTable t = theme.table();
        boolean firstSection = true;
        for (String[] section : sections) {
            if (section == null || section.length == 0) continue;
            // 段落间空行（第一段标题行前不加）
            if (!firstSection) {
                t.add(theme.label(" ")).expandX(); t.row();
            }
            for (String line : section) {
                t.add(theme.label(line)).expandX(); t.row();
            }
            firstSection = false;
        }
        return t;
    }
}
