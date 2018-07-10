package org.jurassicraft.server.json;

import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jurassicraft.JurassiCraft;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Stream;

public class JsonUtil {

    public static <T extends IForgeRegistryEntry.Impl<T>> void getAllRegister(IForgeRegistry<T> registry, Gson gson, String folderName) {
        Loader.instance().getIndexedModList().forEach((s, mod) -> {
            Loader.instance().setActiveModContainer(mod);
            CraftingHelper.findFiles(mod, "assets/" + mod.getModId() + "/jurassicraft/" + folderName, null,
                    (root, file) -> {
                        if (!"json".equals(FilenameUtils.getExtension(file.toString()))) {
                            return true;
                        }
                        String relative = root.relativize(file).toString();
                        ResourceLocation key = new ResourceLocation(mod.getModId(), FilenameUtils.removeExtension(relative).replaceAll("\\\\", "/"));
                        BufferedReader reader = null;
                        try {
                            reader = Files.newBufferedReader(file);
                            T value = JsonUtils.fromJson(gson, reader, registry.getRegistrySuperType());
                           if(value == null) {
                               return false;
                           } else {
                               registry.register(value.setRegistryName(key));
                           }
                        }
                        catch (JsonParseException e) {
                            JurassiCraft.getLogger().error("Parsing error loading json: " + key, e);
                            return false;
                        }
                        catch (IOException e) {
                            JurassiCraft.getLogger().error("Couldn't read json " + key + " from " + file, e);
                            return false;
                        }
                        finally {
                            IOUtils.closeQuietly(reader);
                        }
                        return true;
                    }, true, true);
        });
        Loader.instance().setActiveModContainer(Loader.instance().getIndexedModList().get(JurassiCraft.MODID));
    }

    public static <T> List<T> deserializeArray(JsonArray array, JsonDeserializationContext context, Class<T> clazz) {
        List<T> list = Lists.newArrayList();
        for (JsonElement jsonElement : array) {
            list.add(context.deserialize(JsonUtils.getJsonObject(jsonElement, ""), clazz));
        }
        return list;
    }
}