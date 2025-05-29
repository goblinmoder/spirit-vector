package symbolics.division.spirit_vector.logic.move;

import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import symbolics.division.spirit_vector.SpiritVectorMod;
import symbolics.division.spirit_vector.logic.TravelMovementContext;
import symbolics.division.spirit_vector.logic.input.Input;
import symbolics.division.spirit_vector.logic.state.ManagedState;
import symbolics.division.spirit_vector.logic.vector.SpiritVector;
import symbolics.division.spirit_vector.logic.vector.VectorType;

import java.util.ArrayList;
import java.util.List;

public class WallJumpMovement extends AbstractMovementType {
	protected static final int MOMENTUM_GAINED = SpiritVector.MAX_MOMENTUM / 20;
	protected static final float AXIS_ALIGN_THRESHOLD = -(float) Math.cos(Math.PI / 4 - 0.01); // must be < cos(pi/4) or we can't consistently choose an inverted
	protected static final Identifier WALL_JUMP_PLANE_TRACKER = SpiritVectorMod.id("wall_jump_plane_tracker");

	public static class WallJumpPlaneTracker extends ManagedState {
		public List<Pair<Direction, Integer>> prevPlanes = new ArrayList<>();

		public WallJumpPlaneTracker(SpiritVector sv) {
			super(sv);
		}

		public boolean allowable(Direction dir, Vec3d pos) {
			for (Pair<Direction, Integer> plane : prevPlanes) {
				if(dir == plane.getLeft() && (int) pos.getComponentAlongAxis(dir.getAxis()) == plane.getRight()) {
					return false;
				}
			}
			return true;
		}

		public void set(List<Direction> directions){
			this.clear();
			for (Direction dir : directions) {
				this.prevPlanes.add(new Pair<>(dir, (int) sv.user.getPos().getComponentAlongAxis(dir.getAxis())));
			}
		}

		public void clear() {
			this.prevPlanes.clear();
		}
	}

	public static void resetWallJumpPlane(SpiritVector sv) {
		((WallJumpPlaneTracker) sv.stateManager().getState(WALL_JUMP_PLANE_TRACKER)).clear();
	}

	public static List<Direction> validWallJumpDirections(World world, Vec3d pos){
		List<Direction> validDirections = new ArrayList<>();
		for (Direction dir : Direction.values()){
			if(
					   dir != Direction.UP
					&& dir != Direction.DOWN
					&& MovementUtils.validWallJumpAnchor(world, pos, dir)
			){
				validDirections.add(dir);
			}
		}
		return validDirections;
	}

	public static List<Direction> allowableDirections(World world, Vec3d pos, WallJumpPlaneTracker planeState){
		List<Direction> validDirections = validWallJumpDirections(world, pos);
		List<Direction> allowableDirections = new ArrayList<>();
		for (Direction dir : validDirections){
			if(planeState.allowable(dir, pos)) allowableDirections.add(dir);
		}
		return allowableDirections;
	}

	// convert context to input used for wall jumps
	// normal if normally valid, and
	// orthogonal (to wall) otherwise.
	protected static Pair<Vec3d, List<Direction>> getWalljumpingInput(SpiritVector sv, TravelMovementContext ctx) {
		Vec3d input = MovementUtils.augmentedInput(sv, ctx);
		Vector3f inputV3f = input.toVector3f();
		Vec3d pos = sv.user.getPos().add(0, 0.5, 0);

		World world = sv.user.getWorld();
		WallJumpPlaneTracker planeState = (WallJumpPlaneTracker) sv.stateManager().getState(WALL_JUMP_PLANE_TRACKER);

		List<Direction> validDirections = validWallJumpDirections(world, pos);
		List<Direction> allowableDirections = allowableDirections(world, pos, planeState);

		if (allowableDirections.isEmpty()) return null; // no valid wall jump surface

		Vector3f normal = new Vector3f();
		for (Direction dir : allowableDirections) {
			normal.add(dir.getOpposite().getUnitVector().div(allowableDirections.size()));
		}
		float dp = normal.dot(inputV3f);
		Vec3d invertedInput = new Vec3d(normal);

		if (allowableDirections.size() > 1) return new Pair<>(invertedInput, validDirections); // jump from corner
		if (dp > 0) return new Pair<>(input, validDirections); // jump away
		else if (dp < AXIS_ALIGN_THRESHOLD) return new Pair<>(invertedInput, validDirections); // jump opposite

		// jumping along wall
		Direction jumpDirection = Direction.getFacing(input);
		Direction right = jumpDirection.rotateYClockwise();
		Direction left = jumpDirection.rotateYCounterclockwise();
		boolean hasRight = planeState.allowable(right, pos) && MovementUtils.validWallJumpAnchor(world, pos, right);
		boolean hasLeft = planeState.allowable(left, pos) && MovementUtils.validWallJumpAnchor(world, pos, left);
		Direction result = hasLeft && hasRight && planeState.allowable(jumpDirection, pos) ? jumpDirection // on either side, pretend a wall behind us
			: hasLeft ? left // or else jumping along
			: hasRight ? right
			: null;
		if (result != null) {
			return new Pair<>(new Vec3d(jumpDirection.getUnitVector()), validDirections);
		}
		return null;
	}

	public WallJumpMovement(Identifier id) {
		super(id);
	}

	@Override
	public void configure(SpiritVector sv) {
		sv.stateManager().register(WALL_JUMP_PLANE_TRACKER, new WallJumpPlaneTracker(sv));
	}

	@Override
	public boolean testMovementCondition(SpiritVector sv, TravelMovementContext ctx) {
		return !sv.user.isOnGround()
			&& MovementUtils.idealWalljumpingConditions(sv, ctx)
			&& sv.inputManager().consume(Input.JUMP);
	}

	@Override
	public void travel(SpiritVector sv, TravelMovementContext ctx) {
		var result = getWalljumpingInput(sv, ctx);
		if (result == null) { // input invalid
			NEUTRAL.travel(sv, ctx);
			return;
		}

		List<Direction> dirs = result.getRight();
		Vec3d input = result.getLeft();

		Vec3d motion = new Vec3d(input.x / 2, 0.5, input.z / 2);
		if (sv.getMoveState() == this) { // only apply for normal walljumps
			motion = motion.multiply(sv.consumeSpeedMultiplier());
		}

		if (sv.is(VectorType.DREAM)) {
			sv.user.addVelocity(motion);
		} else {
			sv.user.setVelocity(motion);
		}

		((WallJumpPlaneTracker) sv.stateManager().getState(WALL_JUMP_PLANE_TRACKER)).set(dirs);
		sv.effectsManager().spawnRing(sv.user.getPos(), motion);
	}

	@Override
	public void updateValues(SpiritVector sv) {
		if (!sv.is(VectorType.DREAM)) {
			sv.modifyMomentum(MOMENTUM_GAINED);
			sv.stateManager().enableStateFor(SpiritVector.MOMENTUM_DECAY_GRACE_STATE, 20);
		}
	}
}
