package org.apollo.game.model;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apollo.Service;
import org.apollo.fs.IndexedFileSystem;
import org.apollo.fs.decoder.ItemDefinitionDecoder;
import org.apollo.fs.decoder.NpcDefinitionDecoder;
import org.apollo.fs.decoder.ObjectDefinitionDecoder;
import org.apollo.fs.decoder.StaticObjectDecoder;
import org.apollo.game.command.CommandDispatcher;
import org.apollo.game.login.LoginDispatcher;
import org.apollo.game.login.LogoutDispatcher;
import org.apollo.game.model.def.EquipmentDefinition;
import org.apollo.game.model.def.ItemDefinition;
import org.apollo.game.model.def.NpcDefinition;
import org.apollo.game.model.def.ObjectDefinition;
import org.apollo.game.model.obj.StaticObject;
import org.apollo.game.model.sector.Sector;
import org.apollo.game.model.sector.SectorCoordinates;
import org.apollo.game.model.sector.SectorRepository;
import org.apollo.game.scheduling.ScheduledTask;
import org.apollo.game.scheduling.Scheduler;
import org.apollo.io.EquipmentDefinitionParser;
import org.apollo.util.MobRepository;
import org.apollo.util.NameUtil;
import org.apollo.util.plugin.PluginManager;

/**
 * The world class is a singleton which contains objects like the {@link MobRepository} for players and NPCs. It should
 * only contain things relevant to the in-game world and not classes which deal with I/O and such (these may be better
 * off inside some custom {@link Service} or other code, however, the circumstances are rare).
 * 
 * @author Graham
 */
public final class World {

	/**
	 * Represents the different status codes for registering a player.
	 * 
	 * @author Graham
	 */
	public enum RegistrationStatus {

		/**
		 * Indicates that the player is already online.
		 */
		ALREADY_ONLINE,

		/**
		 * Indicates that the player was registered successfully.
		 */
		OK,

		/**
		 * Indicates the world is full.
		 */
		WORLD_FULL;
	}

	/**
	 * The logger for this class.
	 */
	private static final Logger logger = Logger.getLogger(World.class.getName());

	/**
	 * The world.
	 */
	private static final World world = new World();

	/**
	 * Gets the world.
	 * 
	 * @return The world.
	 */
	public static World getWorld() {
		return world;
	}

	/**
	 * The command dispatcher.
	 */
	private final CommandDispatcher commandDispatcher = new CommandDispatcher();

	/**
	 * The login dispatcher.
	 */
	private final LoginDispatcher loginDispatcher = new LoginDispatcher();

	/**
	 * The logout dispatcher.
	 */
	private final LogoutDispatcher logoutDispatcher = new LogoutDispatcher();

	/**
	 * The {@link MobRepository} of {@link Npc}s.
	 */
	private final MobRepository<Npc> npcRepository = new MobRepository<>(WorldConstants.MAXIMUM_NPCS);

	/**
	 * The {@link MobRepository} of {@link Player}s.
	 */
	private final MobRepository<Player> playerRepository = new MobRepository<>(WorldConstants.MAXIMUM_PLAYERS);

	/**
	 * A {@link Map} of player usernames and the player objects.
	 */
	private final Map<Long, Player> players = new HashMap<>();

	/**
	 * The {@link PluginManager}. TODO: better place than here!!
	 */
	private PluginManager pluginManager;

	/**
	 * The release number (i.e. version) of this world.
	 */
	private int releaseNumber;

	/**
	 * This world's {@link SectorRepository}.
	 */
	private final SectorRepository repository = new SectorRepository(false);

	/**
	 * The scheduler.
	 */
	private final Scheduler scheduler = new Scheduler();

	/**
	 * Creates the world.
	 */
	private World() {

	}

	/**
	 * Gets the command dispatcher.
	 * 
	 * @return The command dispatcher.
	 */
	public CommandDispatcher getCommandDispatcher() {
		return commandDispatcher;
	}

	/**
	 * Gets the {@link LoginDispatcher}.
	 * 
	 * @return The dispatcher.
	 */
	public LoginDispatcher getLoginDispatcher() {
		return loginDispatcher;
	}

	/**
	 * Gets the {@link LogoutDispatcher}.
	 * 
	 * @return The dispatcher.
	 */
	public LogoutDispatcher getLogoutDispatcher() {
		return logoutDispatcher;
	}

	/**
	 * Gets the npc repository.
	 * 
	 * @return The npc repository.
	 */
	public MobRepository<Npc> getNpcRepository() {
		return npcRepository;
	}

	/**
	 * Gets the {@link Player} with the specified username. Note that this will return {@code null} if the player is
	 * offline.
	 * 
	 * @param username The username.
	 * @return The player.
	 */
	public Player getPlayer(String username) {
		return players.get(NameUtil.encodeBase37(username.toLowerCase()));
	}

	/**
	 * Gets the player repository.
	 * 
	 * @return The player repository.
	 */
	public MobRepository<Player> getPlayerRepository() {
		return playerRepository;
	}

	/**
	 * Gets the plugin manager. TODO should this be here?
	 * 
	 * @return The plugin manager.
	 */
	public PluginManager getPluginManager() {
		return pluginManager;
	}

