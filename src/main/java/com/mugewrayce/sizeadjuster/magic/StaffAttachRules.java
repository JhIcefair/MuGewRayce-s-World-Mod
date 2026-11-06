package com.mugewrayce.sizeadjuster.magic;

import com.mugewrayce.sizeadjuster.component.CertData;
import com.mugewrayce.sizeadjuster.component.CertEntry;
import com.mugewrayce.sizeadjuster.component.StaffData;

import java.util.ArrayList;
import java.util.List;

/**
 * 普通魔法杖的绑定 / 附加 / 升级纯函数（W10）。
 *
 * <p>只做数据变换，不读 GUI、不扣魔力、不消耗物品；服务端菜单负责判定与消耗。
 * 法杖只能附加自然魔法（草案 §3.16）；起源 / 净化条目一律过滤。</p>
 */
public final class StaffAttachRules {

    private StaffAttachRules() {
    }

    /** 只保留自然魔法条目（按 {@link MagicSystem#NATURAL} 判定）。 */
    public static List<CertEntry> naturalEntries(List<CertEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        List<CertEntry> result = new ArrayList<>();
        for (CertEntry entry : entries) {
            MagicType type = MagicType.fromId(entry.type());
            if (type != null && type.system() == MagicSystem.NATURAL) {
                result.add(entry);
            }
        }
        return List.copyOf(result);
    }

    /** 合成魔法杖：绑学习证编号，并复制该证里的自然魔法条目。 */
    public static StaffData bindCert(CertData cert) {
        return new StaffData(cert.no(), naturalEntries(cert.entries()));
    }

    /**
     * 把学习证里尚未在法杖上的自然魔法条目加进去；同一条目只升不降。
     */
    public static StaffData merge(StaffData staff, CertData cert) {
        List<CertEntry> merged = new ArrayList<>(staff.entries());
        for (CertEntry candidate : naturalEntries(cert.entries())) {
            int existing = indexOf(merged, candidate);
            if (existing < 0) {
                merged.add(candidate);
            } else if (candidate.level() > merged.get(existing).level()) {
                merged.set(existing, candidate);
            }
        }
        return new StaffData(staff.boundCertNo(), merged);
    }

    /** 合并前后是否有条目变化（用于“没有可附加的新内容”）。 */
    public static boolean hasChange(StaffData before, StaffData after) {
        return before.boundCertNo() != after.boundCertNo()
                || !before.entries().equals(after.entries());
    }

    private static int indexOf(List<CertEntry> entries, CertEntry candidate) {
        for (int i = 0; i < entries.size(); i++) {
            CertEntry existing = entries.get(i);
            if (same(existing.type(), candidate.type())
                    && same(existing.mode(), candidate.mode())
                    && same(existing.sub(), candidate.sub())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean same(String a, String b) {
        return a == null ? b == null : a.equals(b);
    }
}
