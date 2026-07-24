package dev.rdf453.ApothicAutoEnchant.table;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Arrays;

/*
 * 설계 메모 (2026-07-22 기준)
 * - 현재 상태:
 *   1) 2슬롯 ResourceHandler와 createLibraryBuffer 팩토리까지 포함해 재사용 가능한 상태다.
 *   2) 테이블 쪽과 도서관 쪽의 허용 아이템 정책이 분리되어 있다.
 * - 다음 작업:
 *   1) 자동화 엔진에서 쓸 결과 슬롯/출력 정책을 더 확장할지 결정한다.
 *   2) 실제 블록엔티티 상태 저장과 핸들러 변경 이벤트를 연결한다.
 * - 리스크/주의:
 *   1) 트랜잭션 롤백이 없어서 복합 이체 시 정합성 검증이 필요하다.
 *   2) 도서관 버퍼는 책 전용, 기본 테이블은 책+인챈트 가능 아이템 허용 정책을 유지해야 한다.
 */
public class EnchantmentItemHandler implements ResourceHandler<ItemResource> {

    @FunctionalInterface
    public interface SlotValidator {
        boolean isValid(int slot, ItemResource resource);
    }

    @FunctionalInterface
    public interface SlotCapacityResolver {
        int getCapacity(int slot, ItemResource resource);
    }

    // 테이블의 인벤토리 역할을 하는 핸들러로, 슬롯 1은 연료, 슬롯 0은 책을 담당한다.
    public static final AttachmentType<EnchantmentItemHandler> TYPE =
            AttachmentType.builder(EnchantmentItemHandler::new).build();

    // 각 슬롯의 실제 아이템 상태를 저장한다.
    private final ItemStack[] stacks;
    private final SlotValidator slotValidator;
    private final SlotCapacityResolver slotCapacityResolver;

    public EnchantmentItemHandler() {
        this(
                2,
                (slot, resource) -> switch (slot) {
                    case 0 -> resource.is(Tags.Items.ENCHANTABLES) || resource.is(Items.ENCHANTED_BOOK);
                    case 1 -> resource.is(Tags.Items.ENCHANTING_FUELS);
                    default -> false;
                },
                (slot, resource) -> slot == 1 ? 1 : Math.min(64, resource.getMaxStackSize())
        );
    }

    public EnchantmentItemHandler(int slotCount, SlotValidator slotValidator, SlotCapacityResolver slotCapacityResolver) {
        int safeSlotCount = Math.max(1, slotCount);
        this.stacks = new ItemStack[safeSlotCount];
        Arrays.fill(this.stacks, ItemStack.EMPTY);
        this.slotValidator = slotValidator;
        this.slotCapacityResolver = slotCapacityResolver;
    }

    public static EnchantmentItemHandler createLibraryBuffer(int slotCount) {
        return new EnchantmentItemHandler(
                slotCount,
                (slot, resource) -> resource.is(Items.ENCHANTED_BOOK),
                (slot, resource) -> 64
        );
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
        return Math.max(0, this.slotCapacityResolver.getCapacity(slot, resource));
    }

    @Override
    public boolean isValid(int slot, ItemResource resource) {
        // 각 슬롯이 허용하는 아이템 종류를 제한한다.
        if (slot < 0 || slot >= stacks.length || resource == null || resource.isEmpty()) {
            return false;
        }
        return this.slotValidator.isValid(slot, resource);
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
        // 기본 슬롯 제한은 64이며, 실제 허용량은 insert 시점 검증으로 확정한다.
        return 64;
    }
}
