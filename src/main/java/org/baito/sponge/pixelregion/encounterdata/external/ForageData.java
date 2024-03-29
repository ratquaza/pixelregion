package org.baito.sponge.pixelregion.encounterdata.external;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import org.baito.sponge.pixelregion.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.spongepowered.api.block.BlockState;

public class ForageData {
    public String name;
    public int chance;
    public ForageConditions[] conditions;
    public ForageItems[] items;

    private boolean contains(String[] arr, String s) {
        for (String i : arr) {
            if (i.equals(s)) return true;
        }
        return false;
    }

    public ForageItems getForageItem() {
        double totalWeight = 0;
        for (ForageItems i : items) {
            totalWeight += i.weight;
        }
        double chosenWeight = Math.random() * totalWeight;
        for (ForageItems i : items) {
            chosenWeight -= i.weight;
            if (chosenWeight <= 0.0) return i;
        }
        return null;
    }

    public ForageData(JSONObject j) {
        try {
            if (!j.has("name")) {
                throw new NullPointerException("A forage data has no name! Skipping...");
            }
            name = j.getString("name");
            if (!j.has("chance")) {
                throw new NullPointerException("Forage data \"" + name + "\" has no chance! Skipping...");
            }
            chance = j.getInt("chance");
            if (j.has("conditions")) {
                conditions = new ForageConditions[j.getJSONArray("conditions").length()];
                for (int i = 0; i < conditions.length; i++) {
                    conditions[i] = new ForageConditions(j.getJSONArray("conditions").getJSONObject(i), name);
                }
            } else {
                conditions = null;
            }
            if (!j.has("loot")) {
                throw new NullPointerException("Forage data \"" + name + "\" has no loot array! Skipping...");
            }
            items = new ForageItems[j.getJSONArray("loot").length()];
            for (int i = 0; i < j.getJSONArray("loot").length(); i++) {
                items[i] = new ForageItems(j.getJSONArray("loot").getJSONObject(i), name);
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public class ForageConditions {
        public String type;
        public String[] weather = null;
        public int[] time = null;
        public BlockState[] blocks = null;
        public boolean useVar;
        public String[] types = null;

        ForageConditions(JSONObject j, String name) {
            try {
                if (!j.has("type")) {
                    throw new NullPointerException("Forage data " + name + " is missing a condition type! Skipping...");
                }
                type = j.getString("type");
                switch (j.getString("type")) {
                    case "weather":
                        if (!j.has("weather")) {
                            throw new NullPointerException("Forage data " + name + " has no \"weather\" array for condition weather! Skipping...");
                        }
                        weather = toArray(j.getJSONArray("weather"));
                        break;
                    case "time":
                        if (!j.has("times")) {
                            throw new NullPointerException("Forage data " + name + " has no \"times\" array for condition time! Skipping...");
                        }
                        time = new int[2];
                        time[0] = j.getJSONArray("times").getInt(0);
                        time[1] = j.getJSONArray("times").getInt(1);
                        break;
                    case "blocks":
                        if (!j.has("blocks")) {
                            throw new NullPointerException("Forage data " + name + " has no \"blocks\" array for condition blocks! Skipping...");
                        }
                        blocks = new BlockState[j.getJSONArray("blocks").length()];
                        for (int i = 0; i < blocks.length; i++) {
                            blocks[i] = Utils.stringToBlock(j.getJSONArray("blocks").getString(i));
                        }
                        useVar = j.has("useVariant") && j.getBoolean("useVariant");
                        break;
                    case "types":
                        if (!j.has("types")) {
                            throw new NullPointerException("Forage data " + name + " has no \"types\" array for condition types! Skipping...");
                        }
                        types = toArray(j.getJSONArray("types"));
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
    }

    public class ForageItems {
        public ItemStack item;
        public NBTTagCompound nbt;
        int quantity;
        public double weight;

        ForageItems(JSONObject j, String name) {
            if (Item.getByNameOrId(j.getString("item")) == null) {
                throw new NullPointerException("An item in forage data \"" + name + "\" is incorrect! Skipping...");
            } else {
                try {
                    quantity = j.has("quantity") ? j.getInt("quantity") : 1;
                    item = new ItemStack(Item.getByNameOrId(j.getString("item")), quantity);
                    nbt = j.has("nbt") ? JsonToNBT.getTagFromJson(j.getString("nbt")) : null;
                    if (nbt != null) {
                        item.setTagCompound(nbt);
                    }
                    weight = j.getDouble("weight");
                } catch (NBTException e) {
                    e.printStackTrace();;
                }
            }
        }
    }
}
