package choir.adapter.v71_44.options.ui;

import java.util.ArrayList;
import java.util.List;

import init.constant.C;
import init.sprite.UI.UI;
import choir.api.options.OptionApplyMode;
import choir.api.options.OptionSchema;
import choir.api.options.OptionSetting;
import choir.api.options.OptionType;
import choir.adapter.v71_44.options.V7144OptionsProviderCatalog;
import choir.internal.options.OptionsProviderView;
import choir.internal.options.OptionsRegistry;
import snake2d.SPRITE_RENDERER;
import snake2d.util.color.COLOR;
import snake2d.util.color.OPACITY;
import snake2d.util.datatypes.COORDINATE;
import snake2d.util.gui.GuiSection;
import snake2d.util.gui.GUI_BOX;
import snake2d.util.gui.clickable.CLICKABLE;
import snake2d.util.gui.renderable.RENDEROBJ;
import snake2d.util.misc.STRING_RECIEVER;
import snake2d.util.sprite.SPRITE;
import snake2d.util.sprite.text.Font;
import util.colors.GCOLOR;
import util.data.INT.INTE;
import util.gui.misc.GButt;
import util.gui.misc.GText;
import util.gui.slider.GSliderInt;
import util.gui.slider.GSliderVer;
import util.gui.table.GScrollRows;
import view.main.VIEW;
import view.menu.MenuScreen;

public final class ChoirOptionsScreen {

	private static String selectedModId;
	private static final int PANEL_MARGIN = 28;
	private static final int CONTENT_PADDING = 32;
	private static final int COLUMN_GAP = 28;
	private static final int HEADER_GAP = 14;
	private static final int CONTENT_GAP = 12;
	private static final int FOOTER_GAP = 18;
	private static final int ROW_PADDING = 8;
	private static final int ROW_GAP = 6;
	private static final int SETTING_SEPARATOR_GAP = 10;
	private static final int SETTING_SEPARATOR_HEIGHT = 1;
	private static final int TEXT_INDENT = 12;
	private static final int SETTING_GAP = 16;
	private static final int MIN_LABEL_WIDTH = 220;
	private static final int MIN_INLINE_SETTING_WIDTH = 460;
	private static final int CONTROL_MIN_WIDTH = 140;
	private static final int CONTROL_PREFERRED_WIDTH = 280;
	private static final String RUNTIME_SETTINGS_REQUIRED =
			"Enter a settlement by starting a new game or loading a save to access this mod's settings.";

	private ChoirOptionsScreen() {
	}

	public static void openInGame() {
		openInGame(null);
	}

	public static void openInGame(final Runnable afterClose) {
		OptionsRegistry.openEditor();
		VIEW.inters().section.setCloseAction(new snake2d.util.misc.ACTION() {
			@Override
			public void exe() {
				OptionsRegistry.cancelEdits();
				if (afterClose != null) {
					afterClose.run();
				}
			}
		});
		VIEW.inters().section.activate(build(new Runnable() {
			@Override
			public void run() {
				VIEW.inters().section.close();
			}
		}, new Runnable() {
			@Override
			public void run() {
				openInGame(afterClose);
			}
		}, true));
	}

