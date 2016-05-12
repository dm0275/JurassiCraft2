package org.jurassicraft.server.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

import java.util.Random;

public class JurassicPortalBlock extends BlockPortal
{

    public JurassicPortalBlock()
    {
        super();
    }

    @Override
    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {

    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state)
    {
        return null;
    }

    @Override
    public boolean isFullCube()
    {
        return false;
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock)
    {

        byte b = 0;
        byte b1 = 1;

        if (world.getBlockState(new BlockPos(pos.getX() - 1, pos.getY(), pos.getZ())).getBlock() == BlockHandler.INSTANCE.gypsum_bricks || world.getBlockState(new BlockPos(pos.getX() + 1, pos.getY(), pos.getZ())).getBlock() == BlockHandler.INSTANCE.gypsum_bricks)
        {
        }

        int block;

        for (block = pos.getY(); world.getBlockState(new BlockPos(pos.getX(), block - 1, pos.getZ())).getBlock() == this; --block)
        {
        }

        if (world.getBlockState(new BlockPos(pos.getX(), pos.getY() - 1, pos.getZ())).getBlock() != BlockHandler.INSTANCE.gypsum_bricks)
        {
            world.setBlockToAir(pos);
        }
        else
        {

        }
    }

}