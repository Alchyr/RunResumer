package runresumer;

import basemod.BaseMod;
import com.evacipated.cardcrawl.modthespire.Loader;
import com.evacipated.cardcrawl.modthespire.ModInfo;
import com.evacipated.cardcrawl.modthespire.Patcher;
import com.evacipated.cardcrawl.modthespire.lib.SpireInitializer;
import com.megacrit.cardcrawl.helpers.ImageMaster;
import com.megacrit.cardcrawl.localization.UIStrings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.scannotation.AnnotationDB;
import basemod.interfaces.*;
import java.util.*;

@SpireInitializer
public class RunResumer implements PostInitializeSubscriber, EditStringsSubscriber {
    public static ModInfo info;
    public static String modID;
    public static String[] authors;
    static { loadModInfo(); }
    public static final Logger logger = LogManager.getLogger(modID);

    public static void initialize() {
        BaseMod.subscribe(new RunResumer());
    }

    //This determines the mod's ID based on information stored by ModTheSpire.
    private static void loadModInfo() {
        Optional<ModInfo> infos = Arrays.stream(Loader.MODINFOS).filter((modInfo)->{
            AnnotationDB annotationDB = Patcher.annotationDBMap.get(modInfo.jarURL);
            if (annotationDB == null)
                return false;
            Set<String> initializers = annotationDB.getAnnotationIndex().getOrDefault(SpireInitializer.class.getName(), Collections.emptySet());
            return initializers.contains(RunResumer.class.getName());
        }).findFirst();
        if (infos.isPresent()) {
            info = infos.get();
            modID = info.ID;
            authors = info.Authors;
        }
        else {
            throw new RuntimeException("Failed to determine mod info/ID based on initializer.");
        }
    }

    @Override
    public void receivePostInitialize() {
        BaseMod.registerModBadge(ImageMaster.loadImage("modBadge.png"), modID, String.join(",", authors), "", new ModConfig());
    }

    @Override
    public void receiveEditStrings() {
        BaseMod.loadCustomStringsFile(UIStrings.class, "localization/eng/UI-Strings.json");
    }
}