	/**
	 * Builds a non-interactive full-screen host around one centered interactive
	 * panel. Every text-bearing child receives a width derived from one of the
	 * panel's explicit content regions before it is added to a row.
	 */
	public static GuiSection build(final Runnable closeAction, final Runnable refreshAction, final boolean inGame) {
		V7144OptionsProviderCatalog.refresh();
		OptionsRegistry.openEditor();
		List<OptionsProviderView> providers = OptionsRegistry.providersSorted();
		if (selectedModId == null && !providers.isEmpty()) {
			selectedModId = providers.get(0).providerId();
		}
		if (selectedModId != null && OptionsRegistry.provider(selectedModId) == null && !providers.isEmpty()) {
			selectedModId = providers.get(0).providerId();
		}
		OptionsProviderView selectedProvider = OptionsRegistry.provider(selectedModId);

		int panelWidth = Math.min(MenuScreen.bounds.width(), Math.max(1, C.DIM().width() - PANEL_MARGIN * 2));
		int panelHeight = Math.min(MenuScreen.bounds.height(), Math.max(1, C.DIM().height() - PANEL_MARGIN * 2));
		GuiSection panel = new GuiSection();
		panel.body().setDim(panelWidth, panelHeight);

		RENDEROBJ frame = UI.decor().frame(panel.body(), COLOR.WHITE100);
		frame.body().moveX1Y1(0, 0);
		panel.add(frame);
		panel.moveLastToBack();

		int innerWidth = Math.max(1, panelWidth - CONTENT_PADDING * 2);
		int backWidth = Math.min(150, Math.max(80, innerWidth / 3));
		BoundedTextButton back = new BoundedTextButton("< back", backWidth) {
			@Override
			protected void clickA() {
				closeAction.run();
			}
		};
		RENDEROBJ title = label("mod options", Math.max(1, innerWidth - backWidth - HEADER_GAP), true);
		int headerHeight = Math.max(back.body().height(), title.body().height());
		panel.add(title, CONTENT_PADDING, CONTENT_PADDING);
		panel.add(back, panelWidth - CONTENT_PADDING - backWidth, CONTENT_PADDING);

		int footerHeight = Math.max(30, UI.FONT().H1.height());
		int footerY = Math.max(CONTENT_PADDING, panelHeight - CONTENT_PADDING - footerHeight);
		GuiSection footer = footer(panelWidth, footerHeight, closeAction, refreshAction,
				selectedProvider != null && selectedProvider.runtimeRegistered());
		footer.body().moveX1Y1(CONTENT_PADDING, footerY);
		panel.add(footer);

		int headingsY = CONTENT_PADDING + headerHeight + HEADER_GAP;
		ColumnLayout columns = ColumnLayout.create(CONTENT_PADDING, innerWidth, headingsY);
		RENDEROBJ registeredModsHeading = label("Registered Mods", columns.registeredModsColumn.width, true);
		RENDEROBJ settingsHeading = label("Settings", columns.settingsColumn.width, true);
		int contentTop = headingsY + Math.max(registeredModsHeading.body().height(), settingsHeading.body().height()) + CONTENT_GAP;
		int contentHeight = Math.max(36, footerY - FOOTER_GAP - contentTop);
		columns.registeredModsColumn.setY(contentTop).setHeight(contentHeight);
		columns.settingsColumn.setY(contentTop).setHeight(contentHeight);

		panel.add(registeredModsHeading, columns.registeredModsColumn.x, headingsY);
		panel.add(settingsHeading, columns.settingsColumn.x, headingsY);
		panel.add(sidebarRows(providers, columns.registeredModsColumn.width, columns.registeredModsColumn.height, refreshAction),
				columns.registeredModsColumn.x, columns.registeredModsColumn.y);
		panel.add(contentRows(selectedProvider, columns.settingsColumn.width, columns.settingsColumn.height, refreshAction, inGame),
				columns.settingsColumn.x, columns.settingsColumn.y);

		// Child coordinates are local to panel until this single final placement.
		panel.body().centerIn(C.DIM());
		return new ModalHost(panel);
	}

	private static GuiSection footer(int panelWidth, int height, final Runnable closeAction, final Runnable refreshAction,
			boolean settingsAvailable) {
		int width = Math.max(1, panelWidth - CONTENT_PADDING * 2);
		int gap = Math.min(24, Math.max(4, width / 16));
		int buttonWidth = Math.max(1, (width - gap * 2) / 3);
		GuiSection footer = new GuiSection();
		footer.body().setDim(width, height);
		BoundedTextButton apply = new BoundedTextButton("Apply", buttonWidth) {
			@Override
			protected void clickA() {
				OptionsRegistry.applyDrafts();
				OptionsRegistry.openEditor();
				refreshAction.run();
			}
		};
		apply.activeSet(settingsAvailable);
		footer.add(apply, 0, 0);
		footer.add(new BoundedTextButton("Cancel", buttonWidth) {
			@Override
			protected void clickA() {
				closeAction.run();
			}
		}, buttonWidth + gap, 0);
		BoundedTextButton reset = new BoundedTextButton("Reset Page", buttonWidth) {
			@Override
			protected void clickA() {
				if (selectedModId != null) {
					OptionsRegistry.resetDraftToDefaults(selectedModId);
					refreshAction.run();
				}
			}
		};
		reset.activeSet(settingsAvailable);
		footer.add(reset, (buttonWidth + gap) * 2, 0);
		return footer;
	}

