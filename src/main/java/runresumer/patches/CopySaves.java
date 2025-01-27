package runresumer.patches;

import com.badlogic.gdx.Gdx;
import com.evacipated.cardcrawl.modthespire.lib.SpireField;
import com.evacipated.cardcrawl.modthespire.lib.SpireInsertPatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePatch;
import com.evacipated.cardcrawl.modthespire.lib.SpirePostfixPatch;
import com.megacrit.cardcrawl.characters.AbstractPlayer;
import com.megacrit.cardcrawl.saveAndContinue.SaveAndContinue;
import com.megacrit.cardcrawl.saveAndContinue.SaveFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.regex.Pattern;

import static runresumer.RunResumer.logger;

public class CopySaves {
    public static int copyFloorNum = -1;

    @SpirePatch(
            clz = SaveAndContinue.class,
            method = "save"
    )
    public static class ShouldICopyTheSave {
        @SpireInsertPatch(
                rloc = 183
        )
        public static void check(SaveFile save) {
            if (save.floor_num > 0 && !save.post_combat) {
                copyFloorNum = save.floor_num;
            }
        }
    }

    @SpirePatch(
            clz = com.megacrit.cardcrawl.helpers.File.class,
            method = SpirePatch.CLASS
    )
    public static class DoCopyField {
        public static SpireField<Integer> saveFloor = new SpireField<>(()->-1);
    }

    @SpirePatch(
            clz = com.megacrit.cardcrawl.helpers.File.class,
            method = SpirePatch.CONSTRUCTOR
    )
    public static class LockInTheCopying {
        @SpirePostfixPatch
        public static void copyToField(com.megacrit.cardcrawl.helpers.File __instance, String filepath, String data) {
            if (copyFloorNum > 0) {
                DoCopyField.saveFloor.set(__instance, copyFloorNum);
                copyFloorNum = -1;
            }
        }
    }

    @SpirePatch(
            clz = com.megacrit.cardcrawl.helpers.File.class,
            method = "save"
    )
    public static class CopyTheSaves {
        //Normal save process:
        //Determine file location to save.
        //If file exists, copy that file to backup path, and delete original.
        //Create directory if necessary.
        //Save file.

        //Occurs in a separate thread.
        @SpireInsertPatch(
                rloc = 37,
                localvars = { "destination" }
        )
        public static void saveCopy(com.megacrit.cardcrawl.helpers.File __instance, Path destination) {
            if (DoCopyField.saveFloor.get(__instance) > 0) {
                logger.info("Copying save.");

                try {
                    String subFolder = destination.getFileName().toString();
                    int extensionIndex = subFolder.lastIndexOf('.');
                    if (extensionIndex > 0)
                        subFolder = subFolder.substring(0, subFolder.lastIndexOf('.'));

                    Path saveLocation = destination.getParent();
                    saveLocation = saveLocation.resolve(subFolder);

                    try {
                        Files.createDirectories(saveLocation);
                    } catch (IOException e) {
                        logger.info("Failed to create directory", e);
                    }

                    saveLocation = saveLocation.resolve(DoCopyField.saveFloor.get(__instance).toString());
                    copyAndValidate(destination, saveLocation, 2);
                }
                catch (Exception e) {
                    logger.info("Failed to copy save", e);
                }
            }
        }

        private static void copyAndValidate(Path source, Path target, int retry) {
            byte[] sourceData = new byte[0];

            try {
                sourceData = Files.readAllBytes(source);
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                if (retry <= 0) {
                    logger.info("Failed to copy " + source + " to " + target + ", but the retry expired", e);
                    return;
                }

                logger.info("Failed to copy file=" + source, e);
                sleep(300);
                copyAndValidate(source, target, retry - 1);
            }

            Exception err = validateWrite(target, sourceData);
            if (err != null) {// 98
                if (retry <= 0) {// 99
                    logger.info("Failed to copy " + source + " to " + target + ", but the retry expired", err);
                    return;
                }

                logger.info("Failed to copy file=" + source, err);
                sleep(300);
                copyAndValidate(source, target, retry - 1);
            }
        }

        private static void sleep(int milliseconds) {
            try {
                Thread.sleep(milliseconds);
            } catch (InterruptedException e) {
                logger.info(e);
            }
        }

        private static Exception validateWrite(Path filepath, byte[] inMemoryBytes) {
            byte[] writtenBytes;
            try {
                writtenBytes = Files.readAllBytes(filepath);
            } catch (IOException e) {
                return e;
            }

            boolean valid = Arrays.equals(writtenBytes, inMemoryBytes);
            return !valid ? new RuntimeException("Not valid: written=" + Arrays.toString(writtenBytes) + " vs inMemory=" + Arrays.toString(inMemoryBytes)) : null;
        }
    }

    @SpirePatch(
            clz = SaveAndContinue.class,
            method = "deleteSave"
    )
    public static class DeleteRunSaves {
        @SpirePostfixPatch
        public static void byebye(AbstractPlayer p) {
            String path = SaveAndContinue.getPlayerSavePath(p.chosenClass);
            String localStoragePath = Gdx.files.getLocalStoragePath();// 40
            Path destination = FileSystems.getDefault().getPath(localStoragePath + path);

            try {
                String subFolder = destination.getFileName().toString();
                int extensionIndex = subFolder.lastIndexOf('.');
                if (extensionIndex > 0)
                    subFolder = subFolder.substring(0, subFolder.lastIndexOf('.'));

                Path saveLocation = destination.getParent();
                saveLocation = saveLocation.resolve(subFolder);

                if (Files.isDirectory(saveLocation)) {
                    logger.info("Deleting run saves");

                    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(saveLocation)) {
                        for (Path file : directoryStream) {
                            if (Files.isRegularFile(file) && isNumeric(file.getFileName().toString())) {
                                Files.delete(file);
                            }
                        }
                    }
                    catch (IOException e) {
                        logger.error("Error occurred while deleting run saves", e);
                    }
                }
            }
            catch (Exception e) {
                logger.info("Failed to delete run saves", e);
            }
        }

        private static final Pattern numeric = Pattern.compile("^[0-9]*(?:\\.[0-9]+)?$");
        private static boolean isNumeric(String str) {
            return str != null && numeric.matcher(str).matches();
        }
    }
}
