package com.mugewrayce.sizeadjuster.container;

import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.ProficiencyData;
import com.mugewrayce.sizeadjuster.component.PromotionData;
import com.mugewrayce.sizeadjuster.data.CertCounter;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.LecternRules;
import com.mugewrayce.sizeadjuster.magic.MagicConflicts;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import com.mugewrayce.sizeadjuster.magic.MagicSkill;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import com.mugewrayce.sizeadjuster.menu.AbstractModMenu;
import com.mugewrayce.sizeadjuster.menu.InfiniteSlot;
import com.mugewrayce.sizeadjuster.menu.ModSlot;
import com.mugewrayce.sizeadjuster.registry.BlockRegistry;
import com.mugewrayce.sizeadjuster.registry.MenuRegistry;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.sounds.SoundEvents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 魔法学习讲台菜单（W11）：5 个专用格 + 玩家真背包 36 格 = 41 格。
 *
 * <p>专用格：0 ①学习证 / 1 ②证明认证 / 2 ③无限附加台 / 3 ④无限聚合台 / 4 ⑤输出格 A。
 * 状态（模式 / 类型 / 大类 / 具体项 / 页码 / 等级 / 提示 / 技能勾选）全部由服务端字段决定，
 * 通过 {@link ContainerData} 的 16 个 int 同步（接口约定 §3.4）；客户端只读并只发 buttonId。</p>
 *
 * <p>规则判定集中在 {@link LecternRules}（纯函数），本类只负责读写槽位、发号、发物品与消耗证明。</p>
 */
public class MagicStudyLecternMenu extends AbstractModMenu {

    public static final int SIZE = 5;

    public static final int SLOT_CERT = 0;
    public static final int SLOT_PROOF = 1;
    public static final int SLOT_INFINITE_ATTACHMENT = 2;
    public static final int SLOT_INFINITE_AGGREGATION = 3;
    public static final int SLOT_OUTPUT = 4;

    // 按钮 id（交接单 §2.4；客户端/服务端同一套）
    public static final int BUTTON_APPLY = 0;
    public static final int BUTTON_UPDATE = 1;
    public static final int BUTTON_EXAM = 2;
    public static final int BUTTON_CONFIRM = 3;
    public static final int BUTTON_TYPE_BASE = 10;
    public static final int BUTTON_MAJOR_BASE = 20;
    public static final int BUTTON_SUB_BASE = 30;
    public static final int BUTTON_LEVEL_BASE = 40;
    public static final int BUTTON_TYPE_PREV = 50;
    public static final int BUTTON_TYPE_NEXT = 51;
    public static final int BUTTON_SUB_PREV = 52;
    public static final int BUTTON_SUB_NEXT = 53;

    // 模式
    public static final int MODE_NONE = 0;
    public static final int MODE_APPLY = 1;
    public static final int MODE_UPDATE = 2;
    public static final int MODE_EXAM = 3;

    // ContainerData 索引（接口约定 §3.4）
    public static final int DATA_SIZE = 16;
    public static final int DATA_MODE = 0;
    public static final int DATA_TYPE = 1;
    public static final int DATA_MAJOR = 2;
    public static final int DATA_SUB = 3;
    public static final int DATA_TYPE_PAGE = 4;
    public static final int DATA_SUB_PAGE_POINT = 5;
    public static final int DATA_SUB_PAGE_LINE = 6;
    public static final int DATA_SUB_PAGE_FORMATION = 7;
    public static final int DATA_SUB_PAGE_SKILL = 8;
    public static final int DATA_LEVEL = 9;
    public static final int DATA_HINT = 10;
    public static final int DATA_FLAGS = 11;
    public static final int DATA_SKILL_MASK = 12;
    public static final int DATA_CAN_CONFIRM = 13;
    public static final int DATA_UPDATE_AVAILABLE = 14;
    public static final int DATA_CERT_VALID = 15;

    public static final int FLAG_EXAM_NO_CONTENT = 2;

    public static final MenuType<MagicStudyLecternMenu> TYPE =
            MenuRegistry.registerMenu("magic_study_lectern", MagicStudyLecternMenu::new);
    public static final MenuType<MagicStudyLecternMenu> ADVANCED_TYPE =
            MenuRegistry.registerMenu("advanced_magic_study_lectern",
                    (id, inventory) -> new MagicStudyLecternMenu(id, inventory, true));

    private final boolean advanced;
    private final LecternData data = new LecternData();
    private final ItemStack[] lastItems = new ItemStack[SIZE];

