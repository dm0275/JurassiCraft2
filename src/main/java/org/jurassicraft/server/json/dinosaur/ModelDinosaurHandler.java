package org.jurassicraft.server.json.dinosaur;

import org.jurassicraft.JurassiCraft;
import org.jurassicraft.server.json.dinosaur.model.JsonAnimator;
import org.jurassicraft.server.json.dinosaur.model.JsonDinosaurModel;
import org.jurassicraft.server.json.dinosaur.model.objects.Constants;
import org.jurassicraft.server.json.dinosaur.model.objects.JsonAnimationType;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = JurassiCraft.MODID, value = Side.CLIENT)
public class ModelDinosaurHandler {

	public static final Gson GSON = new GsonBuilder()
			
			//Model and animator
			.registerTypeAdapter(Constants.class, new Constants.Deserializer())
			.registerTypeAdapter(JsonDinosaurModel.class, new JsonDinosaurModel.Deserializer())
			.registerTypeAdapter(JsonAnimator.class, new JsonAnimator.Deserializer())
			.registerTypeAdapter(JsonAnimationType.class, new JsonAnimationType.Deserializer()).create();
}
