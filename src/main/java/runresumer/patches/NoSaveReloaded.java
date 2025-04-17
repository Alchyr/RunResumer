package runresumer.patches;

import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.helpers.Prefs;
import com.megacrit.cardcrawl.helpers.SaveHelper;
import com.megacrit.cardcrawl.metrics.Metrics;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.screens.DeathScreen;
import com.megacrit.cardcrawl.screens.VictoryScreen;
import com.megacrit.cardcrawl.screens.options.ExitGameButton;
import com.megacrit.cardcrawl.screens.options.OptionsPanel;
import javassist.CannotCompileException;
import javassist.expr.ExprEditor;
import javassist.expr.MethodCall;
import runresumer.ModConfig;
import runresumer.RunResumer;

import java.awt.*;
import java.lang.reflect.Field;

import static runresumer.RunResumer.logger;

public class NoSaveReloaded {
    @SpirePatch(
            clz = AbstractPlayer.class,
            method = SpirePatch.CLASS
    )
    public static class ResumedField {
        public static SpireField<Boolean> resumed = new SpireField<>(()->false);
    }

    @SpirePatch(
            clz = Metrics.class,
            method = "gatherAllDataAndSave"
    )
    public static class NoRunHistory {
        @SpirePrefixPatch
        public static SpireReturn<?> nope(Metrics __instance, boolean death, boolean trueVictor, MonsterGroup monsters) {
            boolean shouldSave = ModConfig.showShouldCreateNewSaves;
            if (!shouldSave && ResumedField.resumed.get(AbstractDungeon.player))
            {
                return SpireReturn.Return();
            }
            return SpireReturn.Continue();
        }
    }

    @SpirePatch(
            clz = SaveHelper.class,
            method = "shouldSave"
    )
    public static class DoNot
    {
        @SpirePrefixPatch
        public static SpireReturn<Boolean> noYouShouldNotSave()
        {
            boolean shouldSave = ModConfig.showShouldCreateNewSaves;

            if (!shouldSave && AbstractDungeon.player != null && ResumedField.resumed.get(AbstractDungeon.player))
            {
                return SpireReturn.Return(false);
            }
            return SpireReturn.Continue();
        }
    }

    //Gets called in AbstractRoom for post-combat saves regardless
    @SpirePatch(
            clz = SaveAndContinue.class,
            method = "save"
    )
    public static class NotHereEither
    {
        @SpirePrefixPatch
        public static SpireReturn<?> ree(SaveFile h)
        {
            boolean shouldSave = ModConfig.showShouldCreateNewSaves;

            logger.info("Should save config value? {}", shouldSave);

            if (!shouldSave && AbstractDungeon.player != null && ResumedField.resumed.get(AbstractDungeon.player))
            {
                CardCrawlGame.loadingSave = false;
                return SpireReturn.Return(null);
            }
            return SpireReturn.Continue();
        }
    }

    //Make exit button say it won't save
    @SpirePatch(
            clz = OptionsPanel.class,
            method = "refresh"
    )
    public static class SaveAndQuitButton {
        private static Field exitButtonField;

        static
        {
            try
            {
                exitButtonField = OptionsPanel.class.getDeclaredField("exitBtn");
                exitButtonField.setAccessible(true);
            }
            catch (Exception e)
            {
                logger.error("Failed to access exitBtn field of OptionsPanel class.", e);
            }
        }

        @SpirePostfixPatch
        public static void noSaving(OptionsPanel __instance) throws IllegalAccessException {
            boolean shouldSave = ModConfig.showShouldCreateNewSaves;

            if (!shouldSave && AbstractDungeon.player != null && ResumedField.resumed.get(AbstractDungeon.player))
            {
                ExitGameButton button = (ExitGameButton) exitButtonField.get(__instance);
                button.updateLabel(OptionsPanel.TEXT[15]);
            }
        }
    }

    @SpirePatch(
            clz = VictoryScreen.class,
            method = SpirePatch.CONSTRUCTOR
    )
    @SpirePatch(
            clz = DeathScreen.class,
            method = SpirePatch.CONSTRUCTOR
    )
    public static class NoChangeNeowAvailability {
        public static boolean allowSave() {
            boolean shouldSave = ModConfig.showShouldCreateNewSaves;

            return AbstractDungeon.player == null || shouldSave || !ResumedField.resumed.get(AbstractDungeon.player);
        }

        @SpireInstrumentPatch
        public static ExprEditor h() {
            return new ExprEditor() {
                @Override
                public void edit(MethodCall m) throws CannotCompileException {
                    if (m.getMethodName().equals("putInteger") && m.getClassName().equals(Prefs.class.getName())) {
                        m.replace("{" +
                                "if (" + NoChangeNeowAvailability.class.getName() + ".allowSave()) {" +
                                "$proceed($$);" +
                                "}" +
                                "}");
                    }
                }
            };
        }
    }
}
