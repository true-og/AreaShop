package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

import javax.annotation.Nonnull;

public class BuyRegionParser<C> implements ArgumentParser<C, BuyRegion> {

    private final IFileManager fileManager;

    public BuyRegionParser(@Nonnull IFileManager fileManager) {
        this.fileManager = fileManager;
    }

    @Override
    public @Nonnull ArgumentParseResult<BuyRegion> parse(@Nonnull CommandContext<C> commandContext,
                                                                  @Nonnull CommandInput commandInput) {
        String input = commandInput.peekString();
        BuyRegion region = this.fileManager.getBuy(input);
        if (region != null) {
            commandInput.readString();
            return ArgumentParseResult.success(region);
        }
        AreaShopCommandException exception = new AreaShopCommandException("buy-noBuyable", input);
        return ArgumentParseResult.failure(exception);
    }

    @Override
    public @Nonnull SuggestionProvider<C> suggestionProvider() {
        return SuggestionProvider.blockingStrings((ctx, input) -> {
                    String text = input.peekString();
                    return this.fileManager.getBuyNames()
                            .stream()
                            .filter(name -> name.startsWith(text))
                            .toList();
                }
        );
    }
}