package org.apollo.game.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.apollo.game.event.Event;
import org.apollo.game.event.impl.ConfigEvent;
import org.apollo.game.event.impl.IdAssignmentEvent;
import org.apollo.game.event.impl.IgnoreListEvent;
import org.apollo.game.event.impl.LogoutEvent;
import org.apollo.game.event.impl.SendFriendEvent;
import org.apollo.game.event.impl.ServerMessageEvent;
import org.apollo.game.event.impl.SetWidgetTextEvent;
import org.apollo.game.event.impl.SwitchTabInterfaceEvent;
import org.apollo.game.event.impl.UpdateRunEnergyEvent;
import org.apollo.game.model.Inventory.StackMode;
import org.apollo.game.model.inter.InterfaceConstants;
import org.apollo.game.model.inter.InterfaceSet;
import org.apollo.game.model.inter.bank.BankConstants;
import org.apollo.game.model.inv.AppearanceInventoryListener;
import org.apollo.game.model.inv.FullInventoryListener;
import org.apollo.game.model.inv.InventoryListener;
import org.apollo.game.model.inv.SynchronizationInventoryListener;
import org.apollo.game.model.settings.PrivacyState;
import org.apollo.game.model.settings.PrivilegeLevel;
import org.apollo.game.model.settings.ScreenBrightness;
import org.apollo.game.model.skill.LevelUpSkillListener;
import org.apollo.game.model.skill.SkillListener;
import org.apollo.game.model.skill.SynchronizationSkillListener;
import org.apollo.game.sync.block.SynchronizationBlock;
import org.apollo.net.session.GameSession;
import org.apollo.security.PlayerCredentials;
import org.apollo.util.Point;

/**
 * A {@link Player} is a {@link Mob} that a user is controlling.
 * 
 * @author Graham
 */
public final class Player extends Mob {

	/**
	 * The generated serial uid.
	 */
	private static final long serialVersionUID = -5865532568677077237L;

	/**
	 * The player's appearance.
	 */
	private Appearance appearance = Appearance.DEFAULT_APPEARANCE;

	/**
	 * This player's bank.
	 */
	private final Inventory bank = new Inventory(InventoryConstants.BANK_CAPACITY, StackMode.STACK_ALWAYS);

	/**
	 * The privacy state of this player's public chat.
	 */
	private PrivacyState chatPrivacy = PrivacyState.ON;

	/**
	 * A deque of this player's mouse clicks.
	 */
	private transient Deque<Point> clicks = new ArrayDeque<Point>();

	/**
	 * This player's credentials.
	 */
	private PlayerCredentials credentials;

	/**
	 * A flag indicating if the player has designed their avatar.
	 */
	private boolean designedAvatar = false;

	/**
	 * A flag which indicates there are npcs that couldn't be added.
	 */
	private transient boolean excessiveNpcs = false;

	/**
	 * A flag which indicates there are players that couldn't be added.
	 */
	private transient boolean excessivePlayers = false;

	/**
	 * The privacy state of this player's private chat.
	 */
	private PrivacyState friendPrivacy = PrivacyState.ON;

	/**
	 * The list of usernames of players this player has befriended.
	 */
	private List<String> friends = new ArrayList<>();

	/**
	 * This player's head icon.
	 */
	private int headIcon = -1;

	/**
	 * The list of usernames of players this player has ignored.
	 */
	private List<String> ignores = new ArrayList<>();

	/**
	 * This player's interface set.
	 */
	private final transient InterfaceSet interfaceSet = new InterfaceSet(this);

	/**
	 * The centre of the last region the client has loaded.
	 */
	private transient Position lastKnownRegion;

	/**
	 * The membership flag.
	 */
	private transient boolean members = false;

	/**
	 * This player's prayer icon.
	 */
	private int prayerIcon = 0;

	/**
	 * The privilege level.
	 */
	private PrivilegeLevel privilegeLevel = PrivilegeLevel.STANDARD;

	/**
	 * A temporary queue of events sent during the login process.
	 */
	private final transient Deque<Event> queuedEvents = new ArrayDeque<Event>();

	/**
	 * A flag indicating if the region changed in the last cycle.
	 */
	private transient boolean regionChanged = false;

	/**
	 * The player's run energy.
	 */
	private int runEnergy = 100;

	/**
	 * A flag indicating if this player is running.
	 */
	private boolean running = false;

	/**
	 * The brightness of this player's screen.
	 */
	private ScreenBrightness screenBrightness = ScreenBrightness.NORMAL;

