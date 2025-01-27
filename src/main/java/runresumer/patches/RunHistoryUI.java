package runresumer.patches;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.evacipated.cardcrawl.modthespire.lib.*;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.core.CardCrawlGame;
import com.megacrit.cardcrawl.core.Settings;
import com.megacrit.cardcrawl.helpers.ModHelper;
import com.megacrit.cardcrawl.helpers.input.InputHelper;
import com.megacrit.cardcrawl.screens.runHistory.RunHistoryPath;
import com.megacrit.cardcrawl.screens.runHistory.RunHistoryScreen;
import com.megacrit.cardcrawl.screens.runHistory.RunPathElement;
import com.megacrit.cardcrawl.screens.stats.RunData;
import javassist.CtBehavior;
import runresumer.TextButton;
import runresumer.ZipUtility;

import java.awt.Desktop;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.ArrayList;

import static runresumer.RunResumer.logger;

import static runresumer.patches.SaveTheSaves.RUNS_PATH;

public class RunHistoryUI {
    @SpirePatch(
            clz = RunData.class,
            method = SpirePatch.CLASS
    )
    public static class RunDataField {
        public static SpireField<FileHandle> file = new SpireField<>(()->null);
    }

    @SpirePatch(
            clz = RunHistoryScreen.class,
            method = "refreshData"
    )
    public static class StoreRunTimestamp {
        @SpireInsertPatch(
                locator = Locator.class,
                localvars = { "data", "file" }
        )
        public static void storeIt(RunHistoryScreen __instance, RunData data, FileHandle file) {
            RunDataField.file.set(data, file);
        }

        public static class Locator extends SpireInsertLocator {
            @Override
            public int[] Locate(CtBehavior ctMethodToPatch) throws Exception {
                return LineFinder.findInOrder(ctMethodToPatch, new Matcher.MethodCallMatcher(ArrayList.class, "add"));
            }
        }
    }

    @SpirePatch(
            clz = RunHistoryPath.class,
            method = "setRunData"
    )
    public static class CheckForRunSaves {
        private static Path savesDirectory = null;

        @SpirePrefixPatch
        public static void checkExists(RunHistoryPath __instance, RunData newData) {
            savesDirectory = null;

            FileHandle h = RunDataField.file.get(newData);
            if (h != null) {
                String filepath = h.path();
                filepath = filepath.substring(filepath.indexOf('/'), filepath.lastIndexOf('.'));

                String localStoragePath = Gdx.files.getLocalStoragePath();
                Path source = FileSystems.getDefault().getPath(localStoragePath + RUNS_PATH + filepath);

                if (Files.isDirectory(source)) {
                    savesDirectory = source;
                }
                else {
                    RunDataField.file.set(newData, null);
                }
            }
        }

        @SpireInsertPatch(
                rloc = 167,
                localvars = { "element", "floor" }
        )
        public static void checkRoomSave(RunHistoryPath __instance, RunData newData, RunPathElement element, int floor) {
            if (savesDirectory != null) {
                Path floorSave = savesDirectory.resolve(Integer.toString(floor));
                if (Files.isRegularFile(floorSave)) {
                    PathSaveField.save.set(element, floorSave);
                }
            }
        }
    }

    //Exporting Run
    private static final TextButton exportButton;
    static {
        if (Desktop.isDesktopSupported()) {
            String text = "Export Run";
            switch (Settings.language) {

            }
            exportButton = new TextButton(text, Settings.WIDTH - (TextButton.WIDTH * Settings.scale * 0.5f), 185 * Settings.scale,
                    RunHistoryUI::exportFile);
        }
        else {
            exportButton = null;
        }
    }

