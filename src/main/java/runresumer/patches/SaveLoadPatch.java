package runresumer.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePrefixPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpireReturn;
import com.google.gson.Gson;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.ExceptionHandler;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.exceptions.SaveFileLoadError;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;
import com.megacrit.cardcrawl.saveAndContinue.SaveFileObfuscator;

import java.nio.file.Path;

import static runresumer.RunResumer.logger;

public class SaveLoadPatch {
    public static Path altSave = null;

    @SpirePatch(
            clz = SaveAndContinue.class,
            method = "loadSaveFile",
            paramtypez = {AbstractPlayer.PlayerClass.class}
    )
    public static class LoadAltSave {
        @SpirePrefixPatch
        public static SpireReturn<SaveFile> alt(AbstractPlayer.PlayerClass c) {
            if (altSave != null) {
                try {
                    SaveFile file = loadSaveFile(altSave);
                    altSave = null;
                    if (AbstractDungeon.player != null) {
                        NoSaveReloaded.ResumedField.resumed.set(AbstractDungeon.player, true);
                    }
                    else {
                        logger.warn("Player does not exist; failed to set resumed field");
                    }
                    return SpireReturn.Return(file);
                }
                catch (Exception e) {
                    logger.info("Exception occurred while loading save!");
                    ExceptionHandler.handleException(e, logger);
                    Gdx.app.exit();
                    return SpireReturn.Return(null);
                }
            }

            return SpireReturn.Continue();
        }

        private static SaveFile loadSaveFile(Path altSave) throws SaveFileLoadError {
            SaveFile saveFile = null;
            Gson gson = new Gson();
            String savestr = null;
            Exception err = null;

            try {
                FileHandle file = new FileHandle(altSave.toFile());
                String data = file.readString();
                savestr = SaveFileObfuscator.isObfuscated(data) ? SaveFileObfuscator.decode(data, "key") : data;
                saveFile = gson.fromJson(savestr, SaveFile.class);
            } catch (Exception e) {
                throw new SaveFileLoadError("Unable to load save file: " + altSave, e);
            }

            return saveFile;
        }
    }
}