	/**
	 * The {@link GameSession} currently attached to this {@link Player}.
	 */
	private transient GameSession session;

	/**
	 * The privacy state of this player's trade chat.
	 */
	private PrivacyState tradePrivacy = PrivacyState.ON;

	/**
	 * The current maximum viewing distance of this player.
	 */
	private int viewingDistance = 1;

	/**
	 * A flag indicating if the player is withdrawing items as notes.
	 */
	private boolean withdrawingNotes = false;

	/**
	 * The id of the world this player is in.
	 */
	private transient int worldId = 1;

	/**
	 * Creates the {@link Player}.
	 * 
	 * @param credentials The player's credentials.
	 * @param position The initial position.
	 */
	public Player(PlayerCredentials credentials, Position position) {
		super(position);
		init();
		this.credentials = credentials;
	}

	/**
	 * Adds a click, represented by a {@link Point}, to the {@link List} of clicks.
	 * 
	 * @param point The point.
	 * @return {@code true} if the point was added successfully.
	 */
	public boolean addClick(Point point) {
		return clicks.add(point);
	}

	/**
	 * Adds the specified username to this player's friend list.
	 * 
	 * @param username The username.
	 */
	public boolean addFriend(String username) {
		return friends.add(username.toLowerCase());
	}

	/**
	 * Adds the specified username to this player's ignore list.
	 * 
	 * @param username The username.
	 */
	public boolean addIgnore(String username) {
		return ignores.add(username.toLowerCase());
	}

	/**
	 * Decrements this player's viewing distance if it is greater than 1.
	 */
	public void decrementViewingDistance() {
		if (viewingDistance > 1) {
			viewingDistance--;
		}
	}

	/**
	 * Sets the excessive npcs flag.
	 */
	public void flagExcessiveNpcs() {
		excessiveNpcs = true;
	}

	/**
	 * Sets the excessive players flag.
	 */
	public void flagExcessivePlayers() {
		excessivePlayers = true;
	}

	/**
	 * Indicates whether this player is friends with the player with the specified username or not.
	 * 
	 * @param username The username of the other player.
	 * @return {@code true} if the specified username is on this player's friend list, otherwise {@code false}.
	 */
	public boolean friendsWith(String username) {
		return friends.contains(username.toLowerCase());
	}

	/**
	 * Gets the player's appearance.
	 * 
	 * @return The appearance.
	 */
	public Appearance getAppearance() {
		return appearance;
	}

	/**
	 * Gets the mob's bank.
	 * 
	 * @return The bank.
	 */
	public Inventory getBank() {
		return bank;
	}

	/**
	 * Gets this player's public chat privacy state.
	 * 
	 * @return The privacy state.
	 */
	public PrivacyState getChatPrivacy() {
		return chatPrivacy;
	}

	/**
	 * Gets the {@link Deque} of clicks.
	 * 
	 * @return The deque.
	 */
	public Deque<Point> getClicks() {
		return clicks;
	}

	/**
	 * Gets the player's credentials.
	 * 
	 * @return The player's credentials.
	 */
	public PlayerCredentials getCredentials() {
		return credentials;
	}

	/**
	 * Gets the player's name, encoded as a long.
	 * 
	 * @return The encoded player name.
	 */
	public long getEncodedName() {
		return credentials.getEncodedUsername();
	}

	@Override
	public EntityType getEntityType() {
		return EntityType.PLAYER;
	}

	/**
	 * Gets this player's friend chat {@link PrivacyState}.
	 * 
	 * @return The privacy state.
	 */
	public PrivacyState getFriendPrivacy() {
		return friendPrivacy;
	}

	/**
	 * Gets the {@link List} of this player's friends.
	 * 
	 * @return The list.
	 */
	public List<String> getFriendUsernames() {
		return friends;
	}

	/**
	 * Gets the player's head icon.
	 * 
	 * @return The head icon.
	 */
	public int getHeadIcon() {
		return headIcon;
	}

	/**
	 * Gets the {@link List} of usernames of ignored players.
	 * 
	 * @return The list.
	 */
	public List<String> getIgnoredUsernames() {
		return ignores;
	}

	/**
	 * Gets this player's interface set.
	 * 
	 * @return The interface set for this player.
	 */
	public InterfaceSet getInterfaceSet() {
		return interfaceSet;
	}

	/**
	 * Gets this player's last click, represented by a {@link Point}.
	 * 
	 * @return The click.
	 */
	public Point getLastClick() {
		return clicks.pollLast();
	}

