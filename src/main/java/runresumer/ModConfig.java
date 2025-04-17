package runresumer;

import basemod.EasyConfigPanel;

public class ModConfig extends EasyConfigPanel {
    public static boolean showShouldCreateNewSaves = false;
    public ModConfig() {
        super(RunResumer.modID, RunResumer.modID+":ModConfig");
    }
}
