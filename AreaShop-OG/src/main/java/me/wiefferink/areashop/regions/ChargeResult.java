package me.wiefferink.areashop.regions;

/**
 * Outcome of an asynchronous DiamondBank-OG charge, handed back to the main
 * thread to decide how a rent/buy/resell transaction finishes.
 */
record ChargeResult(boolean success, boolean lowMoney, String balance) {

    static ChargeResult ofSuccess() {

        return new ChargeResult(true, false, null);

    }

    static ChargeResult ofLowMoney(String balance) {

        return new ChargeResult(false, true, balance);

    }

    static ChargeResult ofError() {

        return new ChargeResult(false, false, null);

    }

}
