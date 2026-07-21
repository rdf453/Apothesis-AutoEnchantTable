package dev.rdf453.ApothicAutoEnchant.MIxin;

import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
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
 *   1) EnchLibraryTile에 인챈트북 전용 내부 버퍼(ItemStack 432칸)와 삽입/전체추출/입력 플러시 API가 구현되어 있다.
 *   2) 슬롯 검증(인챈트북 전용)과 기본 insert/extract 병합 동작은 동작 가능한 형태다.
 * - 다음 작업:
 *   1) 버퍼 상태를 월드 저장과 연결할 NBT 직렬화/역직렬화 경로를 추가한다.
 *   2) 도서관 좌표 캐시와 연동해 상호작용 시점에만 flush가 일어나도록 정리한다.
 *   3) 전체추출 외에 요청 수량 기반 부분 추출 API가 필요하면 분리한다.
 * - 리스크/주의:
 *   1) insert가 기존 스택 병합 대신 새 스택 대입 중심이라 스택 유지 정책 점검이 필요하다.
 *   2) flushBufferToInput은 입력핸들러가 null이거나 변경되면 반환값 기준으로 실패 처리가 필요하다.
 */
public abstract class LibraryTileMixin implements LibraryTransfer {
    @Unique
    private static final int AUTO_ENCH_BUFFER_SIZE = 432;

    @Unique
    private static final int SLOT_STACK_LIMIT = 64;

    // 알렉산드리아 도서관 인스턴스별 임시 버퍼.
    // 목적: 플레이어 상호작용 시 입력 슬롯으로 한번에 이체하기 전까지 보관.
    @Unique
    private final ItemStack[] autoEnchBuffer = createEmptyBuffer();

    @Unique
    private static ItemStack[] createEmptyBuffer() {
        ItemStack[] buffer = new ItemStack[AUTO_ENCH_BUFFER_SIZE];
        for (int i = 0; i < buffer.length; i++) {
            buffer[i] = ItemStack.EMPTY;
        }
        return buffer;
    }

    @Unique
    private static boolean isInsertableBook(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(Items.ENCHANTED_BOOK);
    }

    @Unique
    public boolean AutoEnch_insertList(ItemStack stack) {
        // 외부에서 받은 인챈트 북을 버퍼에 누적한다.
        if (!isInsertableBook(stack)) {
            return false;
        }

        int remaining = stack.getCount();

        // 1차: 같은 아이템 스택 병합.
        for (int i = 0; i < autoEnchBuffer.length && remaining > 0; i++) {
            ItemStack current = autoEnchBuffer[i];
            if (!current.isEmpty() && ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < SLOT_STACK_LIMIT) {
                int canMove = Math.min(remaining, SLOT_STACK_LIMIT - current.getCount());
                autoEnchBuffer[i] = current.copyWithCount(current.getCount() + canMove);
                remaining -= canMove;
            }
        }

        // 2차: 빈 슬롯 채우기.
        for (int i = 0; i < autoEnchBuffer.length && remaining > 0; i++) {
            if (autoEnchBuffer[i].isEmpty()) {
                int canMove = Math.min(remaining, SLOT_STACK_LIMIT);
                autoEnchBuffer[i] = stack.copyWithCount(canMove);
                remaining -= canMove;
            }
        }

        return remaining < stack.getCount();
    }

    @Unique
    public List<ItemStack> AutoEnch_extractList() {
        // 버퍼에 들어 있는 모든 인챈트 북을 하나씩 분리해서 반환한다.
        List<ItemStack> result = new ArrayList<>();

        for (int i = 0; i < autoEnchBuffer.length; i++) {
            ItemStack current = autoEnchBuffer[i];
            if (!current.isEmpty()) {
                int amount = current.getCount();
                ItemStack single = current.copyWithCount(1);
                for (int j = 0; j < amount; j++) {
                    result.add(single.copy());
                }
                autoEnchBuffer[i] = ItemStack.EMPTY;
            }
        }

        return result;
    }

    @Override
    @Unique
    public int AutoEnch_flushBufferToInput() {
        // 도서관 타일의 입력 핸들러에 버퍼 아이템을 가능한 만큼 밀어넣는다.
        EnchLibraryTile self = (EnchLibraryTile) (Object) this;
        ResourceHandler<ItemResource> inputHandler = self.getItemHandler(null);
        if (inputHandler == null) {
            return 0;
        }

        int movedTotal = 0;

        for (int i = 0; i < autoEnchBuffer.length; i++) {
            ItemStack buffered = autoEnchBuffer[i];
            if (!isInsertableBook(buffered)) {
                continue;
            }

            int remaining = buffered.getCount();
            ItemResource resource = ItemResource.of(buffered);

            for (int slot = 0; slot < inputHandler.size() && remaining > 0; slot++) {
                int inserted = inputHandler.insert(slot, resource, remaining, null);
                if (inserted > 0) {
                    movedTotal += inserted;
                    remaining -= inserted;
                }
            }

            autoEnchBuffer[i] = remaining <= 0 ? ItemStack.EMPTY : buffered.copyWithCount(remaining);
        }

        return movedTotal;
    }
}
