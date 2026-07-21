package dev.rdf453.ApothicAutoEnchant.MIxin;

import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

// 플레이어가 직접 보지 않는 내부 버퍼 역할을 하며, 나중에 UI 입력칸으로 옮길 수 있도록
// 인챈트 북을 임시로 보관하는 용도로 사용한다.
@Mixin(EnchLibraryTile.class)
/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) EnchLibraryTile에 인챈트북 전용 버퍼(ResourceHandler 432칸)와 삽입/전체추출 API가 구현되어 있다.
 *   2) 슬롯 검증(인챈트북 전용)과 기본 insert/extract 동작은 동작 가능한 형태다.
 * - 다음 작업:
 *   1) 버퍼 상태를 월드 저장과 연결할 NBT 직렬화/역직렬화 경로를 추가한다.
 *   2) null TransactionContext 경로를 정리하고 트랜잭션 롤백/동기화 정책을 확정한다.
 *   3) 전체추출 외에 요청 수량 기반 부분 추출 API를 추가한다.
 * - 리스크/주의:
 *   1) insert가 기존 스택 병합 대신 새 스택 대입 중심이라 스택 유지 정책 점검이 필요하다.
 */
public abstract class LibraryTileMixin implements LibraryTransfer {

    // 실제 버퍼 저장소: 432개의 슬롯을 가진 내부 배열이다.
    @Unique
    private final ResourceHandler<ItemResource> bufferHandler = new ResourceHandler<>() {
        private final ItemStack[] slots = new ItemStack[432];

        @Override
        public int size() {
            // 버퍼가 관리하는 슬롯 수를 반환한다.
            return slots.length;
        }

        @Override
        public ItemResource getResource(int slot) {
            // 지정한 슬롯의 현재 아이템을 Resource 형태로 꺼낸다.
            ItemStack stack = slots[slot];
            return stack.isEmpty() ? ItemResource.EMPTY : ItemResource.of(stack);
        }

        @Override
        public long getAmountAsLong(int slot) {
            return slot >= 0 && slot < slots.length ? slots[slot].getCount() : 0;
        }

        @Override
        public long getCapacityAsLong(int slot, ItemResource resource) {
            // 인챈트 북만 허용하므로 슬롯당 최대 용량은 64로 둔다.
            return isValid(slot, resource) ? 64 : 0;
        }

        @Override
        public boolean isValid(int slot, ItemResource resource) {
            // 유효한 슬롯 번호이고, 인챈트 북만 저장할 수 있도록 제한한다.
            return slot >= 0 && slot < slots.length && resource != null && !resource.isEmpty() && resource.is(Items.ENCHANTED_BOOK);
        }

        @Override
        public int insert(int slot, ItemResource resource, int amount, TransactionContext context) {
            // 버퍼에 아이템을 넣을 때, 유효한 슬롯이고 여유 공간이 있으면 넣는다.
            if (!isValid(slot, resource) || amount <= 0) {
                return 0;
            }

            ItemStack current = slots[slot];
            int capacity = 64 - current.getCount();
            int toInsert = Math.min(amount, capacity);
            if (toInsert <= 0) {
                return 0;
            }

            slots[slot] = resource.toStack(toInsert);
            return toInsert;
        }

        @Override
        public int extract(int slot, ItemResource resource, int amount, TransactionContext context) {
            // 버퍼에서 아이템을 꺼낼 때, 요청한 슬롯과 아이템이 일치하면 제거한다.
            if (slot < 0 || slot >= slots.length || amount <= 0 || resource == null || resource.isEmpty()) {
                return 0;
            }

            ItemStack current = slots[slot];
            if (current.isEmpty() || !resource.matches(current)) {
                return 0;
            }

            int toExtract = Math.min(amount, current.getCount());
            slots[slot] = current.copyWithCount(current.getCount() - toExtract);
            if (slots[slot].getCount() <= 0) {
                slots[slot] = ItemStack.EMPTY;
            }
            return toExtract;
        }
    };

    @Unique
    public boolean AutoEnch$insertArray(ItemStack stack) {
        // 외부에서 인챈트 북을 넘겨받으면, 빈 슬롯에 하나씩 저장한다.
        if (stack == null || stack.isEmpty() || !stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }

        for (int i = 0; i < bufferHandler.size(); i++) {
            if (bufferHandler.getAmountAsLong(i) < 64) {
                bufferHandler.insert(i, ItemResource.of(stack), stack.getCount(), null);
                return true;
            }
        }

        return false;
    }

    @Unique
    public List<ItemStack> AutoEnch$extractArray() {
        // 버퍼에 들어 있는 모든 인챈트 북을 하나씩 분리해서 반환한다.
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < bufferHandler.size(); i++) {
            if (bufferHandler.getAmountAsLong(i) > 0) {
                ItemResource resource = bufferHandler.getResource(i);
                if (!resource.isEmpty()) {
                    int amount = (int) bufferHandler.getAmountAsLong(i);
                    bufferHandler.extract(i, resource, amount, null);

                    for (int j = 0; j < amount; j++) {
                        result.add(resource.toStack(1));
                    }
                }
            }
        }

        return result;
    }
}
