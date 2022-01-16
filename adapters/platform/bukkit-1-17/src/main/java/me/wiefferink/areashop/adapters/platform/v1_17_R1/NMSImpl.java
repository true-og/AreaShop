package me.wiefferink.areashop.adapters.platform.v1_17_R1;

import me.wiefferink.areashop.nms.BlockBehaviourHelper;
import me.wiefferink.areashop.nms.NMS;

public class NMSImpl implements NMS {

    private final BlockBehaviourImpl blockBehaviourHelper = new BlockBehaviourImpl();

    @Override
    public BlockBehaviourHelper blockBehaviourHelper() {
        return this.blockBehaviourHelper;
    }

}
