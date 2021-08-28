package me.wolfyscript.customcrafting.recipes;

import me.wolfyscript.customcrafting.recipes.settings.AdvancedRecipeSettings;
import me.wolfyscript.customcrafting.recipes.settings.CraftingRecipeSettings;
import me.wolfyscript.customcrafting.recipes.settings.EliteRecipeSettings;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.JsonNode;
import me.wolfyscript.utilities.util.NamespacedKey;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public interface RecipeType<C extends CustomRecipe<?>> extends RecipeLoader<C> {

    //Crafting recipes
    RecipeType<CraftingRecipeShaped> CRAFTING_SHAPED = new RecipeTypeImpl<>(Type.CRAFTING_SHAPED, CraftingRecipeShaped.class);
    RecipeType<CraftingRecipeShapeless> CRAFTING_SHAPELESS = new RecipeTypeImpl<>(Type.CRAFTING_SHAPELESS, CraftingRecipeShapeless.class);
    RecipeType<CraftingRecipeEliteShaped> ELITE_CRAFTING_SHAPED = new RecipeTypeImpl<>(Type.ELITE_CRAFTING_SHAPED, CraftingRecipeEliteShaped.class);
    RecipeType<CraftingRecipeEliteShapeless> ELITE_CRAFTING_SHAPELESS = new RecipeTypeImpl<>(Type.ELITE_CRAFTING_SHAPELESS, CraftingRecipeEliteShapeless.class);
    //Cooking recipes
    RecipeType<CustomRecipeFurnace> FURNACE = new RecipeTypeImpl<>(Type.FURNACE, CustomRecipeFurnace.class);
    RecipeType<CustomRecipeBlasting> BLAST_FURNACE = new RecipeTypeImpl<>(Type.BLAST_FURNACE, CustomRecipeBlasting.class);
    RecipeType<CustomRecipeSmoking> SMOKER = new RecipeTypeImpl<>(Type.SMOKER, CustomRecipeSmoking.class);
    RecipeType<CustomRecipeCampfire> CAMPFIRE = new RecipeTypeImpl<>(Type.CAMPFIRE, CustomRecipeCampfire.class);
    //Misc recipes
    RecipeType<CustomRecipeAnvil> ANVIL = new RecipeTypeImpl<>(Type.ANVIL, CustomRecipeAnvil.class);
    RecipeType<CustomRecipeStonecutter> STONECUTTER = new RecipeTypeImpl<>(Type.STONECUTTER, CustomRecipeStonecutter.class);
    RecipeType<CustomRecipeCauldron> CAULDRON = new RecipeTypeImpl<>(Type.CAULDRON, CustomRecipeCauldron.class);
    RecipeType<CustomRecipeGrindstone> GRINDSTONE = new RecipeTypeImpl<>(Type.GRINDSTONE, CustomRecipeGrindstone.class);
    RecipeType<CustomRecipeBrewing> BREWING_STAND = new RecipeTypeImpl<>(Type.BREWING_STAND, CustomRecipeBrewing.class);
    RecipeType<CustomRecipeSmithing> SMITHING = new RecipeTypeImpl<>(Type.SMITHING, CustomRecipeSmithing.class);

    static Set<RecipeType<? extends CustomRecipe<?>>> values() {
        return Collections.unmodifiableSet(RecipeTypeImpl.values);
    }

    static RecipeType<?> valueOf(String id) {
        return RecipeTypeImpl.values.stream().filter(rType -> rType.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
    }

    static RecipeType<?> valueOf(Type type) {
        return RecipeTypeImpl.values.stream().filter(rType -> rType.getType().equals(type)).findFirst().orElse(null);
    }

    Type getType();

    String getCreatorID();

    Container<? super C> getContainer();

    Class<C> getRecipeClass();

    boolean isInstance(CustomRecipe<?> recipe);

    /**
     * Casts the recipe to the type of this RecipeType.
     * Make sure that the recipe is actually a type of this type using {@link #isInstance(CustomRecipe)}.
     * If the recipe is not a type of this recipe type this method will throw a {@link ClassCastException}.
     *
     * @param recipe The recipe to cast.
     * @return The recipe cast to the type of this RecipeType.
     */
    C cast(CustomRecipe<?> recipe);

    String name();

    interface Container<C extends CustomRecipe<?>> {

        CraftingContainer<AdvancedRecipeSettings> CRAFTING = new CraftingContainer<>("workbench", "crafting", CRAFTING_SHAPED, CRAFTING_SHAPELESS);
        CraftingContainer<EliteRecipeSettings> ELITE_CRAFTING = new CraftingContainer<>("elite_workbench", "elite_crafting", ELITE_CRAFTING_SHAPED, ELITE_CRAFTING_SHAPELESS);
        Container<CustomRecipeCooking<?, ?>> COOKING = new ContainerImpl<>((Class<CustomRecipeCooking<?, ?>>) (Object) CustomRecipeCooking.class, "cooking", List.of(FURNACE, BLAST_FURNACE, SMOKER, CAMPFIRE));

        static Collection<Container<? extends CustomRecipe<?>>> values() {
            return ContainerImpl.values.values();
        }

        static Container<?> valueOf(String id) {
            return ContainerImpl.values.get(id);
        }

        List<RecipeType<? extends C>> getTypes();

        String getId();

        String getCreatorID();

        /**
         * @param recipe The recipe to check.
         * @return if the recipe is an instance of the included types.
         */
        boolean isInstance(CustomRecipe<?> recipe);

        /**
         * @param type The type to check for.
         * @return True if the type is included in the container.
         */
        boolean has(RecipeType<?> type);

        /**
         * Casts the recipe to the type of this Container.
         * Make sure that the recipe is actually a type of this type using {@link #isInstance(CustomRecipe)}.
         * If the recipe is not a type of this container this method will throw a {@link ClassCastException}.
         *
         * @param recipe The recipe to cast.
         * @return The recipe cast to the type of this RecipeType.
         */
        C cast(CustomRecipe<?> recipe);

        final class CraftingContainer<S extends CraftingRecipeSettings<S>> extends Container.ContainerImpl<CraftingRecipe<?, S>> implements RecipeLoader<CraftingRecipe<?, S>> {

            private CraftingContainer(String folderID, String creatorID, RecipeType<? extends CraftingRecipe<?, S>> shaped, RecipeType<? extends CraftingRecipe<?, S>> shapeless) {
                super((Class<CraftingRecipe<?, S>>) (Object) CraftingRecipe.class, folderID, creatorID, List.of(shaped, shapeless));
            }

            @Override
            public CraftingRecipe<?, S> getInstance(NamespacedKey namespacedKey, JsonNode node) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
                return !node.path("shapeless").asBoolean() ? types.get(0).getInstance(namespacedKey, node) : types.get(1).getInstance(namespacedKey, node);
            }
        }

        class ContainerImpl<C extends CustomRecipe<?>> implements Container<C> {

            static final Map<String, Container<?>> values = new HashMap<>();

            protected final List<RecipeType<? extends C>> types;
            private final String id;
            private final String creatorID;
            private final Class<C> clazz;

            private ContainerImpl(Class<C> clazz, String id, List<RecipeType<? extends C>> types) {
                this(clazz, id, id, types);
            }

            private ContainerImpl(Class<C> clazz, String id, String creatorID, List<RecipeType<? extends C>> types) {
                this.types = types;
                for (RecipeType<? extends C> type : this.types) {
                    if (type instanceof RecipeTypeImpl<?> recipeTypeImpl) {
                        ((RecipeTypeImpl<C>) recipeTypeImpl).setParent(this);
                    }
                }
                this.clazz = clazz;
                this.id = id;
                this.creatorID = creatorID;
                values.putIfAbsent(id, this);
            }

            @Override
            public List<RecipeType<? extends C>> getTypes() {
                return types;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public String getCreatorID() {
                return creatorID;
            }

            @Override
            public boolean isInstance(CustomRecipe<?> recipe) {
                return types.get(0).isInstance(recipe) || types.get(1).isInstance(recipe);
            }

            @Override
            public boolean has(RecipeType<?> type) {
                return types.contains(type);
            }

            @Override
            public C cast(CustomRecipe<?> recipe) {
                return clazz.cast(recipe);
            }

        }
    }

    enum Type {
        CRAFTING_SHAPED,
        CRAFTING_SHAPELESS,
        ELITE_CRAFTING_SHAPED,
        ELITE_CRAFTING_SHAPELESS,
        ANVIL,
        FURNACE,
        BLAST_FURNACE,
        SMOKER,
        CAMPFIRE,
        STONECUTTER,
        CAULDRON,
        GRINDSTONE,
        BREWING_STAND,
        SMITHING
    }

    class RecipeTypeImpl<C extends CustomRecipe<?>> implements RecipeType<C> {

        static Set<RecipeType<? extends CustomRecipe<?>>> values = new HashSet<>();

        private final String id;
        private final Type type;
        private final String creatorID;
        private final Class<C> clazz;
        private Container<? super C> parent;

        private RecipeTypeImpl(Type type, Class<C> clazz) {
            this(type, clazz, type.toString().toLowerCase(Locale.ROOT));
        }

        private RecipeTypeImpl(Type type, Class<C> clazz, String creatorID) {
            this.type = type;
            this.clazz = clazz;
            this.id = type.toString().toLowerCase(Locale.ROOT);
            this.creatorID = creatorID;
            this.parent = null;
            values.add(this);
        }

        public static RecipeType<?> valueOf(String id) {
            return values.stream().filter(rType -> rType.getId().equalsIgnoreCase(id)).findFirst().orElse(null);
        }

        public static RecipeType<?> valueOf(Type type) {
            return values.stream().filter(rType -> rType.getType().equals(type)).findFirst().orElse(null);
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getCreatorID() {
            return parent == null ? creatorID : parent.getCreatorID();
        }

        @Override
        public Class<C> getRecipeClass() {
            return clazz;
        }

        @Override
        public Container<? super C> getContainer() {
            return parent;
        }

        void setParent(Container<? super C> parent) {
            this.parent = parent;
        }

        @Override
        public boolean isInstance(CustomRecipe<?> recipe) {
            return clazz.isInstance(recipe);
        }

        @Override
        public C cast(CustomRecipe<?> recipe) {
            return clazz.cast(recipe);
        }

        @Override
        public C getInstance(NamespacedKey namespacedKey, JsonNode node) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
            return clazz.getDeclaredConstructor(NamespacedKey.class, JsonNode.class).newInstance(namespacedKey, node);
        }

        @Override
        public String name() {
            return getType().toString();
        }

        @Override
        public String toString() {
            return "RecipeType{" +
                    "id='" + id + '\'' +
                    ", creatorID='" + creatorID + '\'' +
                    ", clazz=" + clazz +
                    ", type=" + type +
                    '}';
        }

    }
}
