package symbolics.division.spirit_vector.logic.move;

import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import symbolics.division.spirit_vector.SpiritVectorMod;
import symbolics.division.spirit_vector.SpiritVectorSounds;
import symbolics.division.spirit_vector.logic.TravelMovementContext;
import symbolics.division.spirit_vector.logic.ability.WildMagicAbility;
import symbolics.division.spirit_vector.logic.input.Arrow;
import symbolics.division.spirit_vector.logic.input.ArrowManager;
import symbolics.division.spirit_vector.logic.input.Input;
import symbolics.division.spirit_vector.logic.input.InputManager;
import symbolics.division.spirit_vector.logic.spell.Spell;
import symbolics.division.spirit_vector.logic.spell.SpellDimension;
import symbolics.division.spirit_vector.logic.spell.SpellFXEvents;
import symbolics.division.spirit_vector.logic.spell.StochasticSpell;
import symbolics.division.spirit_vector.logic.state.ManagedState;
import symbolics.division.spirit_vector.logic.vector.SpiritVector;
import symbolics.division.spirit_vector.sfx.ServerConfig;
import symbolics.division.spirit_vector.sfx.SpiritVectorSFX;

import java.util.ArrayList;
import java.util.List;

public class SpellMovement extends NeutralMovement {
	private static final Identifier CASTING_STATE_ID = SpiritVectorMod.id("casting_spell");
	private static final int MAX_CASTING_TICKS = 20 * 5;
	ServerConfig config = AutoConfig.getConfigHolder(ServerConfig.class).getConfig();

	public SpellMovement(Identifier id) {
		super(id);
	}

	@Override
	public void configure(SpiritVector sv) {
		sv.stateManager().register(CASTING_STATE_ID, new SpellcastingState(sv));
	}

	@Override
	public boolean testMovementCondition(SpiritVector sv, TravelMovementContext ctx) {
		if (!ServerConfig.enableSpellDimension()) return false;
		InputManager input = sv.inputManager();
		if (
			!sv.user.getWorld().spellDimension().isCasting() &&
				!sv.user.isOnGround() &&
				sv.getMomentum() >= SpiritVector.MAX_MOMENTUM * 0.9 &&
				input.pressed(Input.CROUCH) &&
				input.pressed(Input.SPRINT) &&
				input.pressed(Input.JUMP)
		) {
			sv.setMomentum(0);
			input.consume(Input.CROUCH);
			input.consume(Input.SPRINT);
			input.consume(Input.JUMP);
			((SpellcastingState) sv.stateManager().getState(CASTING_STATE_ID)).resetInputs();
			sv.arrowManager().consumeAll();
			sv.stateManager().enableStateFor(CASTING_STATE_ID, MAX_CASTING_TICKS);
			SpellFXEvents.activateRuneMatrix();
			return true;
		}

		SpellDimension sd = sv.user.getWorld().spellDimension();
		boolean casting = sd.isCasting();

		// cancel spells
		// cancel early
		if (!sv.user.isOnGround() &&
			sv.user.getWorld().spellDimension().isCasting() &&
			sv.inputManager().pressed(Input.CROUCH) &&
			sv.inputManager().pressed(Input.SPRINT) &&
			sv.inputManager().pressed(Input.JUMP)) {
			sv.inputManager().consume(Input.CROUCH);
			sv.inputManager().consume(Input.SPRINT);
			sv.inputManager().consume(Input.JUMP);
			sv.user.getWorld().spellDimension().cancel();
			sv.user.playSound(SpiritVectorSounds.RUNE_MATRIX_START);
		}

		return false;
	}

	@Override
	public boolean testMovementCompleted(SpiritVector sv, TravelMovementContext ctx) {
		return !sv.stateManager().isActive(CASTING_STATE_ID);
	}

	@Override
	public void exit(SpiritVector sv, TravelMovementContext ctx) {
		SpellFXEvents.openSpellDimension();
		sv.user.setVelocity(
			MovementUtils.augmentedInput(sv, ctx).multiply(0.6).add(0, 0.5, 0)
		);
		((SpellcastingState) sv.stateManager().getState(CASTING_STATE_ID)).resetInputs();
	}

	@Override
	public void travel(SpiritVector sv, TravelMovementContext ctx) {
		if (sv.inputManager().consume(Input.JUMP)) {
			List<Arrow> eigenCode = ((SpellcastingState) sv.stateManager().getState(CASTING_STATE_ID)).eigenCode();
			if (eigenCode.isEmpty() && WildMagicAbility.xyzzy(sv)) {
				sv.user.getWorld().spellDimension().cast(new StochasticSpell(sv));
			} else {
				sv.user.getWorld().spellDimension().cast(new Spell(sv, ((SpellcastingState) sv.stateManager().getState(CASTING_STATE_ID)).eigenCode()));
			}
			sv.stateManager().clearTicks(CASTING_STATE_ID);
		}
		SlideMovement.travelWithInput(sv, Vec3d.ZERO);

		World world = sv.user.getWorld();

		int n = sv.user.getRandom().nextBetween(5, 10);
		Vec3d vel = sv.user.getVelocity().normalize();
		for (int i = 0; i < n; i++) {
			Vec3d pv = vel.multiply(sv.user.getRandom().nextDouble());
			int sign = i % 2 == 0 ? 1 : -1;
//			Vec3d off = vel.multiply(sign).multiply(0.5 + sv.user.getRandom().nextFloat() * 2).add(sv.user.getPos());
//			world.addParticle((ParticleEffect) SpiritVectorSFX.Particles.SPELL_CASTING, off.x, off.y, off.z, pv.x * sign, 0, pv.z * sign);
			Vec3d off = sv.user.getPos(); //.add(vel.multiply(sign * (0.5f + sv.user.getRandom().nextFloat() * 20)));
			world.addParticle((ParticleEffect) SpiritVectorSFX.Particles.SPELL_CASTING, off.x, off.y, off.z, pv.x * sign, 0, pv.z * sign);
		}

		ctx.ci().cancel();
	}

	public static List<Arrow> getCurrentEigenCode(SpiritVector sv) {
		return List.copyOf(((SpellcastingState) sv.stateManager().getState(CASTING_STATE_ID)).eigenCode());
	}

	private static class SpellcastingState extends ManagedState {
		private final List<Arrow> eigenCode = new ArrayList<>();


		public SpellcastingState(SpiritVector sv) {
			super(sv);
		}

		public void resetInputs() {
			eigenCode.clear();
		}

		@Override
		public void tick() {
			super.tick();
			if (isActive() && eigenCode.size() < Spell.MAX_CODE_LENGTH) {
				ArrowManager arrows = sv.arrowManager();
				if (arrows.consume(Arrow.UP)) {
					eigenCode.add(Arrow.UP);
				} else if (arrows.consume(Arrow.DOWN)) {
					eigenCode.add(Arrow.DOWN);
				} else if (arrows.consume(Arrow.LEFT)) {
					eigenCode.add(Arrow.LEFT);
				} else if (arrows.consume(Arrow.RIGHT)) {
					eigenCode.add(Arrow.RIGHT);
				} else {
					return;
				}
				SpellFXEvents.arrowInput();
			}
		}

		public List<Arrow> eigenCode() {
			return List.copyOf(eigenCode);
		}
	}
}
