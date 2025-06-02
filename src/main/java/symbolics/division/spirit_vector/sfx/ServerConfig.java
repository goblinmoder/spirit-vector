package symbolics.division.spirit_vector.sfx;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import symbolics.division.spirit_vector.SpiritVectorMod;

@Config(name = SpiritVectorMod.MODID + "_server")
public class ServerConfig implements ConfigData {
	boolean enableSpellDimension = true;

	public static boolean enableSpellDimension() {
		return AutoConfig.getConfigHolder(ServerConfig.class).getConfig().enableSpellDimension;
	}
}
