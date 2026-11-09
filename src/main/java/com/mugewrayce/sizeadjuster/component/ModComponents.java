package com.mugewrayce.sizeadjuster.component;

import net.minecraft.core.component.DataComponentType;

/**
 * 模组数据组件类型（W03 骨架，W06 填正式组件）。
 *
 * <p>这里只放 {@link DataComponentType} 常量，真正的注册在
 * {@code registry/ComponentRegistry}（统一走注册落点，接口约定 §6）。</p>
 *
 * <p>每个组件都<b>必须同时</b>给 {@code persistent} 与 {@code networkSynchronized}：
 * 只做持久化则客户端拿不到，只做网络则退出重进会丢。</p>
 *
 * <p>正式组件 ID 见接口约定 §2：{@code cert_data} / {@code proficiency_data} /
 * {@code promotion_data} / {@code application_data} / {@code staff_data} /
 * {@code attachment_data} / {@code attachment_book_data} / {@code crystal_data} /
 * {@code staff_selection_data} / {@code magic_book_data}。
 * W10 新增 {@code attachment_book_data}（附加书自身等级）；
 * W12 新增 {@code staff_selection_data} / {@code magic_book_data}。
 * W03 的 {@code w03_test_data} 保留到 W23 统一清理。</p>
 */
public final class ModComponents {

    /** 魔法学习证（{@link CertData}）。 */
    public static final DataComponentType<CertData> CERT_DATA =
            DataComponentType.<CertData>builder()
                    .persistent(CertData.CODEC)
                    .networkSynchronized(CertData.STREAM_CODEC)
                    .build();

    /** 魔法熟练证明（{@link ProficiencyData}）。 */
    public static final DataComponentType<ProficiencyData> PROFICIENCY_DATA =
            DataComponentType.<ProficiencyData>builder()
                    .persistent(ProficiencyData.CODEC)
                    .networkSynchronized(ProficiencyData.STREAM_CODEC)
                    .build();

    /** 魔法试炼晋级认证（{@link PromotionData}）。 */
    public static final DataComponentType<PromotionData> PROMOTION_DATA =
            DataComponentType.<PromotionData>builder()
                    .persistent(PromotionData.CODEC)
                    .networkSynchronized(PromotionData.STREAM_CODEC)
                    .build();

    /** 考试 / 试炼申请书（两份共用，{@link ApplicationData}）。 */
    public static final DataComponentType<ApplicationData> APPLICATION_DATA =
            DataComponentType.<ApplicationData>builder()
                    .persistent(ApplicationData.CODEC)
                    .networkSynchronized(ApplicationData.STREAM_CODEC)
                    .build();

    /** 普通魔法杖（{@link StaffData}）。 */
    public static final DataComponentType<StaffData> STAFF_DATA =
            DataComponentType.<StaffData>builder()
                    .persistent(StaffData.CODEC)
                    .networkSynchronized(StaffData.STREAM_CODEC)
                    .build();

    /** 被附加书附加过的武器（{@link AttachmentData}）。 */
    public static final DataComponentType<AttachmentData> ATTACHMENT_DATA =
            DataComponentType.<AttachmentData>builder()
                    .persistent(AttachmentData.CODEC)
                    .networkSynchronized(AttachmentData.STREAM_CODEC)
                    .build();

    /** 魔法附加书自身等级（{@link AttachmentBookData}，W10 新增）。 */
    public static final DataComponentType<AttachmentBookData> ATTACHMENT_BOOK_DATA =
            DataComponentType.<AttachmentBookData>builder()
                    .persistent(AttachmentBookData.CODEC)
                    .networkSynchronized(AttachmentBookData.STREAM_CODEC)
                    .build();

    /** 普通魔法水晶（{@link CrystalData}）。 */
    public static final DataComponentType<CrystalData> CRYSTAL_DATA =
            DataComponentType.<CrystalData>builder()
                    .persistent(CrystalData.CODEC)
                    .networkSynchronized(CrystalData.STREAM_CODEC)
                    .build();

    /** 普通魔法杖当前选择（{@link StaffSelectionData}，W12 新增）。 */
    public static final DataComponentType<StaffSelectionData> STAFF_SELECTION_DATA =
            DataComponentType.<StaffSelectionData>builder()
                    .persistent(StaffSelectionData.CODEC)
                    .networkSynchronized(StaffSelectionData.STREAM_CODEC)
                    .build();

    /** 魔法书设置（{@link MagicBookData}，W12 新增）。 */
    public static final DataComponentType<MagicBookData> MAGIC_BOOK_DATA =
            DataComponentType.<MagicBookData>builder()
                    .persistent(MagicBookData.CODEC)
                    .networkSynchronized(MagicBookData.STREAM_CODEC)
                    .build();

    /** W03 临时测试组件（接口约定 §2.2，W23 清理）。 */
    public static final DataComponentType<W03TestData> W03_TEST_DATA =
            DataComponentType.<W03TestData>builder()
                    .persistent(W03TestData.CODEC)
                    .networkSynchronized(W03TestData.STREAM_CODEC)
                    .build();

    private ModComponents() {
    }
}
