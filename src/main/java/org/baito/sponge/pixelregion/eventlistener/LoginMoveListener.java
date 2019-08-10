package org.baito.sponge.pixelregion.eventlistener;

import org.baito.sponge.pixelregion.encounterdata.EncounterData;
import org.baito.sponge.pixelregion.playerdata.PlayerLink;
import org.baito.sponge.pixelregion.playerdata.PlayerLinkManager;
import org.baito.sponge.pixelregion.regions.Region;
import org.baito.sponge.pixelregion.regions.RegionManager;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.entity.MoveEntityEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.text.serializer.TextSerializers;

public class LoginMoveListener {
    public static String prefix = "&7[&dPXR&7] &6";

    @Listener
    public void onMove(MoveEntityEvent e) {
        int time = (int) e.getTargetEntity().getWorld().getProperties().getWorldTime() % 24000;
        if (e.getTargetEntity() instanceof Player && ((Player) e.getTargetEntity()).hasPermission("pixelregion.regions.change")) {
            manageRegion((Player) e.getTargetEntity());
        }
        if (e.getTargetEntity() instanceof Player && time % 20 == 0) {
            handleEncounter((Player) e.getTargetEntity());
        }
    }

    @Listener
    public void onLogin(ClientConnectionEvent.Join e) {
        manageRegion(e.getTargetEntity());
    }

    private void manageRegion(Player e) {
        String UUID = e.getUniqueId().toString();
        PlayerLink PL = PlayerLinkManager.getLink(e);
        Region R = RegionManager.getRegionInside(e);
        if (R != null) {
            if (!PL.inRegion || !R.name.equals(PL.region.name)) {
                PL.inRegion = true;
                PL.region = R;
                if (R.notifyEnter && PL.sendNotif) {
                    e.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(prefix + "Now entering >>> &f&l").toBuilder().append(R.displayName).build());
                }
            }
        } else if (PL.inRegion) {
            PL.inRegion = false;
            if (PL.region.notifyExit && PL.sendNotif) {
                e.sendMessage(TextSerializers.FORMATTING_CODE.deserialize(prefix + "Now exiting >>> &f&l").toBuilder().append(PL.region.displayName).build());
            }
            PL.region = null;
        }
    }

    private void handleEncounter(Player e) {
        if (PlayerLinkManager.getLink(e).region != null && PlayerLinkManager.getLink(e).region.encounterData != null) {
            EncounterData toUse = null;
            for (EncounterData i : PlayerLinkManager.getLink(e).region.encounterData) {
                if (Math.floor(Math.random() * 101) < i.tickChance) {
                    if (i.metConditions(e)) {
                        toUse = i;
                        break;
                    }
                }
            }
            if (toUse != null) {
                toUse.getDED().execute(e);
            }
        }
    }
}