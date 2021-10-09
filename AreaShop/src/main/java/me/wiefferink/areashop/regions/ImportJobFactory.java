package me.wiefferink.areashop.regions;

import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;

public interface ImportJobFactory {

    @Nonnull ImportJob createImportJob(@Nonnull CommandSender commandSender);

}
