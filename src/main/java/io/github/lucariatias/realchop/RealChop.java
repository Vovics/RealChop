package io.github.lucariatias.realchop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.util.Vector;

import java.lang.String;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class RealChop extends JavaPlugin {
    public FileConfiguration config;
    private boolean detectBlockBreakLeaves;
    private boolean detectBlockBreakAll;
    private int blockProcessingLimit;
    private boolean fallingLeaves;

    @Override
    public void onEnable() {
        config = this.getConfig();
        config.options().configuration().addDefault("DetectBlockBreak", "all");
        config.options().configuration().addDefault("BlockProcessingLimit", 150);
        config.options().configuration().addDefault("FallingLeaves", true);
        File dir = this.getDataFolder();
        File file = new File(dir, "config.yml");
        if (!file.exists()) {
            // creating new configuration
            FileConfiguration config = this.getConfig();
            String eol = System.getProperty("line.separator");
            config.options().copyDefaults(true);
            String header;
            header = "RealChop Properties: " + eol;
            header += eol;
            header += "DetectBlockBreak log | leaves | all" + eol;
            header += "  Tree will fall (after physics check), when player break this type of blocks:" + eol;
            header += "  log - only LOG blocks (tree parts)" + eol;
            header += "  leaves - LOG and LEAVES blocks" + eol;
            header += "  all - any blocks" + eol;
            header += "  default: all" + eol;
            header += eol;
            header += "BlockProcessingLimit num " + eol;
            header += "  num (5 - 1000) - setting limit of blocks for physics calculation." + eol;
            header += "  Setting high limit can produce heavy server load, when process massive structures, builded from LOG blocks." + eol;
            header += "  default: 150" + eol;
            header += eol;
            header += "FallingLeaves true | false" + eol;
            header += "  Process LEAVES blocks, or just LOG blocks." + eol;
            header += "  default: false" + eol;
            config.options().header(header);
            saveConfig();
        }
        detectBlockBreakLeaves = false;
        detectBlockBreakAll = false;

        String DetectBlockBreak = getConfig().getString("DetectBlockBreak");
        if (DetectBlockBreak.equalsIgnoreCase("leaves")) {
            detectBlockBreakLeaves = true;
        }
        if (DetectBlockBreak.equalsIgnoreCase("all")) {
            detectBlockBreakLeaves = true;
            detectBlockBreakAll = true;
        }

        blockProcessingLimit = getConfig().getInt("BlockProcessingLimit");
        if (blockProcessingLimit < 5 || blockProcessingLimit > 1000)
            blockProcessingLimit = 150;
        fallingLeaves = getConfig().getBoolean("FallingLeaves");
        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new BlockBreakListener(this), this);
        pluginManager.registerEvents(new LeavesDecayListener(this), this);
        pluginManager.registerEvents(new StructureGrowListener(this), this);
    }

    public void blockBreak(Block breakBlock, Location playerLocation) {
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
                Map<Location, Block> near = getNearBlocks(l, 1);
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
            if (limit > blockProcessingLimit) {
                this.getLogger().log(Level.INFO, "Tree Logs search reached BlockProcessingLimit.");
                break;
            }
            if (findNext) {
                search.clear();
                search.putAll(newSearch);
            }

        }
        tree.remove(breakBlockLocation);
        solid.remove(breakBlockLocation);
        breakBlock.removeMetadata("TreeId", this);
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
                Map<Location, Block> near = getNearBlocks(l, 1);
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
            if (limit > blockProcessingLimit) {
                this.getLogger().log(Level.INFO, "Solid Blocks connections search reached BlockProcessingLimit.");
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
            logBlock.removeMetadata("TreeId", this);

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
                    clearBlock.removeMetadata("TreeId", this);
                }
            }
        }

        if (!fallingLeaves)
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
                        leavesBlock.removeMetadata("TreeId", this);
                        //leavesBlock.setData((byte) ((0x3 & leavesBlockData) | 0x8));
                        // player.sendMessage("Ignore!");
                        continue;
                    }
                }
                // player.sendMessage(tempLocation + " " + breakBlockLocation);
            }
            leavesBlock.setType(Material.AIR);
            leavesBlock.removeMetadata("TreeId", this);
            @SuppressWarnings("deprecation")
            FallingBlock blockFalling = world.spawnFallingBlock(leavesLocation, Material.LEAVES, (byte) ((0x3 & leavesBlockData.getData()) | 0x8)); //TODO: Find a way to do this without using deprecated methods
            // FallingBlock blockFalling =
            // world.spawnFallingBlock(leavesLocation, Material.LEAVES,
            // (byte)(leavesBlockData | 0x4));
            blockFalling.setVelocity(direction.clone().multiply(horisontalSpeed));
        }
    }

    private Map<Location, Block> getNearBlocks(Location l, int radius) {
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

    private boolean isLightBlock(Material m) {
        return m == Material.LEAVES || m == Material.AIR || m == Material.TORCH || m == Material.LONG_GRASS || m == Material.RED_MUSHROOM || m == Material.YELLOW_FLOWER || m == Material.VINE || m == Material.SNOW || m == Material.ARROW || m == Material.COCOA || m == Material.LADDER || m == Material.WEB || m == Material.SAPLING || m == Material.WATER || m == Material.STATIONARY_WATER;
    }

    public boolean isDetectBlockBreakLeaves() {
        return detectBlockBreakLeaves;
    }

    public boolean isDetectBlockBreakAll() {
        return detectBlockBreakAll;
    }

}
