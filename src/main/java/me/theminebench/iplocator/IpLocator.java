package me.theminebench.iplocator;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class IpLocator extends JavaPlugin implements Runnable, Listener {

	private PlayerTimeZoneManager playerTimeZoneManager;

	@Override
	public void onEnable() {
		playerTimeZoneManager = new PlayerTimeZoneManager(this);
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().runTaskTimer(this, this, 0, 20 * 30);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player))
			return false;
		Player p = (Player) sender;
		
		if (label.equalsIgnoreCase("setTimeZone") && args.length >= 1) {
			TimeZone timeZone = TimeZone.getTimeZone(args[0]);
			if (timeZone == null) {
				sender.sendMessage(args[0] + " not recognized as a timezone");
				return true;
			} else {
				playerTimeZoneManager.setTimeZone(p.getUniqueId(), timeZone);
				updateTime(p.getUniqueId());
			}
		} else if (label.equalsIgnoreCase("resetTimeZone")) {
			playerTimeZoneManager.resetTimeZone(p.getUniqueId());
			updateTime(p.getUniqueId());
		}

		return false;
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		updateTime(e.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		updateTime(e.getPlayer().getUniqueId());
	}
	
	public void run() {

		Calendar cal = new GregorianCalendar();

		for (Entry<TimeZone, Set<UUID>> entry : playerTimeZoneManager.getTimeZones().entrySet()) {

			cal.setTimeZone(entry.getKey());

			int minecraftTime = getMinecraftTime(cal);

			for (UUID playersUUID : entry.getValue()) {
				Player p = Bukkit.getPlayer(playersUUID);
				p.setPlayerTime(minecraftTime, false);

				p.sendMessage(cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + " " + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm"));
			}
		}
	}
	
	public void updateTime() {
		Calendar cal = new GregorianCalendar();

		for (Entry<TimeZone, Set<UUID>> entry : playerTimeZoneManager.getTimeZones().entrySet()) {

			cal.setTimeZone(entry.getKey());

			int minecraftTime = getMinecraftTime(cal);

			for (UUID playersUUID : entry.getValue()) {
				Player p = Bukkit.getPlayer(playersUUID);
				p.setPlayerTime(minecraftTime, false);

				p.sendMessage(cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + " " + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm"));
			}
		}

	}
	
	public void updateTime(UUID playersUUID) {

		for (Entry<TimeZone, Set<UUID>> entry : playerTimeZoneManager.getTimeZones().entrySet()) {

			if (entry.getValue().contains(playersUUID)) {
				Calendar cal = new GregorianCalendar();

				cal.setTimeZone(entry.getKey());

				int minecraftTime = getMinecraftTime(cal);

				Player p = Bukkit.getPlayer(playersUUID);
				p.setPlayerTime(minecraftTime, false);
				p.sendMessage(cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + " " + (cal.get(Calendar.AM_PM) == 0 ? "am" : "pm"));
				break;
			}
		}
	}

	public static int getMinecraftTime(Calendar cal) {
		int timeInSec = (int) (cal.get(Calendar.SECOND) + (cal.get(Calendar.MINUTE) * 60) + (cal.get(Calendar.HOUR_OF_DAY) * 60 * 60) / 3.6);
		if ((timeInSec - 6000) < 0) {
			return ((timeInSec + 24000) - 6000);
		} else {
			return (timeInSec - 6000);
		}
	}
}