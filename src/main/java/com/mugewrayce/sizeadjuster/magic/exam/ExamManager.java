package com.mugewrayce.sizeadjuster.magic.exam;

import com.mugewrayce.sizeadjuster.SizeAdjusterMod;
import com.mugewrayce.sizeadjuster.component.ApplicationData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.component.ProficiencyData;
import com.mugewrayce.sizeadjuster.component.PromotionData;
import com.mugewrayce.sizeadjuster.entity.ModEntities;
import com.mugewrayce.sizeadjuster.entity.TrialMagicBodyEntity;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.magic.CastMode;
import com.mugewrayce.sizeadjuster.magic.CastSubMode;
import com.mugewrayce.sizeadjuster.magic.MagicDataRules;
import com.mugewrayce.sizeadjuster.magic.MagicType;
import com.mugewrayce.sizeadjuster.magic.SoundFx;
import com.mugewrayce.sizeadjuster.payload.ExamHudPayload;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 考试 / 试炼总控（W15）：开启 → 生成目标与奖励箱 → 计时 / 判定 → 奖励 → 清理。
 *
 * <p>要点：</p>
 * <ul>
 *   <li>限时 12000 tick（10 分钟），存 {@code remainingTicks} 递减（D-6）；</li>
 *   <li>中心 = 奖励箱实际位置所在区块；3×3 常加载带引用计数（D-5）；</li>
 *   <li>目标实体带 {@code sessionId}，多人重叠不串台；</li>
 *   <li>HUD 只发发起者；世界文字用 {@code TextDisplay}（D-2）；</li>
 *   <li>奖励箱进行中受保护：非发起者拆不掉、发起者两次算放弃（D-14）。</li>
 * </ul>
 */
public final class ExamManager {

    /** 统一限时 10 分钟。 */
    public static final int EXAM_TICKS = 20 * 60 * 10;
    private static final int HUD_INTERVAL = 20;
    private static final int SAVE_INTERVAL = 100;
    private static final int RANGE_CHECK_INTERVAL = 20;
    private static final int RANGE_WARN_COOLDOWN = 20 * 60;
    private static final int RESULT_HUD_TICKS = 60;
    private static final int CHEST_RADIUS = 2;
    private static final int CHEST_UP = 2;
    private static final double SPAWN_RADIUS = 8.0D;
    private static final double SPAWN_MIN_GAP = 1.5D;

    /** 猪 / 世界文字实体的归属标记，只用于启动清扫（不新增语言键，D-7）。 */
    public static final String PIG_TAG = "mugewrayce_exam_pig";
    public static final String TEXT_TAG = "mugewrayce_exam_text";

    private static final Map<String, Active> ACTIVE = new LinkedHashMap<>();
    private static final Map<String, Integer> FORCED = new HashMap<>();
    private static final List<PendingResult> PENDING = new ArrayList<>();
    /** W18：激活中的奖励箱（维度 → 位置集合），供方块保护 mixin 查询。 */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> PROTECTED_CHESTS = new HashMap<>();
    private static boolean restored;

    private ExamManager() {
    }

