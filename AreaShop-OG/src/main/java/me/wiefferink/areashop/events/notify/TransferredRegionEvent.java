package me.wiefferink.areashop.events.notify;

import me.wiefferink.areashop.events.NotifyRegionEvent;
import me.wiefferink.areashop.regions.GeneralRegion;

import java.util.UUID;

// Broadcasted when a region has been transferred from one player to another.
public class TransferredRegionEvent extends NotifyRegionEvent<GeneralRegion> {

    private final UUID from;

    private final UUID to;

    private final boolean landlordTransfer;

    // from is the previous owner, to is the new owner.
    public TransferredRegionEvent(GeneralRegion region, UUID from, UUID to, boolean landlordTransfer) {

        super(region);
        this.from = from;
        this.to = to;
        this.landlordTransfer = landlordTransfer;

    }

    // The player the region has been transferred away from, null if it had none.
    public UUID getFromPlayer() {

        return from;

    }

    // The player the region has been transferred to.
    public UUID getToPlayer() {

        return to;

    }

    // True when the landlord changed, false when only the tenant or buyer moved.
    public boolean isLandlordTransfer() {

        return landlordTransfer;

    }

}