	/**
	 * Gets the last known region.
	 * 
	 * @return The last known region, or {@code null} if the player has never known a region.
	 */
	public Position getLastKnownRegion() {
		return lastKnownRegion;
	}

	/**
	 * Gets the player's prayer icon.
	 * 
	 * @return The prayer icon.
	 */
	public int getPrayerIcon() {
		return prayerIcon;
	}

	/**
	 * Gets the privilege level.
	 * 
	 * @return The privilege level.
	 */
	public PrivilegeLevel getPrivilegeLevel() {
		return privilegeLevel;
	}

	/**
	 * Gets the player's run energy.
	 * 
	 * @return The run energy.
	 */
	public int getRunEnergy() {
		return runEnergy;
	}

	/**
	 * Gets this player's {@link ScreenBrightness}.
	 * 
	 * @return The screen brightness.
	 */
	public ScreenBrightness getScreenBrightness() {
		return screenBrightness;
	}

	/**
	 * Gets the game session.
	 * 
	 * @return The game session.
	 */
	public GameSession getSession() {
		return session;
	}

	/**
	 * Gets this player's trade {@link PrivacyState}.
	 * 
	 * @return The privacy state.
	 */
	public PrivacyState getTradePrivacy() {
		return tradePrivacy;
	}

	/**
	 * Gets the player's name.
	 * 
	 * @return The player's name.
	 */
	public String getUsername() {
		return credentials.getUsername();
	}

	/**
	 * Gets this player's viewing distance.
	 * 
	 * @return The viewing distance.
	 */
	public int getViewingDistance() {
		return viewingDistance;
	}

	/**
	 * Gets the id of the world this player is in.
	 * 
	 * @return The id.
	 */
	public int getWorldId() {
		return worldId;
	}

	/**
	 * Checks if the player has designed their avatar.
	 * 
	 * @return A flag indicating if the player has designed their avatar.
	 */
	public boolean hasDesignedAvatar() {
		return designedAvatar;
	}

	/**
	 * Indicates whether or not the player with the specified username is on this player's ignore list.
	 * 
	 * @param username The username of the player.
	 * @return {@code true} if the player is ignored, {@code false} if not.
	 */
	public boolean hasIgnored(String username) {
		return ignores.contains(username.toLowerCase());
	}

	/**
	 * Checks if this player has ever known a region.
	 * 
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean hasLastKnownRegion() {
		return lastKnownRegion != null;
	}

	/**
	 * Checks if the region has changed.
	 * 
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean hasRegionChanged() {
		return regionChanged;
	}

	/**
	 * Increments this player's viewing distance if it is less than the maximum viewing distance.
	 */
	public void incrementViewingDistance() {
		if (viewingDistance < Position.MAX_DISTANCE) {
			viewingDistance++;
		}
	}

	/**
	 * Initialises this player.
	 */
	private void init() {
		initInventories();
		initSkills();
	}

	/**
	 * Initialises the player's inventories.
	 */
	private void initInventories() {
		// inventory full listeners
		InventoryListener fullInventoryListener = new FullInventoryListener(this,
				FullInventoryListener.FULL_INVENTORY_MESSAGE);
		InventoryListener fullBankListener = new FullInventoryListener(this, FullInventoryListener.FULL_BANK_MESSAGE);

		// equipment appearance listener
		InventoryListener appearanceListener = new AppearanceInventoryListener(this);

		// synchronization listeners
		InventoryListener syncInventoryListener = new SynchronizationInventoryListener(this,
				SynchronizationInventoryListener.INVENTORY_ID);
		InventoryListener syncBankListener = new SynchronizationInventoryListener(this, BankConstants.BANK_INVENTORY_ID);
		InventoryListener syncEquipmentListener = new SynchronizationInventoryListener(this,
				SynchronizationInventoryListener.EQUIPMENT_ID);

		// add the listeners
		inventory.addListener(syncInventoryListener);
		inventory.addListener(fullInventoryListener);
		bank.addListener(syncBankListener);
		bank.addListener(fullBankListener);
		equipment.addListener(syncEquipmentListener);
		equipment.addListener(appearanceListener);
	}

	/**
	 * Initialises the player's skills.
	 */
	private void initSkills() {
		SkillSet skills = getSkillSet();

		// synchronization listener
		SkillListener syncListener = new SynchronizationSkillListener(this);

		// level up listener
		SkillListener levelUpListener = new LevelUpSkillListener(this);

		// add the listeners
		skills.addListener(syncListener);
		skills.addListener(levelUpListener);
	}

