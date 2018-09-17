package net.darkhax.eplus.block.tileentity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.darkhax.bookshelf.util.NBTUtils;
import net.darkhax.eplus.EnchLogic;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class EnchantmentLogicController {

    private final TileEntityAdvancedTable table;

    private ItemStack inputStack;

    // All valid enchantments for the current item.
    private List<Enchantment> validEnchantments;

    // The original enchantment map.
    private Map<Enchantment, Integer> initialEnchantments;

    // The new enchantments that will go on the item.
    private Map<Enchantment, Integer> itemEnchantments;

    private float enchantmentPower;
    private int cost;

    public EnchantmentLogicController (TileEntityAdvancedTable table) {

        this.table = table;
        this.validEnchantments = new ArrayList<>();
        this.initialEnchantments = new HashMap<>();
        this.itemEnchantments = new HashMap<>();
    }

    public void onItemUpdated () {

        this.inputStack = this.table.getItem();

        this.initialEnchantments = EnchantmentHelper.getEnchantments(this.inputStack);
        this.itemEnchantments = new HashMap<>(this.initialEnchantments);
        this.validEnchantments = EnchLogic.getValidEnchantments(this.inputStack);
        this.calculateState();
    }

    public void calculateState () {

        this.enchantmentPower = ForgeHooks.getEnchantPower(this.table.getWorld(), this.table.getPos());
        this.cost = 0;

        for (final Entry<Enchantment, Integer> newEntry : this.itemEnchantments.entrySet()) {

            final int original = this.initialEnchantments.getOrDefault(newEntry.getKey(), 0);
            final int newLevels = newEntry.getValue() - original;
            this.cost += EnchLogic.calculateNewEnchCost(newEntry.getKey(), newLevels);
        }
    }

    public int getCurrentLevel (Enchantment enchant) {

        return this.itemEnchantments.getOrDefault(enchant, 0);
    }

    public void updateEnchantment (Enchantment enchantment, int level) {

        // If the level is set to below 0, remove it from the item.
        if (level < 1) {

            this.itemEnchantments.remove(enchantment);
        }

        // If the enchantment is valid for item, update it.
        else if (this.validEnchantments.contains(enchantment)) {

            this.itemEnchantments.put(enchantment, level);
        }

        this.calculateState();
    }

    public boolean isValidEnchantment (Enchantment enchantment) {

        return this.validEnchantments.contains(enchantment);
    }

    public List<Enchantment> getValidEnchantments () {

        return this.validEnchantments;
    }

    public Map<Enchantment, Integer> getInitialEnchantments () {

        return this.initialEnchantments;
    }

    public Map<Enchantment, Integer> getCurrentEnchantments () {

        return this.itemEnchantments;
    }

    public void enchantItem (EntityPlayer player) {

        // If player doesn't have enough exp, ignore them.
        if (!player.isCreative() && player.experienceTotal < this.getCost()) {

            return;
        }

        // Only creative players get charged
        if (!player.isCreative()) {

            EnchLogic.removeExperience(player, this.getCost());
        }

        // Clear all existing enchantments
        EnchantmentHelper.setEnchantments(new HashMap<>(), this.inputStack);

        // Apply new enchantments
        for (final Entry<Enchantment, Integer> entry : this.itemEnchantments.entrySet()) {

            if (entry.getValue() > 0) {

                this.inputStack.addEnchantment(entry.getKey(), entry.getValue());
            }
        }

        // Update the logic.
        this.onItemUpdated();
    }

    public int getCost () {

        return this.cost;
    }

    public float getEnchantmentPower () {

        return this.enchantmentPower;
    }
}