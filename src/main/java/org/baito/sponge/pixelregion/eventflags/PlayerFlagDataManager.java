package org.baito.sponge.pixelregion.eventflags;

import org.apache.commons.io.FilenameUtils;
import org.baito.sponge.pixelregion.Config;
import org.spongepowered.api.entity.living.player.Player;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerFlagDataManager {
    public static Map<String, PlayerFlagData> data = new HashMap<>();

    public static PlayerFlagData getOrCreateData(Player p) {
        Path direc = Config.pxrDir.resolve("events").resolve("playerdata");
        if (data.get(p.getUniqueId() + "") != null) {
            return data.get(p.getUniqueId()+"");
        }
        File file = new File(direc.resolve(p.getUniqueId() + ".json").toString());
        try {
            file.createNewFile();
            PlayerFlagData pfd = new PlayerFlagData(p);
            PrintWriter pw = new PrintWriter(file);
            pw.print(pfd.toJSON().toString(4));
            pw.close();
            data.put(p.getUniqueId()+"", pfd);
            return pfd;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static File getFile(UUID uuid) {
        Path direc = Config.pxrDir.resolve("events").resolve("playerdata");
        for (int i = 0; i < direc.toFile().listFiles().length; i++) {
            if (FilenameUtils.removeExtension(direc.toFile().listFiles()[i].getName()).equals(uuid.toString())) {
                return direc.toFile().listFiles()[i];
            }
        }
        return null;
    }

    public static void generateData(File[] f) {
        data.clear();
        for (int i = 0; i < f.length; i++) {
            PlayerFlagData pfd = new PlayerFlagData(Config.readConfig(f[i]));
            data.put(pfd.uuid + "", pfd);
        }
    }

    public static void save() {
        for (PlayerFlagData i : data.values()) {
            i.save();
        }
    }
}
