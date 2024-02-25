package me.wiefferink.areashop.adapters.plugins.essentials;

import me.wiefferink.areashop.features.homeaccess.AccessControlValidator;
import org.checkerframework.checker.nullness.qual.NonNull;


public interface HomeListenerFactory {

    @NonNull HomeModificationListener createListener(@NonNull AccessControlValidator validator);

}
