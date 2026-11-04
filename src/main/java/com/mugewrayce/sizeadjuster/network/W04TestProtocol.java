package com.mugewrayce.sizeadjuster.network;

/**
 * W04 服务端重算规则（纯函数，接口约定 §3.1）。
 *
 * <p>把"校验 + 重算"从网络回调里剥出来，好处是<b>不启动游戏就能静态验证</b>三种分支
 * （接受 / 拒绝 / 未知动作），后续 W11+ 的业务判定也照这个写法：handler 只负责取字段、
 * 调纯函数、回状态；规则本身可单测。</p>
 *
 * <p>规则（与交接单 / 接口约定一致）：</p>
 * <ul>
 *   <li>{@link #ACTION_PING} → accepted，value=1；</li>
 *   <li>{@link #ACTION_RECOMPUTE} 且 {@code 0 <= value <= 100} → accepted，value=value×2；</li>
 *   <li>{@link #ACTION_RECOMPUTE} 越界 → rejected，value=0，{@link #REASON_OUT_OF_RANGE}；</li>
 *   <li>未知 action → rejected，value=0，{@link #REASON_BAD_ACTION}。</li>
 * </ul>
 */
public final class W04TestProtocol {

    public static final int ACTION_PING = 0;
    public static final int ACTION_RECOMPUTE = 1;

    public static final int REASON_NONE = 0;
    public static final int REASON_BAD_ACTION = 1;
    public static final int REASON_OUT_OF_RANGE = 2;

    /** {@link #ACTION_RECOMPUTE} 允许的输入区间（闭区间）。 */
    public static final int RECOMPUTE_MIN = 0;
    public static final int RECOMPUTE_MAX = 100;

    private W04TestProtocol() {
    }

    /**
     * 服务端重算：只依赖入参，不读写任何状态，拒绝分支也不会产生副作用（草案 §14.5）。
     *
     * @param requestId 客户端请求号，原样回显
     * @param action    动作码
     * @param value     客户端提交的值（不可信，必须校验）
     * @return 回给客户端的状态
     */
    public static W04TestStatePayload evaluate(int requestId, int action, int value) {
        return switch (action) {
            case ACTION_PING -> new W04TestStatePayload(requestId, true, 1, REASON_NONE);
            case ACTION_RECOMPUTE -> {
                if (value < RECOMPUTE_MIN || value > RECOMPUTE_MAX) {
                    yield new W04TestStatePayload(requestId, false, 0, REASON_OUT_OF_RANGE);
                }
                yield new W04TestStatePayload(requestId, true, value * 2, REASON_NONE);
            }
            default -> new W04TestStatePayload(requestId, false, 0, REASON_BAD_ACTION);
        };
    }
}
