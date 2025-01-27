package runresumer.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.dungeons.AbstractDungeon;
import com.megacrit.cardcrawl.metrics.Metrics;
import com.megacrit.cardcrawl.monsters.MonsterGroup;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;

import java.io.IOException;
import java.nio.file.*;

import static runresumer.RunResumer.logger;

//Transfer saves from current run to run history
//if it was "console build" would also need to patch removeExcessRunFiles to get rid of old saves
public class SaveTheSaves {
    public static final String RUNS_PATH = "runresumer";
    @SpirePatch(
            clz = Metrics.class,
            method = "gatherAllDataAndSave"
    )
    public static class TimeToGo {
        @SpireInsertPatch(
                rloc = 37,
                localvars = { "file" }
        )
        public static void gatherTheSaves(Metrics __instance, boolean death, boolean trueVictor, MonsterGroup monsters, long ___lastPlaytimeEnd, FileHandle file) {
            logger.info("Storing run saves.");

            try {
                StringBuilder sb = new StringBuilder(SaveAndContinue.SAVE_PATH);
                if (CardCrawlGame.saveSlot != 0) {
                    sb.append(CardCrawlGame.saveSlot).append("_");
                }
                sb.append(AbstractDungeon.player.chosenClass.name());
                String filepath = sb.toString();

                String localStoragePath = Gdx.files.getLocalStoragePath();
                Path source = FileSystems.getDefault().getPath(localStoragePath + filepath);

                //Runs are stored in runs/slot_CHARACTER/ or runs/slot_DAILY/ for daily runs. slot_ omitted if slot 0.

                filepath = file.path();
                Path destination = FileSystems.getDefault().getPath(localStoragePath + filepath).getParent();
                Path subFolder = destination.getFileName();

                destination = FileSystems.getDefault().getPath(localStoragePath);
                destination = destination.resolve(RUNS_PATH).resolve(subFolder).resolve(Long.toString(___lastPlaytimeEnd));

                try {
                    Files.createDirectories(destination);
                } catch (IOException e) {
                    logger.info("Failed to create directory", e);
                }

                try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(source)) {
                    for (Path path : directoryStream) {
                        Path d2 = destination.resolve(path.getFileName());
                        Files.move(path, d2, StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    logger.error("Error occurred while transferring run saves", e);
                }
            }
            catch (Exception e) {
                logger.error("Failed to store run saves", e);
            }
        }
    }
}