	/**
	 * Gets the release number of this world.
	 * 
	 * @return The release number.
	 */
	public int getReleaseNumber() {
		return releaseNumber;
	}

	/**
	 * Initialises the world by loading definitions from the specified file system.
	 * 
	 * @param release The release number.
	 * @param fs The file system.
	 * @param manager The plugin manager. TODO move this.
	 * @throws IOException If an I/O error occurs.
	 */
	public void init(int release, IndexedFileSystem fs, PluginManager manager) throws Exception {
		this.releaseNumber = release;
		ItemDefinitionDecoder itemDefParser = new ItemDefinitionDecoder(fs);
		ItemDefinition[] itemDefs = itemDefParser.decode();
		ItemDefinition.init(itemDefs);
		logger.info("Loaded " + itemDefs.length + " item definitions.");

		InputStream is = new BufferedInputStream(new FileInputStream("data/equipment-" + release + ".dat"));
		try {
			EquipmentDefinitionParser parser = new EquipmentDefinitionParser(is);
			EquipmentDefinition[] defs = parser.parse();
			EquipmentDefinition.init(defs);
			logger.info("Loaded " + defs.length + " equipment definitions.");
		} finally {
			is.close();
		}

		NpcDefinitionDecoder npcDecoder = new NpcDefinitionDecoder(fs);
		NpcDefinition[] npcDefs = npcDecoder.decode();
		NpcDefinition.init(npcDefs);
		logger.info("Loaded " + npcDefs.length + " npc definitions.");

		ObjectDefinitionDecoder objectDecoder = new ObjectDefinitionDecoder(fs);
		ObjectDefinition[] objDefs = objectDecoder.decode();
		ObjectDefinition.init(objDefs);
		logger.info("Loaded " + objDefs.length + " object definitions.");

		StaticObjectDecoder staticDecoder = new StaticObjectDecoder(fs);
		StaticObject[] objects = staticDecoder.decode();
		placeEntities(objects);
		logger.info("Loaded " + objects.length + " static objects.");

		manager.start();
		pluginManager = manager; // TODO move!!
	}

	/**
	 * Checks if the {@link Player} with the specified name is online.
	 * 
	 * @param username The name.
	 * @return {@code true} if the player is online, otherwise {@code false}.
	 */
	public boolean isPlayerOnline(String username) {
		return players.get(NameUtil.encodeBase37(username.toLowerCase())) != null;
	}

	/**
	 * Adds entities to sectors in the {@link SectorRepository}.
	 * 
	 * @param entities The entities.
	 * @return {@code true} if all entities were added successfully, otherwise {@code false}.
	 */
	private boolean placeEntities(Entity... entities) {
		boolean success = true;

		for (Entity entity : entities) {
			Sector sector = repository.get(SectorCoordinates.fromPosition(entity.getPosition()));
			success &= sector.addEntity(entity);
		}
		return success;
	}

	/**
	 * Pulses the world.
	 */
	public void pulse() {
		scheduler.pulse();
	}

	/**
	 * Registers the specified npc.
	 * 
	 * @param npc The npc.
	 * @return {@code true} if the npc registered successfully, otherwise {@code false}.
	 */
	public boolean register(final Npc npc) {
		boolean success = npcRepository.add(npc);

		if (success) {
			logger.info("Registered npc: " + npc + " [count=" + npcRepository.size() + "]");
		} else {
			logger.warning("Failed to register npc, repository capacity reached: [count=" + npcRepository.size() + "]");
		}
		return success;
	}

	/**
	 * Registers the specified player.
	 * 
	 * @param player The player.
	 * @return A {@link RegistrationStatus}.
	 */
	public RegistrationStatus register(final Player player) {
		if (isPlayerOnline(player.getUsername())) {
			return RegistrationStatus.ALREADY_ONLINE;
		}

		boolean success = playerRepository.add(player);
		if (success) {
			players.put(NameUtil.encodeBase37(player.getUsername().toLowerCase()), player);
			logger.info("Registered player: " + player + " [count=" + playerRepository.size() + "]");
			return RegistrationStatus.OK;
		}

		logger.warning("Failed to register player (server full): " + player + " [count=" + playerRepository.size()
				+ "]");
		return RegistrationStatus.WORLD_FULL;
	}

	/**
	 * Schedules a new task.
	 * 
	 * @param task The {@link ScheduledTask}.
	 */
	public boolean schedule(ScheduledTask task) {
		return scheduler.schedule(task);
	}

	/**
	 * Unregisters the specified {@link Npc}.
	 * 
	 * @param npc The npc.
	 */
	public void unregister(final Npc npc) {
		if (npcRepository.remove(npc)) {
			logger.info("Unregistered npc: " + npc + " [count=" + npcRepository.size() + "]");
		} else {
			logger.warning("Could not find npc " + npc + " to unregister!");
		}
	}

	/**
	 * Unregisters the specified player.
	 * 
	 * @param player The player.
	 */
	public void unregister(final Player player) {
		if (playerRepository.remove(player) & players.remove(NameUtil.encodeBase37(player.getUsername().toLowerCase())) == player) {
			logger.info("Unregistered player: " + player + " [count=" + playerRepository.size() + "]");
			logoutDispatcher.dispatch(player);
		} else {
			logger.warning("Could not find player " + player + " to unregister!");
		}
	}

}