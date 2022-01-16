package me.wiefferink.areashop.nms.v1_18_R1;

import me.wiefferink.areashop.nms.BlockBehaviourHelper;
import me.wiefferink.areashop.nms.NMS;

public class NMSImpl implements NMS {

    private final BlockBehaviourImpl blockBehaviourHelper = new BlockBehaviourImpl();

    @Override
    public BlockBehaviourHelper blockBehaviourHelper() {
        return this.blockBehaviourHelper;
    }

}
