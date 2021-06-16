package com.rs.game.npc.others;

import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.npc.NPC;
import com.rs.game.player.Player;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.lib.game.Animation;
import com.rs.lib.game.WorldTile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class PolyporeNPC extends NPC {

	private int realId;

	public PolyporeNPC(int id, WorldTile tile, boolean spawned) {
		super(id, tile, spawned);
		realId = id;
		setRandomWalk(true);
		if (id == 14698)
			setCantFollowUnderCombat(id == 14698);

	}

	public static void useStairs(final Player player, WorldTile tile, final boolean down) {
		player.useStairs(down ? 15458 : 15456, tile, 2, 3); // TODO find correct
		// emote
		WorldTasksManager.schedule(new WorldTask() {
			@Override
			public void run() {
				player.setNextAnimation(new Animation(down ? 15459 : 15457));
			}
		}, 1);
	}

	@Override
	public void reset() {
		setNPC(realId);
		super.reset();
	}

	public boolean canInfect() {
		return realId == getId();
	}

	@Override
	public void handlePreHit(final Hit hit) {
		if (hit.getLook() == HitLook.MELEE_DAMAGE || hit.getLook() == HitLook.RANGE_DAMAGE)
			hit.setDamage(hit.getDamage() / 5);
		super.handlePreHit(hit);
	}

	public int getInfectEmote() {
		switch (realId) {
		case 14688:
			return 15484;
		case 14690:
			return 15507;
		case 14692:
			return 15514;
		case 14696:
			return 15466;
		case 14698:
			return 15477;
		case 14700:
			return 15492;
		default:
			return -1;
		}
	}
	
	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(14688, 14689, 14690, 14691, 14692, 14693, 14696, 14697, 14698, 14699, 14700, 14701) {
		@Override
		public NPC getNPC(int npcId, WorldTile tile) {
			return new PolyporeNPC(npcId, tile, false);
		}
	};
}