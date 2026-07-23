package dev.rdf453.ApothicAutoEnchant.table;


import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.platform.InputConstants;

import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
/*
 * 설계 메모 (2026-07-23 기준)
 * - 현재 상태:
 *   1) ApothEnchantmentScreen 기반 오버레이 방식으로 좌측 패널 렌더가 연결되어 있다.
 *   2) 입력 슬롯 비었을 때 Shift+좌클릭으로 패널 토글 진입/복귀가 동작한다.
 *   3) 자동화 버튼(+1/+10/-10/+ALL/-ALL/AUTO) 클릭 시 menu button id 전송이 연결되어 있다.
 *   4) 선택된 코스트(3~5)는 원본 옵션 행 위에 펄스 테두리 강조를 덧씌운다.
 * - 다음 작업:
 *   1) 버튼을 텍스트 위젯에서 스프라이트 버튼으로 치환해 최종 UI를 고정한다.
 *   2) 메뉴/BE 동기화 상태값(자동화 ON/OFF, 저장 XP, 코스트 선택)을 라벨과 함께 표시한다.
 *   3) 좌표 상수를 외부 설정으로 분리해 리빌드 없이 위치 튜닝 가능하게 만든다.
 * - 리스크/주의:
 *   1) mod id/리소스 경로 불일치 시 PANEL_TEXTURE 로딩 실패가 발생한다.
 *   2) 버튼 id 체계(0~2 원본, 3~10 자동화) 충돌 시 기존 인챈트 동작이 깨질 수 있다.
 */
//인첸트 부여 아이템이 없을때 부여 버튼 클릭 시 토글 상태 진입 및 on/off 버튼, xp관련 버튼 등장

public class EnchTableScreen extends ApothEnchantmentScreen {
	private static final Identifier PANEL_TEXTURE = Identifier.fromNamespaceAndPath("apothic_auto_enchanting", "textures/gui/table_ui.png");
	private static final int PANEL_WIDTH = 64;
	private static final int PANEL_HEIGHT = 116;
	private static final int PANEL_U = 0;
	private static final int PANEL_V = 0;
	private static final int TEX_WIDTH = 256;
	private static final int TEX_HEIGHT = 256;
	private static final int TOGGLE_AREA_X = 60;
	private static final int TOGGLE_AREA_Y = 14;
	private static final int TOGGLE_AREA_W = 108;
	private static final int TOGGLE_AREA_H = 57;
	private static final int OPTION_ROW_X = 60;
	private static final int OPTION_ROW_Y = 14;
	private static final int OPTION_ROW_W = 108;
	private static final int OPTION_ROW_H = 19;


	protected final EnchantMenu menu;
	private final List<Button> autoButtons = new ArrayList<>();
	private boolean autoPanelOpen = false;
	private int selectedCostButtonId = 3;


	public EnchTableScreen(EnchantMenu container,Inventory inv, Component tile) {
		super(container, inv, tile);
		this.menu = (EnchantMenu) container;
	}

	@Override
	protected void init(){
		super.init();
		this.autoButtons.clear();

		int panelX = this.leftPos - PANEL_WIDTH - 6;
		int panelY = this.topPos + 8;

		this.addAutoButton(panelX + 8, panelY + 8, "+1", 3, true);
		this.addAutoButton(panelX + 34, panelY + 8, "+10", 4, true);
		this.addAutoButton(panelX + 8, panelY + 30, "-10", 5, true);
		this.addAutoButton(panelX + 34, panelY + 30, "+ALL", 6, false);
		this.addAutoButton(panelX + 8, panelY + 52, "-10", 8, false);
		this.addAutoButton(panelX + 34, panelY + 52, "-ALL", 9, false);
		this.addAutoButton(panelX + 8, panelY + 78, "AUTO", 10, false);

		this.syncAutoPanelVisibility();
	}