	private static RENDEROBJ sidebarRows(List<OptionsProviderView> providers, int width, int height, final Runnable refreshAction) {
		int rowWidth = scrollContentWidth(width);
		if (providers.isEmpty()) {
			ArrayList<RENDEROBJ> rows = new ArrayList<RENDEROBJ>();
			rows.add(textRow("No enabled mods declare Choir Options support.", rowWidth));
			return new GScrollRows(rows, height, width, false).view();
		}
		ArrayList<RENDEROBJ> rows = new ArrayList<RENDEROBJ>();
		for (final OptionsProviderView provider : providers) {
			GText name = wrappedText(UI.FONT().M, provider.displayName(), Math.max(1, rowWidth - ROW_PADDING * 2));
			name.lablify();
			GButt.Glow button = new GButt.Glow((SPRITE) name) {
				@Override
				protected void clickA() {
					selectedModId = provider.providerId();
					refreshAction.run();
				}
			};
			button.body.setDim(rowWidth, Math.max(36, name.height() + ROW_PADDING * 2));
			button.selectedSet(provider.providerId().equals(selectedModId));
			button.hoverTitleSet(provider.displayName());
			button.hoverInfoSet(provider.description().isEmpty() ? provider.providerId() : provider.description());
			rows.add(button);
		}
		return new GScrollRows(rows, height, width, true).view();
	}

	private static RENDEROBJ contentRows(final OptionsProviderView provider, int width, int height,
			final Runnable refreshAction, final boolean inGame) {
		int rowWidth = scrollContentWidth(width);
		ArrayList<RENDEROBJ> rows = new ArrayList<RENDEROBJ>();
		if (provider == null) {
			rows.add(textRow("No enabled mods declare Choir Options support.", rowWidth));
			return new GScrollRows(rows, height, width, false).view();
		}
		if (!provider.runtimeRegistered()) return unavailableProviderState(rowWidth, height);
		final OptionSchema schema = provider.schema();
		if (!schema.description().isEmpty()) {
			rows.add(descriptionRow(schema.description(), rowWidth));
		}
		int editable = 0;
		for (int i = 0; i < schema.settings().size(); i++) {
			final OptionSetting setting = schema.settings().get(i);
			if (setting.type() == OptionType.INFO || setting.type() == OptionType.SECTION || setting.type() == OptionType.READ_ONLY) {
				rows.add(infoRow(setting, rowWidth));
				continue;
			}
			editable++;
			rows.add(settingRow(schema, setting, rowWidth, refreshAction, inGame, i + 1 < schema.settings().size()));
		}
		if (editable == 0) {
			rows.add(textRow("This mod registered no editable settings.", rowWidth));
		}
		return new GScrollRows(rows, height, width, true).view();
	}

	private static RENDEROBJ unavailableProviderState(int width, int height) {
		GuiSection state = new GuiSection();
		state.body().setDim(width, height);
		RENDEROBJ message = descriptionLabel(RUNTIME_SETTINGS_REQUIRED, Math.max(1, width - ROW_PADDING * 4));
		message.body().centerIn(state.body());
		state.add(message);
		return state;
	}

