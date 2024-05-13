package xyz.ryhon.chatbinds;

import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.KeybindsScreen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import xyz.ryhon.chatbinds.ChatBinds.ChatBind;

import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.google.common.collect.ImmutableList;

public class BindMenuScreen extends Screen implements ModMenuApi {
	Screen parent;
	public BindList.Entry selectedEntry;

	public BindMenuScreen(Screen parent) {
		super(Text.empty());
		this.parent = parent;
	}

	BindList list;
	ButtonWidget doneButton;
	ButtonWidget reloadButton;
	ButtonWidget addButton;
	ButtonWidget removeButton;

	@Override
	protected void init() {
		int sideBarW = 128;
		int sideBarX = width - 128;
		int padding = 8;
		int buttonHeight = 32;

		list = new BindList(this, client, width - sideBarW, height);
		list.updateEntries();
		addDrawableChild(list);

		doneButton = ButtonWidget.builder(Text.translatable("chatbinds.menuScreen.done"), this::onClose)
				.position(sideBarX + padding, height - buttonHeight - padding)
				.size(sideBarW - (padding * 2), buttonHeight)
				.build();
		addDrawable(doneButton);
		addSelectableChild(doneButton);

		reloadButton = ButtonWidget.builder(Text.translatable("chatbinds.menuScreen.reload"), this::onReload)
				.position(doneButton.getX(), doneButton.getY() - buttonHeight - padding)
				.size(doneButton.getWidth(), buttonHeight)
				.build();
		addDrawable(reloadButton);
		addSelectableChild(reloadButton);

		addButton = ButtonWidget.builder(Text.translatable("chatbinds.menuScreen.new"), this::onAdd)
				.position(reloadButton.getX(), reloadButton.getY() - buttonHeight)
				.size(reloadButton.getWidth(), buttonHeight)
				.build();
		addDrawable(addButton);
		addSelectableChild(addButton);

		removeButton = ButtonWidget.builder(Text.translatable("chatbinds.menuScreen.remove"), this::onRemove)
				.position(addButton.getX(), addButton.getY() - buttonHeight - padding)
				.size(addButton.getWidth(), buttonHeight)
				.build();
		addDrawable(removeButton);
		addSelectableChild(removeButton);
	}

	void onAdd(ButtonWidget w) {
		client.setScreen(new AddChatScreen("", this));
	}

	void onRemove(ButtonWidget w) {
		BindList.Entry e = list.getSelectedOrNull();
		if (e != null) {
			ChatBinds.removeCommand(e.bind);
			ChatBinds.saveConfig();
			list.updateEntries();
		}
	}

	void onReload(ButtonWidget w) {
		ChatBinds.loadConfig();
		init();
	}

	void onClose(ButtonWidget w) {
		close();
	}

	@Override
	public void close() {
		client.setScreen(parent);
	}

	class BindList extends AlwaysSelectedEntryListWidget<BindList.Entry> {
		public BindMenuScreen parent;

		public BindList(BindMenuScreen parent, MinecraftClient client, int w, int h) {
			super(client, w, h, 0, h, 32);
			this.parent = parent;
			updateEntries();
		}

		public void updateEntries() {
			this.clearEntries();
			for (ChatBind b : ChatBinds.Binds) {
				Entry e = new Entry(client, b, this);
				addEntry(e);
				if (getSelectedOrNull() == null)
					setSelected(e);
			}
		}

		static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
			MinecraftClient client;
			public ChatBind bind;
			BindList parent;
			ButtonWidget keyButton;

			Entry(MinecraftClient client, ChatBind b, BindList parent) {
				super();
				this.client = client;
				this.bind = b;
				this.parent = parent;
				keyButton = ButtonWidget.builder(Text.empty(), this::onButton).build();
				update();
			}

			public void update() {
				keyButton.setMessage(bind.bind.getBoundKeyLocalizedText());
			}

			void onButton(ButtonWidget b) {
				parent.parent.selectedEntry = this;
				keyButton.setMessage(Text.literal("..."));
			}

			@Override
			public Text getNarration() {
				return Text.empty();
			}

			@Override
			public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight,
					int mouseX, int mouseY, boolean hovered, float tickDelta) {
				context.drawTextWithShadow(client.textRenderer, Text.literal(bind.title), x + 8, y + 8, 0xFFFFFF);
				context.drawTextWithShadow(client.textRenderer, Text.literal(bind.cmd), x + 8, y + 16, 0x888888);

				keyButton.setX(x + entryWidth - 64 - 4);
				keyButton.setY(y);
				keyButton.setWidth(64);
				keyButton.setHeight(entryHeight);
				keyButton.render(context, mouseX, mouseY, tickDelta);
			}

			@Override
			public boolean mouseClicked(double mouseX, double mouseY, int button) {
				parent.setSelected(this);
				keyButton.mouseClicked(mouseX, mouseY, button);
				return true;
			}
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (selectedEntry != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE)
				client.options.setKeyCode(selectedEntry.bind.bind, InputUtil.UNKNOWN_KEY);
			else
				client.options.setKeyCode(selectedEntry.bind.bind, InputUtil.fromKeyCode(keyCode, scanCode));
			KeyBinding.updateKeysByCode();

			selectedEntry.update();
			selectedEntry = null;

			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (selectedEntry != null) {
			client.options.setKeyCode(selectedEntry.bind.bind, InputUtil.Type.MOUSE.createFromCode(button));
			KeyBinding.updateKeysByCode();
			selectedEntry.update();
			selectedEntry = null;
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
