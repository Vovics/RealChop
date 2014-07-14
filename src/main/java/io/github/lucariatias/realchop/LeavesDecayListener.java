package io.github.lucariatias.realchop;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.LeavesDecayEvent;

public class LeavesDecayListener implements Listener {

    private RealChop plugin;
    private boolean detectBlockBreakLeaves;
    private boolean detectBlockBreakAll;

    public LeavesDecayListener(RealChop plugin) {
        this.plugin = plugin;
        detectBlockBreakLeaves = plugin.isDetectBlockBreakLeaves();
        detectBlockBreakAll = plugin.isDetectBlockBreakAll();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (event.isCancelled()) return;
        Block breakBlock = event.getBlock();
        if (!detectBlockBreakAll && !detectBlockBreakLeaves) {
            return;
        }
        plugin.blockBreak(breakBlock, breakBlock.getLocation().clone().add(1, 0, 0));
    }
}
