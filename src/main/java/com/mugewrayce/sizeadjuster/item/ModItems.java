package com.mugewrayce.sizeadjuster.item;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

import java.util.function.Function;

/**
 * 模组物品注册入口。
 * 注意：26.1 中 Item 构造时会校验 Properties.itemId（否则抛 "Item id not set"），
 * 因此必须先用 setId() 把注册键写入 Properties，再构造并注册。
 * 净化系列物品统一特性：火焰免疫（fireResistant）+ 不可破坏（UNBREAKABLE 组件）。
 */
public final class ModItems {

    /** 净化水晶（Equipoise Crystal）：铁剑基础 + 面板伤害 10；右键可举起。 */
    public static Item equipoiseCrystal;

    /** 起源令牌（Origin Token）：由原分类图标贴图 2.png 注册为实际物品；右键可举起。 */
    public static Item originToken;

    /** 净化法杖（Equipoise Staff）：模型同原版矛；右键可举起。 */
    public static Item equipoiseStaff;

    /** 净化圣剑（Equipoise Sword）：面板伤害 2000、攻速 2.0；有横扫。 */
    public static Item equipoiseSword;

    /** 净化圣弓（Equipoise Bow）：套用原版弓功能。 */
    public static Item equipoiseBow;

    /** 柃之册（Book of Ling）：自家书贴图；右键打开功能选择 GUI。 */
    public static Item bookOfLing;

    /** 归零（Zero Reset）：自家贴图/模型，手持姿态见模型 display；右键直接发射箭矢。 */
    public static Item zeroReset;

    /** 百杉包（Baishan Bag）：右键打开 54 格大箱子式背包；手持模型套用木棍。 */
    public static Item baishanBag;

    /** 净化光环（Equipoise Halo）：头盔位护甲，穿戴后外显；20护甲/20韧性/80%击退抗性；自带附魔。 */
    public static Item equipoiseHalo;

    /** 生命转换令牌（Life Conversion Token）：右键打开角色切换 GUI；贴图 ls\17.png。 */
    public static Item lifeConversionToken;

    private ModItems() {
    }

    public static void register() {
        // 净化水晶：铁剑基础 + 面板伤害 10（不叠加材料加成）+ 火焰免疫 + 不可破坏 + 举起；
        // 长按右键提供增益状态效果（力量X/生命恢复20/抗性提升50/抗火/伤害吸收X/饱和），松开后维持 60 秒
        equipoiseCrystal = registerItem("equipoise_crystal",
                base(new Item.Properties().sword(ToolMaterial.IRON, 0.0F, -2.4F)
                        .attributes(damageModifiers(9.0, -2.4))),
                EquipoiseCrystalItem::new);

        // 起源令牌：由原分类图标物品转为实际物品（贴图 2.png 不变），右键仅右臂举平（BRUSH 单臂动画）
        originToken = registerItem("origin_token", base(new Item.Properties()),
                props -> new LiftableItem(props, ItemUseAnimation.BRUSH));

        // 净化法杖：右键长按施法，按 StaffCastModes 的 7 种模式释放
        equipoiseStaff = registerItem("equipoise_staff",
                base(new Item.Properties()), EquipoiseStaffItem::new);

        // 净化圣剑：钻石剑基础 + 面板伤害 2000、攻速 2.0（不叠加材料加成）
        equipoiseSword = registerItem("equipoise_sword",
                base(new Item.Properties().sword(ToolMaterial.DIAMOND, 0.0F, -2.0F)
                        .attributes(damageModifiers(1999.0, -2.0))));

        // 净化圣弓：套用原版弓（耐久 384，可附魔，右键蓄力）；无需背包有箭即可发射
        equipoiseBow = registerItem("equipoise_bow",
                base(new Item.Properties().durability(384).enchantable(1)), EquipoiseBowItem::new);

        // 柃之册：模型贴图套用自家书贴图（10.png）；右键打开功能选择 GUI
        bookOfLing = registerItem("book_of_ling", new Item.Properties(), BookOfLingItem::new);

        // 归零：右键直接发射箭矢；火焰免疫 + 不可破坏
        zeroReset = registerItem("zero_reset", base(new Item.Properties()), ZeroResetItem::new);

        // 百杉包：右键打开 54 格大箱子式背包（内容随物品持久化）
        baishanBag = registerItem("baishan_bag",
                new Item.Properties().stacksTo(1), BaishanBagItem::new);

        // 净化光环：头盔位可装备（EQUIPPABLE 组件，右键自动穿戴）；
        // W19 去掉 assetId → 不再走护甲贴图层，改为“头戴物品模型”（models/item + display.head），
        // 外显是一枚悬浮的发光环而不是头盔贴图；20 护甲 / 20 韧性 / 80% 击退抗性；
        // 自带附魔（延迟组件，随注册表就绪后解析）；火焰免疫 + 不可破坏（base）
        equipoiseHalo = registerItem("equipoise_halo",
                base(new Item.Properties().stacksTo(1)
                        .attributes(haloModifiers())
                        .component(DataComponents.EQUIPPABLE, haloEquippable())
                        .delayedComponent(DataComponents.ENCHANTMENTS,
                                lookup -> haloEnchantments(lookup))));

        // 生命转换令牌：右键打开角色切换 GUI（默认/魔法体/特殊-icefair）；
        // 火焰免疫 + 不可破坏（同净化系列）；模型/手持样式同起源令牌；贴图 ls\17.png
        lifeConversionToken = registerItem("life_conversion_token",
                base(new Item.Properties()), LifeConversionTokenItem::new);
    }

