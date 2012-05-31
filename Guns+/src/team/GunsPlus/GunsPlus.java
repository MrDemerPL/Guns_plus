package team.GunsPlus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import me.lyneira.MachinaRedstoneBridge.MachinaRedstoneBridge;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.griefcraft.lwc.LWC;
import com.griefcraft.lwc.LWCPlugin;
import com.narrowtux.showcase.Showcase;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;

import team.GunsPlus.API.GunsPlusAPI;
import team.GunsPlus.Block.Tripod;
import team.GunsPlus.Block.TripodData;
import team.GunsPlus.Enum.KeyType;
import team.GunsPlus.Item.Addition;
import team.GunsPlus.Item.Ammo;
import team.GunsPlus.Item.Gun;
import team.GunsPlus.Manager.ConfigLoader;
import team.GunsPlus.Manager.TripodDataHandler;
import team.GunsPlus.Util.Task;
import team.GunsPlus.Util.Util;
import team.GunsPlus.Util.VersionChecker;

public class GunsPlus extends JavaPlugin {
	public static String PRE = "[Guns+]";
	
	public static GunsPlus plugin;
	public static LWC lwc;
	public static WorldGuardPlugin wg;
	public static MachinaRedstoneBridge mrb;
	public static Showcase showcase;
	public static boolean useFurnaceAPI = false;
	
	private static GunsPlusAPI api;
	
	public static Logger log = Bukkit.getLogger();
	
	public static List<GunsPlusPlayer> GunsPlusPlayers = new ArrayList<GunsPlusPlayer>();
	
	//DEFAULT VALUES
	public static boolean warnings = true;
	public static boolean debug = false;
	public static boolean notifications = true;
	public static boolean autoreload = true;
	public static boolean hudenabled = false;
	public static int hudX = 20;
	public static int hudY = 20;
	public static String hudBackground = null;
	public static KeyType zoomKey = new KeyType("right", true);
	public static KeyType fireKey = new KeyType("left", false);
	public static KeyType reloadKey = new KeyType("R", false);

	//ITEM AND BLOCK LISTS
	public static List<Gun> allGuns = new ArrayList<Gun>();
	public static List<Ammo> allAmmo = new ArrayList<Ammo>();
	public static List<Addition> allAdditions = new ArrayList<Addition>();
	public static List<Material> transparentMaterials = new ArrayList<Material>();
	public static List<TripodData> allTripodBlocks = Collections.synchronizedList(new ArrayList<TripodData>());
	public static Tripod tripod;


	

	@Override
	public void onDisable() {
		if(Tripod.tripodenabled = true) {
			for(TripodData td : allTripodBlocks) {
				td.destroy();
			}
			TripodDataHandler.saveAll();
		}
		plugin.resetFields();
		log.log(Level.INFO, PRE + " version " + getDescription().getVersion()
				+ " is now disabled.");
	}
	
	@Override
	public void onEnable() {
		plugin = this;
		ConfigLoader.config();
		new VersionChecker(this,"http://dev.bukkit.org/server-mods/guns/files.rss");
		hook();
		init();
		new FireBinding(this, GunsPlus.fireKey);
		new ReloadBinding(this, GunsPlus.reloadKey);
		new ZoomBinding(this, GunsPlus.zoomKey);
		Bukkit.getPluginManager().registerEvents(new GunsPlusListener(this), this);
		getCommand("guns+").setExecutor(new CommandEx(this));
		api = new GunsPlusAPI(this);
		log.log(Level.INFO, PRE + " version " + getDescription().getVersion()+ " is now enabled.");
	}

	private void init() {
		ConfigLoader.loadGeneral();
		if(Tripod.tripodenabled){
			tripod = new Tripod(this, Tripod.tripodTexture);
		}
		ConfigLoader.loadAdditions();
		ConfigLoader.loadAmmo();
		ConfigLoader.loadGuns();
		ConfigLoader.loadRecipes();
		if(Tripod.tripodenabled) {
			initTripod();
			updateTripods();
		}
		Util.printCustomIDs();
		if(hudenabled)
			updateHUD();
	}
	
	private void initTripod(){
		TripodDataHandler.nextID = ConfigLoader.dataDB.getKeys(false).size();
		TripodDataHandler.allowLoading();//uhh ugly hack ;)
		TripodDataHandler.loadAll();
		TripodDataHandler.denyLoading(); 
	}
	
	private void hook(){
		Plugin spout = getServer().getPluginManager().getPlugin("Spout");
		Plugin lwcPlugin = getServer().getPluginManager().getPlugin("LWC");
		Plugin furnaceAPI = getServer().getPluginManager().getPlugin("FurnaceAPI");
		Plugin worldguard = getServer().getPluginManager().getPlugin("WorldGuardPlugin");
		Plugin machina = getServer().getPluginManager().getPlugin("MachinaRedstoneBridge");
		Plugin show = getServer().getPluginManager().getPlugin("Showcase");
		if(spout != null) {
		    log.log(Level.INFO, PRE+" Plugged into Spout!");
		}else{
			//disable this, because it would do nothing without spout
			log.log(Level.INFO, PRE+" disableing because Spout is missing!");
			this.setEnabled(false);
		}
		if(lwcPlugin != null) {
			lwc = ((LWCPlugin) lwcPlugin).getLWC();
		    log.log(Level.INFO, PRE+" Plugged into LWC!");
		}
		if(furnaceAPI != null) {
			useFurnaceAPI = true;	
			log.log(Level.INFO, PRE+" Plugged into FurnaceAPI!");
		}
		if(worldguard != null) {
			wg = (WorldGuardPlugin) worldguard;
			log.log(Level.INFO, PRE+" Plugged into WorldGuard!");
		}
		if(machina != null) {
			mrb = (MachinaRedstoneBridge) machina;
			log.log(Level.INFO, PRE+" Plugged into MachinaRedstoneBridge!");
		}
		if(show != null) {
			showcase = (Showcase) show;
			log.log(Level.INFO, PRE+" Plugged into Showcase!");
		}
	}
	
	private void updateHUD(){
		Task update = new Task(this){
			public void run(){
				for(GunsPlusPlayer gp:GunsPlus.GunsPlusPlayers){
					gp.getHUD().update(gp.getPlayer());
				}
			}
		};
		update.startTaskRepeating(5, false);
	}
	
	private void updateTripods(){
		if(Tripod.tripodenabled == false) return;
		Task update = new Task(this){
			public void run(){
				for(TripodData td: GunsPlus.allTripodBlocks){
					td.update();
					TripodDataHandler.save(td);
				}
			}
		};
		update.startTaskRepeating(10, false);
		Task save = new Task(this){
			public void run(){
				try {
					ConfigLoader.dataDB.save(ConfigLoader.dataFile);
					ConfigLoader.dataDB.load(ConfigLoader.dataFile);
				} catch (Exception e) {
					Util.debug(e);
				}
			}
		};
		save.startTaskRepeating(200, false);
	}

	private void resetFields() {
		GunsPlus.allGuns = new ArrayList<Gun>();
		GunsPlus.allAmmo = new ArrayList<Ammo>();
		GunsPlus.allAdditions = new ArrayList<Addition>();
		GunsPlus.transparentMaterials = new ArrayList<Material>();
		GunsPlus.allTripodBlocks = new ArrayList<TripodData>();
	}
	
	public void reload(){
		try{
			ConfigLoader.config();
			resetFields();
			init();
		}catch(Exception e){
			Util.debug(e);
			Util.warn("An error occured during reloading: "+ e.getMessage());
		}
	}
	
	public GunsPlusAPI getAPI() {
		return api;
	}
}
