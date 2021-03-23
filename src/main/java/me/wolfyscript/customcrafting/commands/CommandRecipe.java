package me.wolfyscript.customcrafting.commands;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.customcrafting.commands.recipes.DeleteSubCommand;
import me.wolfyscript.customcrafting.commands.recipes.EditSubCommand;
import me.wolfyscript.customcrafting.commands.recipes.SaveSubCommand;
import me.wolfyscript.customcrafting.commands.recipes.ToggleSubCommand;
import me.wolfyscript.customcrafting.configs.recipebook.Categories;
import me.wolfyscript.customcrafting.utils.ChatUtils;
import me.wolfyscript.utilities.api.WolfyUtilities;
import me.wolfyscript.utilities.api.inventory.gui.InventoryAPI;
import me.wolfyscript.utilities.util.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CommandRecipe extends IndexCommand {

    private final CustomCrafting customCrafting;

    public CommandRecipe(CustomCrafting customCrafting) {
        super("recipes", "", "/recipes", new ArrayList<>());
        this.customCrafting = customCrafting;
        registerSubCommand(new EditSubCommand(customCrafting));
        registerSubCommand(new DeleteSubCommand(customCrafting));
        registerSubCommand(new ToggleSubCommand(customCrafting));
        registerSubCommand(new SaveSubCommand(customCrafting));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0 && sender instanceof Player) {
            WolfyUtilities api = CustomCrafting.inst().getApi();
            Player p = (Player) sender;
            InventoryAPI<?> invAPI = api.getInventoryAPI();
            if (ChatUtils.checkPerm(p, "customcrafting.cmd.recipes")) {
                Categories categories = customCrafting.getDataHandler().getCategories();
                if (categories.getSortedMainCategories().size() > 1) {
                    invAPI.openCluster(p, "recipe_book");
                } else {
                    invAPI.openGui(p, new NamespacedKey("recipe_book", "recipe_book"));
                }
            }
        }
        return super.execute(sender, s, args);
    }
}
