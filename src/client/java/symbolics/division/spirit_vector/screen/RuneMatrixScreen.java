package symbolics.division.spirit_vector.screen;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;
import symbolics.division.spirit_vector.ClientConfig;
import symbolics.division.spirit_vector.SpiritVectorMod;
import symbolics.division.spirit_vector.SpiritVectorSounds;
import symbolics.division.spirit_vector.logic.vector.VectorType;
import symbolics.division.spirit_vector.render.SpiritVectorHUD;

import java.util.Objects;

public class RuneMatrixScreen extends HandledScreen<RuneMatrixScreenHandler> {
	public static final Identifier TEXTURE = SpiritVectorMod.id("textures/gui/container/rune_matrix.png");
	public static final Identifier INVENTORY_TEXTURE = SpiritVectorMod.id("textures/gui/container/basic_inventory.png");

	protected static final int MODE_SLOT_LEFT_OFFSET = 56 + 25;
	protected static final int MODE_SLOT_TOP_OFFSET = 7 + 50;
	protected Identifier modeSprite = SpiritVectorMod.id("rune_matrix/vector_mode_spirit");
	protected int prevMode = 0;
	protected int ticksSinceModeChanged = 0;

	private static final int SLOT_FULL_SIZE = 28;
	private static final Identifier SLOT_FULL_TEXTURE = SpiritVectorMod.id("rune_matrix/slot_full");
	private static final Identifier CORE_EMPTY_TEXTURE = SpiritVectorMod.id("rune_matrix/socket_empty");
	private static final Identifier VOLUME_OFF = SpiritVectorMod.id("rune_matrix/volume_off");
	private static final Identifier VOLUME_ON = SpiritVectorMod.id("rune_matrix/volume_on");

	public RuneMatrixScreen(RuneMatrixScreenHandler handler, PlayerInventory inventory, Text title) {
		super(handler, inventory, title);
		modeSprite = getModeSprite();
		prevMode = this.handler.getVectorMode();
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		this.drawMouseoverTooltip(context, mouseX, mouseY);
	}

	@Override
	protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
		context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY + 44, 4210752, false);
		ticksSinceModeChanged--;
		if (ticksSinceModeChanged > 0 && ticksSinceModeChanged < 59) {
			int alpha = (int) (0xff * ((float) Math.min(ticksSinceModeChanged, 40) / 40));
			if (alpha <= 0.01) return;
			int color = ColorHelper.Argb.getArgb(alpha, 0xff, 0xff, 0xff);
			context.drawText(this.textRenderer, Objects.requireNonNull(VectorType.REGISTRY.get(this.handler.getVectorMode())).getDisplayName(), this.playerInventoryTitleX, this.playerInventoryTitleY + 28, color, false);
		}

		Identifier playSoundSprite = ClientConfig.playSound() ? VOLUME_ON : VOLUME_OFF;
		context.drawGuiTexture(playSoundSprite, this.playerInventoryTitleX + 145, this.playerInventoryTitleY + 39, 16, 16);
	}

	@Override
	protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
		context.drawTexture(TEXTURE, this.x + 29, this.y - 20, 0, 0, this.backgroundWidth, this.backgroundHeight);
		context.drawTexture(INVENTORY_TEXTURE, this.x, this.y + 110, 0, 0, this.backgroundWidth, this.backgroundHeight);

		if (prevMode != this.handler.getVectorMode()) {
			prevMode = this.handler.getVectorMode();
			this.modeSprite = getModeSprite();
		}
		context.drawGuiTexture(this.modeSprite, this.x + MODE_SLOT_LEFT_OFFSET, this.y + MODE_SLOT_TOP_OFFSET, 16, 16);

		renderSlotInset(context, this.handler.leftSlot, 0, 0);
		renderSlotInset(context, this.handler.upSlot, 25, -25);
		renderSlotInset(context, this.handler.rightSlot, 50, 0);
		if (!this.handler.coreSlot.hasStack()) {
			context.drawGuiTexture(CORE_EMPTY_TEXTURE, this.x + 50 + 29, this.y + 26 + 4, 20, 20);
		}
	}

	private void renderSlotInset(DrawContext context, Slot slot, int x, int y) {
		if (!slot.hasStack()) return;
		context.drawGuiTexture(SLOT_FULL_TEXTURE, this.x + 50 + x, this.y + 26 + y, SLOT_FULL_SIZE, SLOT_FULL_SIZE);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int x1 = MODE_SLOT_LEFT_OFFSET + this.x;
		int x2 = x1 + 16;
		int y1 = MODE_SLOT_TOP_OFFSET + this.y;
		int y2 = y1 + 16;
//		System.out.println("mx" + (mouseX - this.x));
//		System.out.println("my" + (mouseY - this.y));
		if (mouseX >= x1 && mouseX <= x2 && mouseY >= y1 && mouseY <= y2) {
			SpiritVectorHUD.playUISound(SpiritVectorSounds.RUNE_MATRIX_BUZZ, 1);
			this.client.interactionManager.clickButton(this.handler.syncId, 1);
			this.ticksSinceModeChanged = 60;
		} else if (
			mouseX >= this.x + 156 && mouseX <= this.x + 166 && mouseY >= this.y + 114 && mouseY <= this.y + 124
		) {
			SpiritVectorHUD.playUISound(SpiritVectorSounds.RUNE_MATRIX_CLICK, 1);
			ClientConfig.setPlaySound(!ClientConfig.playSound());
		} else if (this.focusedSlot != null && (!this.handler.getCursorStack().isEmpty() || this.focusedSlot.hasStack())) {
			SpiritVectorHUD.playUISound(SpiritVectorSounds.RUNE_MATRIX_CLICK, 1);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}


	@Override
	protected boolean isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button) {
		// just ensure we're in the correct column
		return super.isClickOutsideBounds(mouseX, top, left, top, button);
	}

	private Identifier getModeSprite() {
		var vectorMode = VectorType.REGISTRY.getEntry(this.handler.getVectorMode()).orElseThrow();
		var id = vectorMode.getKey().orElseThrow().getValue().getPath();
		return SpiritVectorMod.id("rune_matrix/vector_mode_" + id);
	}

	@Override
	protected void drawItem(DrawContext context, ItemStack stack, int x, int y, String amountText) {
		if (stack.equals(handler.coreSlot.getStack())) return;
		super.drawItem(context, stack, x, y, amountText);
	}

	@Override
	protected void drawSlot(DrawContext context, Slot slot) {
		if (slot == handler.coreSlot) {
			return;
		}
		super.drawSlot(context, slot);
	}
}
