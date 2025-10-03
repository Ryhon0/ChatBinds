package xyz.ryhon.chatbinds;

import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import xyz.ryhon.chatbinds.ChatBinds.ChatBind;

import org.lwjgl.glfw.GLFW;

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
		
		if(list != null)
			remove(list);
			
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
			super(client, w, h, 0, 32);
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
				keyButton = ButtonWidget.builder(Text.empty(), this::onButton).size(0, 32) .build();
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
			public boolean mouseClicked(Click click, boolean doubled) {
				parent.setSelected(this);
				keyButton.mouseClicked(click, doubled);
				return true;
			}

			@Override
			public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
				context.drawTextWithShadow(client.textRenderer, Text.literal(bind.title), getX() + 8, getY() + 8, 0xFFFFFFFF);
				context.drawTextWithShadow(client.textRenderer, Text.literal(bind.cmd), getX() + 8, getY() + 16, 0xFF888888);
				
				
				keyButton.setX(getX() + getWidth() - 64 - 4);
				keyButton.setY(getY());
				keyButton.setWidth(64);
				keyButton.setHeight(getHeight());
				keyButton.render(context, mouseX, mouseY, deltaTicks);
			}
		}
	}

	@Override
	public boolean keyPressed(KeyInput input) {
		if (selectedEntry != null) {
			MinecraftClient mc = MinecraftClient.getInstance();
			if (input.key() == GLFW.GLFW_KEY_ESCAPE)
				selectedEntry.bind.bind.setBoundKey(InputUtil.UNKNOWN_KEY);
			else
				selectedEntry.bind.bind.setBoundKey(InputUtil.fromKeyCode(input));
			KeyBinding.updateKeysByCode();
			mc.options.write();

			selectedEntry.update();
			selectedEntry = null;

			return true;
		}
		return super.keyPressed(input);
	}
}
