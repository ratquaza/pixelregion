package org.baito.sponge.pixelregion.encounterdata.external;

import org.baito.sponge.pixelregion.encounterdata.EncounterData;
import org.baito.sponge.pixelregion.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongepowered.api.block.BlockState;

public class ExternalEncounterData {
    public String name;
    public int chance;
    public ExternalConditions[] conditions;
    public EncounterData.Encounters encounterData;

    public ExternalEncounterData(JSONObject j) {
        try {
            if (!j.has("name")) {
                throw new NullPointerException("An external encounter data has no name! Skipping...");
            }
            name = j.getString("name");
            if (!j.has("chance")) {
                throw new NullPointerException("External encounter data \"" + name + "\" has no chance! Skipping...");
            }
            chance = j.getInt("chance");
            if (!j.has("encounters")) {
                throw new NullPointerException("Extenral encounter data \"" + name + "\" has no encounters! Skipping...");
            }
            if (j.has("conditions")) {
                conditions = new ExternalConditions[j.getJSONArray("conditions").length()];
                for (int i = 0; i < conditions.length; i++) {
                    conditions[i] = new ExternalConditions(j.getJSONArray("conditions").getJSONObject(i), name);
                }
            } else {
                conditions = null;
            }
            encounterData = new EncounterData.Encounters(j.getJSONObject("encounters"), name);
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public EncounterData.Encounters.DeepEncounterData getDED() {
        double totalWeight = 0;
        for (EncounterData.Encounters.DeepEncounterData i : encounterData.deepEncounters) {
            totalWeight += i.weight;
        }
        double chosenWeight = Math.random() * totalWeight;
        for (EncounterData.Encounters.DeepEncounterData i : encounterData.deepEncounters) {
            chosenWeight -= i.weight;
            if (chosenWeight <= 0.0) return i;
        }
        return null;
    }

    public class ExternalConditions {
        public String type;
        public String[] weather = null;
        public int[] time = null;
        public BlockState[] blocks = null;
        public boolean useVar;

        ExternalConditions(JSONObject j, String name) {
            try {
                if (!j.has("type")) {
                    throw new NullPointerException("Encounter data " + name + " is missing a condition type! Skipping...");
                }
                type = j.getString("type");
                switch (type) {
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
                    case "blocks":
                        if (!j.has("blocks")) {
                            throw new NullPointerException("Encounter data " + name + " has no \"blocks\" array for condition blocks! Skipping...");
                        }
                        blocks = new BlockState[j.getJSONArray("blocks").length()];
                        for (int i = 0; i < blocks.length; i++) {
                            blocks[i] = Utils.stringToBlock(j.getJSONArray("blocks").getString(i));
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
                case "blocks":
                    String[] ret = new String[blocks.length];
                    for (int i = 0; i < ret.length; i++) {
                        ret[i] = blocks[i].getType().getName();
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
                case "blocks":
                    return "Blocks";
            }
            return null;
        }
    }
}
