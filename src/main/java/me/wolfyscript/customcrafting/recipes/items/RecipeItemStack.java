/*
 *       ____ _  _ ____ ___ ____ _  _ ____ ____ ____ ____ ___ _ _  _ ____
 *       |    |  | [__   |  |  | |\/| |    |__/ |__| |___  |  | |\ | | __
 *       |___ |__| ___]  |  |__| |  | |___ |  \ |  | |     |  | | \| |__]
 *
 *       CustomCrafting Recipe creation and management tool for Minecraft
 *                      Copyright (C) 2021  WolfyScript
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.wolfyscript.customcrafting.recipes.items;

import com.wolfyscript.utilities.validator.ValidationContainer;
import com.wolfyscript.utilities.validator.Validator;
import com.wolfyscript.utilities.validator.ValidatorBuilder;
import me.wolfyscript.customcrafting.utils.ItemLoader;
import me.wolfyscript.customcrafting.utils.NamespacedKeyUtils;
import me.wolfyscript.lib.com.fasterxml.jackson.annotation.JsonIgnore;
import me.wolfyscript.lib.com.fasterxml.jackson.annotation.JsonPropertyOrder;
import me.wolfyscript.utilities.api.WolfyUtilCore;
import me.wolfyscript.utilities.api.inventory.custom_items.CustomItem;
import me.wolfyscript.utilities.api.inventory.custom_items.references.APIReference;
import me.wolfyscript.utilities.api.inventory.custom_items.references.VanillaRef;
import me.wolfyscript.utilities.api.inventory.tags.CustomTag;
import me.wolfyscript.utilities.util.NamespacedKey;
import me.wolfyscript.utilities.util.inventory.InventoryUtils;
import me.wolfyscript.utilities.util.inventory.ItemUtils;
import me.wolfyscript.utilities.util.inventory.item_builder.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonPropertyOrder({"items", "tags"})
public abstract class RecipeItemStack {

    private static final String NO_ITEMS_OR_TAGS = "Must have either valid items or valid tags!";
    private static final String MISSING_THIRD_PARTY = "References a missing third-party item!";
    private static final String INVALID_TAG = "Tag '%s' could not be found!";
    private static final String INVALID_ITEM = "Item could not be loaded!";
    private static final String NULL_TAG = "Tag cannot be null!";
    private static final String NULL_ITEM = "Item cannot be null!";

    static <T extends RecipeItemStack> Validator<T> validatorFor() {
        return ValidatorBuilder.<T>object(new NamespacedKey(NamespacedKeyUtils.NAMESPACE, "recipe/abstract_itemstack")).def()
                .collection(RecipeItemStack::getItems, initStep -> initStep.def()
                        .name(container -> "Items")
                        .forEach(apiReferenceInitStep -> apiReferenceInitStep.def()
                                .validate(container -> container.value()
                                        .map(apiReference -> {
                                            if (!ItemUtils.isAirOrNull(apiReference.getLinkedItem())) {
                                                return container.update().type(ValidationContainer.ResultType.VALID);
                                            }
                                            if (apiReference instanceof VanillaRef) {
                                                return container.update().type(ValidationContainer.ResultType.INVALID).fault(INVALID_ITEM);
                                            }
                                            return container.update().type(ValidationContainer.ResultType.PENDING).fault(MISSING_THIRD_PARTY);
                                        }).orElseGet(() -> container.update().type(ValidationContainer.ResultType.INVALID).fault(NULL_ITEM))
                                ))
                        .optional()
                )
                .collection(RecipeItemStack::getTags, initStep -> initStep.def()
                        .name(container -> "Tags")
                        .forEach(tagInitStep -> tagInitStep.def()
                                .validate(container -> container.value()
                                        .map(key -> {
                                            if (key.getNamespace().equals("minecraft")) {
                                                Tag<Material> tag = Bukkit.getTag("items", org.bukkit.NamespacedKey.minecraft(key.getKey()), Material.class);
                                                if (tag != null) {
                                                    return container.update().type(ValidationContainer.ResultType.VALID).fault(String.format(INVALID_TAG, key));
                                                }
                                            } else {
                                                CustomTag<CustomItem> tag = WolfyUtilCore.getInstance().getRegistries().getItemTags().getTag(key);
                                                if (tag != null) {
                                                    return container.update().type(ValidationContainer.ResultType.VALID);
                                                }
                                            }
                                            return container.update().type(ValidationContainer.ResultType.INVALID).fault(String.format(INVALID_TAG, key));
                                        }).orElseGet(() -> container.update().type(ValidationContainer.ResultType.INVALID).fault(NULL_TAG))
                                ))
                        .optional()
                )
                .require(1) // There must be either an item or tag available
                .validate(resultValidationContainer -> {
                    if (resultValidationContainer.type() == ValidationContainer.ResultType.INVALID || resultValidationContainer.type() == ValidationContainer.ResultType.PENDING) {
                        return resultValidationContainer.update().fault(NO_ITEMS_OR_TAGS);
                    }
                    return resultValidationContainer.update();
                })
                .build();
    }

    @JsonIgnore
    protected final List<CustomItem> choices;
    private List<APIReference> items;
    private Set<NamespacedKey> tags;

    protected RecipeItemStack() {
        this(new ArrayList<>(), new LinkedHashSet<>());
    }

    protected RecipeItemStack(RecipeItemStack recipeItemStack) {
        this.choices = new ArrayList<>();
        this.items = new ArrayList<>(recipeItemStack.items);
        this.tags = new LinkedHashSet<>(recipeItemStack.tags);
        buildChoices();
    }

    protected RecipeItemStack(Material... materials) {
        this(Arrays.stream(materials).map(material -> new VanillaRef(new ItemStack(material))).collect(Collectors.toList()), new LinkedHashSet<>());
    }

    protected RecipeItemStack(ItemStack... items) {
        this(Arrays.stream(items).map(VanillaRef::new).collect(Collectors.toList()), new LinkedHashSet<>());
    }

    protected RecipeItemStack(NamespacedKey... tags) {
        this(new ArrayList<>(), new LinkedHashSet<>(Arrays.asList(tags)));
    }

    protected RecipeItemStack(APIReference... references) {
        this(Arrays.asList(references), new LinkedHashSet<>());
    }

    protected RecipeItemStack(List<APIReference> references, Set<NamespacedKey> tags) {
        this.choices = new ArrayList<>();
        this.items = references;
        this.tags = tags;
        buildChoices();
    }

    public Set<NamespacedKey> getTags() {
        return tags;
    }

    public void setTags(Set<NamespacedKey> tags) {
        this.tags = tags;
    }

    public List<APIReference> getItems() {
        return items;
    }

    public void setItems(List<APIReference> items) {
        this.items = items;
    }

    public void put(int variantSlot, CustomItem variant) {
        if (this.items.size() > variantSlot) {
            if (variant != null) {
                this.items.set(variantSlot, variant.getApiReference());
            } else {
                this.items.remove(variantSlot);
            }
        } else if (variant != null) {
            this.items.add(variant.getApiReference());
        }
    }

    public void buildChoices() {
        this.choices.clear();
        this.choices.addAll(items.stream().map(ItemLoader::load).filter(customItem -> !ItemUtils.isAirOrNull(customItem)).toList());
        this.tags.stream().map(namespacedKey -> {
            if (namespacedKey.getNamespace().equals("minecraft")) {
                Tag<Material> tag = Bukkit.getTag("items", org.bukkit.NamespacedKey.minecraft(namespacedKey.getKey()), Material.class);
                if (tag != null) {
                    return tag.getValues().stream().map(CustomItem::new).collect(Collectors.toSet());
                }
            } else {
                CustomTag<CustomItem> tag = WolfyUtilCore.getInstance().getRegistries().getItemTags().getTag(namespacedKey);
                if (tag != null) {
                    return tag.getValues();
                }
            }
            return null;
        }).filter(Objects::nonNull).distinct().forEach(this.choices::addAll);
    }

    @JsonIgnore
    public List<CustomItem> getChoices() {
        return new ArrayList<>(choices);
    }

    @JsonIgnore
    public List<CustomItem> getChoices(Player player) {
        return getChoicesStream().filter(customItem -> !customItem.hasPermission() || player.hasPermission(customItem.getPermission())).toList();
    }

    @JsonIgnore
    public Stream<CustomItem> getChoicesStream() {
        return choices.stream();
    }

    @JsonIgnore
    public List<ItemStack> getBukkitChoices() {
        return getChoicesStream().map(CustomItem::create).toList();
    }

    @JsonIgnore
    public int size() {
        return getChoices().size();
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (items.isEmpty() && tags.isEmpty()) || InventoryUtils.isCustomItemsListEmpty(this.choices);
    }

    /**
     * @return
     */
    @JsonIgnore
    public ItemStack getItemStack() {
        if (!getChoices().isEmpty()) {
            return getChoices().get(0).create();
        } else if (!getTags().isEmpty()) {
            Optional<NamespacedKey> tag = getTags().stream().findFirst();
            if (tag.isPresent()) {
                var itemBuilder = new ItemBuilder(Material.NAME_TAG);
                itemBuilder.setDisplayName("§6§lTag");
                itemBuilder.addLoreLine("§7" + tag.get());
                return itemBuilder.create();
            }
        }
        return ItemUtils.AIR;
    }

    @JsonIgnore
    public ItemStack getItemStack(int slot) {
        List<APIReference> refs = getItems();
        if (refs.size() > slot) {
            APIReference ref = refs.get(slot);
            return ref != null ? CustomItem.with(ref).create() : ItemUtils.AIR;
        }
        return ItemUtils.AIR;
    }

    @Override
    public String toString() {
        return "RecipeItemStack{" +
                "choices=" + choices +
                ", items=" + items +
                ", tags=" + tags +
                '}';
    }

    public abstract RecipeItemStack clone();
}
