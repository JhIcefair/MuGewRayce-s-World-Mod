package com.mugewrayce.sizeadjuster.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * 选项组（W09 通用控件）。
 *
 * <p>一排并列的选项键必须按两条规则实现（草案 §10.0 / §14.5）：</p>
 * <ol>
 *   <li>再按同一键 → 弹起（回到默认状态）；</li>
 *   <li>按同组另一键 → 自己弹起、对方按下（同组互斥，不会两个同时按下）。</li>
 * </ol>
 *
 * <p>返回键不属于选项组。组内按键的“按下”状态由本类统一维护；
 * 不应在组外直接 {@code setToggled(true)}，否则互斥会被破坏。</p>
 */
public final class OptionGroup {

    private final List<TriStateButton> buttons = new ArrayList<>();
    private TriStateButton selected;
    private Consumer<TriStateButton> onChange = button -> {
    };

    /** 加入一个选项键；加入时若它已经是按下状态，则以它作为当前选项。 */
    public void add(TriStateButton button) {
        this.buttons.add(button);
        button.setToggleMode(true);
        button.setOnPress(() -> this.toggle(button));
        if (button.isToggled()) {
            this.selectOnly(button);
        }
    }

    public void setOnChange(Consumer<TriStateButton> onChange) {
        this.onChange = onChange;
    }

    public List<TriStateButton> getButtons() {
        return Collections.unmodifiableList(this.buttons);
    }

    public TriStateButton getSelected() {
        return this.selected;
    }

    /** 点按同一键：已按下则弹起，否则选中它。 */
    public void toggle(TriStateButton button) {
        if (!button.isActive()) {
            return;
        }
        if (this.selected == button) {
            button.setToggled(false);
            this.selected = null;
            this.onChange.accept(null);
        } else {
            this.selectOnly(button);
        }
    }

    /** 直接选中某个键（同组其他键全部弹起）。 */
    public void select(TriStateButton button) {
        if (!button.isActive()) {
            return;
        }
        this.selectOnly(button);
    }

    /** 清空选择（全部弹起）。 */
    public void clearSelection() {
        if (this.selected != null) {
            this.selected.setToggled(false);
            this.selected = null;
            this.onChange.accept(null);
        }
    }

    private void selectOnly(TriStateButton button) {
        if (this.selected == button) {
            return;
        }
        if (this.selected != null) {
            this.selected.setToggled(false);
        }
        this.selected = button;
        button.setToggled(true);
        this.onChange.accept(button);
    }
}
