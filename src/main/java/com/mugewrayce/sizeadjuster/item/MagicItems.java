package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.registry.ItemRegistry;
import net.minecraft.world.item.Item;

/**
 * W08 魔法体系 12 件非方块物品（接口约定 §1.2）。
 *
 * <p>本任务只做注册、默认组件与创造分类；GUI / 魔力 / 施法 / 考试全部留后续任务。
 * 证书类（学习证 / 申请书 / 证明 / 认证）<b>不写默认组件</b>，创造模式直接拿到即为无效物品，
 * 与 W06 的 {@code InvalidItemRules} 规则一致。</p>
 */
public final class MagicItems {

    /** 魔力值探测器；GUI/扫描 W12，区块魔力 W13。 */
    public static Item manaDetector;

    /** 普通魔法杖；默认 {@code StaffData(0, [])}，合成/绑定 W10，施法 W14。 */
    public static Item magicStaff;

    /** 普通魔法水晶；默认 {@code CrystalData(0, false, false)}，设置 GUI W12，供能 W13。 */
    public static Item magicCrystal;

    /** 永恒水晶；无组件（免消耗规则 W13）。 */
    public static Item eternalCrystal;

    /** 魔法学习证；默认无 {@code cert_data}，讲台发放/更新 W11。 */
    public static Item magicStudyCertificate;

    /** 魔法书；设置 GUI/起源技能 W12/W14。 */
    public static Item magicBook;

    /** 初学魔法考试申请书；默认无 {@code application_data}，讲台生成 W11、考试 W15。 */
    public static Item beginnerExamApplication;

    /** 魔法熟练证明；默认无 {@code proficiency_data}，讲台消耗/升级 W11。 */
    public static Item proficiencyCertificate;

    /** 魔法试炼申请书；默认无 {@code application_data}，讲台生成 W11、试炼 W15。 */
    public static Item trialApplication;

    /** 魔法试炼晋级认证；默认无 {@code promotion_data}，讲台消耗/升级 W11。 */
    public static Item promotionCertificate;

    /** 魔法附加书；自身带 I/II/III 等级（W10），倍率写在武器上（W14）。 */
    public static Item attachmentBook;

    /** 生命迁移石；角色转换 GUI W12。 */
    public static Item lifeMigrationStone;

    private MagicItems() {
    }

    public static void register() {
        manaDetector = ItemRegistry.register("mana_detector", Item::new);

        magicStaff = ItemRegistry.register("magic_staff", MagicStaffItem::new);
        magicCrystal = ItemRegistry.register("magic_crystal", MagicCrystalItem::new);

        eternalCrystal = ItemRegistry.register("eternal_crystal", Item::new);
        magicStudyCertificate = ItemRegistry.register("magic_study_certificate", Item::new);
        magicBook = ItemRegistry.register("magic_book", MagicBookItem::new);
        beginnerExamApplication = ItemRegistry.register("beginner_magic_exam_application", Item::new);
        proficiencyCertificate = ItemRegistry.register("magic_proficiency_certificate", Item::new);
        trialApplication = ItemRegistry.register("magic_trial_application", Item::new);
        promotionCertificate = ItemRegistry.register("magic_trial_promotion_certificate", Item::new);
        attachmentBook = ItemRegistry.register("magic_attachment_book", AttachmentBookItem::new);
        lifeMigrationStone = ItemRegistry.register("life_migration_stone", Item::new);
    }
}