    private int mode = MODE_NONE;
    private MagicType type;
    private CastMode major;
    private CastSubMode sub;
    private int typePage;
    private final int[] subPage = new int[4];
    private int level;
    private int skillMask;
    private int hintCode = LecternRules.HINT_NONE;

    /** 客户端：普通讲台 MenuType 工厂用。 */
    public MagicStudyLecternMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, false);
    }

    /** 客户端：高级讲台 MenuType 工厂用。 */
    public MagicStudyLecternMenu(int id, Inventory playerInventory, boolean advanced) {
        this(id, playerInventory, new SimpleContainer(SIZE), null, null, advanced);
    }

    /** 服务端：绑定真实 BlockEntity 容器 + 方块访问。 */
    public MagicStudyLecternMenu(int id, Inventory playerInventory, Container container,
                                 ContainerLevelAccess access, Block block) {
        this(id, playerInventory, container, access, block, false);
    }

    /** 服务端：高级讲台绑定自己的容器；advanced 决定 MenuType 与更新规则。 */
    public MagicStudyLecternMenu(int id, Inventory playerInventory, Container container,
                                 ContainerLevelAccess access, Block block, boolean advanced) {
        super(advanced ? ADVANCED_TYPE : TYPE, id, playerInventory, container, SIZE, access, block);
        this.advanced = advanced;

        this.addOneItemSlot(container, SLOT_CERT, 9, 20,
                stack -> stack.is(MagicItems.magicStudyCertificate));
        this.addOneItemSlot(container, SLOT_PROOF, 9, 44,
                stack -> stack.is(MagicItems.proficiencyCertificate)
                        || stack.is(MagicItems.promotionCertificate));
        this.addModSlot(new InfiniteSlot(container, SLOT_INFINITE_ATTACHMENT, 9, 68,
                () -> new ItemStack(BlockRegistry.MAGIC_ATTACHMENT_TABLE_ITEM)));
        this.addModSlot(new InfiniteSlot(container, SLOT_INFINITE_AGGREGATION, 9, 86,
                () -> new ItemStack(BlockRegistry.MAGIC_AGGREGATION_TABLE_ITEM)));
        // ⑤ 输出格 A：只出不进，且最多 1 个（不能用 W09 的 addOutputSlot，它默认 64）。
        this.addModSlot(new ModSlot(container, SLOT_OUTPUT, 9, 124, 1, stack -> false));

        this.addPlayerInventory(39, 166);
        this.addModDataSlots(this.data);
    }

    // ------------------------------------------------------------------
    // 客户端读取（全部从 ContainerData 回读）
    // ------------------------------------------------------------------

    public int getDataValue(int index) {
        return this.data.get(index);
    }

    public int getHintCode() {
        return this.data.get(DATA_HINT);
    }

    public int getMode() {
        return this.data.get(DATA_MODE);
    }

    public int getSelectedTypeIndex() {
        return this.data.get(DATA_TYPE);
    }

    public MagicType getSelectedType() {
        int index = this.getSelectedTypeIndex();
        List<MagicType> types = LecternRules.types();
        return index >= 0 && index < types.size() ? types.get(index) : null;
    }

    public CastMode getSelectedMajor() {
        int code = this.data.get(DATA_MAJOR);
        return code >= 0 && code < 4 ? castModeByCode(code) : null;
    }

    public CastSubMode getSelectedSub() {
        int code = this.data.get(DATA_SUB);
        return code >= 0 && code < CastSubMode.values().length ? CastSubMode.values()[code] : null;
    }

    public int getTypePage() {
        return this.data.get(DATA_TYPE_PAGE);
    }

    public int getSubPage(CastMode mode) {
        return this.data.get(subPageIndex(mode));
    }

    public int getLevel() {
        return this.data.get(DATA_LEVEL);
    }

    public int getSkillMask() {
        return this.data.get(DATA_SKILL_MASK);
    }

    public boolean isAdvanced() {
        return this.advanced;
    }

    public boolean isExamNoContent() {
        return (this.data.get(DATA_FLAGS) & FLAG_EXAM_NO_CONTENT) != 0;
    }

    public boolean isCertValid() {
        return this.data.get(DATA_CERT_VALID) != 0;
    }

    public boolean isUpdateAvailable() {
        return this.data.get(DATA_UPDATE_AVAILABLE) != 0;
    }

    public boolean isCanConfirm() {
        return this.data.get(DATA_CAN_CONFIRM) != 0;
    }

    /** 当前选中的类型在学习证里是否已学过（客户端判断块②是否出现）。 */
    public boolean isSelectedTypeLearned() {
        MagicType selected = this.getSelectedType();
        CertData cert = this.certData();
        return selected != null && cert != null && LecternRules.hasType(cert, selected);
    }

    /** 若选中的类型与已学类型冲突，返回冲突的已学类型（供红字 %s 用）；否则 null。 */
    public MagicType getConflictingType() {
        return this.getConflictFor(this.getSelectedType());
    }

    /** 候选类型与学习证里已学类型是否冲突；冲突时返回冲突的已学类型（客户端置灰用）。 */
    public MagicType getConflictFor(MagicType candidate) {
        CertData cert = this.certData();
        if (candidate == null || cert == null) {
            return null;
        }
        return MagicConflicts.firstConflict(LecternRules.ownedTypes(cert), candidate).orElse(null);
    }

    // ------------------------------------------------------------------
    // 按钮
    // ------------------------------------------------------------------

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player.level().isClientSide()) {
            return false;
        }
        this.hintCode = LecternRules.HINT_NONE;

        if (id >= BUTTON_TYPE_BASE && id < BUTTON_TYPE_BASE + LecternRules.TYPE_PAGE_SIZE) {
            this.selectType(id - BUTTON_TYPE_BASE);
        } else if (id >= BUTTON_MAJOR_BASE && id < BUTTON_MAJOR_BASE + 4) {
            this.selectMajor(id - BUTTON_MAJOR_BASE);
        } else if (id >= BUTTON_SUB_BASE && id < BUTTON_SUB_BASE + 6) {
            this.selectSub(id - BUTTON_SUB_BASE);
        } else if (id >= BUTTON_LEVEL_BASE && id < BUTTON_LEVEL_BASE + 5) {
            this.selectLevel(id - BUTTON_LEVEL_BASE + 1);
        } else {
            switch (id) {
                case BUTTON_APPLY -> this.toggleMode(MODE_APPLY);
                case BUTTON_UPDATE -> this.toggleMode(MODE_UPDATE);
                case BUTTON_EXAM -> this.toggleMode(MODE_EXAM);
                case BUTTON_CONFIRM -> this.confirm();
                case BUTTON_TYPE_PREV -> this.turnTypePage(-1);
                case BUTTON_TYPE_NEXT -> this.turnTypePage(1);
                case BUTTON_SUB_PREV -> this.turnSubPage(-1);
                case BUTTON_SUB_NEXT -> this.turnSubPage(1);
                default -> {
                    return false;
                }
            }
        }
        this.broadcastChanges();
        return true;
    }

    private void toggleMode(int newMode) {
        if (newMode == MODE_UPDATE && this.mode != MODE_UPDATE && !this.certValid()) {
            this.hintCode = this.slots.get(SLOT_CERT).getItem().isEmpty()
                    ? LecternRules.HINT_CERT_MISSING : LecternRules.HINT_CERT_INVALID;
            return;
        }
        this.mode = this.mode == newMode ? MODE_NONE : newMode;
        this.clearSelections();
        this.hintCode = LecternRules.HINT_NONE;
    }

    private void selectType(int indexOnPage) {
        int global = this.typePage * LecternRules.TYPE_PAGE_SIZE + indexOnPage;
        List<MagicType> types = LecternRules.types();
        if (indexOnPage < 0 || global < 0 || global >= types.size()) {
            return;
        }
        MagicType candidate = types.get(global);
        if (candidate == this.type) {
            this.type = null;
            this.typePage = 0;
            this.clearSelections();
            return;
        }
        this.type = candidate;
        this.typePage = 0;
        this.major = null;
        this.sub = null;
        this.skillMask = 0;
        this.level = 0;
        this.resetSubPages();
        this.hintCode = this.mode == MODE_EXAM
                && LecternRules.examMajorsFor(candidate).isEmpty()
                ? LecternRules.HINT_NO_EXAM_CONTENT : LecternRules.HINT_NONE;
    }

    private void selectMajor(int index) {
        List<CastMode> majors = this.currentMajors();
        if (index < 0 || index >= majors.size()) {
            return;
        }
        CastMode candidate = majors.get(index);
        if (candidate == this.major) {
            this.major = null;
            this.sub = null;
            this.skillMask = 0;
            this.setSubPage(candidate, 0);
            this.syncLevelToEntry();
            return;
        }
        this.major = candidate;
        this.sub = null;
        this.skillMask = 0;
        this.setSubPage(candidate, 0);
        this.hintCode = LecternRules.HINT_NONE;
        this.syncLevelToEntry();
    }

    private void selectSub(int index) {
        if (this.type == null || this.major == null) {
            return;
        }
        if (this.major == CastMode.SKILL) {
            this.toggleSkill(index);
            return;
        }
        List<CastSubMode> subs = LecternRules.subsFor(this.type, this.major);
        if (index < 0 || index >= subs.size()) {
            return;
        }
        CastSubMode candidate = subs.get(index);
        this.sub = candidate == this.sub ? null : candidate;
        this.setSubPage(this.major, 0);
        this.hintCode = LecternRules.HINT_NONE;
        this.syncLevelToEntry();
    }

    private void toggleSkill(int index) {
        List<MagicSkill> skills = LecternRules.skillsFor(this.type);
        if (index < 0 || index >= skills.size()) {
            return;
        }
        this.skillMask ^= 1 << skills.get(index).ordinal();
        this.setSubPage(CastMode.SKILL, 0);
        this.hintCode = LecternRules.HINT_NONE;
    }

    private void selectLevel(int newLevel) {
        this.level = this.level == newLevel ? 0 : newLevel;
        this.hintCode = LecternRules.HINT_NONE;
    }

    private void turnTypePage(int delta) {
        int pages = LecternRules.typePageCount();
        this.typePage = Math.floorMod(this.typePage + delta, pages);
        this.hintCode = LecternRules.HINT_NONE;
    }

    private void turnSubPage(int delta) {
        if (this.type == null || this.major == null || this.major == CastMode.SKILL) {
            return;
        }
        int pages = LecternRules.subPageCount(this.type, this.major);
        this.setSubPage(this.major, Math.floorMod(this.getSubPage(this.major) + delta, pages));
        this.hintCode = LecternRules.HINT_NONE;
    }

    // ------------------------------------------------------------------
    // 确认的三个分支
    // ------------------------------------------------------------------

    private void confirm() {
        if (this.mode == MODE_NONE) {
            this.hintCode = LecternRules.HINT_NO_MODE;
            return;
        }
        if (this.type == null) {
            this.hintCode = LecternRules.HINT_NO_TYPE;
            return;
        }
        switch (this.mode) {
            case MODE_APPLY -> this.confirmApply();
            case MODE_UPDATE -> this.confirmUpdate();
            case MODE_EXAM -> this.confirmExam();
            default -> this.hintCode = LecternRules.HINT_NO_MODE;
        }
    }

    /** ① 申请学习证：新编号 + 点式（闪光例外 = 全部技能）；不消耗任何输入。 */
    private void confirmApply() {
        if (!this.outputEmpty()) {
            this.hintCode = LecternRules.HINT_OUTPUT_OCCUPIED;
            return;
        }
        OptionalInt number = CertCounter.nextNumber();
        if (number.isEmpty()) {
            this.hintCode = LecternRules.HINT_NUMBER_FAILED;
            return;
        }
        CertData cert = LecternRules.newCert(number.getAsInt(), this.type, LecternRules.LEVEL_BEGINNER);
        ItemStack certificate = new ItemStack(MagicItems.magicStudyCertificate);
        certificate.set(ModComponents.CERT_DATA, cert);
        this.slots.get(SLOT_OUTPUT).set(certificate);
        this.hintCode = LecternRules.HINT_NONE;
        SoundFx.at(this.playerInventory.player, SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.0F);
    }

    /** ② 更新学习证：新类型 / 学新模式 / 证明升级；不要求⑤空、不产出物品。 */
    private void confirmUpdate() {
        ItemStack certStack = this.slots.get(SLOT_CERT).getItem();
        if (certStack.isEmpty() || !certStack.is(MagicItems.magicStudyCertificate)) {
            this.hintCode = LecternRules.HINT_CERT_MISSING;
            return;
        }
        CertData cert = certStack.get(ModComponents.CERT_DATA);
        if (!MagicDataRules.isValidCert(cert)) {
            this.hintCode = LecternRules.HINT_CERT_INVALID;
            return;
        }

        boolean learnedType = LecternRules.hasType(cert, this.type);
        if (!learnedType) {
            this.learnNewType(certStack, cert);
            return;
        }
        if (this.major == null) {
            this.hintCode = LecternRules.HINT_NO_CATEGORY;
            return;
        }
        if (this.major == CastMode.SKILL) {
            this.learnSkills(certStack, cert);
            return;
        }
        List<CastSubMode> subs = LecternRules.subsFor(this.type, this.major);
        if (!subs.isEmpty() && (this.sub == null || !subs.contains(this.sub))) {
            this.hintCode = LecternRules.HINT_NO_SUB;
            return;
        }
        Optional<CertEntry> existing = LecternRules.findEntry(cert, this.type, this.major, this.sub);
        if (existing.isPresent()) {
            this.upgradeEntry(certStack, cert, existing.get());
            return;
        }
        this.learnNewMode(certStack, cert);
    }

    /** 学新类型：冲突检查 → 点式（或闪光全技能），②格证明不消耗。 */
    private void learnNewType(ItemStack certStack, CertData cert) {
        Optional<MagicType> conflict = MagicConflicts.firstConflict(
                LecternRules.ownedTypes(cert), this.type);
        if (conflict.isPresent()) {
            this.hintCode = LecternRules.HINT_CONFLICT;
            return;
        }
        CertData updated;
        if (this.type == MagicType.FLASH) {
            List<CertEntry> entries = new ArrayList<>();
            for (MagicSkill skill : LecternRules.skillsFor(this.type)) {
                entries.add(new CertEntry(this.type.id(), CastMode.SKILL.id(), skill.id(), 0));
            }
            updated = LecternRules.addEntries(cert, entries);
        } else {
            updated = LecternRules.addEntry(cert, new CertEntry(
                    this.type.id(), CastMode.POINT.id(), "", LecternRules.LEVEL_BEGINNER));
        }
        this.applyCert(certStack, updated);
        this.hintCode = LecternRules.HINT_NONE;
    }

    /** 学技能：多选，一次把选中的、尚未学过的技能全部加入（无等级、不消耗证明）。 */
    private void learnSkills(ItemStack certStack, CertData cert) {
        List<MagicSkill> skills = LecternRules.skillsFor(this.type);
        int selected = this.skillMask & skillMask(skills);
        if (selected == 0) {
            this.hintCode = LecternRules.HINT_SKILL_NONE;
            return;
        }
        List<CertEntry> toAdd = new ArrayList<>();
        for (MagicSkill skill : skills) {
            if ((selected & (1 << skill.ordinal())) != 0
                    && !LecternRules.isSkillLearned(cert, this.type, skill)) {
                toAdd.add(new CertEntry(this.type.id(), CastMode.SKILL.id(), skill.id(), 0));
            }
        }
        if (toAdd.isEmpty()) {
            this.hintCode = LecternRules.HINT_SKILL_LEARNED;
            return;
        }
        this.applyCert(certStack, LecternRules.addEntries(cert, toAdd));
        this.hintCode = LecternRules.HINT_NONE;
    }

    /** 已学条目：讲台只能靠②格证明升级；高级讲台无条件设等级、不读②格、不消耗。 */
    private void upgradeEntry(ItemStack certStack, CertData cert, CertEntry entry) {
        LecternRules.ApplyResult result = this.advanced
                ? LecternRules.forceLevel(entry, this.level)
                : LecternRules.applyProof(entry, this.proofRef());
        if (!result.ok()) {
            this.hintCode = result.hintCode();
            return;
        }
        this.applyCert(certStack, LecternRules.replaceEntry(cert, entry, result.newLevel()));
        if (!this.advanced) {
            this.shrinkProof();
        } else {
            SoundFx.at(this.playerInventory.player, SoundEvents.PLAYER_LEVELUP, 0.5F, 1.4F);
        }
        this.hintCode = LecternRules.HINT_NONE;
    }

    /**
     * 学新模式：普通讲台按“前置 + ②格证明”判定；高级讲台（W17）跳过前置、
     * 不读②格、不消耗，且新条目直接带选中等级。
     */
    private void learnNewMode(ItemStack certStack, CertData cert) {
        LecternRules.LearnResult result = this.advanced
                ? LecternRules.canLearnAdvanced(cert, this.type, this.major, this.sub, this.level)
                : LecternRules.canLearn(cert, this.type, this.major, this.sub, this.proofRef());
        if (!result.ok()) {
            this.hintCode = result.hintCode();
            return;
        }
        CertData updated = LecternRules.addEntry(cert, result.newEntry());
        if (result.upgradedEntry() != null) {
            updated = LecternRules.replaceEntry(updated, result.upgradedEntry(),
                    result.upgradedEntry().level());
        }
        this.applyCert(certStack, updated);
        if (result.consumesProof()) {
            this.shrinkProof();
        }
        this.hintCode = LecternRules.HINT_NONE;
    }

    /** ③ 考试或试炼：只写 type/major/sub/targetLevel，不依赖学习证、不消耗。 */
    private void confirmExam() {
        if (!LecternRules.canExam(this.type, this.major, this.sub)) {
            if (LecternRules.examMajorsFor(this.type).isEmpty()) {
                this.hintCode = LecternRules.HINT_NO_EXAM_CONTENT;
            } else if (this.major == null) {
                this.hintCode = LecternRules.HINT_NO_CATEGORY;
            } else {
                this.hintCode = LecternRules.HINT_NO_SUB;
            }
            return;
        }
        if (!this.outputEmpty()) {
            this.hintCode = LecternRules.HINT_OUTPUT_OCCUPIED;
            return;
        }
        ApplicationData application = LecternRules.examApplication(
                this.type, this.major, this.sub, this.level);
        ItemStack stack = new ItemStack(application.targetLevel() == 1
                ? MagicItems.beginnerExamApplication : MagicItems.trialApplication);
        stack.set(ModComponents.APPLICATION_DATA, application);
        this.slots.get(SLOT_OUTPUT).set(stack);
        this.hintCode = LecternRules.HINT_NONE;
        SoundFx.at(this.playerInventory.player, SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.0F);
    }

    // ------------------------------------------------------------------
    // 槽位拦截
    // ------------------------------------------------------------------

    @Override
    public void clicked(int slotId, int button, ContainerInput input, Player player) {
        boolean client = player.level().isClientSide();
        if (slotId == SLOT_INFINITE_ATTACHMENT || slotId == SLOT_INFINITE_AGGREGATION) {
            if (!client) {
                this.handleInfiniteClick(slotId, button, input, player);
            }
            return;
        }
        if (!client && input != null && this.rejectsPlacement(slotId, button, input, player)) {
            return;
        }
        super.clicked(slotId, button, input, player);
    }

    /** ①/②/⑤ 放错物品：不放、给红字（不能只靠 mayPlace 静默拒绝）。 */
    private boolean rejectsPlacement(int slotId, int button, ContainerInput input, Player player) {
        if (slotId < 0 || slotId >= SIZE) {
            return false;
        }
        Slot slot = this.slots.get(slotId);
        ItemStack carried = this.getCarried();
        switch (input) {
            case PICKUP, QUICK_CRAFT, PICKUP_ALL -> {
                if (carried.isEmpty() || slot.mayPlace(carried)) {
                    return false;
                }
                this.hintForSlot(slotId);
                return true;
            }
            case SWAP -> {
                if (button < 0 || button >= 9) {
                    return false;
                }
                ItemStack hotbar = player.getInventory().getItem(button);
                if (hotbar.isEmpty() || slot.mayPlace(hotbar)) {
                    return false;
                }
                this.hintForSlot(slotId);
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void hintForSlot(int slotId) {
        this.hintCode = slotId == SLOT_CERT ? LecternRules.HINT_CERT_SLOT_ONLY
                : slotId == SLOT_PROOF ? LecternRules.HINT_PROOF_SLOT_ONLY
                : LecternRules.HINT_OUTPUT_ONLY;
    }

    /** ③④：点一次给 1 个；异常情况静默失败（2026-11-06 决定，不弹红字）。 */
    private void handleInfiniteClick(int slotId, int button, ContainerInput input, Player player) {
        if (input == ContainerInput.QUICK_MOVE) {
            player.getInventory().add(this.infiniteStack(slotId));
            return;
        }
        if (input != ContainerInput.PICKUP || (button != 0 && button != 1)) {
            return;
        }
        ItemStack resident = this.infiniteStack(slotId);
        ItemStack carried = this.getCarried();
        if (carried.isEmpty()) {
            this.setCarried(resident);
            return;
        }
        if (!ItemStack.isSameItemSameComponents(carried, resident)) {
            return;
        }
        int max = Math.min(carried.getMaxStackSize(), resident.getMaxStackSize());
        if (carried.getCount() >= max) {
            return;
        }
        carried.grow(1);
        this.setCarried(carried);
    }

    private ItemStack infiniteStack(int slotId) {
        Slot slot = this.slots.get(slotId);
        return slot instanceof InfiniteSlot infinite ? infinite.residentStack() : ItemStack.EMPTY;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= this.slots.size()) {
            return ItemStack.EMPTY;
        }
        if (player.level().isClientSide()) {
            // 客户端不做预测，等服务端结果（避免与服务端判断不一致）
            return ItemStack.EMPTY;
        }
        if (index == SLOT_INFINITE_ATTACHMENT || index == SLOT_INFINITE_AGGREGATION) {
            // Shift+左键：同样只给 1 个，直接进背包；先合并同类堆，再找空格。
            player.getInventory().add(this.infiniteStack(index));
            return ItemStack.EMPTY;
        }
        return super.quickMoveStack(player, index);
    }

    // ------------------------------------------------------------------
    // 每 tick 服务端维护
    // ------------------------------------------------------------------

    @Override
    public void broadcastChanges() {
        if (!this.playerInventory.player.level().isClientSide()) {
            this.serverTick();
        }
        super.broadcastChanges();
    }

    private void serverTick() {
        if (this.itemSnapshotChanged()) {
            this.hintCode = LecternRules.HINT_NONE;
        }
        if (this.mode == MODE_UPDATE && !this.certValid()) {
            // 学习证被拿走 / 变成无效证 → 「更新学习证」自动弹起并清空选择。
            this.mode = MODE_NONE;
            this.clearSelections();
            this.hintCode = LecternRules.HINT_NONE;
        }
        if (this.type != null && !LecternRules.isLearnableType(this.type)) {
            this.type = null;
            this.clearSelections();
            this.hintCode = LecternRules.HINT_NONE;
        }
    }

    private boolean itemSnapshotChanged() {
        boolean changed = false;
        for (int i = 0; i < SIZE; i++) {
            ItemStack current = this.slots.get(i).getItem();
            ItemStack previous = this.lastItems[i];
            if (previous == null) {
                changed |= !current.isEmpty();
            } else if (!ItemStack.isSameItemSameComponents(previous, current)
                    || previous.getCount() != current.getCount()) {
                changed = true;
            }
            this.lastItems[i] = current.copy();
        }
        return changed;
    }

    // ------------------------------------------------------------------
    // 状态辅助
    // ------------------------------------------------------------------

    private void clearSelections() {
        this.type = null;
        this.major = null;
        this.sub = null;
        this.typePage = 0;
        this.level = 0;
        this.skillMask = 0;
        this.resetSubPages();
    }

    private void resetSubPages() {
        Arrays.fill(this.subPage, 0);
    }

    private void setSubPage(CastMode mode, int page) {
        if (mode != null) {
            this.subPage[majorCode(mode)] = page;
        }
    }

    private List<CastMode> currentMajors() {
        if (this.type == null) {
            return List.of();
        }
        return this.mode == MODE_EXAM
                ? LecternRules.examMajorsFor(this.type)
                : LecternRules.majorsFor(this.type);
    }

    private boolean outputEmpty() {
        return this.slots.get(SLOT_OUTPUT).getItem().isEmpty();
    }

    private CertData certData() {
        ItemStack stack = this.slots.get(SLOT_CERT).getItem();
        return stack.isEmpty() ? null : stack.get(ModComponents.CERT_DATA);
    }

    /**
     * 高级讲台更新模式：选中现有条目时把 DATA_LEVEL 初始化成该条目当前等级，
     * 没动等级就确认 = 保持原级（W16 §1.4）。技能 / 未选条目 → 0。
     */
    private void syncLevelToEntry() {
        if (!this.advanced || this.mode != MODE_UPDATE) {
            return;
        }
        if (this.type == null || this.major == null || this.major == CastMode.SKILL) {
            this.level = 0;
            return;
        }
        this.level = LecternRules.findEntry(this.certData(), this.type, this.major, this.sub)
                .map(CertEntry::level).orElse(0);
    }

    private boolean certValid() {
        ItemStack stack = this.slots.get(SLOT_CERT).getItem();
        if (stack.isEmpty() || !stack.is(MagicItems.magicStudyCertificate)) {
            return false;
        }
        return MagicDataRules.isValidCert(stack.get(ModComponents.CERT_DATA));
    }

    private LecternRules.ProofRef proofRef() {
        ItemStack stack = this.slots.get(SLOT_PROOF).getItem();
        if (stack.isEmpty()) {
            return LecternRules.ProofRef.EMPTY;
        }
        ProficiencyData proficiency = stack.get(ModComponents.PROFICIENCY_DATA);
        if (proficiency != null && !MagicDataRules.isValidProficiency(proficiency)) {
            proficiency = null;
        }
        PromotionData promotion = stack.get(ModComponents.PROMOTION_DATA);
        if (promotion != null && !MagicDataRules.isValidPromotion(promotion)) {
            promotion = null;
        }
        return new LecternRules.ProofRef(proficiency, promotion);
    }

    private void applyCert(ItemStack certStack, CertData updated) {
        certStack.set(ModComponents.CERT_DATA, updated);
        this.slots.get(SLOT_CERT).setChanged();
        SoundFx.at(this.playerInventory.player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.6F, 1.2F);
    }

    private void shrinkProof() {
        ItemStack proof = this.slots.get(SLOT_PROOF).getItem();
        proof.shrink(1);
        if (proof.isEmpty()) {
            this.slots.get(SLOT_PROOF).setByPlayer(ItemStack.EMPTY);
        } else {
            this.slots.get(SLOT_PROOF).setChanged();
        }
    }

    private boolean canConfirm() {
        if (this.mode == MODE_NONE || this.type == null) {
            return false;
        }
        return switch (this.mode) {
            case MODE_APPLY -> true;
            case MODE_UPDATE -> this.certValid();
            case MODE_EXAM -> {
                if (LecternRules.examMajorsFor(this.type).isEmpty() || this.major == null) {
                    yield false;
                }
                List<CastSubMode> subs = LecternRules.examSubsFor(this.type, this.major);
                yield subs.isEmpty() || this.sub != null;
            }
            default -> false;
        };
    }

    private int skillMask(List<MagicSkill> skills) {
        int mask = 0;
        for (MagicSkill skill : skills) {
            mask |= 1 << skill.ordinal();
        }
        return mask;
    }

    private static int majorCode(CastMode mode) {
        return switch (mode) {
            case POINT -> 0;
            case LINE -> 1;
            case FORMATION -> 2;
            case SKILL -> 3;
        };
    }

    private static CastMode castModeByCode(int code) {
        return switch (code) {
            case 0 -> CastMode.POINT;
            case 1 -> CastMode.LINE;
            case 2 -> CastMode.FORMATION;
            case 3 -> CastMode.SKILL;
            default -> null;
        };
    }

    private static int subPageIndex(CastMode mode) {
        if (mode == null) {
            return DATA_SUB_PAGE_POINT;
        }
        return switch (mode) {
            case POINT -> DATA_SUB_PAGE_POINT;
            case LINE -> DATA_SUB_PAGE_LINE;
            case FORMATION -> DATA_SUB_PAGE_FORMATION;
            case SKILL -> DATA_SUB_PAGE_SKILL;
        };
    }

    // ------------------------------------------------------------------
    // ContainerData
    // ------------------------------------------------------------------

    private int serverData(int index) {
        return switch (index) {
            case DATA_MODE -> this.mode;
            case DATA_TYPE -> this.type == null ? -1 : LecternRules.types().indexOf(this.type);
            case DATA_MAJOR -> this.major == null ? -1 : majorCode(this.major);
            case DATA_SUB -> this.sub == null ? -1 : this.sub.ordinal();
            case DATA_TYPE_PAGE -> this.typePage;
            case DATA_SUB_PAGE_POINT -> this.subPage[0];
            case DATA_SUB_PAGE_LINE -> this.subPage[1];
            case DATA_SUB_PAGE_FORMATION -> this.subPage[2];
            case DATA_SUB_PAGE_SKILL -> this.subPage[3];
            case DATA_LEVEL -> this.level;
            case DATA_HINT -> this.hintCode;
            case DATA_FLAGS -> this.mode == MODE_EXAM && this.type != null
                    && LecternRules.examMajorsFor(this.type).isEmpty() ? FLAG_EXAM_NO_CONTENT : 0;
            case DATA_SKILL_MASK -> this.skillMask;
            case DATA_CAN_CONFIRM -> this.canConfirm() ? 1 : 0;
            case DATA_UPDATE_AVAILABLE -> this.certValid() ? 1 : 0;
            case DATA_CERT_VALID -> this.certValid() ? 1 : 0;
            default -> 0;
        };
    }

    private final class LecternData implements ContainerData {

        private final int[] synced = new int[DATA_SIZE];

        @Override
        public int get(int index) {
            if (MagicStudyLecternMenu.this.playerInventory.player.level().isClientSide()) {
                return index >= 0 && index < DATA_SIZE ? this.synced[index] : 0;
            }
            return index >= 0 && index < DATA_SIZE ? serverData(index) : 0;
        }

        @Override
        public void set(int index, int value) {
            if (index >= 0 && index < DATA_SIZE) {
                this.synced[index] = value;
            }
        }

        @Override
        public int getCount() {
            return DATA_SIZE;
        }
    }
}
