package me.wolfyscript.customcrafting.configs.recipebook;

import me.wolfyscript.customcrafting.CustomCrafting;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.core.JsonGenerator;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.core.JsonParser;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.DeserializationContext;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.JsonNode;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.SerializerProvider;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.annotation.JsonSerialize;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import me.wolfyscript.utilities.libraries.com.fasterxml.jackson.databind.ser.std.StdSerializer;
import me.wolfyscript.utilities.util.json.jackson.JacksonUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = Categories.Serializer.class)
@JsonDeserialize(using = Categories.Deserializer.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Categories {

    private final Map<String, Category> categoryMap = new HashMap<>();
    private final Map<String, CategoryFilter> filters = new HashMap<>();
    private List<String> sortedCategories;
    private List<String> sortedFilters;

    public Categories(List<String> sortedCategories, List<String> sortedFilters) {
        this.sortedFilters = sortedFilters;
        this.sortedCategories = sortedCategories;
    }

    public Categories() {
        this.sortedFilters = new ArrayList<>();
        this.sortedCategories = new ArrayList<>();
    }

    public void registerCategory(String key, Category category) {
        category.setId(key);
        if (!sortedCategories.contains(key)) {
            sortedCategories.add(key);
        }
        categoryMap.put(key, category);
    }

    public void registerFilter(String key, CategoryFilter category) {
        category.setId(key);
        if (!sortedFilters.contains(key)) {
            sortedFilters.add(key);
        }
        filters.put(key, category);
    }

    public void removeFilter(String key) {
        sortedFilters.remove(key);
        filters.remove(key);
    }

    public void removeCategory(String key) {
        sortedCategories.remove(key);
        categoryMap.remove(key);
    }

    public CategoryFilter getFilter(String key) {
        return filters.get(key);
    }

    public CategoryFilter getFilter(int index) {
        if (getSortedFilters().isEmpty()) return null;
        return getFilter(getSortedFilters().get(index));
    }

    public Category getCategory(String key) {
        return categoryMap.get(key);
    }

    public Category getCategory(int index) {
        if (getSortedCategories().isEmpty()) return null;
        return getCategory(getSortedCategories().get(index));
    }

    public List<String> getSortedFilters() {
        return sortedFilters;
    }

    public void setSortedFilters(List<String> sortedSwitchCategories) {
        this.sortedFilters = sortedSwitchCategories;
    }

    public List<String> getSortedCategories() {
        return sortedCategories;
    }

    public void setSortedCategories(List<String> sortedMainCategories) {
        this.sortedCategories = sortedMainCategories;
    }

    public Map<String, Category> getCategories() {
        return categoryMap;
    }

    public Map<String, CategoryFilter> getFilters() {
        return filters;
    }

    public void index() {
        CustomCrafting.inst().getApi().getConsole().info("Indexing Recipe Book...");
        this.categoryMap.values().forEach(category -> {
            category.index();
            this.filters.values().forEach(category::indexFilters);
        });
    }

    @Override
    public String toString() {
        return "Categories{" +
                "sortedCategories=" + sortedCategories +
                ", sortedFilters=" + sortedFilters +
                ", categories=" + categoryMap +
                ", filters=" + filters +
                '}';
    }

    public static class Serializer extends StdSerializer<Categories> {

        public Serializer() {
            super(Categories.class);
        }

        protected Serializer(Class<Categories> t) {
            super(t);
        }

        @Override
        public void serialize(Categories categories, JsonGenerator gen, SerializerProvider serializerProvider) throws IOException {
            gen.writeStartObject();
            gen.writeObjectFieldStart("categories");
            gen.writeObjectField("sort", categories.getSortedCategories());
            gen.writeObjectField("options", categories.categoryMap.values());
            gen.writeEndObject();
            gen.writeObjectFieldStart("filters");
            gen.writeObjectField("sort", categories.getSortedFilters());
            gen.writeObjectField("options", categories.filters.values());
            gen.writeEndObject();
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends StdDeserializer<Categories> {

        public Deserializer() {
            super(Categories.class);
        }

        protected Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Categories deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
            JsonNode node = jsonParser.readValueAsTree();
            Categories categories = new Categories();
            if (node.has("categories")) {
                JsonNode mainCategories = node.path("categories");
                ArrayList<String> sortedMainList = new ArrayList<>();
                if (mainCategories.has("sort")) {
                    JsonNode sortedMain = mainCategories.path("sort");
                    sortedMain.elements().forEachRemaining(element -> sortedMainList.add(element.asText()));
                }
                categories.setSortedCategories(sortedMainList);
                mainCategories.path("options").elements().forEachRemaining(element -> {
                    Category category = JacksonUtil.getObjectMapper().convertValue(element, Category.class);
                    categories.registerCategory(category.getId(), category);
                });
            }
            if (node.has("filters")) {
                JsonNode switchCategories = node.path("filters");
                ArrayList<String> sortedSwitchList = new ArrayList<>();
                if (switchCategories.has("sort")) {
                    JsonNode sortedSwitch = switchCategories.path("sort");
                    sortedSwitch.elements().forEachRemaining(jsonElement -> sortedSwitchList.add(jsonElement.asText()));
                }
                categories.setSortedFilters(sortedSwitchList);
                switchCategories.path("options").elements().forEachRemaining(element -> {
                    CategoryFilter category = JacksonUtil.getObjectMapper().convertValue(element, CategoryFilter.class);
                    categories.registerFilter(category.getId(), category);
                });
            }
            return categories;
        }
    }

}