	private static RENDEROBJ settingRow(final OptionSchema schema, final OptionSetting setting, int width, final Runnable refreshAction,
			final boolean inGame, boolean showSeparator) {
		SettingLayout layout = SettingLayout.create(width);
		RENDEROBJ settingLabel = label(setting.label(), layout.settingLabelAreaWidth, false);
		int textY = 0;
		int textBottom = settingLabel.body().height();
		RENDEROBJ description = null;
		if (!setting.description().isEmpty()) {
			description = descriptionLabel(setting.description(), Math.max(1, layout.settingLabelAreaWidth - TEXT_INDENT));
			textY = textBottom + ROW_GAP;
			textBottom = textY + description.body().height();
		}
		RENDEROBJ applyMode = null;
		if (setting.applyMode() != OptionApplyMode.IMMEDIATE) {
			applyMode = descriptionLabel(setting.applyMode() == OptionApplyMode.RESTART ? "Restart required" : "World reload required",
					Math.max(1, layout.settingLabelAreaWidth - TEXT_INDENT));
			textY = textBottom + ROW_GAP;
			textBottom = textY + applyMode.body().height();
		}

		RENDEROBJ control = settingControl(schema, setting, refreshAction, inGame, layout.settingControlAreaWidth);
		int controlY = layout.inline ? 0 : textBottom + ROW_GAP;
		int contentBottom = Math.max(textBottom, controlY + control.body().height());
		int rowHeight = contentBottom + ROW_PADDING;
		if (showSeparator) {
			rowHeight += SETTING_SEPARATOR_GAP + SETTING_SEPARATOR_HEIGHT;
		}
		GuiSection row = new GuiSection();
		row.body().setDim(width, rowHeight);
		row.add(settingLabel, 0, 0);
		if (description != null) {
			row.add(description, TEXT_INDENT, settingLabel.body().height() + ROW_GAP);
		}
		if (applyMode != null) {
			row.add(applyMode, TEXT_INDENT, textY);
		}
		row.add(control, layout.settingControlAreaX, controlY);
		if (showSeparator) {
			row.add(settingSeparator(width), 0, rowHeight - SETTING_SEPARATOR_HEIGHT);
		}
		return row;
	}

	private static RENDEROBJ settingControl(final OptionSchema schema, final OptionSetting setting, final Runnable refreshAction, final boolean inGame, int width) {
		Object value = OptionsRegistry.draftValue(schema.providerId(), setting.key());
		switch (setting.type()) {
		case BOOLEAN:
			return toggle(schema, setting, value, refreshAction, width);
		case INT:
		case FLOAT:
			return numberControl(schema, setting, value, refreshAction, width);
		case STRING:
			return stringControl(schema, setting, value, refreshAction, inGame, width);
		case ENUM:
			return enumControl(schema, setting, value, refreshAction, width);
		default:
			return new RENDEROBJ.RenderDummy(0, 0);
		}
	}

	private static RENDEROBJ infoRow(OptionSetting setting, int width) {
		int textWidth = Math.max(1, width - TEXT_INDENT);
		boolean section = setting.type() == OptionType.SECTION;
		RENDEROBJ title = label(setting.label(), width, section);
		RENDEROBJ description = setting.description().isEmpty() ? null : descriptionLabel(setting.description(), textWidth);
		RENDEROBJ value = section ? null : label(String.valueOf(setting.defaultValue()), textWidth, false);
		int y = title.body().height();
		int descriptionY = -1;
		if (description != null) {
			descriptionY = y + ROW_GAP;
			y = descriptionY + description.body().height();
		}
		int valueY = -1;
		if (value != null) {
			valueY = y + ROW_GAP;
			y = valueY + value.body().height();
		}
		GuiSection row = new GuiSection();
		row.body().setDim(width, y + ROW_PADDING);
		row.add(title, 0, 0);
		if (description != null) {
			row.add(description, TEXT_INDENT, descriptionY);
		}
		if (value != null) {
			row.add(value, TEXT_INDENT, valueY);
		}
		return row;
	}

