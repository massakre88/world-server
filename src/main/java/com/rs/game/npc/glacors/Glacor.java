package com.rs.game.npc.glacors;

import com.rs.cores.CoresManager;
import com.rs.game.Entity;
import com.rs.game.Hit;
import com.rs.game.Hit.HitLook;
import com.rs.game.TimerBar;
import com.rs.game.World;
import com.rs.game.npc.NPC;
import com.rs.game.npc.combat.NPCCombatDefinitions;
import com.rs.game.player.Player;
import com.rs.game.player.content.combat.CombatSpell;
import com.rs.game.tasks.WorldTask;
import com.rs.game.tasks.WorldTasksManager;
import com.rs.lib.game.Animation;
import com.rs.lib.game.SpotAnim;
import com.rs.lib.game.WorldTile;
import com.rs.lib.util.Logger;
import com.rs.lib.util.Utils;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.handlers.NPCInstanceHandler;

@PluginEventHandler
public class Glacor extends NPC {

	public enum InheritedType {
		ENDURING, SAPPING, UNSTABLE;
	}

	public enum Stage {
		FIRST, MINIONS, FINAL;
	}

	private boolean minionsSpawned = false;
	private InheritedType minionType = null;
	private Stage stage = Stage.FIRST;

	public UnstableMinion unstable = null;
	public SappingMinion sapping = null;
	public EnduringMinion enduring = null;

	public boolean startedTimer = false;
	public boolean hasExploded = false;

	public NPC thisNpc = this;

	public Player lastAttacked = null;

	public Glacor(int id, WorldTile tile, boolean spawned) {
		super(id, tile, spawned);
		this.setForceMultiAttacked(true);
	}

	public boolean minionsKilled() {
		if (minionsSpawned) {
			if (unstable.defeated && sapping.defeated && enduring.defeated)
				return true;
		}
		return false;
	}

	public void resetNpcs() {
		if (unstable != null) {
			unstable.finish();
		}
		if (sapping != null) {
			sapping.finish();
		}
		if (enduring != null) {
			enduring.finish();
		}
		setStage(Stage.FIRST);
		setHitpoints(5000);
		minionsSpawned = false;
		minionType = null;
		setAttackedBy(null);
		setCapDamage(-1);
		setMinionType(null);
		startedTimer = false;
		hasExploded = false;
		lastAttacked = null;
	}

	public void deathReset() {
		if (unstable != null) {
			unstable.finish();
		}
		if (sapping != null) {
			sapping.finish();
		}
		if (enduring != null) {
			enduring.finish();
		}
		minionsSpawned = false;
		minionType = null;
		setAttackedBy(null);
		setStage(Stage.FIRST);
		setCapDamage(-1);
		setMinionType(null);
		startedTimer = false;
		hasExploded = false;
		lastAttacked = null;
	}

	@Override
	public void handlePreHit(Hit hit) {
		if (getMinionType() == InheritedType.ENDURING)
			hit.setDamage((int) (hit.getDamage() * .40));
		if (hit.getData("combatSpell") != null && hit.getData("combatSpell", CombatSpell.class).isFireSpell())
			hit.setDamage(hit.getDamage() * 2);
		if (!isMinionsSpawned() && getHitpoints() < 2500) {
			spawnMinions();
			unstable.setTarget(lastAttacked);
			sapping.setTarget(lastAttacked);
		}
		super.handlePreHit(hit);
	}

