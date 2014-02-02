package io.github.lucariatias.realchop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.material.MaterialData;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RealChopListener implements Listener {

    private final RealChop plugin;
    private boolean configDetectBlockBreakLeaves;
    private boolean configDetectBlockBreakAll;
    private boolean configFallingLeaves;
    private int configBlockProcessingLimit;

    public RealChopListener(RealChop plugin) {
        this.plugin = plugin;
        configDetectBlockBreakLeaves = false;
        configDetectBlockBreakAll = false;

        String DetectBlockBreak = plugin.config.getString("DetectBlockBreak");
        if (DetectBlockBreak.equalsIgnoreCase("leaves")) {
            configDetectBlockBreakLeaves = true;
        }
        if (DetectBlockBreak.equalsIgnoreCase("all")) {
            configDetectBlockBreakLeaves = true;
            configDetectBlockBreakAll = true;
        }

        configBlockProcessingLimit = plugin.config.getInt("BlockProcessingLimit");
        if (configBlockProcessingLimit < 5 || configBlockProcessingLimit > 1000)
            configBlockProcessingLimit = 150;
        configFallingLeaves = plugin.config.getBoolean("FallingLeaves");
        // plugin.getLogger().log(Level.INFO,"Fallingleaves: " +
        // configFallingLeaves);
        // plugin.getLogger().log(Level.INFO,"DetectBlockBreakAll: " +
        // configDetectBlockBreakAll);
        // plugin.getLogger().log(Level.INFO,"DetectBlockBreakLeaves: " +
        // configDetectBlockBreakLeaves);
        // plugin.getLogger().log(Level.INFO,"DetectBlockBreakLog: " +
        // configDetectBlockBreakLog);
        // plugin.getLogger().log(Level.INFO,"BlockProcessingLimit: " +
        // configBlockProcessingLimit);

    }

    private boolean isLightBlock(Material m) {
        return m == Material.LEAVES || m == Material.AIR || m == Material.TORCH || m == Material.LONG_GRASS || m == Material.RED_MUSHROOM || m == Material.YELLOW_FLOWER || m == Material.VINE || m == Material.SNOW || m == Material.ARROW || m == Material.COCOA || m == Material.LADDER || m == Material.WEB || m == Material.SAPLING || m == Material.WATER || m == Material.STATIONARY_WATER;
    }

    private HashMap<Location, Block> getNearBlocks(Location l, int radius) {
        int lx = l.getBlockX();
        int ly = l.getBlockY();
        int lz = l.getBlockZ();
        World w = l.getWorld();
        HashMap<Location, Block> m = new HashMap<>();
        for (int z = lz - radius; z <= lz + radius; z++) {
            for (int x = lx - radius; x <= lx + radius; x++) {
                for (int y = ly - radius; y <= ly + radius; y++) {
                    Location tl = new Location(w, (double) x, (double) y, (double) z);
                    Block b = w.getBlockAt(tl);
                    if (tl != l) {
                        m.put(tl, b);
                    }
                }
            }
        }
        return m;
    }

    float calcSpeed(float horisontalDistance, float verticalDistance, int horisontalOffset) {
        float speed = 0;
        if (verticalDistance > 0) {
            speed = (horisontalDistance - horisontalOffset) / (float) Math.sqrt(2 * (verticalDistance) / 0.064814);
        }
        return speed;

    }

    void BlockBreak(Block breakBlock, Location playerLocation) {
        Material breakBlockType = breakBlock.getType();
        Location breakBlockLocation = breakBlock.getLocation();

        int treeId = 0;
        if (breakBlockType != Material.LOG)
            treeId = 1;

        if (breakBlock.getMetadata("TreeId").iterator().hasNext()) {
            treeId = breakBlock.getMetadata("TreeId").iterator().next().asInt();
        }
        World world = breakBlock.getWorld();
        Vector direction = new Vector(playerLocation.getX() - breakBlockLocation.getX(), 0, playerLocation.getZ() - breakBlockLocation.getZ());
        direction.normalize();
        float angle = direction.angle(new Vector(0, 0, 1));
        // double angle2 = angle * 180 / Math.PI;
        int angle1 = 90;
        if (direction.getX() > 0) {
            if (angle > Math.PI * 1 / 4)
                angle1 = 180;
            if (angle > Math.PI * 3 / 4)
                angle1 = 270;
        } else {
            if (angle > Math.PI * 1 / 4)
                angle1 = 0;
            if (angle > Math.PI * 3 / 4)
                angle1 = 270;
        }
        switch (angle1) {
            case 0:
                direction.setX(0);
                direction.setZ(1);
                break;
            case 90:
                direction.setX(1);
                direction.setZ(0);
                break;
            case 180:
                direction.setX(0);
                direction.setZ(-1);
                break;
            case 270:
                direction.setX(-1);
                direction.setZ(0);
                break;
        }
        // player.sendMessage("angle: "+ angle1 + " " + angle2 + "^ " + angle +
        // " x:" + direction.getX() + " z:" + direction.getZ());
        // player.sendMessage("direction: "+ direction.toString());

        HashMap<Location, Block> tree = new HashMap<>();
        HashMap<Location, Block> solid = new HashMap<>();
        HashMap<Location, Block> search = new HashMap<>();
        search.put(breakBlockLocation, breakBlock);

        // filling tree
        boolean findNext = true;
        int limit = 0;
        while (findNext) {
            findNext = false;
            HashMap<Location, Block> newSearch = new HashMap<>();
            for (Map.Entry<Location, Block> pairs : search.entrySet()) {
                Location l = pairs.getKey();
                HashMap<Location, Block> near = getNearBlocks(l, 1);
                for (Map.Entry<Location, Block> nearPairs : near.entrySet()) {
                    Location nearLocation = nearPairs.getKey();
                    Block nearBlock = nearPairs.getValue();
                    if (nearBlock.getType() == Material.LOG) {
                        if (!tree.containsKey(nearLocation)) {
                            Boolean put = false;
                            if (treeId == 0 && nearBlock.getMetadata("TreeId").isEmpty())
                                put = true;
                            if (treeId == 1)
                                put = true;
                            if (treeId != 0) {
                                if (nearBlock.getMetadata("TreeId").iterator().hasNext() && nearBlock.getMetadata("TreeId").iterator().next().asInt() == treeId) {
                                    put = true;
                                }
                            }
                            if (put) {
                                tree.put(nearLocation, nearBlock);
                                newSearch.put(nearLocation, nearBlock);
                                findNext = true;
                            }
                        }
                    }
                    if (nearBlock.getType() != Material.LOG && !isLightBlock(nearBlock.getType())) {
                        solid.put(nearLocation, nearBlock);
                        // player.sendMessage("SolidBlock : " +
                        // nearBlock.getType());
                    }

                }
            }
            limit++;
            if (limit > configBlockProcessingLimit) {
                plugin.getLogger().log(Level.INFO, "Tree Logs search reached BlockProcessingLimit.");
                break;
            }
            if (findNext) {
                search.clear();
                search.putAll(newSearch);
            }

        }
        tree.remove(breakBlockLocation);
        solid.remove(breakBlockLocation);
        breakBlock.removeMetadata("TreeId", plugin);
        // player.sendMessage("This tree contains " + tree.size() +
        // " blocks, and connected " + solid.size() + " solid blocks");

        // int logsCount = tree.size();

        // defilling tree depends on solid blocks
        search.clear();
        search.putAll(solid);
        findNext = true;
        limit = 0;
        while (findNext) {
            findNext = false;
            HashMap<Location, Block> newSearch = new HashMap<>();
            for (Map.Entry<Location, Block> pairs : search.entrySet()) {
                Location l = pairs.getKey();
                // if (l == breakBlockLocation) continue;
                HashMap<Location, Block> near = getNearBlocks(l, 1);
                for (Map.Entry<Location, Block> nearPairs : near.entrySet()) {
                    Location nearLocation = nearPairs.getKey();
                    // if (nearLocation == breakBlockLocation) continue;
                    Block nearBlock = nearPairs.getValue();
                    if (nearBlock.getType() == Material.LOG) {
                        if (tree.containsKey(nearLocation)) {
                            tree.remove(nearLocation);
                            newSearch.put(nearLocation, nearBlock);
                            findNext = true;
                        }
                    }
                }
            }
            limit++;
            if (limit > configBlockProcessingLimit) {
                plugin.getLogger().log(Level.INFO, "Solid Blocks connections search reached BlockProcessingLimit.");
                break;
            }
            if (findNext) {
                search.clear();
                search.putAll(newSearch);
                newSearch.clear();
            }

        }

        // detecting distance to ground
        int fallingDistance;
        for (fallingDistance = 1; fallingDistance < 50; fallingDistance++) {
            Block newBlock = world.getBlockAt(breakBlockLocation.getBlockX(), breakBlockLocation.getBlockY() - fallingDistance, breakBlockLocation.getBlockZ());
            Material newBlockType = newBlock.getType();
            if (!isLightBlock(newBlockType))
                break;
        }

        HashMap<Location, Integer> clearWay = new HashMap<>();

        // falling tree
        for (Map.Entry<Location, Block> logPairs : tree.entrySet()) {
            Location newBlockLocation = logPairs.getKey();
            // if (newBlockLocation == breakBlockLocation) continue;
            Block logBlock = logPairs.getValue();
            Material logBlockType = logBlock.getType();
            // byte logBlockData = logBlock.getData();
            MaterialData logBlockData = logBlock.getState().getData();
            logBlock.setType(Material.AIR);
            logBlock.removeMetadata("TreeId", plugin);

            int horisontalDistance = newBlockLocation.getBlockY() - breakBlockLocation.getBlockY() - 1;
            if (horisontalDistance < 0)
                horisontalDistance = 0;
            int verticalDistance = horisontalDistance + fallingDistance;
            // int horisontalOffset=0;
            int horisontalOffset = (int) Math.floor((horisontalDistance) / 1.5);
            float horisontalSpeed = calcSpeed(horisontalDistance, verticalDistance, horisontalOffset);
            // player.sendMessage("horisontalDistance: "+ horisontalDistance +
            // " verticalDistance:" + verticalDistance + " fallingdistance " +
            // fallingDistance);
            if (fallingDistance == 1) {
                switch (horisontalDistance) {
                    case 1:
                        horisontalOffset = 1;
                        horisontalSpeed = 0;
                        break;
                    case 2:
                        horisontalOffset = 1;
                        horisontalSpeed = 0.1191f;
                        break;
                    case 3:
                        horisontalOffset = 1;
                        horisontalSpeed = 0.185f;
                        break;
                    case 4:
                        horisontalOffset = 2;
                        horisontalSpeed = 0.17f;
                        break;
                    case 5:
                        horisontalOffset = 2;
                        horisontalSpeed = 0.22f;
                        break;
                    case 6:
                        horisontalOffset = 3;
                        horisontalSpeed = 0.21f;
                        break;
                    case 7:
                        horisontalOffset = 3;
                        horisontalSpeed = 0.26f;
                        break;
                    case 8:
                        horisontalOffset = 4;
                        horisontalSpeed = 0.241f;
                        break;
                    case 9:
                        horisontalOffset = 4;
                        horisontalSpeed = 0.28f;
                        break;

                }
            }
            if (fallingDistance == 2) {
                switch (horisontalDistance) {
                    case 1:
                        horisontalOffset = 1;
                        horisontalSpeed = 0;
                        break;
                    case 2:
                        horisontalOffset = 1;
                        horisontalSpeed = 0.1f;
                        break;
                    case 5:
                        horisontalOffset = 2;
                        horisontalSpeed = 0.2f;
                        break;

                }
            }
            Vector vOffset = direction.clone().multiply(horisontalOffset);
            newBlockLocation.add(vOffset);
            Block testBlock = world.getBlockAt(newBlockLocation);
            if (isLightBlock(testBlock.getType())) {
                testBlock.breakNaturally();
            } else {
                newBlockLocation.subtract(vOffset);
                horisontalSpeed = calcSpeed(horisontalDistance, verticalDistance, 0);
            }
            byte face;
            if (Math.abs(direction.getZ()) > Math.abs(direction.getX())) {
                face = 0x8;
            } else {
                face = 0x4;
            }
            @SuppressWarnings("deprecation")
            FallingBlock blockFalling = world.spawnFallingBlock(newBlockLocation, logBlockType, (byte) ((3 & logBlockData.getData()) | face)); //TODO: Find a way to do this without using deprecated methods
            blockFalling.setVelocity(direction.clone().multiply(horisontalSpeed));

            // calc clear falling way
            int minClearVertical = newBlockLocation.getBlockY() - verticalDistance;
            for (int clearY = newBlockLocation.getBlockY(); clearY >= minClearVertical; clearY--) {
                int horisontalClearDistance = (int) Math.ceil(Math.sqrt(horisontalDistance * horisontalDistance - (clearY - minClearVertical) * (clearY - minClearVertical)));
                Location l = new Location(world, newBlockLocation.getBlockX(), clearY, newBlockLocation.getBlockZ());
                if (clearWay.containsKey(l)) {
                    if (clearWay.get(l) < horisontalClearDistance) {
                        clearWay.put(l, horisontalClearDistance);
                    }
                } else {
                    clearWay.put(l, horisontalClearDistance);
                }
            }
        }

        // clear falling way
        for (Map.Entry<Location, Integer> clearPairs : clearWay.entrySet()) {
            Location clearBlockLocation = clearPairs.getKey();
            int clearDistance = clearPairs.getValue();
            // player.sendMessage("clearDistance: "+ clearDistance + " y:" +
            // clearBlockLocation.getY());
            for (int c = 0; c <= clearDistance; c++) {
                Location tempClearLoc = clearBlockLocation.clone().add(direction.clone().multiply(c));
                Block clearBlock = world.getBlockAt(tempClearLoc);
                if (isLightBlock(clearBlock.getType())) {
                    clearBlock.breakNaturally();
                    clearBlock.removeMetadata("TreeId", plugin);
                }
            }
        }

        if (!configFallingLeaves)
            return;

        // get blocks around tree to find leaves
        HashMap<Location, Block> leaves = new HashMap<>();
        for (Map.Entry<Location, Block> logPairs : tree.entrySet()) {
            leaves.putAll(getNearBlocks(logPairs.getKey(), 3));
        }

        if (tree.size() == 0) {
            if (breakBlockType == Material.LOG) {
                leaves.putAll(getNearBlocks(breakBlockLocation, 3));
            } else {
                for (int i = 1; i <= 5; i++) {
                    Location tempLocation = breakBlockLocation.clone().add(0, i, 0);
                    leaves.put(tempLocation, world.getBlockAt(tempLocation));
                }
            }
        }

        leaves.remove(breakBlockLocation);

        // falling leaves
        for (Map.Entry<Location, Block> leavesPairs : leaves.entrySet()) {
            Location leavesLocation = leavesPairs.getKey();
            Block leavesBlock = leavesPairs.getValue();
            Material leavesMaterial = leavesBlock.getType();
            // byte leavesBlockData = leavesBlock.getData();
            MaterialData leavesBlockData = leavesBlock.getState().getData();
            if (leavesMaterial != Material.LEAVES)
                continue;

            if (treeId == 0 && !leavesBlock.getMetadata("TreeId").isEmpty())
                continue;
            // if (treeId !=0 && treeId !=1) {
            // if (!leavesBlock.getMetadata("TreeId").iterator().hasNext())
            // continue;
            // if
            // (leavesBlock.getMetadata("TreeId").iterator().next().asInt()!=treeId)
            // continue;
            // }
            float horisontalSpeed;
            int horisontalDistance = leavesLocation.getBlockY() - breakBlockLocation.getBlockY() - 1;
            if (horisontalDistance < 0)
                horisontalDistance = 0;
            int verticalDistance = horisontalDistance + fallingDistance;
            horisontalSpeed = calcSpeed(horisontalDistance, verticalDistance, 0);

            // if (verticalDistance==0 ) continue;
            if (tree.size() < 2)
                horisontalSpeed = 0;
            if (horisontalSpeed == 0) {
                Location tempLocation = leavesLocation.clone().add(0, -1, 0);
                if (tempLocation.getBlockX() != breakBlockLocation.getBlockX() || tempLocation.getBlockY() != breakBlockLocation.getBlockY() || tempLocation.getBlockZ() != breakBlockLocation.getBlockZ()) {
                    Block tempBlock = world.getBlockAt(tempLocation);
                    if (tempBlock.getType() != Material.AIR && tempBlock.getType() != Material.WATER && tempBlock.getType() != Material.STATIONARY_WATER) {
                        leavesBlock.removeMetadata("TreeId", plugin);
                        //leavesBlock.setData((byte) ((0x3 & leavesBlockData) | 0x8));
                        // player.sendMessage("Ignore!");
                        continue;
                    }
                }
                // player.sendMessage(tempLocation + " " + breakBlockLocation);
            }
            leavesBlock.setType(Material.AIR);
            leavesBlock.removeMetadata("TreeId", plugin);
            @SuppressWarnings("deprecation")
            FallingBlock blockFalling = world.spawnFallingBlock(leavesLocation, Material.LEAVES, (byte) ((0x3 & leavesBlockData.getData()) | 0x8)); //TODO: Find a way to do this without using deprecated methods
            // FallingBlock blockFalling =
            // world.spawnFallingBlock(leavesLocation, Material.LEAVES,
            // (byte)(leavesBlockData | 0x4));
            blockFalling.setVelocity(direction.clone().multiply(horisontalSpeed));
        }

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block breakBlock = event.getBlock();
        Material breakBlockType = breakBlock.getType();
        if (!(configDetectBlockBreakAll || breakBlockType == Material.LOG || (configDetectBlockBreakLeaves && breakBlockType == Material.LEAVES)))
            return;
        Player player = event.getPlayer();
        BlockBreak(breakBlock, player.getLocation());
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        Block breakBlock = event.getBlock();
        if (!configDetectBlockBreakAll && !configDetectBlockBreakLeaves) {
            return;
        }
        BlockBreak(breakBlock, breakBlock.getLocation().clone().add(1, 0, 0));
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
/*
 * @EventHandler public void onBlockDamage(BlockDamageEvent event){ Block
 * block=event.getBlock(); Player player = event.getPlayer();
 * //player.sendMessage("BlockType : " + block.getType()); //if
 * (block.getMetadata("TreeId").size()>0) //player.sendMessage("TreeId: " +
 * block.getMetadata("TreeId").get(0).asInt()); //player.sendMessage("data : " +
 * block.getData()); //player.sendMessage("typeId : " +
 * event.getItemInHand().getTypeId()); if
 * (event.getItemInHand().getTypeId()==270) { if (block.getType() ==
 * Material.LOG) { block.breakNaturally(); // setTypeId(0); } }
 * 
 * if (event.getItemInHand().getTypeId()==272) { Location breakBlockLocation =
 * block.getLocation(); byte data=0; Block
 * b=world.getBlockAt((int)breakBlockLocation.getX(),
 * (int)breakBlockLocation.getY()+1, (int)breakBlockLocation.getZ());
 * b.setTypeIdAndData(3, data, true); for (int y = 2 ; y <= 20; y++) { Block
 * newBlock=world.getBlockAt((int)breakBlockLocation.getX(),
 * (int)breakBlockLocation.getY()+y, (int)breakBlockLocation.getZ());
 * newBlock.setTypeIdAndData(17, data, true); //data++; //if (data>4) data=0; }
 * } BlockState state=block.getState(); int rawData=state.getRawData(); int
 * typeId=block.getTypeId(); //event.setCancelled(true); Player player =
 * event.getPlayer(); Location playerLoc = player.getLocation(); int x =
 * (int)playerLoc.getX(); int y = (int)playerLoc.getY(); int z =
 * (int)playerLoc.getZ(); player.sendMessage("Damage block : " + x + " , " + y +
 * " , " + z); player.sendMessage("typeId : " + typeId);
 * player.sendMessage("rawData : " + rawData); List<MetadataValue>
 * meta=block.getMetadata("vovic"); player.sendMessage("metasize : " +
 * meta.size()); for (int i = 0; i < meta.size(); i++) {
 * player.sendMessage("metaData : " + meta.get(i).asString());
 * //state.setRawData(); //meta. }
 */

/*
 * @EventHandler public void onPlayerMove(PlayerMoveEvent event){ Player player
 * = event.getPlayer(); Location playerLoc = player.getLocation(); int x =
 * (int)playerLoc.getX(); int y = (int)playerLoc.getY(); int z =
 * (int)playerLoc.getZ(); if (x != old_x || y != old_y || z != old_z) {
 * player.sendMessage("Your Coordinates : " + x + " , " + y + " , " + z); old_x
 * = x; old_y = y; old_z = z; } }
 */

