package com.mugewrayce.sizeadjuster.magic;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 2.0.0 魔法类型冲突表（草案 §6.7）。
 *
 * <p>目前只有一对：<b>冰魔法 ↔ 火魔法</b>不能同时附加在一根法杖上。
 * 实现成一张可扩充的对称表：以后要加新的冲突对，只改 {@link #buildTable()}。</p>
 *
 * <p>调用口径（草案 §6.7）：学习证上已有火魔法时"冰"不可选（反之亦然）；
 * 若代码里出现同时拥有两者的情况，按"后学的被拒绝"处理——
 * 即 {@link #canOwn(java.util.Collection, MagicType)} 对候选类型返回 {@code false}。</p>
 *
 * <p>本类只做判定，不改学习证 / 法杖数据；实际拦截在 W15 / W21。</p>
 */
public final class MagicConflicts {

    /** 冲突表本体：每个类型 → 与它互斥的类型集合（对称写入）。 */
    private static final Map<MagicType, Set<MagicType>> TABLE = buildTable();

    private MagicConflicts() {
    }

    private static Map<MagicType, Set<MagicType>> buildTable() {
        EnumMap<MagicType, EnumSet<MagicType>> table = new EnumMap<>(MagicType.class);
        // 草案 §6.7：冰魔法 ↔ 火魔法（目前只有这一对）
        link(table, MagicType.FIRE, MagicType.ICE);
        return Collections.unmodifiableMap(table);
    }

    private static void link(EnumMap<MagicType, EnumSet<MagicType>> table, MagicType a, MagicType b) {
        table.computeIfAbsent(a, key -> EnumSet.noneOf(MagicType.class)).add(b);
        table.computeIfAbsent(b, key -> EnumSet.noneOf(MagicType.class)).add(a);
    }

    /**
     * 两个类型是否互斥（对称；同一类型 / {@code null} 一律不算冲突）。
     */
    public static boolean conflicts(MagicType a, MagicType b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        return conflictsOf(a).contains(b);
    }

    /** 与指定类型互斥的类型集合；没有冲突时返回空集合（不可变）。 */
    public static Set<MagicType> conflictsOf(MagicType type) {
        if (type == null) {
            return Set.of();
        }
        return TABLE.getOrDefault(type, Set.of());
    }

    /** 该类型是否参与任何冲突。 */
    public static boolean hasConflict(MagicType type) {
        return !conflictsOf(type).isEmpty();
    }

    /**
     * "后学的被拒绝"：在已拥有的类型集合上再学 {@code candidate} 是否允许。
     * 已拥有集合为 {@code null} 或空 → 允许。
     */
    public static boolean canOwn(Collection<MagicType> owned, MagicType candidate) {
        return firstConflict(owned, candidate).isEmpty();
    }

    /**
     * 若不允许，返回第一个冲突的已拥有类型；允许时返回 {@link Optional#empty()}。
     * 供界面给出红字原因（"与已有火魔法冲突"）时取名字用。
     */
    public static Optional<MagicType> firstConflict(Collection<MagicType> owned, MagicType candidate) {
        if (candidate == null || owned == null || owned.isEmpty()) {
            return Optional.empty();
        }
        for (MagicType existing : owned) {
            if (conflicts(existing, candidate)) {
                return Optional.of(existing);
            }
        }
        return Optional.empty();
    }
}