	@Override
	public void processEntity() {
		super.processEntity();

		if (stage == Stage.MINIONS && minionsKilled()) {
			setStage(Stage.FINAL);
			setCapDamage(-1);
		}

		if (lastAttacked != null && (!lastAttacked.withinDistance(this, 40) || lastAttacked.isDead())) {
			resetNpcs();
			return;
		}

		if (getMinionType() == null) {
			if (unstable == null && sapping == null && enduring == null)
				return;
			if (unstable.defeated && enduring.defeated && !sapping.defeated) {
				setMinionType(InheritedType.SAPPING);
			} else if (unstable.defeated && !enduring.defeated && sapping.defeated) {
				setMinionType(InheritedType.ENDURING);
			} else if (!unstable.defeated && enduring.defeated && sapping.defeated) {
				setMinionType(InheritedType.UNSTABLE);
			}
		}

		if (getMinionType() == InheritedType.UNSTABLE && unstable.defeated) {
			if (!startedTimer && !hasExploded) {
				getNextHitBars().add(new TimerBar(700));
				startedTimer = true;
				WorldTasksManager.schedule(new WorldTask() {
					@Override
					public void run() {
						if (thisNpc.getHitpoints() <= 0 || thisNpc.isDead())
							return;
						for (Player player : World.getPlayersInRegionRange(getRegionId())) {
							if (Utils.getDistance(thisNpc.getX(), thisNpc.getY(), player.getX(), player.getY()) < 3)
								player.applyHit(new Hit(player, player.getHitpoints() / 2, HitLook.TRUE_DAMAGE));
						}
						thisNpc.applyHit(new Hit(thisNpc, (int) (thisNpc.getHitpoints() * 0.80), HitLook.TRUE_DAMAGE));
						thisNpc.setNextSpotAnim(new SpotAnim(739));
						hasExploded = true;
					}
				}, 25);
			}
		}
	}
	
	@Override
	public void sendDeath(Entity source) {
		final NPCCombatDefinitions defs = getCombatDefinitions();
		resetWalkSteps();
		getCombat().removeTarget();
		setNextAnimation(null);
		deathReset();
		WorldTasksManager.schedule(new WorldTask() {
			int loop;

			@Override
			public void run() {
				if (loop == 0) {
					setNextAnimation(new Animation(defs.getDeathEmote()));
				} else if (loop >= defs.getDeathDelay()) {
					resetNpcs();
					drop();
					reset();
					setLocation(getRespawnTile());
					finish();
					setRespawnTask();
					stop();
				}
				loop++;
			}
		}, 0, 1);
	}
	
	@Override
	public void setRespawnTask() {
		if (!hasFinished()) {
			reset();
			setLocation(getRespawnTile());
			finish();
		}
		final NPC npc = this;
		CoresManager.schedule(new Runnable() {
			@Override
			public void run() {
				try {
					setFinished(false);
					World.addNPC(npc);
					npc.setLastRegionId(0);
					World.updateEntityRegion(npc);
					loadMapRegions();
					checkMultiArea();
				} catch (Throwable e) {
					Logger.handle(e);
				}
			}
		}, getCombatDefinitions().getRespawnDelay());
	}

	public void spawnMinions() {
		setNextAnimation(new Animation(9964));
		setNextSpotAnim(new SpotAnim(635));
		unstable = new UnstableMinion(14302, new WorldTile(this.getX() + 1, this.getY() + 1, this.getPlane()), -1, true, true, this);
		sapping = new SappingMinion(14303, new WorldTile(this.getX() + 1, this.getY(), this.getPlane()), -1, true, true, this);
		enduring = new EnduringMinion(14304, new WorldTile(this.getX() + 1, this.getY() - 1, this.getPlane()), -1, true, true, this);
		World.sendProjectile(this, unstable, 634, 60, 32, 50, 0.7, 0, 0);
		World.sendProjectile(this, sapping, 634, 60, 32, 50, 0.7, 0, 0);
		World.sendProjectile(this, enduring, 634, 60, 32, 50, 0.7, 0, 0);
		minionsSpawned = true;
		setStage(Stage.MINIONS);
		setCapDamage(0);
	}

	public InheritedType getMinionType() {
		return minionType;
	}

	public void setMinionType(InheritedType minionType) {
		this.minionType = minionType;
	}

	public boolean isMinionsSpawned() {
		return minionsSpawned;
	}

	public void setMinionsSpawned(boolean minionsSpawned) {
		this.minionsSpawned = minionsSpawned;
	}

	public Stage getStage() {
		return stage;
	}

	public void setStage(Stage stage) {
		this.stage = stage;
	}
	
	public static NPCInstanceHandler toFunc = new NPCInstanceHandler(14301) {
		@Override
		public NPC getNPC(int npcId, WorldTile tile) {
			return new Glacor(npcId, tile, false);
		}
	};
}