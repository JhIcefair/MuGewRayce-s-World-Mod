package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

/**
 * 2.0.0 无效物品规则（草案 §14.9 / 接口约定 §7）。
 *
 * <p>全局规则：本模组所有物品都能从创造模式物品栏或指令拿到；
 * 但下面这几类<b>直接拿到是无效的</b>——它们必须有对应的数据组件
 * （学习证编号 / 类型 / 模式 / 目标等级等）才能用，创造模式直接取的没有组件：</p>
 *
 * <ul>
 *   <li>{@code magic_study_certificate} 魔法学习证；</li>
 *   <li>{@code beginner_magic_exam_application} 初学魔法考试申请书；</li>
 *   <li>{@code magic_trial_application} 魔法试炼申请书；</li>
 *   <li>{@code magic_proficiency_certificate} 魔法熟练证明；</li>
 *   <li>{@code magic_trial_promotion_certificate} 魔法试炼晋级认证。</li>
 * </ul>
 *
 * <p><b>本包只定义判定规则与占位</b>：真实组件（{@code CertData} 等）由 W03 注册、
 * W06 实现，届时把 {@link #hasRequiredData(ItemStack)} 换成读组件即可，
 * 本类对外签名不变。界面按"无效证 / 无效物品"处理（红字提示属 W15 GUI）。</p>
 */
public final class InvalidItemRules {

    /** 需要数据组件才有意义的物品（ID 为注册名，不含命名空间）。 */
    public static final Set<String> REQUIRES_DATA_COMPONENT = Set.of(
            "magic_study_certificate",
            "beginner_magic_exam_application",
            "magic_trial_application",
            "magic_proficiency_certificate",
            "magic_trial_promotion_certificate"
    );

    private InvalidItemRules() {
    }

    /** 该注册名（不含命名空间）是否属于"无组件即无效"的物品。 */
    public static boolean requiresDataComponent(String path) {
        return path != null && REQUIRES_DATA_COMPONENT.contains(path);
    }

    /** 该注册 ID 是否属于"无组件即无效"的物品；命名空间不是本模组时返回 {@code false}。 */
    public static boolean requiresDataComponent(Identifier itemId) {
        return itemId != null
                && SizeAdjusterMod.MOD_ID.equals(itemId.getNamespace())
                && requiresDataComponent(itemId.getPath());
    }

    /** 该物品是否属于"无组件即无效"的物品。 */
    public static boolean requiresDataComponent(Item item) {
        return item != null && requiresDataComponent(BuiltInRegistries.ITEM.getKey(item));
    }

    /**
     * 物品是否无效（草案 §14.9）：只有"需要组件"的那几类可能无效，
     * 空栈返回 {@code false}（空手不是"无效物品"）。
     */
    public static boolean isInvalid(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !requiresDataComponent(stack.getItem())) {
            return false;
        }
        return !hasRequiredData(stack);
    }

    /**
     * 是否带有该类物品必需的数据组件。
     *
     * <p>TODO(W03/W06)：组件类型在 W03 注册、W06 实现；在那之前任何栈都拿不到组件，
     * 因此这里固定返回 {@code false}——与草案"创造模式直接拿到的证不可用"一致。
     * 实现组件后改成"读对应 ComponentType 是否为默认值"。</p>
     */
    private static boolean hasRequiredData(ItemStack stack) {
        return false;
    }
}
