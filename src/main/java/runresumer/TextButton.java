package runresumer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.*;
import com.megacrit.cardcrawl.helpers.controller.CInputActionSet;
import com.megacrit.cardcrawl.helpers.input.InputHelper;

public class TextButton {
    public static final int WIDTH = 358;
    public static final float SPACE_Y = 50.0F * Settings.scale;;
    private static Texture highlightImg = null;

    private String label;
    public Hitbox hb;
    private Color tint;
    private final Color highlightColor;
    private boolean hidden;
    private final float baseX;
    private float x;
    private float targetX;

    private final Runnable effect;

    public TextButton(String text, float x, float y, Runnable effect) {
        this.tint = Color.WHITE.cpy();
        this.highlightColor = new Color(1.0F, 1.0F, 1.0F, 0.0F);
        this.hidden = false;
        this.x = baseX = x;
        this.targetX = x;
        if (highlightImg == null) {
            highlightImg = ImageMaster.loadImage("images/ui/mainMenu/menu_option_highlight.png");
        }

        this.label = text;
        this.effect = effect;

        if (!Settings.isTouchScreen && !Settings.isMobile) {
            this.hb = new Hitbox(FontHelper.getSmartWidth(FontHelper.buttonLabelFont, this.label, 9999.0F, 1.0F) + 100.0F * Settings.scale, SPACE_Y);
            this.hb.move(x, y);
        } else {
            this.hb = new Hitbox(FontHelper.getSmartWidth(FontHelper.losePowerFont, this.label, 9999.0F, 1.0F) * 1.25F + 100.0F * Settings.scale, SPACE_Y * 2.0F);
            this.hb.move(x, y);
        }
    }

    public void update(float hoverOffset) {
        this.hb.update();

        this.x = MathHelper.uiLerpSnap(this.x, this.targetX);
        if (this.hb.justHovered && !this.hidden) {
            CardCrawlGame.sound.playV("UI_HOVER", 0.75F);
        }

        if (this.hb.hovered) {
            this.highlightColor.a = 0.9F;
            this.targetX = baseX + hoverOffset * Settings.scale;
            if (InputHelper.justClickedLeft) {
                CardCrawlGame.sound.playA("UI_CLICK_1", -0.1F);
                this.hb.clickStarted = true;
            }

            this.tint = Color.WHITE.cpy();
        } else {
            this.highlightColor.a = MathHelper.fadeLerpSnap(this.highlightColor.a, 0.0F);
            this.targetX = baseX;
            this.hidden = false;
            this.tint.r = MathHelper.fadeLerpSnap(this.tint.r, 0.3F);
            this.tint.g = this.tint.r;
            this.tint.b = this.tint.r;
        }

        if (this.hb.hovered && CInputActionSet.select.isJustPressed()) {
            CInputActionSet.select.unpress();
            this.hb.clicked = true;
            CardCrawlGame.sound.playA("UI_CLICK_1", -0.1F);
        }

        if (this.hb.clicked) {
            this.hb.clicked = false;
            this.buttonEffect();
        }
    }

    public void hide(float hideX) {
        this.hb.hovered = false;
        this.targetX = hideX;
        this.hidden = true;
    }

    public void buttonEffect() {
        effect.run();
    }

    public void render(SpriteBatch sb) {
        sb.setBlendFunction(770, 1);
        sb.setColor(this.highlightColor);
        if (!Settings.isTouchScreen && !Settings.isMobile) {
            sb.draw(highlightImg, x - 179, this.hb.cY - 52.0F, 179.0F, 52.0F, WIDTH, 104.0F, Settings.scale, Settings.scale * 0.8F, 0.0F, 0, 0, WIDTH, 104, false, false);
        } else {
            sb.draw(highlightImg, x - 179, this.hb.cY - 56.0F, 179.0F, 52.0F, WIDTH, 104.0F, Settings.scale, Settings.scale * 1.2F, 0.0F, 0, 0, WIDTH, 104, false, false);
        }

        sb.setBlendFunction(770, 771);
        if (!Settings.isTouchScreen && !Settings.isMobile) {
            FontHelper.renderFontCentered(sb, FontHelper.buttonLabelFont, this.label, this.x, this.hb.cY, Settings.CREAM_COLOR);
        } else {
            FontHelper.renderFontCentered(sb, FontHelper.losePowerFont, this.label, this.x, this.hb.cY, Settings.CREAM_COLOR, 1.25f);
        }

        this.hb.render(sb);
    }
}