    /** 服务端初始化：注册 tick / 死亡 / 破坏 / 停服保存。 */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ExamManager::tick);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> save(server));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (!(entity.level() instanceof ServerLevel level)) {
                return;
            }
            MinecraftServer server = level.getServer();
            if (entity instanceof Player player) {
                Active active = findByOwner(player.getUUID());
                if (active != null && (active.spec.kind() == ExamSpec.Kind.SELF_HEAL
                        || active.spec.kind() == ExamSpec.Kind.OUTLAST)) {
                    finish(server, active, Result.FAILED);
                }
                return;
            }
            if (entity instanceof TrialMagicBodyEntity body && body.hasSession()) {
                Active active = ACTIVE.get(body.getSessionId());
                if (active != null) {
                    // 死亡由 aliveBodies() 每 tick 复查，这里只保证会话仍在追
                    SizeAdjusterMod.LOGGER.debug("Exam target {} of session {} died",
                            body.getUUID(), active.id);
                }
            }
        });

        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel) || player == null) {
                return true;
            }
            Active active = findByChest(serverLevel, pos);
            if (active == null) {
                return true;
            }
            if (!active.owner.equals(player.getUUID())) {
                // D-14：非发起者直接拆不掉，不提示、不计数
                return false;
            }
            active.breakAttempts++;
            if (active.breakAttempts >= 2) {
                finish(serverLevel.getServer(), active, Result.ABANDON);
                return true;
            }
            player.sendSystemMessage(Component.translatable(
                    "message.mugewrayce-mod.exam_chest_guard"));
            save(serverLevel.getServer());
            return false;
        });

        // W17：开考后 shift + 右键奖励箱 → 给本场存活目标加 30 秒发光描边
        // （效果同原版敲钟照出袭击者；发起者与旁观玩家都可以用）
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (player == null || !player.isShiftKeyDown()
                    || !(level instanceof ServerLevel serverLevel)) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hit.getBlockPos();
            if (!serverLevel.getBlockState(pos).is(Blocks.CHEST)) {
                return InteractionResult.PASS;
            }
            Active active = findByChest(serverLevel, pos);
            if (active == null) {
                return InteractionResult.PASS;
            }
            int count = 0;
            for (UUID id : active.targets) {
                if (serverLevel.getEntity(id) instanceof LivingEntity living && living.isAlive()) {
                    living.addEffect(new MobEffectInstance(
                            MobEffects.GLOWING, 20 * 30, 0, false, false, true));
                    count++;
                }
            }
            player.sendOverlayMessage(Component.translatable(
                    "message.mugewrayce-mod.exam_highlight", count));
            SoundFx.at(player, SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.2F);
            return InteractionResult.SUCCESS;
        });
    }

    /** 服务端停止 / 切换世界时清空运行期状态（存档已由 SERVER_STOPPING 写盘）。 */
    public static void reset() {
        ACTIVE.clear();
        FORCED.clear();
        PENDING.clear();
        PROTECTED_CHESTS.clear();
        restored = false;
    }

    // ------------------------------------------------------------------
    // W18：奖励箱保护（防非玩家方块移除）
    // ------------------------------------------------------------------

    private static void protectChest(ServerLevel level, BlockPos pos) {
        PROTECTED_CHESTS.computeIfAbsent(level.dimension(), key -> new HashSet<>())
                .add(pos.immutable());
    }

    /** 解除登记；考试自己撤箱前必须先调用，否则会被保护逻辑挡住。 */
    private static void unprotectChest(ServerLevel level, BlockPos pos) {
        Set<BlockPos> set = PROTECTED_CHESTS.get(level.dimension());
        if (set == null) {
            return;
        }
        set.remove(pos);
        if (set.isEmpty()) {
            PROTECTED_CHESTS.remove(level.dimension());
        }
    }

    /** 该位置是否是激活中的考试奖励箱（{@code ExamChestProtectionMixin} 查询用）。 */
    public static boolean isProtectedChest(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        Set<BlockPos> set = PROTECTED_CHESTS.get(level.dimension());
        return set != null && set.contains(pos);
    }

    // ------------------------------------------------------------------
    // 开启
    // ------------------------------------------------------------------

    /**
     * 手持申请书右键开启。
     *
     * @return true = 开启成功（调用方负责 {@code shrink(1)}）；false = 失败，申请书保留
     */
    public static boolean start(ServerPlayer player, BlockPos clickPos, ItemStack stack) {
        ApplicationData data = stack.get(ModComponents.APPLICATION_DATA);
        if (!MagicDataRules.isValidApplication(data)) {
            player.sendSystemMessage(Component.translatable(
                    "command.mugewrayce.item.invalid", stack.getHoverName()));
            return false;
        }
        MagicType type = MagicType.fromId(data.type());
        CastMode mode = CastMode.fromId(data.mode());
        CastSubMode sub = data.sub().isEmpty() ? null : CastSubMode.fromId(data.sub());
        ExamSpec spec = ExamSpec.of(type, mode, sub, data.targetLevel());
        if (type == null || mode == null || spec.isNoExam()) {
            player.sendSystemMessage(Component.translatable(
                    "gui.mugewrayce-mod.magic_study_lectern.hint.no_exam_content"));
            return false;
        }
        if (findByOwner(player.getUUID()) != null) {
            player.sendSystemMessage(Component.translatable(
                    "message.mugewrayce-mod.exam_already_running"));
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        BlockPos chestPos = findChestPos(level, clickPos);
        if (chestPos == null) {
            player.sendSystemMessage(Component.translatable(
                    "message.mugewrayce-mod.exam_chest_blocked"));
            return false;
        }
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

        String id = UUID.randomUUID().toString();
        Active active = new Active(id, player.getUUID(), level, chestPos, ChunkPos.containing(chestPos), clickPos,
                type.id(), mode.id(), sub == null ? "" : sub.id(), data.targetLevel(), spec,
                System.currentTimeMillis());
        active.remainingTicks = EXAM_TICKS;

        spawnTargets(level, active, clickPos);
        boolean missingPigs = active.spec.pigCount() > 0 && active.spawnedPigs < active.spec.pigCount();
        boolean missingBodies = active.spec.spawnBodyCount() > 0 && active.spawnedBodies == 0;
        if (missingPigs || missingBodies) {
            // 目标没生成出来：撤掉刚放的箱子，申请书不消耗（与 D-1 同一原则）
            unprotectChest(level, chestPos);
            level.setBlock(chestPos, Blocks.AIR.defaultBlockState(), 3);
            for (UUID target : active.targets) {
                Entity entity = level.getEntity(target);
                if (entity != null) {
                    entity.discard();
                }
            }
            player.sendSystemMessage(Component.translatable(
                    "message.mugewrayce-mod.exam_chest_blocked"));
            return false;
        }
        forceChunks(level, active.centerChunk);
        active.displayId = spawnTextDisplay(level, active);

        // W18：开考成功后才登记奖励箱保护（失败撤箱路径已在上面先解除）
        protectChest(level, chestPos);
        ACTIVE.put(id, active);
        if (spec.kind() == ExamSpec.Kind.SELF_HEAL) {
            // 对己治疗 I：强制扣到 50%，考试期间禁止自然回血（§6 坑 9）
            player.setHealth(Math.max(1.0F, player.getMaxHealth() * 0.5F));
        }
        sendChatStart(player, active);
        sendHud(level.getServer(), active, ExamHudPayload.STATE_RUNNING);
        save(level.getServer());
        SoundFx.at(player, SoundEvents.ENCHANTMENT_TABLE_USE, 0.8F, 1.0F);
        return true;
    }

    /** 5×5 水平 × 上方 3 层（D-15）里找最近可放置点；找不到返回 null。 */
    private static BlockPos findChestPos(ServerLevel level, BlockPos center) {
        BlockState chest = Blocks.CHEST.defaultBlockState();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dy = 0; dy <= CHEST_UP; dy++) {
            for (int dx = -CHEST_RADIUS; dx <= CHEST_RADIUS; dx++) {
                for (int dz = -CHEST_RADIUS; dz <= CHEST_RADIUS; dz++) {
                    BlockPos pos = center.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).canBeReplaced()) {
                        continue;
                    }
                    if (!chest.canSurvive(level, pos)) {
                        continue;
                    }
                    if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                        continue;
                    }
                    double distance = pos.distSqr(center);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /** 以右键位置为中心、半径 8 格内随机生成目标（≥2 只时间隔 ≥1.5 格，不与玩家/箱子重叠）。 */
    private static void spawnTargets(ServerLevel level, Active active, BlockPos center) {
        List<UUID> pigs = new ArrayList<>();
        if (active.spec.pigCount() > 0) {
            for (int i = 0; i < active.spec.pigCount(); i++) {
                BlockPos pos = findSpawnPos(level, center, active.spawned, active.chestPos);
                if (pos == null) {
                    continue;
                }
                Pig pig = EntityType.PIG.create(level, EntitySpawnReason.MOB_SUMMONED);
                if (pig == null) {
                    continue;
                }
                pig.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
                pig.setNoAi(true);
                pig.setPersistenceRequired();
                pig.addTag(PIG_TAG);
                pig.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH)
                        .setBaseValue(active.spec.pigHealth());
                pig.setHealth(active.spec.pigHealth());
                level.addFreshEntity(pig);
                active.targets.add(pig.getUUID());
                active.pigIds.add(pig.getUUID());
                active.spawnedPigs++;
                active.spawned.add(new net.minecraft.world.phys.Vec3(
                        pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
                pigs.add(pig.getUUID());
            }
        }
        boolean attackPlayers = active.spec.kind() != ExamSpec.Kind.PROTECT_PIG;
        int bodies = active.spec.spawnBodyCount();
        for (int i = 0; i < bodies; i++) {
            BlockPos pos = findSpawnPos(level, center, active.spawned, active.chestPos);
            if (pos == null) {
                continue;
            }
            TrialMagicBodyEntity body = ModEntities.TRIAL_MAGIC_BODY.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (body == null) {
                continue;
            }
            body.applyExamConfig(active.spec, active.owner, active.id, attackPlayers, pigs);
            if (active.spec.kind() == ExamSpec.Kind.HEAL_TARGET) {
                // 治疗 I：先扣目标 10 / 40 血，玩家把它治满才通过（草案 §9.8）
                body.setHealth(Math.max(1.0F, active.spec.health() - active.spec.damage()));
            }
            body.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
            level.addFreshEntity(body);
            active.targets.add(body.getUUID());
            active.spawned.add(new net.minecraft.world.phys.Vec3(
                    pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D));
            active.spawnedBodies++;
        }
    }

    private static BlockPos findSpawnPos(ServerLevel level, BlockPos center, List<net.minecraft.world.phys.Vec3> taken,
                                         BlockPos chestPos) {
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2.0D;
            double radius = 2.0D + level.getRandom().nextDouble() * (SPAWN_RADIUS - 2.0D);
            int x = center.getX() + (int) Math.round(Math.cos(angle) * radius);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * radius);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - center.getY()) > 8) {
                y = center.getY();
            }
            BlockPos pos = new BlockPos(x, y, z);
            if (!level.getBlockState(pos).canBeReplaced()
                    || !level.getBlockState(pos.above()).canBeReplaced()) {
                continue;
            }
            if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) {
                continue;
            }
            net.minecraft.world.phys.Vec3 vec = new net.minecraft.world.phys.Vec3(
                    x + 0.5D, y, z + 0.5D);
            if (vec.distanceToSqr(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D) < 4.0D) {
                continue;
            }
            if (level.getNearestPlayer(vec.x, vec.y, vec.z, 1.5D, false) != null) {
                continue;
            }
            boolean tooClose = false;
            for (net.minecraft.world.phys.Vec3 other : taken) {
                if (other.distanceToSqr(vec) < SPAWN_MIN_GAP * SPAWN_MIN_GAP) {
                    tooClose = true;
                    break;
                }
            }
            if (tooClose) {
                continue;
            }
            return pos;
        }
        return null;
    }

    private static UUID spawnTextDisplay(ServerLevel level, Active active) {
        EntityType<Display.TextDisplay> type = EntityType.TEXT_DISPLAY;
        Display.TextDisplay display = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (display == null) {
            return null;
        }
        try {
            TagValueOutput output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
            output.store("text", ComponentSerialization.CODEC,
                    ExamText.firstLine(active.type, active.mode, active.sub, active.targetLevel));
            output.store("billboard", Display.BillboardConstraints.CODEC,
                    Display.BillboardConstraints.CENTER);
            output.putBoolean("see_through", true);
            output.putBoolean("shadow", false);
            output.putInt("background", 0);
            display.load(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(),
                    output.buildResult()));
        } catch (RuntimeException e) {
            // 世界文字只是表现，失败不能拖垮考试会话
            SizeAdjusterMod.LOGGER.warn("Failed to configure exam TextDisplay ({})", e.toString());
            display.discard();
            return null;
        }
        display.addTag(TEXT_TAG);
        display.setPos(active.chestPos.getX() + 0.5D, active.chestPos.getY() + 1.5D,
                active.chestPos.getZ() + 0.5D);
        level.addFreshEntity(display);
        return display.getUUID();
    }

    private static void sendChatStart(ServerPlayer player, Active active) {
        player.sendSystemMessage(Component.translatable("message.mugewrayce-mod.exam_started",
                ExamText.firstLine(active.type, active.mode, active.sub, active.targetLevel)));
        player.sendSystemMessage(Component.translatable("message.mugewrayce-mod.exam_duration", 10));
        player.sendSystemMessage(Component.translatable("message.mugewrayce-mod.exam_goal",
                ExamText.goal(active.spec)));
    }

    // ------------------------------------------------------------------
    // 常加载（九格，引用计数，D-5）
    // ------------------------------------------------------------------

    private static String chunkKey(ServerLevel level, ChunkPos pos) {
        return level.dimension().identifier() + "@" + pos.x() + "," + pos.z();
    }

    private static void forceChunks(ServerLevel level, ChunkPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos pos = new ChunkPos(center.x() + dx, center.z() + dz);
                String key = chunkKey(level, pos);
                int count = FORCED.merge(key, 1, Integer::sum);
                if (count == 1) {
                    level.setChunkForced(pos.x(), pos.z(), true);
                }
            }
        }
    }

    private static void releaseChunks(ServerLevel level, ChunkPos center) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos pos = new ChunkPos(center.x() + dx, center.z() + dz);
                String key = chunkKey(level, pos);
                Integer count = FORCED.get(key);
                if (count == null || count <= 0) {
                    continue;
                }
                if (count == 1) {
                    FORCED.remove(key);
                    level.setChunkForced(pos.x(), pos.z(), false);
                } else {
                    FORCED.put(key, count - 1);
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 每 tick：计时 / 判定 / HUD / 越界
    // ------------------------------------------------------------------

    private static void tick(MinecraftServer server) {
        if (!restored) {
            restored = true;
            restore(server);
        }
        tickPending(server);
        if (ACTIVE.isEmpty()) {
            return;
        }
        int serverTick = server.getTickCount();
        for (Active active : new ArrayList<>(ACTIVE.values())) {
            if (active.state == ExamSession.State.RUNNING) {
                tickActive(server, active, serverTick);
            }
        }
        if (serverTick % SAVE_INTERVAL == 0) {
            save(server);
        }
    }

    private static void tickActive(MinecraftServer server, Active active, int serverTick) {
        active.remainingTicks--;
        if (active.spec.kind() == ExamSpec.Kind.SELF_HEAL) {
            suppressNaturalRegen(active);
        }
        if (active.remainingTicks % HUD_INTERVAL == 0) {
            sendHud(server, active, ExamHudPayload.STATE_RUNNING);
        }
        if (active.remainingTicks % RANGE_CHECK_INTERVAL == 0) {
            checkRange(server, active, serverTick);
        }
        if (judge(server, active)) {
            return;
        }
        if (active.remainingTicks <= 0) {
            finish(server, active, Result.TIMEOUT);
        }
    }

    /** @return true = 已经结束（调用方不要再做超时判定） */
    private static boolean judge(MinecraftServer server, Active active) {
        ExamSpec spec = active.spec;
        switch (spec.kind()) {
            case KILL -> {
                if (active.aliveBodies() == 0) {
                    finish(server, active, Result.PASSED);
                    return true;
                }
            }
            case HEAL_TARGET -> {
                TrialMagicBodyEntity body = active.firstBody();
                if (body == null) {
                    finish(server, active, Result.FAILED);
                    return true;
                }
                if (body.getHealth() >= body.getMaxHealth()) {
                    finish(server, active, Result.PASSED);
                    return true;
                }
            }
            case SELF_HEAL -> {
                ServerPlayer player = active.ownerPlayer(server);
                if (player != null && player.isAlive() && player.getHealth() >= player.getMaxHealth()) {
                    finish(server, active, Result.PASSED);
                    return true;
                }
            }
            case PROTECT_PIG -> {
                if (!active.pigsAlive()) {
                    finish(server, active, Result.FAILED);
                    return true;
                }
                if (active.elapsedTicks() >= spec.seconds() * 20) {
                    finish(server, active, Result.PASSED);
                    return true;
                }
            }
            case OUTLAST -> {
                if (active.elapsedTicks() >= spec.seconds() * 20) {
                    ServerPlayer player = active.ownerPlayer(server);
                    if (player != null && player.isAlive()) {
                        finish(server, active, Result.PASSED);
                        return true;
                    }
                }
            }
            default -> {
            }
        }
        return false;
    }

    private static void checkRange(MinecraftServer server, Active active, int serverTick) {
        ServerPlayer player = active.ownerPlayer(server);
        if (player != null) {
            ChunkPos playerChunk = ChunkPos.containing(player.blockPosition());
            boolean outside = Math.abs(playerChunk.x() - active.centerChunk.x()) > 1
                    || Math.abs(playerChunk.z() - active.centerChunk.z()) > 1;
            if (outside && serverTick - active.lastRangeWarn >= RANGE_WARN_COOLDOWN) {
                active.lastRangeWarn = serverTick;
                player.sendSystemMessage(Component.translatable(
                        "message.mugewrayce-mod.exam_out_of_range"));
            }
        }
        for (UUID id : new ArrayList<>(active.targets)) {
            Entity entity = active.level.getEntity(id);
            if (entity == null) {
                continue;
            }
            ChunkPos chunk = ChunkPos.containing(entity.blockPosition());
            boolean outside = Math.abs(chunk.x() - active.centerChunk.x()) > 1
                    || Math.abs(chunk.z() - active.centerChunk.z()) > 1;
            if (outside) {
                // W18：越界回到奖励箱附近的安全落脚点，而不是当初右键的位置
                Vec3 landing = safeLanding(active.level, active.chestPos);
                entity.teleportTo(landing.x, landing.y, landing.z);
                entity.resetFallDistance();
                entity.setDeltaMovement(Vec3.ZERO);
            }
        }
    }

    /** 优先箱子正上方；上方被堵时依次看四邻；都不行回正上方兜底。 */
    private static Vec3 safeLanding(ServerLevel level, BlockPos chestPos) {
        BlockPos above = chestPos.above();
        if (isStandable(level, above)) {
            return Vec3.atBottomCenterOf(above);
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos candidate = chestPos.relative(direction).above();
            if (isStandable(level, candidate)) {
                return Vec3.atBottomCenterOf(candidate);
            }
        }
        return Vec3.atBottomCenterOf(above);
    }

    /** 落脚点：脚与头两格空气，且脚下是实体方块。 */
    private static boolean isStandable(ServerLevel level, BlockPos feet) {
        BlockPos head = feet.above();
        if (!level.getBlockState(feet).isAir() || !level.getBlockState(head).isAir()) {
            return false;
        }
        return level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP);
    }

    private static void suppressNaturalRegen(Active active) {
        ServerPlayer player = active.ownerPlayer(active.level.getServer());
        if (player == null) {
            return;
        }
        if (!active.saturationSaved) {
            active.savedSaturation = player.getFoodData().getSaturationLevel();
            active.saturationSaved = true;
        }
        // 26.1 FoodData.tick 的自然恢复条件是 saturation>0 && foodLevel>=20 && isHurt
        player.getFoodData().setSaturation(0.0F);
    }

    private static void restoreSaturation(Active active) {
        if (!active.saturationSaved) {
            return;
        }
        ServerPlayer player = active.ownerPlayer(active.level.getServer());
        if (player != null) {
            player.getFoodData().setSaturation(active.savedSaturation);
        }
        active.saturationSaved = false;
    }

    // ------------------------------------------------------------------
    // 结束：奖励 / 清理 / 提示
    // ------------------------------------------------------------------

    private enum Result {
        PASSED,
        TIMEOUT,
        ABANDON,
        FAILED
    }

    private static void finish(MinecraftServer server, Active active, Result result) {
        if (active.state != ExamSession.State.RUNNING) {
            return;
        }
        boolean passed = result == Result.PASSED;
        active.state = passed ? ExamSession.State.PASSED : ExamSession.State.FAILED;
        if (passed) {
            giveReward(active);
        }
        ServerPlayer owner = active.ownerPlayer(server);
        if (owner != null) {
            Component message = switch (result) {
                case PASSED -> Component.translatable("message.mugewrayce-mod.exam_passed");
                case TIMEOUT -> Component.translatable("message.mugewrayce-mod.exam_failed_timeout");
                case ABANDON -> Component.translatable("message.mugewrayce-mod.exam_failed_abandon");
                case FAILED -> Component.translatable("message.mugewrayce-mod.exam_failed");
            };
            owner.sendSystemMessage(message);
            SoundFx.at(owner, passed ? SoundEvents.PLAYER_LEVELUP : SoundEvents.GLASS_BREAK,
                    0.6F, passed ? 1.2F : 0.8F);
        }
        if (passed || result == Result.FAILED) {
            sendHud(server, active, passed ? ExamHudPayload.STATE_PASSED : ExamHudPayload.STATE_FAILED);
            PENDING.add(new PendingResult(active.owner, active.level.dimension().identifier()));
        } else {
            sendHud(server, active, ExamHudPayload.STATE_NONE);
        }
        discardTargets(active);
        releaseChunks(active.level, active.centerChunk);
        restoreSaturation(active);
        // W18：考试结束解除奖励箱保护（箱子留作奖励箱，玩家可正常取用/破坏）
        unprotectChest(active.level, active.chestPos);
        ACTIVE.remove(active.id);
        save(server);
    }

    private static void giveReward(Active active) {
        ItemStack reward;
        if (active.targetLevel <= 1) {
            reward = new ItemStack(MagicItems.proficiencyCertificate);
            reward.set(ModComponents.PROFICIENCY_DATA,
                    new ProficiencyData(active.type, active.mode, active.sub));
        } else {
            reward = new ItemStack(MagicItems.promotionCertificate);
            reward.set(ModComponents.PROMOTION_DATA,
                    new PromotionData(active.type, active.mode, active.sub, active.targetLevel));
        }
        if (active.level.getBlockEntity(active.chestPos) instanceof ChestBlockEntity chest) {
            for (int slot = 0; slot < chest.getContainerSize(); slot++) {
                if (chest.getItem(slot).isEmpty()) {
                    chest.setItem(slot, reward);
                    return;
                }
            }
        }
        active.level.addFreshEntity(new ItemEntity(active.level,
                active.chestPos.getX() + 0.5D, active.chestPos.getY() + 0.5D,
                active.chestPos.getZ() + 0.5D, reward));
    }

    private static void discardTargets(Active active) {
        for (UUID id : active.targets) {
            Entity entity = active.level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        if (active.displayId != null) {
            Entity display = active.level.getEntity(active.displayId);
            if (display != null) {
                display.discard();
            }
        }
    }

    private static void sendHud(MinecraftServer server, Active active, int state) {
        ServerPlayer player = active.ownerPlayer(server);
        if (player == null) {
            return;
        }
        int remainingSeconds = Math.max(0, active.remainingTicks / 20);
        ExamHudPayload payload;
        if (state == ExamHudPayload.STATE_NONE) {
            payload = ExamHudPayload.none(active.level.dimension().identifier());
        } else {
            payload = new ExamHudPayload(state, remainingSeconds, active.type, active.mode, active.sub,
                    active.targetLevel, active.chestPos, active.level.dimension().identifier());
        }
        ServerPlayNetworking.send(player, payload);
    }

    private static void tickPending(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }
        List<PendingResult> done = new ArrayList<>();
        for (PendingResult pending : PENDING) {
            pending.ticksLeft--;
            if (pending.ticksLeft > 0) {
                continue;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(pending.owner);
            if (player != null) {
                ServerPlayNetworking.send(player, ExamHudPayload.none(pending.dimension));
            }
            done.add(pending);
        }
        PENDING.removeAll(done);
    }

    // ------------------------------------------------------------------
    // 持久化 / 重启恢复 / 孤儿清扫
    // ------------------------------------------------------------------

    private static void save(MinecraftServer server) {
        List<ExamSession> sessions = new ArrayList<>();
        for (Active active : ACTIVE.values()) {
            sessions.add(active.toSession());
        }
        ExamStorage.save(server, sessions);
    }

    /** 启动后 1 tick：读盘、恢复常加载 / 文字 / HUD，并清扫孤儿（D-12）。 */
    private static void restore(MinecraftServer server) {
        List<ExamSession> sessions = ExamStorage.load(server);
        List<Active> kept = new ArrayList<>();
        for (ExamSession session : sessions) {
            if (session.state() != ExamSession.State.RUNNING) {
                continue;
            }
            ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, session.dimension());
            ServerLevel level = server.getLevel(key);
            if (level == null) {
                SizeAdjusterMod.LOGGER.warn("Exam session {} points to missing dimension {}; dropped",
                        session.id(), session.dimension());
                continue;
            }
            ExamSpec spec = ExamSpec.of(MagicType.fromId(session.type()), CastMode.fromId(session.mode()),
                    session.sub().isEmpty() ? null : CastSubMode.fromId(session.sub()), session.targetLevel());
            if (spec.isNoExam()) {
                SizeAdjusterMod.LOGGER.warn("Exam session {} has no valid spec; dropped", session.id());
                continue;
            }
            // 先恢复常加载，区块才会被加载，后面才能找到目标实体 / 箱子
            forceChunks(level, session.centerChunk());
            if (level.getBlockState(session.chestPos()).getBlock() != Blocks.CHEST) {
                SizeAdjusterMod.LOGGER.warn("Exam session {} lost its reward chest; dropped", session.id());
                releaseChunks(level, session.centerChunk());
                continue;
            }
            Active active = Active.restore(session, level, spec);
            if (active.aliveBodyOrPigCount() == 0) {
                // 目标被杀光 / 被游戏外删除：静默判失败并清理（不提示，草案 §9.4）
                SizeAdjusterMod.LOGGER.info("Exam session {} has no remaining targets; dropped", session.id());
                discardTargets(active);
                releaseChunks(level, session.centerChunk());
                continue;
            }
            kept.add(active);
            // W18：重启恢复后重新登记奖励箱保护
            protectChest(level, session.chestPos());
        }
        // 用“保留的会话目标”做白名单，清掉孤儿考试实体与全部旧世界文字
        List<UUID> known = new ArrayList<>();
        for (Active active : kept) {
            known.addAll(active.targets);
        }
        sweepOrphans(server, known);

        for (Active active : kept) {
            active.displayId = spawnTextDisplay(active.level, active);
            ACTIVE.put(active.id, active);
            sendHud(server, active, ExamHudPayload.STATE_RUNNING);
        }
        save(server);
    }

    /** 清扫没有会话归属的考试实体 / 世界文字（D-12）。 */
    private static void sweepOrphans(MinecraftServer server, List<UUID> known) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Display.TextDisplay && entity.entityTags().contains(TEXT_TAG)) {
                    entity.discard();
                    continue;
                }
                if (!known.contains(entity.getUUID())) {
                    if (entity instanceof TrialMagicBodyEntity) {
                        entity.discard();
                    } else if (entity instanceof Pig pig && pig.entityTags().contains(PIG_TAG)) {
                        entity.discard();
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 查询
    // ------------------------------------------------------------------

    private static Active findByOwner(UUID owner) {
        for (Active active : ACTIVE.values()) {
            if (active.state == ExamSession.State.RUNNING && active.owner.equals(owner)) {
                return active;
            }
        }
        return null;
    }

    private static Active findByChest(ServerLevel level, BlockPos pos) {
        for (Active active : ACTIVE.values()) {
            if (active.state == ExamSession.State.RUNNING
                    && active.level == level
                    && active.chestPos.equals(pos)) {
                return active;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 运行期会话
    // ------------------------------------------------------------------

    private static final class Active {

        private final String id;
        private final UUID owner;
        private final ServerLevel level;
        private final BlockPos chestPos;
        private final ChunkPos centerChunk;
        private final BlockPos rightClickPos;
        private final String type;
        private final String mode;
        private final String sub;
        private final int targetLevel;
        private final ExamSpec spec;
        private final long startedAt;
        private final List<UUID> targets = new ArrayList<>();
        private final List<net.minecraft.world.phys.Vec3> spawned = new ArrayList<>();
        private int remainingTicks = EXAM_TICKS;
        private int breakAttempts;
        private ExamSession.State state = ExamSession.State.RUNNING;
        private int spawnedBodies;
        private int spawnedPigs;
        private final List<UUID> pigIds = new ArrayList<>();
        private UUID displayId;
        private long lastRangeWarn = -RANGE_WARN_COOLDOWN;
        private float savedSaturation;
        private boolean saturationSaved;

        private Active(String id, UUID owner, ServerLevel level, BlockPos chestPos, ChunkPos centerChunk,
                       BlockPos rightClickPos, String type, String mode, String sub, int targetLevel,
                       ExamSpec spec, long startedAt) {
            this.id = id;
            this.owner = owner;
            this.level = level;
            this.chestPos = chestPos;
            this.centerChunk = centerChunk;
            this.rightClickPos = rightClickPos;
            this.type = type;
            this.mode = mode;
            this.sub = sub;
            this.targetLevel = targetLevel;
            this.spec = spec;
            this.startedAt = startedAt;
        }

        private static Active restore(ExamSession session, ServerLevel level, ExamSpec spec) {
            Active active = new Active(session.id(), session.owner(), level, session.chestPos(),
                    session.centerChunk(), session.rightClickPos(), session.type(), session.mode(),
                    session.sub(), session.targetLevel(), spec, session.startedAt());
            active.remainingTicks = session.remainingTicks();
            active.breakAttempts = session.breakAttempts();
            active.targets.addAll(session.targets());
            for (UUID id : active.targets) {
                Entity entity = level.getEntity(id);
                if (entity instanceof TrialMagicBodyEntity) {
                    active.spawnedBodies++;
                } else if (entity instanceof Pig) {
                    active.pigIds.add(id);
                    active.spawnedPigs++;
                }
            }
            return active;
        }

        private int elapsedTicks() {
            return EXAM_TICKS - this.remainingTicks;
        }

        private ServerPlayer ownerPlayer(MinecraftServer server) {
            ServerPlayer player = server.getPlayerList().getPlayer(this.owner);
            return player != null && player.level() == this.level ? player : null;
        }

        private int aliveBodies() {
            int alive = 0;
            for (UUID id : this.targets) {
                if (this.level.getEntity(id) instanceof TrialMagicBodyEntity body && body.isAlive()) {
                    alive++;
                }
            }
            return alive;
        }

        private int aliveBodyOrPigCount() {
            int alive = 0;
            for (UUID id : this.targets) {
                Entity entity = this.level.getEntity(id);
                if (entity != null && entity.isAlive()) {
                    alive++;
                }
            }
            return alive;
        }

        private TrialMagicBodyEntity firstBody() {
            for (UUID id : this.targets) {
                if (this.level.getEntity(id) instanceof TrialMagicBodyEntity body && body.isAlive()) {
                    return body;
                }
            }
            return null;
        }

        /** 猪必须全部存活；猪被杀死 / 实体消失都算失败（D-18）。 */
        private boolean pigsAlive() {
            for (UUID id : this.pigIds) {
                Entity entity = this.level.getEntity(id);
                if (!(entity instanceof Pig pig) || !pig.isAlive()) {
                    return false;
                }
            }
            return true;
        }

        private ExamSession toSession() {
            return new ExamSession(this.id, this.owner, this.level.dimension().identifier(),
                    this.chestPos, this.centerChunk, this.rightClickPos, this.type, this.mode, this.sub,
                    this.targetLevel, this.remainingTicks, this.state, this.targets,
                    this.breakAttempts, this.startedAt);
        }
    }

    /** 通过 / 失败后 3 秒再清 HUD（D-13）。 */
    private static final class PendingResult {
        private final UUID owner;
        private final Identifier dimension;
        private int ticksLeft = RESULT_HUD_TICKS;

        private PendingResult(UUID owner, Identifier dimension) {
            this.owner = owner;
            this.dimension = dimension;
        }
    }
}