    private static void exportFile() {
        try {
            RunData run = (RunData) AllowLoad.viewedRun.get(CardCrawlGame.mainMenuScreen.runHistoryScreen);
            FileHandle h = RunDataField.file.get(run);
            if (h != null) {
                String filepath = h.path();
                filepath = filepath.substring(filepath.indexOf('/'), filepath.lastIndexOf('.'));
                //filepath = /slot_char/run_time

                String localStoragePath = Gdx.files.getLocalStoragePath();
                Path source = FileSystems.getDefault().getPath(localStoragePath + RUNS_PATH + filepath);

                if (Files.isDirectory(source)) {
                    Path filename = source.getFileName();
                    Path exportDest = FileSystems.getDefault().getPath(localStoragePath + RUNS_PATH + "/exports");
                    exportDest = exportDest.resolve(filename); //Export dest is runresumer/exports/run_time

                    Files.createDirectories(exportDest);
                    filepath = filepath.substring(1); //remove initial /

                    Path d2;

                    //Copy run saves to export directory
                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(source)) {
                        for (Path path : directoryStream) {
                            d2 = exportDest.resolve("runresumer").resolve(filepath);
                            Files.createDirectories(d2);
                            d2 = d2.resolve(path.getFileName());
                            Files.copy(path, d2, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        logger.error("Error occurred while transferring run saves", e);
                    }

                    //Copy run history file to export directory
                    source = FileSystems.getDefault().getPath(localStoragePath + h.path());
                    d2 = exportDest.resolve(h.path());
                    Files.createDirectories(d2.getParent());
                    Files.copy(source, d2, StandardCopyOption.REPLACE_EXISTING);

                    try {
                        new ZipUtility().zip(exportDest.resolveSibling("export.zip"), exportDest.resolve("runresumer"), exportDest.resolve("runs"));
                    }
                    catch (Exception e) {
                        logger.error("Failed to compress export to zip", e);
                    }
                    Desktop.getDesktop().open(exportDest.getParent().toFile());
                }
            }
        }
        catch (Exception e) {
            logger.error("Failed to export", e);
        }
    }

    @SpirePatch(
            clz = RunHistoryScreen.class,
            method = "update"
    )
    public static class UpdateExportButton {
        @SpirePrefixPatch
        public static void upd(RunHistoryScreen __instance) throws IllegalAccessException {
            RunData run = (RunData) AllowLoad.viewedRun.get(__instance);
            if (exportButton != null  && run != null && RunDataField.file.get(run) != null) {
                exportButton.update(-25f);
            }
        }
    }

    @SpirePatch(
            clz = RunHistoryScreen.class,
            method = "render"
    )
    public static class RenderExportButton {
        @SpirePostfixPatch
        public static void render(RunHistoryScreen __instance, SpriteBatch sb) throws IllegalAccessException {
            RunData run = (RunData) AllowLoad.viewedRun.get(__instance);
            if (exportButton != null && run != null && RunDataField.file.get(run) != null) {
                exportButton.render(sb);
            }
        }
    }




    //Resuming Run
    @SpirePatch(
            clz = RunPathElement.class,
            method = SpirePatch.CLASS
    )
    public static class PathSaveField {
        public static SpireField<Path> save = new SpireField<>(()->null);
    }

    @SpirePatch(
            clz = RunPathElement.class,
            method = "update"
    )
    public static class AllowLoad {
        private static String continuableText;
        static {
            switch (Settings.language) {
                default:
                    continuableText = " (Continuable)";
                    break;
            }
        }

        private static final Field viewedRun;
        static {
            try {
                viewedRun = RunHistoryScreen.class.getDeclaredField("viewedRun");
                viewedRun.setAccessible(true);
            } catch (NoSuchFieldException e) {
                throw new RuntimeException(e);
            }
        }

        @SpireInsertPatch(
                rloc = 6,
                localvars = { "header" }
        )
        public static void changeTextAndAlsoCheckClick(RunPathElement __instance, @ByRef String[] header) throws IllegalAccessException {
            if (PathSaveField.save.get(__instance) != null) {
                header[0] = header[0] + continuableText;

                if (InputHelper.justReleasedClickRight) {
                    InputHelper.justReleasedClickRight = false;
                    logger.info("Continuing run...");

                    RunData run = (RunData) viewedRun.get(CardCrawlGame.mainMenuScreen.runHistoryScreen);
                    if (run == null) return;

                    AbstractPlayer.PlayerClass toLoad = null;
                    for (AbstractPlayer.PlayerClass pClass : AbstractPlayer.PlayerClass.values()) {
                        if (run.character_chosen.equals(pClass.name())) {
                            toLoad = pClass;
                            break;
                        }
                    }

                    SaveLoadPatch.altSave = PathSaveField.save.get(__instance);

                    CardCrawlGame.loadingSave = true;
                    CardCrawlGame.chosenCharacter = toLoad;
                    CardCrawlGame.mainMenuScreen.isFadingOut = true;
                    CardCrawlGame.mainMenuScreen.fadeOutMusic();
                    Settings.isDailyRun = false;
                    Settings.isTrial = false;
                    ModHelper.setModsFalse();
                    if (CardCrawlGame.steelSeries.isEnabled) {
                        CardCrawlGame.steelSeries.event_character_chosen(CardCrawlGame.chosenCharacter);
                    }
                }
            }
        }
    }
}
