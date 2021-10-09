package me.wiefferink.areashop.regions;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;

public class RegionModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new FactoryModuleBuilder().build(RegionFactory.class));
    }
}