    /**
     * 自定义武器属性：攻击伤害 = 1（基础）+ damage（不叠加材料加成）；
     * 攻速 = 4（基础）+ speedModifier。
     */
    private static ItemAttributeModifiers damageModifiers(double damage, double speedModifier) {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, damage,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, speedModifier,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** 净化系列物品统一特性：不怕火/岩浆 + 不可破坏。 */
    private static Item.Properties base(Item.Properties properties) {
        return properties
                .fireResistant()
                .component(DataComponents.UNBREAKABLE, Unit.INSTANCE);
    }

    /** 净化光环属性：+20 护甲、+20 盔甲韧性、+80% 击退抗性（穿戴于头盔位生效）。 */
    private static ItemAttributeModifiers haloModifiers() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ARMOR,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "halo.armor"),
                                20.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD)
                .add(Attributes.ARMOR_TOUGHNESS,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "halo.armor_toughness"),
                                20.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD)
                .add(Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(
                                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, "halo.knockback_resistance"),
                                0.8, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.HEAD)
                .build();
    }

    /**
     * 净化光环可装备性：头盔位；右键自动穿戴/交换（swappable）；声音用通用穿戴音。
     *
     * <p>W19：<b>不设 assetId</b>。26.1 里 {@code HumanoidArmorLayer.shouldRender} 只在
     * {@code EQUIPPABLE.assetId()} 存在时才走护甲贴图层；去掉后由 {@code CustomHeadLayer}
     * 在 {@code ItemDisplayContext.HEAD} 下渲染<b>物品模型</b>，并应用模型 JSON 的
     * {@code display.head}（见 {@code models/item/equipoise_halo.json}）。</p>
     *
     * <p>原头盔贴图与 {@code equipment/equipoise_halo.json} 暂时保留但不再引用，
     * 下一版由用户决定是否删除。</p>
     */
    private static Equippable haloEquippable() {
        return Equippable.builder(EquipmentSlot.HEAD)
                .setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC)
                .setDispensable(true)
                .setSwappable(true)
                .build();
    }

    /**
     * 净化光环自带附魔：水下速掘 I、保护 50、爆炸保护 50、火焰保护 50、弹射物保护 50、水下呼吸 X。
     * 附魔注册表是数据驱动（世界加载后才可用），故用延迟组件在注册表就绪时解析持有者。
     */
    private static ItemEnchantments haloEnchantments(HolderLookup.Provider lookup) {
        HolderLookup.RegistryLookup<Enchantment> enchantments = lookup.lookupOrThrow(Registries.ENCHANTMENT);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantments.getOrThrow(Enchantments.AQUA_AFFINITY), 1);
        mutable.set(enchantments.getOrThrow(Enchantments.PROTECTION), 50);
        mutable.set(enchantments.getOrThrow(Enchantments.BLAST_PROTECTION), 50);
        mutable.set(enchantments.getOrThrow(Enchantments.FIRE_PROTECTION), 50);
        mutable.set(enchantments.getOrThrow(Enchantments.PROJECTILE_PROTECTION), 50);
        mutable.set(enchantments.getOrThrow(Enchantments.RESPIRATION), 10);
        return mutable.toImmutable();
    }

    private static Item registerItem(String id, Item.Properties properties) {
        return registerItem(id, properties, Item::new);
    }

    private static Item registerItem(String id, Item.Properties properties,
                                     Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(
                Registries.ITEM,
                Identifier.fromNamespaceAndPath(SizeAdjusterMod.MOD_ID, id));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }
}
