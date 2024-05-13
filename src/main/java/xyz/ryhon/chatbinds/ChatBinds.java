package xyz.ryhon.chatbinds;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.impl.client.keybinding.KeyBindingRegistryImpl;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class ChatBinds implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("chat-binds");
	static ArrayList<ChatBind> Binds = new ArrayList<>();

	@Override
	public void onInitialize() {
		KeyBinding menuBind = new KeyBinding("chatbinds.key.menu",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
				"category.chatbinds");
		KeyBindingHelper.registerKeyBinding(menuBind);
		KeyBinding addChatBind = new KeyBinding("chatbinds.key.add_chat",
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
				"category.chatbinds");
		KeyBindingHelper.registerKeyBinding(addChatBind);

		ClientTickEvents.START_CLIENT_TICK.register(client -> {
			if (menuBind.wasPressed())
				client.setScreen(new BindMenuScreen(null));
			if (addChatBind.wasPressed())
				client.setScreen(new AddChatScreen("", null));

			for (ChatBind b : Binds)
				if (b.bind.wasPressed())
					sendMessage(b.cmd);
		});
		loadConfig();
	}

	public static void sendMessage(String msg) {
		if (msg.startsWith("/"))
			MinecraftClient.getInstance().player.networkHandler.sendChatCommand(msg.substring(1));
		else
			MinecraftClient.getInstance().player.networkHandler.sendChatMessage(msg);
	}

	static class ChatBind {
		public KeyBinding bind;
		public String cmd;
		public String title;
	}

	public static ChatBind registerCommand(String cmd, String title) {
		ChatBind b = new ChatBind();
		b.cmd = cmd;
		b.title = title;
		b.bind = new KeyBinding(title,
				InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN,
				"category.chatbinds.user");

		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.options != null) {
			KeyBindingRegistryImpl.addCategory(b.bind.getCategory());
			ArrayList<KeyBinding> keys = new ArrayList<>();
			keys.addAll(Arrays.asList(mc.options.allKeys));
			keys.add(b.bind);
			mc.options.allKeys = keys.toArray(new KeyBinding[0]);
		} else
			KeyBindingHelper.registerKeyBinding(b.bind);

		Binds.add(b);
		return b;
	}

	public static void removeCommand(ChatBind bind) {
		Binds.remove(bind);

		MinecraftClient mc = MinecraftClient.getInstance();
		ArrayList<KeyBinding> keys = new ArrayList<>();
		keys.addAll(Arrays.asList(mc.options.allKeys));
		keys.remove(bind.bind);
		mc.options.allKeys = keys.toArray(new KeyBinding[0]);
	}

	static Path configDir = FabricLoader.getInstance().getConfigDir().resolve("chatbinds");
	static Path configFileBinds = configDir.resolve("binds.json");

	static void loadConfig() {
		Binds = new ArrayList<>();

		try {
			Files.createDirectories(configDir);
			if (!Files.exists(configFileBinds))
				return;

			String str = Files.readString(configFileBinds);
			JsonArray ja = (JsonArray) JsonParser.parseString(str);

			for (JsonElement je : ja)
				if (je instanceof JsonObject jo)
					registerCommand(jo.get("cmd").getAsString(), jo.get("title").getAsString());
		} catch (Exception e) {
			LOGGER.error("Failed to load config", e);
		}
	}

	static void saveConfig() {
		JsonArray ja = new JsonArray();

		for (ChatBind b : Binds) {
			JsonObject jo = new JsonObject();
			jo.add("cmd", new JsonPrimitive(b.cmd));
			jo.add("title", new JsonPrimitive(b.title));

			ja.add(jo);
		}

		String json = new Gson().toJson(ja);

		try {
			Files.createDirectories(configDir);
			Files.writeString(configFileBinds, json);
		} catch (Exception e) {
			LOGGER.error("Failed to save config", e);
		}
	}
}