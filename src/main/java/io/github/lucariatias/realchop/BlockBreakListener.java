package io.github.lucariatias.realchop;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class BlockBreakListener implements Listener {

    private RealChop plugin;
    private boolean detectBlockBreakLeaves;
    private boolean detectBlockBreakAll;

    public BlockBreakListener(RealChop plugin) {
        this.plugin = plugin;
        detectBlockBreakLeaves = plugin.isDetectBlockBreakLeaves();
        detectBlockBreakAll = plugin.isDetectBlockBreakAll();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block breakBlock = event.getBlock();
        Material breakBlockType = breakBlock.getType();
        if (!(detectBlockBreakAll || breakBlockType == Material.LOG || (detectBlockBreakLeaves && breakBlockType == Material.LEAVES)))
            return;
        Player player = event.getPlayer();
        plugin.blockBreak(breakBlock, player.getLocation());
    }
}
