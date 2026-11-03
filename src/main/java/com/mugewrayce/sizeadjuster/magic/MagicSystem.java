package com.mugewrayce.sizeadjuster.magic;

/**
 * 2.0.0 魔法体系（草案 §6.1）：自然魔法 / 起源魔法 / 净化魔法。
 *
 * <p>这里只有"体系"这一层的静态定义，用来给 {@link MagicType} 分组；</p>
 * <ul>
 *   <li>自然魔法：7 种类型，靠魔法学习证附加到普通魔法杖上学习与升级；</li>
 *   <li>起源魔法：与魔法书绑定，只有技能，不涉及升级；</li>
 *   <li>净化魔法：本版只保留类型本身（不实装、无选项、不做魔力值计算）。</li>
 * </ul>
 */
public enum MagicSystem {

    NATURAL("natural", "自然魔法", "Natural Magic"),
    ORIGIN("origin", "起源魔法", "Origin Magic"),
    EQUIPOISE("equipoise", "净化魔法", "Equipoise Magic");

    private final String id;
    private final String zhName;
    private final String enName;

    MagicSystem(String id, String zhName, String enName) {
        this.id = id;
        this.zhName = zhName;
        this.enName = enName;
    }

    /** 稳定 ID（小写下划线），用于存档字段 / 界面键后缀，不要用 {@link #name()}。 */
    public String id() {
        return id;
    }

    public String zhName() {
        return zhName;
    }

    public String enName() {
        return enName;
    }

    /** 按稳定 ID 查找；找不到返回 {@code null}。 */
    public static MagicSystem fromId(String id) {
        for (MagicSystem system : values()) {
            if (system.id.equals(id)) {
                return system;
            }
        }
        return null;
    }
}
