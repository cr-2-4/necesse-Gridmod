package colox.gridmod.ui;

import java.awt.Color;
import java.util.function.Consumer;

import necesse.gfx.Renderer;
import necesse.gfx.forms.Form;
import necesse.gfx.forms.components.FormCustomDraw;
import necesse.gfx.forms.components.FormTextButton;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.gfx.ui.ButtonColor;

final class UiParts {

    private UiParts() {}

    // Lightweight SectionCard used by both tabs
    static final class SectionCard extends FormCustomDraw {
        SectionCard(int x, int y, int width, int height) {
            super(x, y, width, height);
            this.canBePutOnTopByClick = false;
            this.zIndex = Integer.MIN_VALUE / 2;
        }
        @Override public boolean shouldUseMouseEvents() { return false; }
        @Override public java.util.List<java.awt.Rectangle> getHitboxes() { return java.util.Collections.emptyList(); }
        @Override
        public void draw(necesse.engine.gameLoop.tickManager.TickManager tm,
                         necesse.entity.mobs.PlayerMob perspective,
                         java.awt.Rectangle renderBox) {
            Color base = this.getInterfaceStyle().activeElementColor;
            Color bg   = new Color(base.getRed(), base.getGreen(), base.getBlue(), 28);
            Color border = this.getInterfaceStyle().activeTextColor;
            Color borderA = new Color(border.getRed(), border.getGreen(), border.getBlue(), 100);

            Renderer.initQuadDraw(this.width, this.height).color(bg).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(this.width, 1).color(borderA).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(this.width, 1).color(borderA).draw(this.getX(), this.getY() + this.height - 1);
            Renderer.initQuadDraw(1, this.height).color(borderA).draw(this.getX(), this.getY());
            Renderer.initQuadDraw(1, this.height).color(borderA).draw(this.getX() + this.width - 1, this.getY());
        }
    }

    static void finishCard(SectionCard card, int bottomContentY) {
        final int CARD_PAD_Y = 8;
        int desired = (bottomContentY - card.getY()) + CARD_PAD_Y;
        if (desired < 10) desired = 10;
        card.height = desired;
    }

    // Shared tab header builder (keeps GridUIForm slim)
    static int buildTabs(
            Form form,
            int startX, int maxX,
            int yTop,
            String leftLabel, String rightLabel,
            Consumer<FormTextButton> leftOut,
            Consumer<FormTextButton> rightOut
    ) {
        final int gapX = 6;
        final int gapY = 6;
        final necesse.gfx.forms.components.FormInputSize tabSize = necesse.gfx.forms.components.FormInputSize.SIZE_24;
        final int tabH = tabSize.height;

        int leftW  = computeTabWidth(leftLabel, tabSize);
        int rightW = computeTabWidth(rightLabel, tabSize);

        int x = startX;
        int y = yTop;
        int rowMaxY = y + tabH;

        FormTextButton left = new FormTextButton(leftLabel, x, y - 2, leftW, tabSize, ButtonColor.BASE);
        form.addComponent(left);
        x += leftW + gapX;
        if (x > maxX) { x = startX; y = rowMaxY + gapY; rowMaxY = y + tabH; }

        FormTextButton right = new FormTextButton(rightLabel, x, y - 2, rightW, tabSize, ButtonColor.BASE);
        form.addComponent(right);

        leftOut.accept(left);
        rightOut.accept(right);
        return (rowMaxY - yTop);
    }

    private static int computeTabWidth(String label, necesse.gfx.forms.components.FormInputSize size) {
        FontOptions fo = size.getFontOptions();
        int textW = FontManager.bit.getWidthCeil(label, fo);
        int padding = 24, minW = 84, maxW = 220;
        int w = textW + padding;
        if (w < minW) w = minW;
        if (w > maxW) w = maxW;
        return w;
    }
}