	/**
	 * Checks if there are excessive npcs.
	 * 
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean isExcessiveNpcsSet() {
		return excessiveNpcs;
	}

	/**
	 * Checks if there are excessive players.
	 * 
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean isExcessivePlayersSet() {
		return excessivePlayers;
	}

	/**
	 * Checks if this player account has membership.
	 * 
	 * @return {@code true} if so, {@code false} if not.
	 */
	public boolean isMembers() {
		return members;
	}

	/**
	 * Gets whether the player is running or not.
	 * 
	 * @return {@code true} if the player is running, otherwise {@code false}.
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * Gets the withdrawing notes flag.
	 * 
	 * @return The flag.
	 */
	public boolean isWithdrawingNotes() {
		return withdrawingNotes;
	}

	/**
	 * Logs the player out, if possible.
	 */
	public void logout() {
		send(new LogoutEvent());
	}

	/**
	 * Removes the specified username from this player's friend list.
	 * 
	 * @param username The username.
	 */
	public boolean removeFriend(String username) {
		return friends.remove(username.toLowerCase());
	}

	/**
	 * Removes the specified username from this player's ignore list.
	 * 
	 * @param username The username.
	 */
	public boolean removeIgnore(String username) {
		return ignores.remove(username.toLowerCase());
	}

	/**
	 * Resets the excessive players flag.
	 */
	public void resetExcessivePlayers() {
		excessivePlayers = false;
	}

	/**
	 * Resets this player's viewing distance.
	 */
	public void resetViewingDistance() {
		viewingDistance = 1;
	}

	/**
	 * Sends an {@link Event} to this player.
	 * 
	 * @param event The event.
	 */
	public void send(Event event) {
		if (isActive()) {
			if (!queuedEvents.isEmpty()) {
				for (Event queuedEvent : queuedEvents) {
					session.dispatchEvent(queuedEvent);
				}
				queuedEvents.clear();
			}
			session.dispatchEvent(event);
		} else {
			queuedEvents.add(event);
		}
	}

	/**
	 * Sends the initial events.
	 */
	private void sendInitialEvents() {
		send(new IdAssignmentEvent(index, members)); // TODO should this be sent when we reconnect?
		sendMessage("Welcome to RuneScape.");

		if (!designedAvatar) {
			interfaceSet.openWindow(InterfaceConstants.AVATAR_DESIGN);
		}

		int[] tabs = InterfaceConstants.DEFAULT_INVENTORY_TABS;
		for (int tab = 0; tab < tabs.length; tab++) {
			send(new SwitchTabInterfaceEvent(tab, tabs[tab]));
		}

		inventory.forceRefresh();
		equipment.forceRefresh();
		bank.forceRefresh();
		skillSet.forceRefresh();

		World.getWorld().getLoginDispatcher().dispatch(this);
	}

	/**
	 * Sends a message to the player.
	 * 
	 * @param message The message.
	 */
	public void sendMessage(String message) {
		send(new ServerMessageEvent(message));
	}

	/**
	 * Sends the quest interface
	 * 
	 * @param text The text to display on the interface.
	 */
	public void sendQuestInterface(List<String> text) {
		int size = text.size(), lines = InterfaceConstants.QUEST_TEXT.length;
		if (size > lines) {
			throw new IllegalArgumentException("List contains too much text to display on this interface.");
		}

		for (int pos = 0; pos < lines; pos++) {
			send(new SetWidgetTextEvent(InterfaceConstants.QUEST_TEXT[pos], pos < size ? text.get(pos) : ""));
		}
		interfaceSet.openWindow(InterfaceConstants.QUEST_INTERFACE);
	}

	/**
	 * Sends the friend and ignore user lists.
	 */
	public void sendUserLists() {
		if (ignores.size() > 0) {
			send(new IgnoreListEvent(ignores));
		}

		World world = World.getWorld();
		for (String username : friends) {
			int worldId = world.isPlayerOnline(username) ? world.getPlayer(username).worldId : 0;
			send(new SendFriendEvent(username, worldId));
		}
	}

	/**
	 * Sets the player's appearance.
	 * 
	 * @param appearance The new appearance.
	 */
	public void setAppearance(Appearance appearance) {
		this.appearance = appearance;
		blockSet.add(SynchronizationBlock.createAppearanceBlock(this));
	}

	/**
	 * Sets the chat {@link PrivacyState}.
	 * 
	 * @param chatPrivacy The privacy state.
	 */
	public void setChatPrivacy(PrivacyState chatPrivacy) {
		this.chatPrivacy = chatPrivacy;
	}

