package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.ProficiencyData;
import com.mugewrayce.sizeadjuster.component.PromotionData;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
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
 *   <li>{@code magic_study_certificate} 魔法学习证 → {@link CertData}；</li>
 *   <li>{@code beginner_magic_exam_application} / {@code magic_trial_application} 申请书 → {@link ApplicationData}；</li>
 *   <li>{@code magic_proficiency_certificate} 魔法熟练证明 → {@link ProficiencyData}；</li>
 *   <li>{@code magic_trial_promotion_certificate} 魔法试炼晋级认证 → {@link PromotionData}。</li>
 * </ul>
 *
 * <p>W06 起不再占位：组件存在<b>且内部字段合法</b>才算有效（如 {@code cert.no=0}、
 * {@code targetLevel} 越界仍算无效）；法杖 / 水晶 / 附加书不属于证书类，不在此集合。
 * 正式物品要到 W09 才注册，真实物品上的无效提示留 W09 / W15 运行验收。</p>
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
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!requiresDataComponent(id)) {
            return false;
        }
        return !hasValidRequiredData(id.getPath(), stack);
    }

    /**
     * 按注册名读取对应组件并做合法性校验（W06）。
     *
     * <p>路径 → 组件的映射即本方法；未列入"无组件即无效"的路径一律返回 {@code false}。
     * 单独暴露出来便于离线自检（真实物品未注册时也能用任意 {@code ItemStack} 验证映射）。</p>
     */
    public static boolean hasValidRequiredData(String path, ItemStack stack) {
        if (path == null || stack == null || stack.isEmpty()) {
            return false;
        }
        return switch (path) {
            case "magic_study_certificate" -> {
                CertData data = stack.get(ModComponents.CERT_DATA);
                yield data != null && MagicDataRules.isValidCert(data);
            }
            case "beginner_magic_exam_application", "magic_trial_application" -> {
                ApplicationData data = stack.get(ModComponents.APPLICATION_DATA);
                yield data != null && MagicDataRules.isValidApplication(data);
            }
            case "magic_proficiency_certificate" -> {
                ProficiencyData data = stack.get(ModComponents.PROFICIENCY_DATA);
                yield data != null && MagicDataRules.isValidProficiency(data);
            }
            case "magic_trial_promotion_certificate" -> {
                PromotionData data = stack.get(ModComponents.PROMOTION_DATA);
                yield data != null && MagicDataRules.isValidPromotion(data);
            }
            default -> false;
        };
    }
}
