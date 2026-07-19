package dev.rdf453.ApothicAutoEnchant.util;

import net.minecraft.world.item.ItemStack;

import java.util.List;

// 동적 바인드를 위하여 인터페이스로 구현
public interface LibraryTransfer {
    boolean AutoEnch$insertArray(ItemStack stack);

    List<ItemStack> AutoEnch$extractArray();
}