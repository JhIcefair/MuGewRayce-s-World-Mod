package com.mugewrayce.sizeadjuster.container;

import com.mugewrayce.sizeadjuster.block.MagicAttachmentTableBlockEntity;
import com.mugewrayce.sizeadjuster.component.AttachmentBookData;
import com.mugewrayce.sizeadjuster.component.AttachmentData;
import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.StaffData;
import com.mugewrayce.sizeadjuster.item.AttachmentBookItem;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.item.MagicStaffItem;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import com.mugewrayce.sizeadjuster.magic.StaffAttachRules;
import com.mugewrayce.sizeadjuster.magic.WeaponRules;
import com.mugewrayce.sizeadjuster.magic.mana.ManaCost;
import com.mugewrayce.sizeadjuster.menu.AbstractModMenu;
import com.mugewrayce.sizeadjuster.registry.MenuRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

/**
 * 魔法附加台菜单（W10）：5 个专用格 + 玩家真实背包 36 格 = 41 格。
 *
 * <p>① 学习证/附加书；②③④ 木棍/普通水晶（无序）；⑤ 法杖/普通武器（也是产物格）。
 * 两键都是点按式，客户端只在松开时发 button id；全部判定与消耗在服务端重算。</p>
 */
public class MagicAttachmentTableMenu extends AbstractModMenu {

    public static final int SIZE = 5;

    public static final int SLOT_SOURCE = 0;
    public static final int SLOT_MATERIAL_1 = 1;
    public static final int SLOT_MATERIAL_2 = 2;
    public static final int SLOT_MATERIAL_3 = 3;
    public static final int SLOT_TARGET = 4;

    public static final int BUTTON_CRAFT_STAFF = 0;
    public static final int BUTTON_ATTACH = 1;

    /** 合成魔法杖消耗 30；分支 A（法杖升级/附加）不扣。 */
    public static final int COST_CRAFT_STAFF = ManaCost.COST_CRAFT_STAFF;
    /** 附加书给普通武器消耗 10。 */
    public static final int COST_ATTACH_BOOK = ManaCost.COST_ATTACH_BOOK;

    public static final int HINT_NONE = 0;
    public static final int HINT_MATERIALS = 1;
    public static final int HINT_CERT = 2;
    public static final int HINT_CERT_MISMATCH = 3;
    public static final int HINT_STAFF_UNBOUND = 4;
    public static final int HINT_NON_NATURAL = 5;
    public static final int HINT_NO_MANA = 6;
    public static final int HINT_OUTPUT_BLOCKED = 7;
    public static final int HINT_BOOK = 8;
    public static final int HINT_WEAPON = 9;
    public static final int HINT_ATTACHMENT_LEVEL = 10;
    public static final int HINT_NOTHING = 11;

    public static final MenuType<MagicAttachmentTableMenu> TYPE =
            MenuRegistry.registerMenu("magic_attachment_table", MagicAttachmentTableMenu::new);

    private final MagicAttachmentTableBlockEntity table;
    private final AttachmentTableData data = new AttachmentTableData();
    private int hintCode = HINT_NONE;

