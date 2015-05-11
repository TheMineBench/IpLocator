package me.theminebench.iplocator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.ImmutableMap;
import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import com.maxmind.geoip.timeZone;

public class PlayerTimeZoneManager implements Listener {

	private LookupService lookupService;

	private HashMap<TimeZone, Set<UUID>> timeZones = new HashMap<TimeZone, Set<UUID>>();

	private JavaPlugin plugin;

	private TimeZone defaultTimeZone;

	public PlayerTimeZoneManager(JavaPlugin plugin) {
		this(plugin, TimeZone.getDefault());
	}

	public PlayerTimeZoneManager(JavaPlugin plugin, TimeZone defaultTimeZone) {
		this.plugin = plugin;
		this.defaultTimeZone = defaultTimeZone;

		String dbName = "GeoLiteCity.dat";

		File dbfile = new File(getPlugin().getDataFolder(), dbName);

		try {
			if (!dbfile.exists()) {
				InputStream in = getClass().getClassLoader().getResourceAsStream(dbName);
				getPlugin().getDataFolder().mkdirs();
				dbfile.createNewFile();
				OutputStream outStream = new FileOutputStream(dbfile);
				byte[] buffer = new byte[1024];
				int bytesRead;
				while ((bytesRead = in.read(buffer)) != -1) {
					outStream.write(buffer, 0, bytesRead);
				}
				in.close();
				outStream.flush();
				outStream.close();
			}
			lookupService = new LookupService(dbfile, LookupService.GEOIP_MEMORY_CACHE);
		} catch (IOException e) {
			e.printStackTrace();
			Bukkit.shutdown();
		}
		Bukkit.getPluginManager().registerEvents(this, plugin);
		for (Player p : Bukkit.getOnlinePlayers()) {
			Location location = lookupService.getLocation(p.getAddress().getAddress());
			setZone(p.getUniqueId(), getTimeZone(location));
		}
	}
	
	public void resetTimeZone(UUID playersUUID) {
		removeTimeZone(playersUUID);
		Location location = lookupService.getLocation(Bukkit.getPlayer(playersUUID).getAddress().getAddress());
		setZone(playersUUID, getTimeZone(location));
		
	}
	
	public void setTimeZone(UUID playersUUID, TimeZone timeZone) {
		removeTimeZone(playersUUID);
		setZone(playersUUID, timeZone);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		Location location = lookupService.getLocation(p.getAddress().getAddress());
		
		setZone(p.getUniqueId(), getTimeZone(location));
	}
	
	private void setZone(UUID playersUUID, TimeZone timeZone) {
		
		if (timeZone == null)
			timeZone = getDefaultTimeZone();
		
		if (timeZones.get(timeZone) == null)
			timeZones.put(timeZone, new HashSet<UUID>());
		
		timeZones.get(timeZone).add(playersUUID);
	}
	private void removeTimeZone(UUID playersUUID) {
		for (Entry<TimeZone, Set<UUID>> entry : timeZones.entrySet()) {
			if (entry.getValue().remove(playersUUID)) {
				if (entry.getValue().isEmpty()) {
					timeZones.remove(entry.getKey());
				}
				break;
			}
		}
	}
	@EventHandler(priority = EventPriority.LOW)
	public void onQuit(PlayerQuitEvent e) {
		removeTimeZone(e.getPlayer().getUniqueId());
	}
	public static TimeZone getTimeZone(Location location) {
		if (location == null)
			return null;
		String timeZoneString = timeZone.timeZoneByCountryAndRegion(location.countryCode, location.region);
		if (timeZoneString == null)
			return null;
		return TimeZone.getTimeZone(timeZoneString);
	}

	public void setDefaultTimeZone(TimeZone defaultTimeZone) {
		Set<UUID> players = timeZones.get(this.defaultTimeZone);
		timeZones.remove(this.defaultTimeZone);
		timeZones.put(defaultTimeZone, players);
		this.defaultTimeZone = defaultTimeZone;
	}

	public TimeZone getDefaultTimeZone() {
		return defaultTimeZone;
	}

	public ImmutableMap<TimeZone, Set<UUID>> getTimeZones() {
		return ImmutableMap.copyOf(timeZones);
	}

	public JavaPlugin getPlugin() {
		return plugin;
	}
}