	private static RENDEROBJ toggle(final OptionSchema schema, final OptionSetting setting, Object value, final Runnable refreshAction, int width) {
		String text = Boolean.TRUE.equals(value) ? "On" : "Off";
		GButt.Glow button = new GButt.Glow((SPRITE) compactText(UI.FONT().S, text, Math.max(1, width - ROW_PADDING * 2))) {
			@Override
			protected void clickA() {
				boolean old = Boolean.TRUE.equals(OptionsRegistry.draftValue(schema.providerId(), setting.key()));
				OptionsRegistry.setDraft(schema.providerId(), setting.key(), Boolean.valueOf(!old));
				refreshAction.run();
			}
		};
		button.body.setDim(width, 30);
		return button;
	}

	private static RENDEROBJ enumControl(final OptionSchema schema, final OptionSetting setting, Object value, final Runnable refreshAction, int width) {
		GButt.Glow button = new GButt.Glow((SPRITE) compactText(UI.FONT().S, String.valueOf(value), Math.max(1, width - ROW_PADDING * 2))) {
			@Override
			protected void clickA() {
				List<String> values = setting.enumValues();
				Object current = OptionsRegistry.draftValue(schema.providerId(), setting.key());
				int index = values.indexOf(String.valueOf(current));
				index = (index + 1) % values.size();
				OptionsRegistry.setDraft(schema.providerId(), setting.key(), values.get(index));
				refreshAction.run();
			}
		};
		button.body.setDim(width, 30);
		button.hoverInfoSet("Click to cycle allowed values.");
		return button;
	}

	private static RENDEROBJ numberControl(final OptionSchema schema, final OptionSetting setting, Object value, final Runnable refreshAction, int width) {
		GuiSection row = new GuiSection();
		row.body().setDim(width, 32);
		final double minimum = setting.minimum() == null ? 0 : setting.minimum().doubleValue();
		final double maximum = setting.maximum() == null ? 100 : setting.maximum().doubleValue();
		final double step = setting.step() == null ? 1 : setting.step().doubleValue();
		final int maxIndex = Math.max(1, (int) Math.round((maximum - minimum) / step));
		int valueWidth = Math.min(90, Math.max(48, width / 3));
		int sliderWidth = Math.max(32, width - valueWidth - ROW_GAP);
		GSliderInt slider = new GSliderInt(new INTE() {
			@Override
			public int min() {
				return 0;
			}

			@Override
			public int max() {
				return maxIndex;
			}

			@Override
			public int get() {
				Object raw = OptionsRegistry.draftValue(schema.providerId(), setting.key());
				double current = raw instanceof Number ? ((Number) raw).doubleValue() : minimum;
				return clampIndex((int) Math.round((current - minimum) / step), maxIndex);
			}

			@Override
			public void set(int t) {
				int index = clampIndex(t, maxIndex);
				double next = minimum + step * index;
				if (setting.type() == OptionType.INT) {
					OptionsRegistry.setDraft(schema.providerId(), setting.key(), Integer.valueOf((int) Math.round(next)));
				} else {
					OptionsRegistry.setDraft(schema.providerId(), setting.key(), Double.valueOf(next));
				}
			}
		}, sliderWidth, true, false);
		row.add(slider, 0, 0);
		row.add(liveValueLabel(schema.providerId(), setting.key(), valueWidth), sliderWidth + ROW_GAP, 5);
		return row;
	}

	private static RENDEROBJ stringControl(final OptionSchema schema, final OptionSetting setting, Object value, final Runnable refreshAction, boolean inGame, int width) {
		String shown = String.valueOf(value);
		GButt.Glow button = new GButt.Glow((SPRITE) compactText(UI.FONT().S, shown.isEmpty() ? "(empty)" : shown, Math.max(1, width - ROW_PADDING * 2))) {
			@Override
			protected void clickA() {
				VIEW.inters().input.requestInput(new STRING_RECIEVER() {
					@Override
					public void acceptString(CharSequence string) {
						if (string != null) {
							OptionsRegistry.setDraft(schema.providerId(), setting.key(), string.toString());
							refreshAction.run();
						}
					}
				}, setting.label(), String.valueOf(OptionsRegistry.draftValue(schema.providerId(), setting.key())));
			}
		};
		button.body.setDim(width, 30);
		button.activeSet(inGame);
		button.hoverInfoSet(inGame ? "Click to edit text." : "Text editing is only available while a game view is active.");
		return button;
	}

