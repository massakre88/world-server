package com.rs.game.content.holidayevents.easter.easter22;

import com.rs.game.World;
import com.rs.game.content.dialogue.Dialogue;
import com.rs.game.content.dialogue.HeadE;
import com.rs.game.content.dialogue.Options;
import com.rs.game.content.holidayevents.easter.easter22.EggHunt.Spawns;
import com.rs.game.content.holidayevents.easter.easter22.npcs.EasterChick;
import com.rs.game.model.entity.Entity;
import com.rs.game.model.entity.npc.NPC;
import com.rs.game.model.entity.player.Equipment;
import com.rs.game.model.entity.player.Player;
import com.rs.game.model.entity.player.interactions.PlayerEntityInteraction;
import com.rs.game.tasks.WorldTasks;
import com.rs.lib.game.Animation;
import com.rs.lib.game.SpotAnim;
import com.rs.lib.game.WorldTile;
import com.rs.plugin.annotations.PluginEventHandler;
import com.rs.plugin.events.ItemEquipEvent;
import com.rs.plugin.events.LoginEvent;
import com.rs.plugin.events.NPCClickEvent;
import com.rs.plugin.events.ObjectClickEvent;
import com.rs.plugin.events.PlayerClickEvent;
import com.rs.plugin.handlers.ItemEquipHandler;
import com.rs.plugin.handlers.LoginHandler;
import com.rs.plugin.handlers.NPCClickHandler;
import com.rs.plugin.handlers.ObjectClickHandler;
import com.rs.plugin.handlers.PlayerClickHandler;

@PluginEventHandler
public class EggsterminatorInteraction extends PlayerEntityInteraction {

    public EggsterminatorInteraction(Entity target) {
        super(target, 7);
    }

    @Override
    public void interact(Player player) {
        player.setNextFaceWorldTile(target.getTile());
        int attackStyle = player.getCombatDefinitions().getAttackStyleId();
        int delay = World.sendProjectile(player.getTile(), target.getTile(), (attackStyle == 0 ? 3034 : 3035), 20, 10, 10, 1, 0, 0).getTaskDelay();

        if (target instanceof NPC) {
            NPC npc = ((NPC) target);
            player.setNextAnimation(new Animation(12174));
            player.setNextSpotAnim(new SpotAnim(2138));
            WorldTasks.schedule(delay, () -> {
            	target.sendDeath(player);
            });
        }
    }

    @Override
    public boolean canStart(Player player) { return true; }

    @Override
    public boolean checkAll(Player player) { return true; }

    @Override
    public void onStop(Player player) {}

    public static PlayerClickHandler handlePlayerSplatter = new PlayerClickHandler(false, "Splatter") {
        @Override
        public void handle(PlayerClickEvent e) {
            e.getPlayer().getInteractionManager().setInteraction(new EggsterminatorInteraction(e.getTarget()));
        }
    };
    
    public static NPCClickHandler handleNPCSplatter = new NPCClickHandler(new Object[] { Easter2022.CHOCOCHICK, Easter2022.CHICK }, new String[] { "Splatter" }) {
		@Override
		public void handle(NPCClickEvent e) {
            e.getPlayer().getInteractionManager().setInteraction(new EggsterminatorInteraction(e.getNPC()));
		}
    };


    public static ItemEquipHandler handleEggsterminatorWield = new ItemEquipHandler(Easter2022.EGGSTERMINATOR, Easter2022.PERMANENT_EGGSTERMINATOR) {
        @Override
        public void handle(ItemEquipEvent e) {
            e.getPlayer().setPlayerOption(e.equip() ? "Splatter" : "null", 8, true);
            if (e.dequip() && e.getItem().getId() == Easter2022.EGGSTERMINATOR) {
                e.getPlayer().sendOptionDialogue("Destroy the Eggsterminator?", options -> {
                    options.add("Yes", () -> {
                        e.getPlayer().getEquipment().deleteItem(Easter2022.EGGSTERMINATOR, 1);
                        e.getPlayer().getEquipment().refresh(Equipment.WEAPON);
                        e.getPlayer().getAppearance().generateAppearanceData();
                    });
                    options.add("No");
                });
                return;
            }
            if (Easter2022.ENABLED)
                e.getPlayer().sendMessage("Using the Combat Styles menu, you can choose whether to fire marshmallows (in support of the Chocatrice) or scotch-eggs (in support of the Evil Chicken).");
        }
    };

    public static LoginHandler removeTempEggsterminator = new LoginHandler() {
        @Override
        public void handle(LoginEvent e) {
            if (Easter2022.ENABLED)
                return;
            if (e.getPlayer().getEquipment().getWeaponId() == Easter2022.EGGSTERMINATOR) {
                e.getPlayer().getEquipment().deleteItem(Easter2022.EGGSTERMINATOR, 1);
                e.getPlayer().getEquipment().refresh(Equipment.WEAPON);
                e.getPlayer().getAppearance().generateAppearanceData();
                e.getPlayer().sendMessage("Your Eggsterminator has vanished. Start a hunt to obtain a new one and even unlock an enchanted permanent version.");
            }
        }
    };
}