    /** 客户端：MenuType 工厂用。 */
    public MagicAttachmentTableMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SIZE), null, null);
    }

    /** 服务端：绑定真实 BlockEntity 容器 + 方块访问。 */
    public MagicAttachmentTableMenu(int id, Inventory playerInventory, Container container,
                                    ContainerLevelAccess access, Block block) {
        super(TYPE, id, playerInventory, container, SIZE, access, block);
        this.table = container instanceof MagicAttachmentTableBlockEntity attachment ? attachment : null;

        this.addOneItemSlot(container, SLOT_SOURCE, 9, 20,
                stack -> stack.is(MagicItems.magicStudyCertificate)
                        || stack.getItem() instanceof AttachmentBookItem);
        this.addOneItemSlot(container, SLOT_MATERIAL_1, 9, 55,
                stack -> stack.is(Items.STICK) || stack.is(MagicItems.magicCrystal));
        this.addOneItemSlot(container, SLOT_MATERIAL_2, 9, 73,
                stack -> stack.is(Items.STICK) || stack.is(MagicItems.magicCrystal));
        this.addOneItemSlot(container, SLOT_MATERIAL_3, 9, 91,
                stack -> stack.is(Items.STICK) || stack.is(MagicItems.magicCrystal));
        this.addOneItemSlot(container, SLOT_TARGET, 111, 75,
                stack -> stack.getItem() instanceof MagicStaffItem
                        || WeaponRules.isAttachableWeapon(stack));

        this.addPlayerInventory(39, 166);
        this.addModDataSlots(this.data);
    }

    // ------------------------------------------------------------------
    // 按钮
    // ------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        return switch (id) {
            case BUTTON_CRAFT_STAFF -> this.craftStaff(player);
            case BUTTON_ATTACH -> this.attachOrUpgrade(player);
            default -> false;
        };
    }

    private boolean craftStaff(Player player) {
        this.clearHint();
        ItemStack source = this.slots.get(SLOT_SOURCE).getItem();
        CertData cert = source.get(ModComponents.CERT_DATA);
        if (cert == null || !MagicDataRules.isValidCert(cert)) {
            this.hintCode = HINT_CERT;
            return true;
        }
        if (!this.hasStaffMaterials()) {
            this.hintCode = HINT_MATERIALS;
            return true;
        }
        if (!this.slots.get(SLOT_TARGET).getItem().isEmpty()) {
            this.hintCode = HINT_OUTPUT_BLOCKED;
            return true;
        }

        ServerLevel server = this.serverLevel(player);
        if (server == null || this.table == null) {
            return false;
        }
        if (!ManaCost.payBlockChunk(server, this.table.getBlockPos(), COST_CRAFT_STAFF)) {
            this.hintCode = HINT_NO_MANA;
            return true;
        }

        this.consumeStaffMaterials();
        ItemStack staff = new ItemStack(MagicItems.magicStaff);
        staff.set(ModComponents.STAFF_DATA, StaffAttachRules.bindCert(cert));
        this.slots.get(SLOT_TARGET).set(staff);
        this.hintCode = HINT_NONE;
        return true;
    }

    private boolean attachOrUpgrade(Player player) {
        this.clearHint();
        ItemStack source = this.slots.get(SLOT_SOURCE).getItem();
        if (source.getItem() instanceof AttachmentBookItem) {
            return this.attachBook(player);
        }
        return this.upgradeStaff();
    }

    /** 分支 A：学习证 + 已绑定法杖 → 追加/升级，不消耗、不扣魔。 */
    private boolean upgradeStaff() {
        ItemStack source = this.slots.get(SLOT_SOURCE).getItem();
        CertData cert = source.get(ModComponents.CERT_DATA);
        if (cert == null || !MagicDataRules.isValidCert(cert)) {
            this.hintCode = HINT_CERT;
            return true;
        }

        ItemStack staffStack = this.slots.get(SLOT_TARGET).getItem();
        if (!(staffStack.getItem() instanceof MagicStaffItem)) {
            this.hintCode = HINT_WEAPON;
            return true;
        }

        StaffData staff = staffStack.getOrDefault(ModComponents.STAFF_DATA, StaffData.DEFAULT);
        if (!MagicDataRules.canUpgradeStaff(staff, cert)) {
            if (!MagicDataRules.isValidStaff(staff) || staff.boundCertNo() <= 0) {
                this.hintCode = HINT_STAFF_UNBOUND;
            } else if (staff.boundCertNo() != cert.no()) {
                this.hintCode = HINT_CERT_MISMATCH;
            } else {
                this.hintCode = HINT_CERT;
            }
            return true;
        }
        if (StaffAttachRules.naturalEntries(cert.entries()).isEmpty()) {
            this.hintCode = HINT_NON_NATURAL;
            return true;
        }

        StaffData merged = StaffAttachRules.merge(staff, cert);
        if (!MagicDataRules.isValidStaff(merged)) {
            this.hintCode = HINT_NON_NATURAL;
            return true;
        }
        if (!StaffAttachRules.hasChange(staff, merged)) {
            this.hintCode = HINT_NOTHING;
            return true;
        }

        staffStack.set(ModComponents.STAFF_DATA, merged);
        this.slots.get(SLOT_TARGET).setChanged();
        this.hintCode = HINT_NONE;
        return true;
    }

    /** 分支 B：带等级附加书 + 普通武器 → 写 AttachmentData；消耗 1 本书 + 10 点。 */
    private boolean attachBook(Player player) {
        ItemStack source = this.slots.get(SLOT_SOURCE).getItem();
        AttachmentBookData book = source.getOrDefault(
                ModComponents.ATTACHMENT_BOOK_DATA, AttachmentBookData.DEFAULT);
        if (book.level() < MagicDataRules.ATTACH_MIN || book.level() > MagicDataRules.ATTACH_MAX) {
            this.hintCode = HINT_BOOK;
            return true;
        }

        ItemStack weapon = this.slots.get(SLOT_TARGET).getItem();
        if (!WeaponRules.isAttachableWeapon(weapon)) {
            this.hintCode = HINT_WEAPON;
            return true;
        }

        int current = weapon.getOrDefault(ModComponents.ATTACHMENT_DATA, AttachmentData.DEFAULT).level();
        if (!MagicDataRules.canOverwriteAttachment(current, book.level())) {
            this.hintCode = HINT_ATTACHMENT_LEVEL;
            return true;
        }

        ServerLevel server = this.serverLevel(player);
        if (server == null || this.table == null) {
            return false;
        }
        if (!ManaCost.payBlockChunk(server, this.table.getBlockPos(), COST_ATTACH_BOOK)) {
            this.hintCode = HINT_NO_MANA;
            return true;
        }

        weapon.set(ModComponents.ATTACHMENT_DATA, new AttachmentData(book.level()));
        this.slots.get(SLOT_TARGET).setChanged();
        this.removeFromSlot(SLOT_SOURCE, 1);
        this.hintCode = HINT_NONE;
        return true;
    }

    // ------------------------------------------------------------------
    // 材料 / 槽位辅助
    // ------------------------------------------------------------------

    private boolean hasStaffMaterials() {
        int sticks = 0;
        int crystals = 0;
        for (int i = SLOT_MATERIAL_1; i <= SLOT_MATERIAL_3; i++) {
            ItemStack stack = this.slots.get(i).getItem();
            if (stack.is(Items.STICK)) {
                sticks += stack.getCount();
            } else if (stack.is(MagicItems.magicCrystal)) {
                crystals += stack.getCount();
            }
        }
        return sticks >= 2 && crystals >= 1;
    }

    private void consumeStaffMaterials() {
        int needSticks = 2;
        int needCrystal = 1;
        for (int i = SLOT_MATERIAL_1; i <= SLOT_MATERIAL_3 && (needSticks > 0 || needCrystal > 0); i++) {
            ItemStack stack = this.slots.get(i).getItem();
            if (needSticks > 0 && stack.is(Items.STICK)) {
                int take = Math.min(needSticks, stack.getCount());
                this.removeFromSlot(i, take);
                needSticks -= take;
            } else if (needCrystal > 0 && stack.is(MagicItems.magicCrystal)) {
                int take = Math.min(needCrystal, stack.getCount());
                this.removeFromSlot(i, take);
                needCrystal -= take;
            }
        }
    }

    private void removeFromSlot(int index, int amount) {
        ItemStack stack = this.slots.get(index).getItem();
        stack.shrink(amount);
        if (stack.isEmpty()) {
            this.slots.get(index).setByPlayer(ItemStack.EMPTY);
        } else {
            this.slots.get(index).setChanged();
        }
    }

    private ServerLevel serverLevel(Player player) {
        return player.level() instanceof ServerLevel server ? server : null;
    }

    // ------------------------------------------------------------------
    // 提示 / 同步
    // ------------------------------------------------------------------

    public int getHintCode() {
        return this.data.get(0);
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        this.hintCode = HINT_NONE;
    }

    private void clearHint() {
        this.hintCode = HINT_NONE;
    }

    private final class AttachmentTableData implements ContainerData {

        private final int[] synced = new int[1];

        @Override
        public int get(int index) {
            if (MagicAttachmentTableMenu.this.playerInventory.player.level().isClientSide()) {
                return index == 0 ? this.synced[0] : 0;
            }
            return index == 0 ? MagicAttachmentTableMenu.this.hintCode : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                this.synced[0] = value;
            }
        }

        @Override
        public int getCount() {
            return 1;
        }
    }
}
