package dev.havoc.havoctab.platforms.fabric;

import com.mojang.brigadier.CommandDispatcher;
import dev.havoc.havoctab.shared.HavocTab;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ServerLevelData;
import org.jetbrains.annotations.NotNull;

/**
 * Main class for Fabric.
 */
public class FabricHavocTab implements DedicatedServerModInitializer {

    /** Command dispatcher instance for later command registration. */
    public static CommandDispatcher<CommandSourceStack> COMMAND_DISPATCHER;

    @Override
    public void onInitializeServer() {
        CommandRegistrationCallback.EVENT.register((dispatcher, commandBuildContext, commandSelection) -> COMMAND_DISPATCHER = dispatcher);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> HavocTab.create(new FabricPlatform(server)));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> HavocTab.getInstance().unload());
    }

    /**
     * Gets level name with dimension suffix to match Bukkit's behavior.
     *
     * @param   level
     *          Level to get name of
     * @return  Level name with dimension suffix
     */
    @NotNull
    public static String getLevelName(@NotNull Level level) {
        String path = level.dimension().identifier().getPath();
        return ((ServerLevelData)level.getLevelData()).getLevelName() + switch (path) {
            case "overworld" -> ""; // No suffix for overworld
            case "the_nether" -> "_nether";
            default -> "_" + path; // End + default behavior for other dimensions created by mods
        };
    }
}
