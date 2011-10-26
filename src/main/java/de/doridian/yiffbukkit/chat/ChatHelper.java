package de.doridian.yiffbukkit.chat;

import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.entity.Player;

import de.doridian.yiffbukkit.StateContainer;
import de.doridian.yiffbukkit.YiffBukkit;
import de.doridian.yiffbukkit.YiffBukkitCommandException;

public class ChatHelper extends StateContainer {
	//maps STRING (name) to CHATCHANNEL
	public HashMap<String, ChatChannel> channels = new HashMap<String, ChatChannel>();
	//maps PLAYER to CHATCHANNEL
	public HashMap<String, ChatChannel> activeChannel = new HashMap<String, ChatChannel>();
	
	public final ChatChannel DEFAULT;
	
	public void joinChannel(Player player, ChatChannel channel) throws YiffBukkitCommandException {
		String plyname = player.getName().toLowerCase();
		if(!channel.players.containsKey(plyname)) {
			channel.players.put(plyname, true);
		} else {
			throw new YiffBukkitCommandException("Player already in channel!");
		}
	}
	
	public void leaveChannel(Player player, ChatChannel channel) throws YiffBukkitCommandException {
		String plyname = player.getName().toLowerCase();
		if(channel.players.containsKey(plyname)) {
			channel.players.remove(plyname);
		} else {
			throw new YiffBukkitCommandException("Player not in channel!");
		}
	}
	
	public ChatChannel addChannel(Player owner, String name) throws YiffBukkitCommandException {
		return addChannel(owner, name, false);
	}
	
	public ChatChannel addChannel(Player owner, String name, boolean overwrite) throws YiffBukkitCommandException {
		String keyname = name.toLowerCase();
		ChatChannel newChan;
		if(channels.containsKey(keyname)) {
			if(overwrite) {
				newChan = channels.get(keyname);
			} else {
				throw new YiffBukkitCommandException("Channel exists already!");
			}
		} else {
			newChan = new ChatChannel(name);
			channels.put(keyname, newChan);
		}
		newChan.owner = owner.getName().toLowerCase();
		return newChan;
	}
	
	@SuppressWarnings("unchecked")
	public void removeChannel(ChatChannel channel) {
		channel.players.clear();
		channel.moderators.clear();
		channel.users.clear();
		channel.owner = null;
		channel.mode = ChatChannel.ChatChannelMode.PRIVATE;
		
		String channame = channel.name.toLowerCase();
		
		channels.remove(channame);
		
		for(Entry<String,ChatChannel> entry : ((HashMap<String,ChatChannel>)activeChannel.clone()).entrySet()) {
			if(entry.getValue() == channel) {
				activeChannel.remove(entry.getKey());
			}
		}
	}
	
	public ChatChannel getChannel(String name) throws YiffBukkitCommandException {
		if(channels.containsKey(name)) {
			return channels.get(name);
		} else {
			throw new YiffBukkitCommandException("Channel does not exist!");
		}
	}
	
	public ChatChannel getActiveChannel(Player ply) {
		if(ply == null) return DEFAULT;
		
		ChatChannel chan = activeChannel.get(ply.getName().toLowerCase());
		if(chan == null) chan = DEFAULT;
		
		return chan;
	}
	
	public void sendChat(Player ply, String msg) {
		sendChat(ply, msg, null);
	}
	
	public void sendChat(Player ply, String msg, ChatChannel chan) {
		if(chan == null) chan = getActiveChannel(ply);
		if(!chan.canSpeak(ply)) return;
		
		for(Entry<String,Boolean> entry : chan.players.entrySet()) {
			if(!entry.getValue()) continue; //for speed!
			
			Player player = plugin.getServer().getPlayerExact(entry.getKey());
			if(player == null) continue;
			
			if(chan.canHear(player, ply)) {
				if(getActiveChannel(player) == chan) {
					player.sendRawMessage(msg);
				} else {
					player.sendRawMessage("�2[" + chan.name + "]�f " + msg);
				}
			}
		}
	}
	
	private static ChatHelper instance;
	YiffBukkit plugin;
	ChatHelper(YiffBukkit plug) {
		plugin = plug;
		instance = this;
		
		ChatChannel cc = null;
		try {
			cc = addChannel(null, "DEFAULT", true);
		} catch(Exception e) { }
		
		DEFAULT = cc;
	}
	
	public static ChatHelper getInstance() {
		return instance;
	}
}