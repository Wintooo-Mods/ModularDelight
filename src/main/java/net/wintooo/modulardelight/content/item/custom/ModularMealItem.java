package net.wintooo.modulardelight.content.item.custom;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.client.item.TooltipData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.wintooo.modulardelight.content.effect.ModStatusEffects;
import net.wintooo.modulardelight.content.item.ModItems;
import net.wintooo.modulardelight.content.item.custom.tooltip.MealSummaryTooltip;
import net.wintooo.modulardelight.content.util.DigestionManager;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class ModularMealItem extends Item {
    public static final int REQUIRED_SLOTS = 3;
    private static final int SLOT_AMBIENT = 0;
    private static final int SLOT_CONDITION = 1;
    private static final int SLOT_ACTIVATED = 2;
    private static final int DIGESTION_DURATION_TICKS = 6000;

    public ModularMealItem(Settings settings) {
        super(settings);
    }

    public static ItemStack create(List<Identifier> ingredientIds) {
        if (ingredientIds.size() != REQUIRED_SLOTS) {
            throw new IllegalArgumentException(
                    "Modular meal requires exactly " + REQUIRED_SLOTS + " ingredients, got " + ingredientIds.size());
        }
        ItemStack stack = new ItemStack(ModItems.MODULAR_MEAL);
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        ingredientIds.forEach(id -> list.add(NbtString.of(id.toString())));
        nbt.put("Ingredients", list);
        stack.setNbt(nbt);
        return stack;
    }

    public static List<Identifier> getIngredientIds(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null || !nbt.contains("Ingredients")) return List.of();
        NbtList list = nbt.getList("Ingredients", NbtElement.STRING_TYPE);
        return list.stream()
                .map(tag -> Identifier.tryParse(tag.asString()))
                .filter(Objects::nonNull)
                .toList();
    }

    public static boolean isComplete(ItemStack stack) {
        return getIngredientIds(stack).size() == REQUIRED_SLOTS;
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient && user instanceof ServerPlayerEntity player && isComplete(stack)) {
            grantDigestion(player, stack);
        }

        ItemStack remainder = super.finishUsing(stack, world, user);

        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            player.getInventory().insertStack(new ItemStack(Items.BOWL));
        }

        return remainder;
    }

    private void grantDigestion(ServerPlayerEntity player, ItemStack stack) {
        List<Identifier> ids = getIngredientIds(stack);

        MealEffect ambient = sourceEffect(ids.get(SLOT_AMBIENT));
        MealEffect condition = sourceEffect(ids.get(SLOT_CONDITION));
        MealEffect activated = sourceEffect(ids.get(SLOT_ACTIVATED));

        if (ambient == null || condition == null || activated == null) return;

        MealEffect composite = MealEffect.combine(ambient, condition, activated);

        DigestionManager.grant(
                player,
                composite,
                ambient,
                condition,
                activated,
                DIGESTION_DURATION_TICKS
        );
        player.addStatusEffect(new StatusEffectInstance(
                ModStatusEffects.DIGESTION, DIGESTION_DURATION_TICKS, 0, true, false));
    }

    private static MealEffect resolveComposite(List<Identifier> ingredientIds) {
        if (ingredientIds.size() != REQUIRED_SLOTS) return null;

        MealEffect ambientSource = sourceEffect(ingredientIds.get(SLOT_AMBIENT));
        MealEffect conditionSource = sourceEffect(ingredientIds.get(SLOT_CONDITION));
        MealEffect activatedSource = sourceEffect(ingredientIds.get(SLOT_ACTIVATED));

        if (ambientSource == null || conditionSource == null || activatedSource == null) return null;
        return MealEffect.combine(ambientSource, conditionSource, activatedSource);
    }

    private static MealEffect sourceEffect(Identifier ingredientId) {
        MealProperty property = resolveProperty(ingredientId);
        return property == null ? null : MealEffect.byProperty(property);
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

        MealProperty ambient = resolveProperty(ingredientIds.get(SLOT_AMBIENT));
        MealProperty condition = resolveProperty(ingredientIds.get(SLOT_CONDITION));
        MealProperty activated = resolveProperty(ingredientIds.get(SLOT_ACTIVATED));

        if (ambient == null || condition == null || activated == null) return null;
        return MealPattern.resolve(ambient, condition, activated);
    }

    public static float getPatternModelIndex(ItemStack stack) {
        MealPattern pattern = resolvePattern(getIngredientIds(stack));
        return pattern == null ? 0f : pattern.modelIndex();
    }

    @Override
    public Optional<TooltipData> getTooltipData(ItemStack stack) {
        List<Identifier> ingredientIds = getIngredientIds(stack);
        if (ingredientIds.isEmpty()) return Optional.empty();

        List<ItemStack> ingredientStacks = ingredientIds.stream()
                .map(id -> new ItemStack(Registries.ITEM.get(id)))
                .toList();

        MealEffect composite = resolveComposite(ingredientIds);
        List<Text> descriptionLines =
                composite == null ? List.of() : composite.getMealTooltip();

        return Optional.of(new MealSummaryTooltip.Data(descriptionLines, ingredientStacks));
    }

    @Override
    public Text getName(ItemStack stack) {
        List<Identifier> ingredientIds = getIngredientIds(stack);
        if (ingredientIds.size() != REQUIRED_SLOTS) return super.getName(stack);

        MealPattern pattern = resolvePattern(ingredientIds);
        if (pattern == null) return super.getName(stack);

        Text ambientName = new ItemStack(Registries.ITEM.get(ingredientIds.get(SLOT_AMBIENT))).getName();
        Text conditionName = new ItemStack(Registries.ITEM.get(ingredientIds.get(SLOT_CONDITION))).getName();
        Text activatedName = new ItemStack(Registries.ITEM.get(ingredientIds.get(SLOT_ACTIVATED))).getName();

        return switch (pattern) {
            case UNIFORM -> Text.translatable(pattern.nameTranslationKey(), ambientName);
            case AMBIENT_CONDITION -> Text.translatable(pattern.nameTranslationKey(), ambientName, activatedName);
            case AMBIENT_ACTIVATED -> Text.translatable(pattern.nameTranslationKey(), ambientName, conditionName);
            case CONDITION_ACTIVATED -> Text.translatable(pattern.nameTranslationKey(), conditionName, ambientName);
            case UNIQUE -> Text.translatable(pattern.nameTranslationKey(), ambientName, conditionName, activatedName);
        };
    }

    public static List<MealColor> resolveTintColors(ItemStack stack) {
        List<Identifier> ingredientIds = getIngredientIds(stack);
        if (ingredientIds.size() != REQUIRED_SLOTS) return List.of();

        MealPattern pattern = resolvePattern(ingredientIds);
        if (pattern == null) return List.of();

        MealColor ambient = resolveColor(ingredientIds.get(SLOT_AMBIENT));
        MealColor condition = resolveColor(ingredientIds.get(SLOT_CONDITION));
        MealColor activated = resolveColor(ingredientIds.get(SLOT_ACTIVATED));
        if (ambient == null) return List.of();
        if (condition == null) return List.of();
        if (activated == null) return List.of();

        return switch (pattern) {
            case UNIFORM -> List.of(ambient);
            case AMBIENT_CONDITION -> List.of(ambient, activated);
            case AMBIENT_ACTIVATED -> List.of(ambient, condition);
            case CONDITION_ACTIVATED -> List.of(condition, ambient);
            case UNIQUE -> List.of(ambient, condition, activated);
        };
    }
}