	@Override
	public void containerTick() {
		super.containerTick();

		float pulse = 0.675f + 0.325f * (float) Math.sin(System.currentTimeMillis() / 150.0D);
		for (Button button : this.autoButtons) {
			String text = button.getMessage().getString();
			int id = this.buttonTextToId(text);
			if (id >= 3 && id <= 5 && id == this.selectedCostButtonId) {
				button.setAlpha(pulse);
				button.setFGColor(0xFFEAA200);
			} else {
				button.setAlpha(1.0F);
				button.clearFGColor();
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		boolean shiftDown = (event.modifiers() & InputConstants.MOD_SHIFT) != 0;
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && shiftDown && this.isToggleArea(event.x(), event.y())) {
			if (this.autoPanelOpen) {
				this.autoPanelOpen = false;
			} else if (this.isInputSlotEmpty()) {
				this.autoPanelOpen = true;
			}
			this.syncAutoPanelVisibility();
			return true;
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
		if (!this.autoPanelOpen) return;

		int panelX = this.leftPos - PANEL_WIDTH - 6;
		int panelY = this.topPos + 8;
		guiGraphics.blit(
			RenderPipelines.GUI_TEXTURED,
			PANEL_TEXTURE,
			panelX,
			panelY,
			(float) PANEL_U,
			(float) PANEL_V,
			PANEL_WIDTH,
			PANEL_HEIGHT,
			TEX_WIDTH,
			TEX_HEIGHT
		);

		this.drawSelectedCostOverlay(guiGraphics);
	}

	private void drawSelectedCostOverlay(GuiGraphicsExtractor guiGraphics) {
		int rowIdx = switch (this.selectedCostButtonId) {
			case 3 -> 0;
			case 4 -> 1;
			case 5 -> 2;
			default -> -1;
		};
		if (rowIdx < 0) return;

		int x = this.leftPos + OPTION_ROW_X;
		int y = this.topPos + OPTION_ROW_Y + rowIdx * OPTION_ROW_H;
		int w = OPTION_ROW_W;
		int h = OPTION_ROW_H;
		float pulse = 0.675f + 0.325f * (float) Math.sin(System.currentTimeMillis() / 150.0D);
		int alpha = (int) (pulse * 180.0F);
		int glow = (alpha << 24) | 0xF5D000;

		// 원본 룬/필요 레벨 텍스트가 그대로 보이도록 얇은 외곽선만 덧씌운다.
		guiGraphics.fill(x, y, x + w, y + 1, glow);
		guiGraphics.fill(x, y + h - 1, x + w, y + h, glow);
		guiGraphics.fill(x, y, x + 1, y + h, glow);
		guiGraphics.fill(x + w - 1, y, x + w, y + h, glow);
	}

	private void addAutoButton(int x, int y, String text, int buttonId, boolean isCostButton) {
		Button btn = Button.builder(Component.literal(text), b -> {
			if (isCostButton) this.selectedCostButtonId = buttonId;
			this.sendMenuButton(buttonId);
		}).bounds(x, y, 24, 20).build();
		btn.visible = false;
		btn.active = false;
		this.autoButtons.add(this.addRenderableWidget(btn));
	}

	private void sendMenuButton(int id) {
		if (this.minecraft == null || this.minecraft.gameMode == null) return;
		this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
	}

	private boolean isInputSlotEmpty() {
		if (this.menu.slots.isEmpty()) return true;
		Slot input = this.menu.getSlot(0);
		ItemStack stack = input.getItem();
		return stack == null || stack.isEmpty();
	}

	private boolean isToggleArea(double mouseX, double mouseY) {
		double x = this.leftPos + TOGGLE_AREA_X;
		double y = this.topPos + TOGGLE_AREA_Y;
		return mouseX >= x && mouseX < x + TOGGLE_AREA_W && mouseY >= y && mouseY < y + TOGGLE_AREA_H;
	}

	private void syncAutoPanelVisibility() {
		for (Button button : this.autoButtons) {
			button.visible = this.autoPanelOpen;
			button.active = this.autoPanelOpen;
		}
	}

	private int buttonTextToId(String text) {
		return switch (text) {
			case "+1" -> 3;
			case "+10" -> 4;
			case "-10" -> 5;
			default -> -1;
		};
	}

}
