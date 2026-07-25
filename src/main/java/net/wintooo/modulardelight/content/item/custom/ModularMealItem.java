package net.wintooo.modulardelight.content.item.custom;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.item.TooltipData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.potion.PotionUtil;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.InvalidIdentifierException;
import net.minecraft.world.World;
import net.wintooo.modulardelight.content.data.MealNameFilterRegistry;
import net.wintooo.modulardelight.content.data.MealOverride;
import net.wintooo.modulardelight.content.data.MealOverrideRegistry;
import net.wintooo.modulardelight.content.meal.DigestionEffect;
import net.wintooo.modulardelight.content.meal.MealColor;
import net.wintooo.modulardelight.content.meal.MealPattern;
import net.wintooo.modulardelight.content.meal.MealProperty;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.tooltip.MealSummaryTooltip;
import net.wintooo.modulardelight.content.util.DigestionManager;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ModularMealItem extends Item {
    public static final int REQUIRED_SLOTS = 3;
    private static final int SLOT_AMBIENT = 0;
    private static final int SLOT_CONDITION = 1;
    private static final int SLOT_ACTIVATED = 2;
    private static final int DIGESTION_DURATION_TICKS = 6000;

    private static final int MIN_HUNGER_PER_INGREDIENT = 1;
    private static final float MIN_SATURATION_MODIFIER_PER_INGREDIENT = 0.1f;
    private static final int DEFAULT_TINT_COLOR = 0xE8E4D8;

    private record FoodStats(int hunger, float saturationModifier) {}

    public ModularMealItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(List<ItemStack> ingredients) {
        if (ingredients.size() != REQUIRED_SLOTS) {
            throw new IllegalArgumentException(
                    "Modular meal requires exactly " + REQUIRED_SLOTS + " ingredients, got " + ingredients.size());
        }
        ItemStack stack = new ItemStack(ModItems.MODULAR_MEAL);
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        ingredients.forEach(ingredient -> list.add(ingredient.copyWithCount(1).writeNbt(new NbtCompound())));
        nbt.put("Ingredients", list);
        stack.setNbt(nbt);
        return stack;
    }

    public static List<ItemStack> getIngredientStacks(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("Ingredients")) return List.of();
        NbtList list = nbt.getList("Ingredients", NbtElement.COMPOUND_TYPE);

        List<ItemStack> stacks = new ArrayList<>(list.size());
        for (NbtElement element : list) {
            stacks.add(ItemStack.fromNbt((NbtCompound) element));
        }
        return stacks;
    }

    public static List<Identifier> getIngredientIds(ItemStack stack) {
        return getIngredientStacks(stack).stream()
                .map(ingredient -> Registries.ITEM.getId(ingredient.getItem()))
                .toList();
    }

    public static boolean isComplete(ItemStack stack) {
        return getIngredientIds(stack).size() == REQUIRED_SLOTS;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity player && isComplete(stack)) {
            List<ItemStack> ingredientStacks = getIngredientStacks(stack);
            grantDigestion(player, stack);
            applyFoodStats(player, ingredientStacks);
            applyIngredientEffects(player, ingredientStacks);
        }

        ItemStack remainder = super.finishUsing(stack, world, user);

        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            player.getInventory().insertStack(new ItemStack(Items.BOWL));
        }

        return remainder;
    }

    private void applyFoodStats(ServerPlayerEntity player, List<ItemStack> ingredientStacks) {
        FoodStats stats = computeFoodStats(ingredientStacks);
        player.getHungerManager().add(stats.hunger(), stats.saturationModifier());
    }

    private static List<StatusEffectInstance> getCombinedEffects(ItemStack meal) {
        Map<StatusEffect, List<StatusEffectInstance>> collected = new LinkedHashMap<>();

        for (ItemStack ingredient : getIngredientStacks(meal)) {
            FoodComponent food = ingredient.getItem().getFoodComponent();

            if (food != null) {
                for (Pair<StatusEffectInstance, Float> pair : food.getStatusEffects()) {
                    collected.computeIfAbsent(pair.getFirst().getEffectType(), k -> new ArrayList<>()).add(pair.getFirst());
                }
            }

            for (StatusEffectInstance effect : PotionUtil.getPotionEffects(ingredient)) {
                collected.computeIfAbsent(effect.getEffectType(), k -> new ArrayList<>()).add(effect);
            }
        }

        List<StatusEffectInstance> result = new ArrayList<>();
        for (List<StatusEffectInstance> effects : collected.values()) {
            int duration = effects.stream().mapToInt(StatusEffectInstance::getDuration).sum();
            int amplifier = effects.stream().mapToInt(StatusEffectInstance::getAmplifier).max().orElse(0);
            StatusEffectInstance base = effects.get(0);
            result.add(new StatusEffectInstance(base.getEffectType(), duration, amplifier, false, true, true));
        }
        return result;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        super.appendTooltip(stack, world, tooltip, context);

        List<StatusEffectInstance> effects = getCombinedEffects(stack);
        if (!effects.isEmpty()) {
            PotionUtil.buildTooltip(effects, tooltip, 1.0F);
        }
    }

    private void applyIngredientEffects(ServerPlayerEntity player, List<ItemStack> ingredientStacks) {
        Map<StatusEffect, List<StatusEffectInstance>> collected = new LinkedHashMap<>();

        for (ItemStack ingredientStack : ingredientStacks) {
            FoodComponent food = ingredientStack.getItem().getFoodComponent();
            if (food != null) {
                for (Pair<StatusEffectInstance, Float> pair : food.getStatusEffects()) {
                    if (player.getRandom().nextFloat() < pair.getSecond()) {
                        collected.computeIfAbsent(pair.getFirst().getEffectType(), k -> new ArrayList<>()).add(pair.getFirst());
                    }
                }
            }

            for (StatusEffectInstance potionEffect : PotionUtil.getPotionEffects(ingredientStack)) {
                collected.computeIfAbsent(potionEffect.getEffectType(), k -> new ArrayList<>()).add(potionEffect);
            }
        }

        for (Map.Entry<StatusEffect, List<StatusEffectInstance>> entry : collected.entrySet()) {
            int totalDuration = entry.getValue().stream().mapToInt(StatusEffectInstance::getDuration).sum();
            int maxAmplifier = entry.getValue().stream().mapToInt(StatusEffectInstance::getAmplifier).max().orElse(0);
            player.addStatusEffect(new StatusEffectInstance(entry.getKey(), totalDuration, maxAmplifier, false, true, true));
        }
    }

    private static FoodStats computeFoodStats(List<ItemStack> ingredientStacks) {
        int totalHunger = 0;
        float totalSaturationModifier = 0f;

        for (ItemStack ingredientStack : ingredientStacks) {
            FoodComponent food = ingredientStack.getItem().getFoodComponent();

            if (food == null) {
                totalHunger += MIN_HUNGER_PER_INGREDIENT;
                totalSaturationModifier += MIN_SATURATION_MODIFIER_PER_INGREDIENT;
            } else {
                totalHunger += Math.max(MIN_HUNGER_PER_INGREDIENT, food.getHunger() / 2);
                totalSaturationModifier += food.getSaturationModifier() / 2f;
            }
        }

        float averageSaturationModifier = ingredientStacks.isEmpty()
                ? MIN_SATURATION_MODIFIER_PER_INGREDIENT
                : totalSaturationModifier / ingredientStacks.size();

        return new FoodStats(totalHunger, averageSaturationModifier);
    }

    private void grantDigestion(ServerPlayerEntity player, ItemStack stack) {
        List<Identifier> ids = getIngredientIds(stack);

        DigestionEffect ambient = sourceEffect(ids.get(SLOT_AMBIENT));
        DigestionEffect condition = sourceEffect(ids.get(SLOT_CONDITION));
        DigestionEffect activated = sourceEffect(ids.get(SLOT_ACTIVATED));

        if (ambient == null || condition == null || activated == null) return;

        DigestionEffect composite = DigestionEffect.combine(ambient, condition, activated);

        DigestionManager.grant(player, composite, ambient, condition, activated, DIGESTION_DURATION_TICKS);
        player.addStatusEffect(new StatusEffectInstance(
                ModStatusEffects.DIGESTION, DIGESTION_DURATION_TICKS, 0, true, false));
    }

    private static DigestionEffect resolveComposite(List<Identifier> ingredientIds) {
        if (ingredientIds.size() != REQUIRED_SLOTS) return null;

        DigestionEffect ambientSource = sourceEffect(ingredientIds.get(SLOT_AMBIENT));
        DigestionEffect conditionSource = sourceEffect(ingredientIds.get(SLOT_CONDITION));
        DigestionEffect activatedSource = sourceEffect(ingredientIds.get(SLOT_ACTIVATED));

        if (ambientSource == null || conditionSource == null || activatedSource == null) return null;
        return DigestionEffect.combine(ambientSource, conditionSource, activatedSource);
    }

    private static DigestionEffect sourceEffect(Identifier ingredientId) {
        MealProperty property = resolveProperty(ingredientId);
        return property == null ? null : DigestionEffect.byProperty(property);
    }

    public static MealProperty resolveProperty(Identifier ingredientId) {
        Item ingredient = Registries.ITEM.get(ingredientId);
        ItemStack ingredientStack = new ItemStack(ingredient);
        for (MealProperty property : MealProperty.all()) {
            if (ingredientStack.isIn(property.tag())) return property;
        }
        return null;
    }

    public static MealColor resolveColor(Identifier ingredientId) {
        Item ingredient = Registries.ITEM.get(ingredientId);
        ItemStack ingredientStack = new ItemStack(ingredient);
        for (MealColor color : MealColor.all()) {
            if (ingredientStack.isIn(color.tag())) return color;
        }
        return null;
    }

    private static MealPattern resolvePattern(List<Identifier> ingredientIds) {
        if (ingredientIds.size() != REQUIRED_SLOTS) return null;
        return MealPattern.resolve(
                ingredientIds.get(SLOT_AMBIENT),
                ingredientIds.get(SLOT_CONDITION),
                ingredientIds.get(SLOT_ACTIVATED));
    }

    public static float getPatternModelIndex(ItemStack stack) {
        List<Identifier> ingredientIds = getIngredientIds(stack);

        MealOverride override = MealOverrideRegistry.find(ingredientIds);
        ModularMealItem.setOverrideModel(stack, override == null ? null : override.model());

        MealPattern pattern = resolvePattern(ingredientIds);
        return pattern == null ? 0f : pattern.modelIndex();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        List<ItemStack> ingredientStacks = getIngredientStacks(stack);
        if (ingredientStacks.isEmpty()) return Optional.empty();

        List<Identifier> ingredientIds = ingredientStacks.stream()
                .map(s -> Registries.ITEM.getId(s.getItem()))
                .toList();

        DigestionEffect composite = resolveComposite(ingredientIds);
        List<Text> descriptionLines = composite == null ? List.of() : composite.getMealTooltip();

        return Optional.of(new MealSummaryTooltip.Data(descriptionLines, ingredientStacks));
    }

    @Override
    public Text getName(ItemStack stack) {
        List<ItemStack> ingredientStacks = getIngredientStacks(stack);
        if (ingredientStacks.size() != REQUIRED_SLOTS) return super.getName(stack);

        List<Identifier> ingredientIds = ingredientStacks.stream()
                .map(s -> Registries.ITEM.getId(s.getItem()))
                .toList();

        MealOverride override = MealOverrideRegistry.find(ingredientIds);
        if (override != null && override.name() != null) return override.name();

        MealPattern pattern = resolvePattern(ingredientIds);
        if (pattern == null) return super.getName(stack);

        Text ambientName = filteredName(ingredientStacks.get(SLOT_AMBIENT));
        Text conditionName = filteredName(ingredientStacks.get(SLOT_CONDITION));
        Text activatedName = filteredName(ingredientStacks.get(SLOT_ACTIVATED));

        return switch (pattern) {
            case UNIFORM -> Text.translatable(pattern.nameTranslationKey(), ambientName);
            case AMBIENT_CONDITION -> Text.translatable(pattern.nameTranslationKey(), ambientName, activatedName);
            case AMBIENT_ACTIVATED -> Text.translatable(pattern.nameTranslationKey(), ambientName, conditionName);
            case CONDITION_ACTIVATED -> Text.translatable(pattern.nameTranslationKey(), conditionName, ambientName);
            case UNIQUE -> Text.translatable(pattern.nameTranslationKey(), ambientName, conditionName, activatedName);
        };
    }

    private static Text filteredName(ItemStack ingredientStack) {
        String raw = ingredientStack.getName().getString();
        String filtered = MealNameFilterRegistry.strip(raw);
        return Text.literal(filtered.isBlank() ? raw : filtered);
    }

    public static List<Integer> resolveTintColors(ItemStack stack) {
        List<ItemStack> ingredientStacks = getIngredientStacks(stack);
        if (ingredientStacks.size() != REQUIRED_SLOTS) return List.of();

        List<Identifier> ingredientIds = ingredientStacks.stream()
                .map(s -> Registries.ITEM.getId(s.getItem()))
                .toList();

        MealPattern pattern = resolvePattern(ingredientIds);
        if (pattern == null) return List.of();

        ItemStack ambientStack = ingredientStacks.get(SLOT_AMBIENT);
        ItemStack conditionStack = ingredientStacks.get(SLOT_CONDITION);
        ItemStack activatedStack = ingredientStacks.get(SLOT_ACTIVATED);

        return switch (pattern) {
            case UNIFORM -> List.of(tintColorFor(ambientStack));
            case AMBIENT_CONDITION -> List.of(tintColorFor(ambientStack), tintColorFor(activatedStack));
            case AMBIENT_ACTIVATED -> List.of(tintColorFor(ambientStack), tintColorFor(conditionStack));
            case CONDITION_ACTIVATED -> List.of(tintColorFor(conditionStack), tintColorFor(ambientStack));
            case UNIQUE -> List.of(tintColorFor(ambientStack), tintColorFor(conditionStack), tintColorFor(activatedStack));
        };
    }

    private static int tintColorFor(ItemStack ingredientStack) {
        if (!PotionUtil.getPotionEffects(ingredientStack).isEmpty()) {
            return PotionUtil.getColor(ingredientStack);
        }
        Identifier id = Registries.ITEM.getId(ingredientStack.getItem());
        MealColor color = resolveColor(id);
        return color != null ? color.rgb() : DEFAULT_TINT_COLOR;
    }

    public static final String OVERRIDE_MODEL_KEY = "MealOverrideModel";

    public static void setOverrideModel(ItemStack stack, @Nullable Identifier model) {
        if (model == null) {
            stack.removeSubNbt(OVERRIDE_MODEL_KEY);
        } else {
            stack.getOrCreateNbt().putString(OVERRIDE_MODEL_KEY, model.toString());
        }
    }

    @Nullable
    public static Identifier getOverrideModel(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains(OVERRIDE_MODEL_KEY, NbtElement.STRING_TYPE)) {
            return null;
        }

        try {
            return new Identifier(nbt.getString(OVERRIDE_MODEL_KEY));
        } catch (InvalidIdentifierException ignored) {
            return null;
        }
    }
}