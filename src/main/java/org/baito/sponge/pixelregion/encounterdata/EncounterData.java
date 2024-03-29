package org.baito.sponge.pixelregion.encounterdata;

import com.pixelmonmod.pixelmon.Pixelmon;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.pokemon.PokemonSpec;
import com.pixelmonmod.pixelmon.api.storage.StoragePosition;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.WildPixelmonParticipant;
import com.pixelmonmod.pixelmon.config.PixelmonConfig;
import com.pixelmonmod.pixelmon.entities.pixelmon.EntityPixelmon;
import com.pixelmonmod.pixelmon.enums.EnumBossMode;
import com.pixelmonmod.pixelmon.listener.RepelHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import org.baito.sponge.pixelregion.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.entity.living.player.Player;

public class EncounterData {
    public String name;
    public int tickChance;
    public Conditions[] conditions;
    public Encounters encounterData;

    EncounterData(JSONObject j) {
        try {
            if (!j.has("name")) {
                throw new NullPointerException("An encounter data has no name! Skipping...");
            }
            name = j.getString("name");
            if (!j.has("tickChance")) {
                throw new NullPointerException("Encounter data \"" + name + "\" has no tick chance! Skipping...");
            }
            tickChance = j.getInt("tickChance");
            if (j.has("conditions")) {
                conditions = new Conditions[j.getJSONArray("conditions").length()];
                for (int i = 0; i < conditions.length; i++) {
                    conditions[i] = new Conditions(j.getJSONArray("conditions").getJSONObject(i), name);
                }
            } else {
                conditions = null;
            }
            if (!j.has("encounters")) {
                throw new NullPointerException("Encounter data \"" + name + "\" has no encounters! Skipping...");
            }
            encounterData = new Encounters(j.getJSONObject("encounters"), name);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public Encounters.DeepEncounterData getDED() {
        double totalWeight = 0;
        for (Encounters.DeepEncounterData i : encounterData.deepEncounters) {
            totalWeight += i.weight;
        }
        double chosenWeight = Math.random() * totalWeight;
        for (Encounters.DeepEncounterData i : encounterData.deepEncounters) {
            chosenWeight -= i.weight;
            if (chosenWeight <= 0.0) return i;
        }
        return null;
    }

    public class Conditions {
        public String type;
        public String[] weather = null;
        public int[] time = null;
        public BlockState[] ontop = null;
        public BlockState[] inside = null;
        public boolean useVar;

        Conditions(JSONObject j, String name) {
            try {
                if (!j.has("type")) {
                    throw new NullPointerException("Encounter data " + name + " is missing a condition type! Skipping...");
                }
                type = j.getString("type");
                switch (j.getString("type")) {
                    case "weather":
                        if (!j.has("weather")) {
                            throw new NullPointerException("Encounter data " + name + " has no \"weather\" array for condition weather! Skipping...");
                        }
                        weather = toArray(j.getJSONArray("weather"));
                        break;
                    case "time":
                        if (!j.has("times")) {
                            throw new NullPointerException("Encounter data " + name + " has no \"times\" array for condition time! Skipping...");
                        }
                        time = new int[2];
                        time[0] = j.getJSONArray("times").getInt(0);
                        time[1] = j.getJSONArray("times").getInt(1);
                        break;
                    case "ontop":
                        if (!j.has("blocks")) {
                            throw new NullPointerException("Encounter data " + name + " has no \"blocks\" array for condition ontop! Skipping...");
                        }
                        ontop = new BlockState[j.getJSONArray("blocks").length()];
                        for (int i = 0; i < ontop.length; i++) {
                            ontop[i] = Utils.stringToBlock(j.getJSONArray("blocks").getString(i));
                        }
                        useVar = j.has("useVariant") && j.getBoolean("useVariant");
                        break;
                    case "inside":
                        if (!j.has("blocks")) {
                            throw new NullPointerException("Encounter data " + name + " has no \"blocks\" array for condition inside! Skipping...");
                        }
                        inside = new BlockState[j.getJSONArray("blocks").length()];
                        for (int i = 0; i < inside.length; i++) {
                            inside[i] = Utils.stringToBlock(j.getJSONArray("blocks").getString(i));
                        }
                        useVar = j.has("useVariant") && j.getBoolean("useVariant");
                        break;
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        private String[] toArray(JSONArray a) {
            String[] returner = new String[a.length()];
            for (int i = 0; i < returner.length; i++) {
                returner[i] = a.getString(i);
            }
            return returner;
        }

        public String[] getDataPrint() {
            switch (type) {
                case "weather":
                    return weather.clone();
                case "time":
                    String[] r = {time[0] + "", time[1] + ""};
                    return r;
                case "ontop":
                    String[] ret = new String[ontop.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = ontop[i].getType().getName();
                    }
                    return ret;
                case "inside":
                    ret = new String[inside.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = inside[i].getType().getName();
                    }
                    return ret;
            }
            return null;
        }

        public String getTypePrint() {
            switch (type) {
                case "weather":
                    return "Weather";
                case "time":
                    return "Time";
                case "ontop":
                    return "Ontop";
                case "inside":
                    return "Inside";
            }
            return null;
        }
    }

    public static class Encounters {
        public int[] defaultLevels = new int[2];
        public int defaultShiny;
        public int defaultBoss;
        public DeepEncounterData[] deepEncounters;

        public Encounters(JSONObject j, String name) {
            try {
                if (!j.has("levelMin") || !j.has("levelMax")) {
                    throw new NullPointerException("Encounter data " + name + " has no global default levels! Skipping...");
                }
                defaultLevels[0] = Math.max(j.getInt("levelMin"), 1);
                defaultLevels[1] = Math.min(j.getInt("levelMax"), 100);
                if (!j.has("shinyChance")) {
                    defaultShiny = (int) PixelmonConfig.shinyRate;
                } else {
                    defaultShiny =j.getInt("shinyChance");
                }
                if (!j.has("bossChance")) {
                    defaultBoss = (int) PixelmonConfig.bossSpawnChance;
                } else {
                    defaultBoss = j.getInt("bossChance");
                }
                if (!j.has("pokemon")) {
                    throw new NullPointerException("Encounter data " + name + " has no Pokemon encounters! Skipping...");
                }
                deepEncounters = new DeepEncounterData[j.getJSONArray("pokemon").length()];
                for (int i = 0; i < deepEncounters.length; i++) {
                    deepEncounters[i] = new DeepEncounterData(j.getJSONArray("pokemon").getJSONObject(i), this, name);
                }
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        public class DeepEncounterData {
            public String[] species;
            public double weight;
            public int[] deepLevels = new int[2];
            public int deepShiny;
            public int deepBoss;

            DeepEncounterData(JSONObject j, Encounters def, String name) {
                try {
                    if (!j.has("species")) {
                        throw new NullPointerException("Encounter data " + name + " has no species in a Pokemon field! Skipping...");
                    }
                    species = toArray(j.getJSONArray("species"));
                    if (!j.has("weight")) {
                        throw new NullPointerException("Encounter data " + name + " has no weight in a Pokemon field! Skipping...");
                    }
                    weight = (double) j.getNumber("weight");
                    deepLevels[0] = j.has("levelMin") ? j.getInt("levelMin") : def.defaultLevels[0];
                    deepLevels[1] = j.has("levelMax") ? j.getInt("levelMax") : def.defaultLevels[1];
                    deepShiny = j.has("shinyChance") ? j.getInt("shinyChance") : def.defaultShiny;
                    deepBoss = j.has("bossChance") ? j.getInt("bossChance") : def.defaultBoss;
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

            private String[] toArray(JSONArray a) {
                String[] r = new String[a.length()];
                for (int i = 0; i < a.length(); i++) {
                    r[i] = a.getString(i);
                }
                return r;
            }

            public void execute(Player plr) {
                if (BattleRegistry.getBattle((EntityPlayerMP)plr) != null) {
                    return;
                }
                int lvl = ((int) (Math.floor(Math.random() * (deepLevels[1] - deepLevels[0])) + deepLevels[0]) + 1);
                if (RepelHandler.hasRepel(((EntityPlayerMP)plr))) {
                    Pokemon slotOne = Pixelmon.storageManager.getPokemon((EntityPlayerMP) plr, new StoragePosition(-1, 0));
                    if (slotOne != null && slotOne.getLevel() >= lvl) {
                        return;
                    }
                }
                StringBuilder sb = new StringBuilder();
                sb.append(species[(int) Math.floor(Math.random() * species.length)]);
                sb.append(" lvl:" + lvl);
                int shinyBonus = 0;
                if (Pixelmon.storageManager.getParty(plr.getUniqueId()).getShinyCharm().isActive()) {
                    shinyBonus = 3;
                }
                if (Math.floor(Math.random() * Math.max(deepShiny - shinyBonus, 1)) == 0) {
                    sb.append(" s");
                }
                if (Math.floor(Math.random() * deepBoss) == 0) {
                    startBattle(plr, sb.toString(), EnumBossMode.getRandomMode());
                } else {
                    startBattle(plr, sb.toString(), EnumBossMode.NotBoss);
                }
            }

            public boolean startBattle(Player plr, String pokemonSpec, EnumBossMode b) {
                EntityPlayerMP source = (EntityPlayerMP)plr;
                if (Pixelmon.storageManager.getParty(source.getUniqueID()).getAndSendOutFirstAblePokemon(source) != null) {
                    PlayerParticipant pp = new PlayerParticipant(source, Pixelmon.storageManager.
                            getParty(source.getUniqueID()).getAndSendOutFirstAblePokemon(source));
                    EntityPixelmon pos = Pixelmon.pokemonFactory.create(PokemonSpec.from(pokemonSpec.split(" "))).
                            getOrSpawnPixelmon(source.getEntityWorld(), source.getPosition().getX(),
                                    source.getPosition().getY(), source.getPosition().getZ());
                    pos.setBoss(b);
                    BattleRegistry.startBattle(pp, new WildPixelmonParticipant(pos));
                    return true;
                }
                return false;
            }
        }
    }

    public String info() {
        StringBuilder s = new StringBuilder();
        s.append("  &a= Encounter chance: &f").append(tickChance).append("%");
        s.append("\n  &a= Conditions:");
        for (int cond = 0; cond < conditions.length; cond++) {
            s.append("\n    &b= ").append(conditions[cond].getTypePrint()).append(": ");
            s.append("\n      &3- ");
            for (int dp = 0; dp < conditions[cond].getDataPrint().length; dp++) {
                String dps = conditions[cond].getDataPrint()[dp];
                if (dp == conditions[cond].getDataPrint().length - 1) {
                    s.append("&f").append(dps);
                } else {
                    s.append("&f").append(dps).append(", ");
                }
            }
        }
        for (int pl = 0; pl < encounterData.deepEncounters.length; pl++) {
            s.append("\n  &a= Pokemon: ");
            for (int pkmn = 0; pkmn < encounterData.deepEncounters[pl].species.length; pkmn++) {
                String pokemon = encounterData.deepEncounters[pl].species[pkmn];
                if (pkmn == encounterData.deepEncounters[pl].species.length-1) {
                    s.append("&f").append(pokemon);
                } else {
                    s.append("&f").append(pokemon).append(", ");
                }
            }
            Encounters.DeepEncounterData dd = encounterData.deepEncounters[pl];
            s.append("\n    &b= Level Range: &f").append(dd.deepLevels[0]).append(" - ").append(dd.deepLevels[1]);
            s.append("\n    &b= Weighting: &f").append(dd.weight);
        }
        return s.toString();
    }
}
