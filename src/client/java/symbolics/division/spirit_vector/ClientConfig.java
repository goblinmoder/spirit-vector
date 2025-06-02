package symbolics.division.spirit_vector;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = SpiritVectorMod.MODID + "_client")
public class ClientConfig implements ConfigData {
	boolean playSound = true;

	public static boolean playSound() {
		return AutoConfig.getConfigHolder(ClientConfig.class).getConfig().playSound;
	}

	public static void setPlaySound(boolean v) {
		ClientConfig c = AutoConfig.getConfigHolder(ClientConfig.class).getConfig();
		c.playSound = v;
		AutoConfig.getConfigHolder(ClientConfig.class).save();
	}
}
