package me.wiefferink.areashop.features.signs;


import javax.annotation.Nonnull;

public interface SignFactory {

    @Nonnull RegionSign createRegionSign(@Nonnull SignsFeature signsFeature, @Nonnull String key);

}
