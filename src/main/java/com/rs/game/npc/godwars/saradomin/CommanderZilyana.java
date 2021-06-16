package com.rs.game.npc.godwars.saradomin;

import com.rs.cores.CoresManager;
import com.rs.game.npc.NPC;
import com.rs.game.npc.godwars.GodWarMinion;
import com.rs.lib.game.WorldTile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class CommanderZilyana extends NPC {

	private GodWarMinion[] minions = new GodWarMinion[3];

	public CommanderZilyana(int id, WorldTile tile, boolean spawned) {
		super(id, tile, spawned);
		setIntelligentRouteFinder(true);
		setForceFollowClose(true);
		setIgnoreDocile(true);
		minions[0] = new GodWarMinion(6248, tile.transform(4, -4), spawned);
		minions[1] = new GodWarMinion(6250, tile.transform(0, -6), spawned);
		minions[2] = new GodWarMinion(6252, tile.transform(4, 2), spawned);
	}
	
	@Override
	public void onRespawn() {
		respawnMinions();
	}
	
	public void respawnMinions() {
		CoresManager.schedule(() -> {
			for (GodWarMinion minion : minions) {
				if (minion.hasFinished() || minion.isDead())
					minion.respawn();
			}
		}, 2);
	}
	
	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(6247) {
		@Override
		public NPC getNPC(int npcId, WorldTile tile) {
			return new CommanderZilyana(npcId, tile, false);
		}
	};
}
