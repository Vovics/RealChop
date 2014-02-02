package io.github.lucariatias.realchop;

import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class StructureGrowListener implements Listener {

    private RealChop plugin;

    public StructureGrowListener(RealChop plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onStructureGrowEvent(StructureGrowEvent event) {
        int hash = 0;
        for (int i = 0; i < event.getBlocks().size(); i++) {
            BlockState blockState = event.getBlocks().get(i);
            if (hash == 0) {
                hash = blockState.getBlock().hashCode();
            }
            blockState.setMetadata("TreeId", new FixedMetadataValue(plugin, hash));
        }
    }

}
