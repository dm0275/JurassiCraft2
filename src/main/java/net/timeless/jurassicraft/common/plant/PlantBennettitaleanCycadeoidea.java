package net.timeless.jurassicraft.common.plant;

import net.minecraft.block.Block;
import net.timeless.jurassicraft.common.block.JCBlockRegistry;

public class PlantBennettitaleanCycadeoidea extends Plant
{
    @Override
    public EnumPlantType getPlantType()
    {
        return EnumPlantType.FERN;
    }

    @Override
    public String getName()
    {
        return "Bennettitalean Cycadeoidea";
    }

    @Override
    public Block getBlock()
    {
        return JCBlockRegistry.bennettitalean_cycadeoidea;
    }
}
