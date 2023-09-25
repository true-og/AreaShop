package me.wiefferink.areashop.adapters.platform.v1_20_R1;

import me.wiefferink.areashop.nms.BlockBehaviourHelper;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;

import java.util.Objects;

public class BlockBehaviourImpl implements BlockBehaviourHelper {

    @Override
    public boolean canPlace(Location location, BlockData blockData) {
        Objects.requireNonNull(location.getWorld(), "Null World!");
        return location.getBlock().canPlace(blockData);
    }

    @Override
    public boolean isBlockValid(Block block) {
        return block.canPlace(block.getBlockData());
    }

    @Override
    public boolean isBlockStateValid(BlockState blockState) {
        return blockState.getBlock().canPlace(blockState.getBlockData());
    }
}