package com.gmail.nossr50.config;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.sounds.SoundType;

import java.util.ArrayList;
import java.util.List;

public class SoundConfig extends ConfigValidated {
    public static final String SOUNDS = "Sounds";
    public static final String VOLUME = "Volume";
    public static final String PITCH = "Pitch";
    public static final String ENABLED = "Enabled";
    public static final String MASTER_VOLUME = "MasterVolume";
    //private static SoundConfig instance;

    public SoundConfig() {
        //super(McmmoCore.getDataFolderPath().getAbsoluteFile(), "sounds.yml", true);
        super(mcMMO.p.getDataFolder().getAbsoluteFile(), "sounds.yml", true, true, true);
    }

    /**
     * This grabs an instance of this config class from the Config Manager
     * This method is deprecated and will be removed in the future
     * @see mcMMO#getConfigManager()
     * @return the instance of this config
     * @deprecated Please use mcMMO.getConfigManager() to grab a specific config instead
     */
    @Deprecated
    public static SoundConfig getInstance() {
        return mcMMO.getConfigManager().getSoundConfig();
    }

    @Override
    public void unload() {

    }

    /**
     * The version of this config
     *
     * @return
     */
    @Override
    public double getConfigVersion() {
        return 1;
    }

    @Override
    public List<String> validateKeys() {
        ArrayList<String> reasons = new ArrayList<>();

        for (SoundType soundType : SoundType.values()) {
            if (getDoubleValue(SOUNDS, soundType.toString(), VOLUME) < 0) {
                reasons.add("[mcMMO] Sound volume cannot be below 0 for " + soundType.toString());
            }

            //Sounds with custom pitching don't use pitch values
            if (!soundType.usesCustomPitch()) {
                if (getDoubleValue(SOUNDS, soundType.toString(), PITCH) < 0) {
                    reasons.add("[mcMMO] Sound pitch cannot be below 0 for " + soundType.toString());
                }
            }
        }

        return reasons;
    }

    public float getMasterVolume() {
        return (float) getDoubleValue(SOUNDS, MASTER_VOLUME);
    }

    public float getVolume(SoundType soundType) {
        return (float) getDoubleValue(SOUNDS, soundType.toString(), VOLUME);
    }

    public float getPitch(SoundType soundType) {
        return (float) getDoubleValue(SOUNDS, soundType.toString(), PITCH);
    }

    public boolean getIsEnabled(SoundType soundType) {
        return getBooleanValue(SOUNDS, soundType.toString(), ENABLED);
    }
}
