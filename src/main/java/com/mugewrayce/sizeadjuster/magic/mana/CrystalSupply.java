package com.mugewrayce.sizeadjuster.magic.mana;

import com.mugewrayce.sizeadjuster.component.CrystalData;
import com.mugewrayce.sizeadjuster.component.ModComponents;
import com.mugewrayce.sizeadjuster.item.MagicItems;
import com.mugewrayce.sizeadjuster.util.ComponentSync;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;

import java.util.List;

/**
 * 普通魔法水晶的供能兜底（W13，接口约定 §3.6 / 草案 §3.17、§7.4）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>容量 30000、维持存在值 800；</li>
 *   <li>玩家身上按<b>快捷栏（0..8）→ 主背包（9..35）</b>取第一枚可用水晶
 *       （{@code supplyEnabled} 且 {@code mana > 0}）；未开供能的水晶不参与；</li>
 *   <li>扣到 {@code < 800}：本次允许扣到 800 以下，<b>随后立即消失</b>，把此时剩余的 mana
 *       （必然 &lt;800）按使用者坐标的九宫格全部分摊释放（允许超过最大值），然后 {@code shrink(1)}；</li>
 *   <li>水晶 {@code mana < 本次消耗} → 直接失败，<b>不扣、不写组件、不消失</b>（Q8）；
 *   <li>扣魔/消失后必须写回 {@code crystal_data} 并标记背包变更，否则客户端显示旧值。</li>
 * </ul>
 *
 * <p>永恒水晶不在这里：它连区块都不读，由 {@link ManaService} 直接返回
 * {@code EXEMPT_ETERNAL}；方块功能（聚合台/附加台）也走不到这里。</p>
 */
public final class CrystalSupply {

    /** 容量。 */
    public static final int CAPACITY = CrystalData.CAPACITY;
    /** 维持存在值：扣到该值以下即消失。 */
    public static final int KEEP_ALIVE = 800;
    /** 快捷栏 + 主背包（0..35），不含盔甲/副手。 */
    public static final int USABLE_SLOTS = 36;

    /** 找到的水晶：槽位、物品栈、当前组件。 */
    public record Supply(int slot, ItemStack stack, CrystalData data) {
    }

    private CrystalSupply() {
    }

    /** 快捷栏 → 主背包，取第一枚“开着供能且还有魔力”的普通水晶。 */
    public static Supply find(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < USABLE_SLOTS; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.is(MagicItems.magicCrystal)) {
                continue;
            }
            CrystalData data = stack.getOrDefault(ModComponents.CRYSTAL_DATA, CrystalData.DEFAULT);
            if (!data.supplyEnabled() || data.mana() <= 0) {
                continue;
            }
            return new Supply(slot, stack, data);
        }
        return null;
    }

    /** 背包/物品栏里是否带着永恒水晶（施法直接免消耗）。 */
    public static boolean hasEternal(ServerPlayer player) {
        return player.getInventory().contains(stack -> stack.is(MagicItems.eternalCrystal));
    }

    /**
     * 用水晶支付一笔消耗；不足时<b>没有任何副作用</b>。
     *
     * @return 是否支付成功
     */
    public static boolean pay(ServerPlayer player, Supply supply, int amount,
                              ChunkPos owner, int localX, int localZ) {
        if (supply == null || amount <= 0) {
            return supply != null;
        }
        CrystalData data = supply.data();
        if (data.mana() < amount) {
            return false;
        }
        int remaining = data.mana() - amount;
        if (remaining < KEEP_ALIVE) {
            if (remaining > 0) {
                release(player.level(), owner, localX, localZ, remaining);
            }
            supply.stack().shrink(1);
        } else {
            supply.stack().set(ModComponents.CRYSTAL_DATA,
                    new CrystalData(remaining, data.supplyEnabled(), data.autoSupplyEnabled()));
        }
        ComponentSync.markChanged(player);
        return true;
    }

    /** 消失时把剩余 mana 按九宫格全部分摊释放（允许超过最大值）。 */
    private static void release(ServerLevel level, ChunkPos owner, int localX, int localZ, int remaining) {
        ManaPosition.ManaSplit split = ManaPosition.split(owner, localX, localZ, remaining);
        List<ChunkPos> chunks = split.chunks();
        int[] amounts = split.amounts();
        for (int index = 0; index < chunks.size(); index++) {
            if (amounts[index] > 0) {
                ManaStorage.addChunk(level, chunks.get(index), amounts[index]);
            }
        }
    }

    /** 纯函数：本次扣减后剩余值 &lt;800 时，可释放的量就是剩余值本身。 */
    static int releaseAmount(int remaining) {
        return remaining > 0 && remaining < KEEP_ALIVE ? remaining : 0;
    }
}