	private static String formatValue(Object value) {
		if (value instanceof Double || value instanceof Float) {
			return String.format(java.util.Locale.ROOT, "%.3f", ((Number) value).doubleValue()).replaceAll("0+$", "").replaceAll("\\.$", "");
		}
		return String.valueOf(value);
	}

	private static RENDEROBJ liveValueLabel(final String providerId, final String key, final int width) {
		return new RENDEROBJ.RenderImp(width, 24) {
			private final GText text = new GText(UI.FONT().M, 48);

			@Override
			public void render(SPRITE_RENDERER r, float ds) {
				text.clear().add(formatValue(OptionsRegistry.draftValue(providerId, key)));
				text.setMultipleLines(false);
				text.setMaxWidth(body.width());
				text.adjustWidth();
				text.render(r, body.x1(), body.x2(), body.y1(), body.y2());
			}
		};
	}

	private static int clampIndex(int value, int max) {
		if (value < 0) {
			return 0;
		}
		if (value > max) {
			return max;
		}
		return value;
	}

	private static int scrollContentWidth(int width) {
		return Math.max(1, width - GSliderVer.WIDTH());
	}

	private static RENDEROBJ textRow(String text, int width) {
		RENDEROBJ wrapped = label(text, Math.max(1, width - ROW_PADDING * 2), false);
		GuiSection row = new GuiSection();
		row.body().setDim(width, Math.max(32, wrapped.body().height() + ROW_PADDING * 2));
		row.add(wrapped, ROW_PADDING, ROW_PADDING);
		return row;
	}

	private static RENDEROBJ descriptionRow(String text, int width) {
		RENDEROBJ wrapped = descriptionLabel(text, Math.max(1, width - ROW_PADDING * 2));
		GuiSection row = new GuiSection();
		row.body().setDim(width, Math.max(28, wrapped.body().height() + ROW_PADDING * 2));
		row.add(wrapped, ROW_PADDING, ROW_PADDING);
		return row;
	}

	private static RENDEROBJ descriptionLabel(String text, int width) {
		GText description = wrappedText(UI.FONT().S, text, width);
		description.normalify();
		return new RENDEROBJ.Sprite(description);
	}

	private static RENDEROBJ settingSeparator(final int width) {
		return new RENDEROBJ.RenderImp(Math.max(1, width), SETTING_SEPARATOR_HEIGHT) {
			@Override
			public void render(SPRITE_RENDERER r, float ds) {
				COLOR.WHITE20.render(r, body.x1(), body.x2(), body.y1(), body.y2());
			}
		};
	}

	private static RENDEROBJ label(String text, int width, boolean title) {
		GText label = new GText(title ? UI.FONT().H2 : UI.FONT().M, text == null ? "" : text);
		label.setMaxWidth(Math.max(1, width));
		label.adjustWidth();
		if (title) {
			label.lablify();
		}
		return new RENDEROBJ.Sprite(label);
	}

	private static GText compactText(Font font, String text, int width) {
		GText compact = new GText(font, text == null ? "" : text);
		compact.setMultipleLines(false);
		compact.setMaxWidth(Math.max(1, width));
		compact.adjustWidth();
		return compact;
	}

	private static GText wrappedText(Font font, String text, int width) {
		GText wrapped = new GText(font, text == null ? "" : text);
		wrapped.setMaxWidth(Math.max(1, width));
		wrapped.adjustWidth();
		return wrapped;
	}

	private static final class ColumnLayout {
		final ContentRect registeredModsColumn;
		final ContentRect settingsColumn;

