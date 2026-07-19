package dev.rdf453.ApothicAutoEnchant.table;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public class EnchantmentItemHandler implements ResourceHandler<ItemResource> {

    // 테이블의 인벤토리 역할을 하는 핸들러로, 슬롯 0은 연료, 슬롯 1은 책을 담당한다.
    public static final AttachmentType<EnchantmentItemHandler> TYPE =
            AttachmentType.builder(EnchantmentItemHandler::new).build();

    // 각 슬롯의 실제 아이템 상태를 저장한다.
    private final ItemStack[] stacks;

    public EnchantmentItemHandler() {
        this.stacks = new ItemStack[2];
        this.stacks[0] = ItemStack.EMPTY;
        this.stacks[1] = ItemStack.EMPTY;
    }

    @Override
    public int size() {
        // 현재 이 핸들러가 관리하는 슬롯 수를 반환한다.
        return stacks.length;
    }

    @Override
    public ItemResource getResource(int slot) {
        // 요청한 슬롯의 현재 아이템을 Resource 형태로 반환한다.
        if (slot < 0 || slot >= stacks.length) {
            return ItemResource.EMPTY;
        }

        ItemStack stack = stacks[slot];
        return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
    }

    @Override
    public long getAmountAsLong(int slot) {
        if (slot < 0 || slot >= stacks.length) {
            return 0;
        }
        return stacks[slot].getCount();
    }

    @Override
    public long getCapacityAsLong(int slot, ItemResource resource) {
        // 슬롯 1은 책 한 권만 들어가도록 용량을 1로 제한한다.
        if (!isValid(slot, resource)) {
            return 0;
        }
        return slot == 1 ? 1 : Math.min(64, resource.getMaxStackSize());
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        // 각 슬롯이 허용하는 아이템 종류를 제한한다.
        if (resource == null || resource.isEmpty()) {
            return false;
        }

        return switch (slot) {
            case 0 -> resource.is(Tags.Items.ENCHANTING_FUELS);
            case 1 -> resource.is(Items.BOOK) || resource.is(Items.ENCHANTED_BOOK);
            default -> false;
        };
    }

    @Override
    public int insert(int slot, ItemResource resource, int amount, TransactionContext context) {
        // 외부에서 아이템이 들어오면 해당 슬롯에 맞는지 검사한 뒤, 여유 공간만큼만 넣는다.
        if (slot < 0 || slot >= stacks.length || amount <= 0 || resource == null || resource.isEmpty() || !isValid(slot, resource)) {
            return 0;
        }

        ItemStack current = stacks[slot];
        int capacity = (int) Math.min(getCapacityAsLong(slot, resource), Integer.MAX_VALUE);
        if (capacity <= 0) {
            return 0;
        }

        int toInsert = Math.min(amount, capacity - current.getCount());
        if (toInsert <= 0) {
            return 0;
        }

        if (current.isEmpty()) {
            stacks[slot] = resource.toStack(toInsert);
        } else if (resource.matches(current)) {
            stacks[slot] = current.copyWithCount(current.getCount() + toInsert);
        } else {
            return 0;
        }

        return toInsert;
    }

    @Override
    public int extract(int slot, ItemResource resource, int amount, TransactionContext context) {
        // 슬롯에서 아이템을 꺼낼 때, 요청한 아이템과 일치하는 경우에만 개수만큼 제거한다.
        if (slot < 0 || slot >= stacks.length || amount <= 0 || resource == null || resource.isEmpty()) {
            return 0;
        }

        ItemStack current = stacks[slot];
        if (current.isEmpty() || !resource.matches(current)) {
            return 0;
        }

        int toExtract = Math.min(amount, current.getCount());
        stacks[slot] = current.copyWithCount(current.getCount() - toExtract);
        if (stacks[slot].getCount() <= 0) {
            stacks[slot] = ItemStack.EMPTY;
        }

        return toExtract;
    }

    public int getSlotLimit(int slot) {
        // 책 슬롯은 한 권만 넣을 수 있고, 연료 슬롯은 기본적으로 64개까지 허용한다.
        return slot == 1 ? 1 : 64;
    }
}
