package dev.havoc.havoctab.shared.config.files;

import lombok.Getter;
import dev.havoc.havoctab.shared.HavocTab;
import dev.havoc.havoctab.shared.config.converter.LegacyConverter;
import dev.havoc.havoctab.shared.config.file.ConfigurationFile;
import dev.havoc.havoctab.shared.config.file.YamlConfigurationFile;
import dev.havoc.havoctab.shared.placeholders.animation.AnimationConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

@Getter
public class Animations {

    private final ConfigurationFile animationFile = new YamlConfigurationFile(getClass().getClassLoader().getResourceAsStream("config/animations.yml"),
            new File(HavocTab.getInstance().getDataFolder(), "animations.yml"));

    @NotNull private final AnimationConfiguration animations;

    public Animations() throws IOException {
        new LegacyConverter().convert2810to290(animationFile);
        animations = AnimationConfiguration.fromSection(animationFile.getConfigurationSection(""));
    }
}
