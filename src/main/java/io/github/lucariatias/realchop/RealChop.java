package io.github.lucariatias.realchop;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import java.lang.String;
import java.io.File;

public class RealChop extends JavaPlugin {
	public FileConfiguration config;

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
		getServer().getPluginManager().registerEvents(new RealChopListener(this), this);
	}

}