	/**
	 * Sets the design flag.
	 * 
	 * @param designedAvatar A flag indicating if the player has designed their avatar.
	 */
	public void setDesigned(boolean designedAvatar) {
		this.designedAvatar = designedAvatar;
	}

	/**
	 * Sets the friend {@link PrivacyState}.
	 * 
	 * @param friendPrivacy The privacy state.
	 */
	public void setFriendPrivacy(PrivacyState friendPrivacy) {
		this.friendPrivacy = friendPrivacy;
	}

	/**
	 * Sets the {@link List} of this player's friends.
	 * 
	 * @param friends The friends.
	 */
	public void setFriendUsernames(List<String> friends) {
		this.friends = friends;
	}

	/**
	 * Sets the player's head icon.
	 * 
	 * @param headIcon The head icon.
	 */
	public void setHeadIcon(int headIcon) {
		this.headIcon = headIcon;
	}

	/**
	 * Sets the {@link List} of this player's ignored players.
	 * 
	 * @param ignores The ignored player list.
	 */
	public void setIgnoredUsernames(List<String> ignores) {
		this.ignores = ignores;
	}

	/**
	 * Sets the last known region.
	 * 
	 * @param lastKnownRegion The last known region.
	 */
	public void setLastKnownRegion(Position lastKnownRegion) {
		this.lastKnownRegion = lastKnownRegion;
	}

	/**
	 * Changes the membership status of this player.
	 * 
	 * @param members The new membership flag.
	 */
	public void setMembers(boolean members) {
		this.members = members;
	}

	/**
	 * Sets the player's prayer icon.
	 * 
	 * @param prayerIcon The prayer icon.
	 */
	public void setPrayerIcon(int prayerIcon) {
		this.prayerIcon = prayerIcon;
	}

	/**
	 * Sets the privilege level.
	 * 
	 * @param privilegeLevel The privilege level.
	 */
	public void setPrivilegeLevel(PrivilegeLevel privilegeLevel) {
		this.privilegeLevel = privilegeLevel;
	}

	/**
	 * Sets the region changed flag.
	 * 
	 * @param regionChanged A flag indicating if the region has changed.
	 */
	public void setRegionChanged(boolean regionChanged) {
		this.regionChanged = regionChanged;
	}

	/**
	 * Sets the player's run energy.
	 * 
	 * @param runEnergy The energy.
	 */
	public void setRunEnergy(int runEnergy) {
		this.runEnergy = runEnergy;
		send(new UpdateRunEnergyEvent(runEnergy));
	}

	/**
	 * Sets the {@link ScreenBrightness} of this player.
	 * 
	 * @param brightness The screen brightness.
	 */
	public void setScreenBrightness(ScreenBrightness brightness) {
		this.screenBrightness = brightness;
	}

	/**
	 * Sets the player's {@link GameSession}.
	 * 
	 * @param session The player's {@link GameSession}.
	 * @param reconnecting The reconnecting flag.
	 */
	public void setSession(GameSession session, boolean reconnecting) {
		this.session = session;
		if (!reconnecting) {
			sendInitialEvents();
		}
		blockSet.add(SynchronizationBlock.createAppearanceBlock(this));
	}

	/**
	 * Sets the trade {@link PrivacyState}.
	 * 
	 * @param tradePrivacy The privacy state.
	 */
	public void setTradePrivacy(PrivacyState tradePrivacy) {
		this.tradePrivacy = tradePrivacy;
	}

	/**
	 * Sets whether the player is withdrawing notes from the bank.
	 * 
	 * @param withdrawingNotes Whether the player is withdrawing noted items or not.
	 */
	public void setWithdrawingNotes(boolean withdrawingNotes) {
		this.withdrawingNotes = withdrawingNotes;
	}

	@Override
	public void shout(String message, boolean chatOnly) {
		blockSet.add(SynchronizationBlock.createForceChatBlock(chatOnly ? message : '~' + message));
	}

	@Override
	public void teleport(Position position) {
		super.teleport(position);
		if (interfaceSet.size() > 0) {
			interfaceSet.close();
		}
	}

	/**
	 * Toggles whether the player is running or not.
	 */
	public void toggleRunning() {
		running = !running;
		walkingQueue.setRunningQueue(running);
		send(new ConfigEvent(173, running ? 1 : 0));
	}

	@Override
	public String toString() {
		return Player.class.getName() + " [username=" + credentials.getUsername() + ", privilege=" + privilegeLevel
				+ "]";
	}

}