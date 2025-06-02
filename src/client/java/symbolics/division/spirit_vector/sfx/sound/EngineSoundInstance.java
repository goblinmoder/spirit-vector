package symbolics.division.spirit_vector.sfx.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import symbolics.division.spirit_vector.ClientConfig;
import symbolics.division.spirit_vector.SpiritVectorSounds;
import symbolics.division.spirit_vector.logic.ISpiritVectorUser;
import symbolics.division.spirit_vector.logic.vector.SpiritVector;

public class EngineSoundInstance extends MovingSoundInstance {

    public static boolean shouldPlayFor(PlayerEntity player) {
        return !player.isRemoved()
                && SpiritVector.hasEquipped(player)
			&& !player.isInFluid()
			&& (player != MinecraftClient.getInstance().player || ClientConfig.playSound());
    }

    private static final float VOLUME_RELATIVE = 0.25f;
	private Vec3d prevPos = Vec3d.ZERO;
    private final PlayerEntity player;

    public EngineSoundInstance(PlayerEntity player) {
		super(SpiritVectorSounds.ENGINE, SoundCategory.PLAYERS, player.getRandom());
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
		this.volume = 0.6f;
        this.pitch = 0.8f;
		this.prevPos = player.getPos();
    }

    @Override
    public void tick() {
		Vec3d pos = this.player.getPos();
		float speed = 0;
		if (MinecraftClient.getInstance().player == player) {
			speed = (float) this.player.getVelocity().length();
		} else {
			speed = Math.min(Math.max(MathHelper.sqrt((float) pos.subtract(prevPos).length() * 20), 0.01f), 0.4f);
		}
		this.prevPos = pos;
        if (shouldPlayFor(this.player)) {
			this.volume = speed / 0.5f * VOLUME_RELATIVE;
            if (player instanceof ISpiritVectorUser user) {
                user.getSpiritVector().ifPresent(
                        sv -> this.pitch = ((float)sv.getMomentum() / (float)SpiritVector.MAX_MOMENTUM * 0.5f) + 0.5f + (player.getRandom().nextFloat() * 0.2f - 0.1f)
                );
            }
            this.x = this.player.getX();
            this.y = this.player.getY();
            this.z = this.player.getZ();
        } else {
            this.setDone();
        }
    }
}