		private ColumnLayout(ContentRect registeredModsColumn, ContentRect settingsColumn) {
			this.registeredModsColumn = registeredModsColumn;
			this.settingsColumn = settingsColumn;
		}

		static ColumnLayout create(int x, int width, int y) {
			int gap = Math.min(COLUMN_GAP, Math.max(8, width / 12));
			int available = Math.max(1, width - gap);
			int minimumSettingsWidth = Math.min(320, Math.max(1, available / 2));
			int sidebarWidth = Math.min(260, Math.max(120, available / 3));
			sidebarWidth = Math.min(sidebarWidth, Math.max(1, available - minimumSettingsWidth));
			int settingsWidth = Math.max(1, available - sidebarWidth);
			return new ColumnLayout(new ContentRect(x, y, sidebarWidth, 0), new ContentRect(x + sidebarWidth + gap, y, settingsWidth, 0));
		}
	}

	private static final class ContentRect {
		final int x;
		int y;
		final int width;
		int height;

		ContentRect(int x, int y, int width, int height) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
		}

		ContentRect setY(int y) {
			this.y = y;
			return this;
		}

		ContentRect setHeight(int height) {
			this.height = height;
			return this;
		}
	}

	private static final class SettingLayout {
		final int settingLabelAreaWidth;
		final int settingControlAreaX;
		final int settingControlAreaWidth;
		final boolean inline;

		private SettingLayout(int settingLabelAreaWidth, int settingControlAreaX, int settingControlAreaWidth, boolean inline) {
			this.settingLabelAreaWidth = settingLabelAreaWidth;
			this.settingControlAreaX = settingControlAreaX;
			this.settingControlAreaWidth = settingControlAreaWidth;
			this.inline = inline;
		}

		static SettingLayout create(int width) {
			int available = Math.max(1, width);
			int controlWidth = Math.min(CONTROL_PREFERRED_WIDTH, Math.max(CONTROL_MIN_WIDTH, available / 3));
			int labelWidth = available - controlWidth - SETTING_GAP;
			if (available >= MIN_INLINE_SETTING_WIDTH && labelWidth >= MIN_LABEL_WIDTH) {
				return new SettingLayout(labelWidth, labelWidth + SETTING_GAP, controlWidth, true);
			}
			return new SettingLayout(available, 0, available, false);
		}
	}

	private abstract static class BoundedTextButton extends CLICKABLE.ClickableAbs {
		private final GText text;

		BoundedTextButton(String label, int width) {
			text = compactText(UI.FONT().H1, label, width);
			body.setDim(width, Math.max(UI.FONT().H1.height(), text.height()));
		}

		@Override
		protected void render(SPRITE_RENDERER r, float ds, boolean isActive, boolean isSelected, boolean isHovered) {
			if (!isActive) {
				GCOLOR.T().INACTIVE.bind();
			} else if (isHovered && isSelected) {
				GCOLOR.T().HOVER_SELECTED.bind();
			} else if (isHovered) {
				GCOLOR.T().HOVERED.bind();
			} else if (isSelected) {
				GCOLOR.T().SELECTED.bind();
			} else {
				GCOLOR.T().CLICKABLE.bind();
			}
			text.render(r, body.x1(), body.x2(), body.y1(), body.y2());
			COLOR.unbind();
		}
	}

	private static final class ModalHost extends GuiSection {

		private final GuiSection panel;

		ModalHost(GuiSection panel) {
			this.panel = panel;
			body().set(C.DIM());
		}

		@Override
		public boolean hover(COORDINATE mCoo) {
			return panel.hover(mCoo);
		}

		@Override
		public boolean click() {
			return panel.click();
		}

		@Override
		public void hoverInfoGet(GUI_BOX text) {
			panel.hoverInfoGet(text);
		}

		@Override
		public void render(SPRITE_RENDERER r, float ds) {
			body().set(C.DIM());
			OPACITY.O85.bind();
			COLOR.BLACK.render(r, C.DIM());
			OPACITY.unbind();
			panel.render(r, ds);
		}
	}
}
