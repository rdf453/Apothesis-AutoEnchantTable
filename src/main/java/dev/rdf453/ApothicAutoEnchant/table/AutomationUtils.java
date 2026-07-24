package dev.rdf453.ApothicAutoEnchant.table;
//TODO:패킷처리하기

import java.util.Optional;

import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.rdf453.ApothicAutoEnchant.util.XpTransfer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
/*
 * 설계 메모 (2026-07-22 기준)
 * - 현재 상태:
 *   1) 자동화 공통 유틸은 아직 메모 전용이고 실제 함수 분리는 시작 전이다.
 * - 다음 작업:
 *   1) XP 주입/회수량 계산 클램프를 순수 함수로 분리한다.
 *   2) 자동화 틱 간격과 처리량 상한 계산을 분리한다.
 *   3) 블록 검색 반경과 필터 계산을 분리해 TableBlockEntity 복잡도를 낮춘다.
 * - 리스크/주의:
 *   1) 유틸 부재 상태로 로직이 분산되면 XP 부호와 레벨 경계 버그가 재발하기 쉽다.
 */

// 블럭 엔티티에 남겨둔 상태를 받아 실제 동작만 수행한다.
public final class AutomationUtils {
    private AutomationUtils() {
    }

    // id 매핑
    public static void costSetter(TableBlockEntity tableBlockEntity, int id) {
        tableBlockEntity.toggleCost = id - 3;
        tableBlockEntity.setChanged();
    }

    // 자동화 토글
    public static void toggleAutoEnabled(TableBlockEntity tableBlockEntity) {
        tableBlockEntity.setAutoEnabled = !tableBlockEntity.setAutoEnabled;
        tableBlockEntity.setChanged();
    }

    // 10레벨 주입
    public static void inject10Lv(TableBlockEntity tableBlockEntity, Player player) {
        int needed = XpTransfer.getXpNeedPoint(player, Math.max(0, player.experienceLevel - 10));

        tableBlockEntity.xpTank -= needed;
        player.giveExperiencePoints(needed);
        tableBlockEntity.setChanged();
    }

    // 모든 레벨 주입
    public static void injectAllLv(TableBlockEntity tableBlockEntity, Player player) {
        if (player.experienceLevel <= 0)
            return;
        int needed = XpTransfer.getXpNeedPoint(player, 0);

        tableBlockEntity.xpTank -= needed;
        player.giveExperiencePoints(needed);
        tableBlockEntity.setChanged();
    }

    // 10레벨 회수
    public static void eject10Lv(TableBlockEntity tableBlockEntity, Player player) {
        if (tableBlockEntity.xpTank < 0)
            return;
        int needed = XpTransfer.getXpNeedPoint(player, player.experienceLevel + 10);

        if (tableBlockEntity.xpTank < needed)
            return;
        tableBlockEntity.xpTank -= needed;
        player.giveExperiencePoints(needed);

        tableBlockEntity.setChanged();
    }

    // 레벨 전부 회수
    public static void ejectAllLv(TableBlockEntity tableBlockEntity, Player player) {
        player.giveExperiencePoints((int) tableBlockEntity.xpTank);
        tableBlockEntity.xpTank = 0;
        tableBlockEntity.setChanged();
    }

    // 결과물 복사
    private static ItemStack copyResult(EnchantMenu em) {
        return em.getSlot(1).getItem().copy();
    }

    // 슬롯 비우기
    private static void clearSlot(EnchantMenu em) {
        em.getSlot(1).set(ItemStack.EMPTY);
    }

    // 도서관으로 결과물 배출
    public static void doTransfer(TableBlockEntity tableBlockEntity, EnchantMenu em) {
        if (tableBlockEntity.libraryPos.isEmpty() || tableBlockEntity.tableLevel() == null)
            return;

        BlockEntity blockEntity = tableBlockEntity.tableLevel().getBlockEntity(tableBlockEntity.libraryPos.get());
        if (blockEntity instanceof LibraryTransfer transfer) {
            ItemStack copy = copyResult(em);
            if (transfer.AutoEnch_insertList(copy)) {
                clearSlot(em);
            }
        }
    }

    // 인첸트 연료 가져오기
    public static void bringFuel(TableBlockEntity tableBlockEntity, EnchantMenu em) {
        if (tableBlockEntity.chestPos.isEmpty() || tableBlockEntity.tableLevel() == null)
            return;

        BlockPos pos = tableBlockEntity.chestPos.get();
        Level level = tableBlockEntity.tableLevel();

        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.DOWN);
        if (itemHandler == null)
            return;

        for (int i = 0; i < itemHandler.size(); i++) {
            ItemResource itemResource = itemHandler.getResource(i);
            if (!itemResource.isEmpty()) {
                if (itemResource.is(Items.LAPIS_LAZULI)) {

                    Slot fuel = em.getSlot(0);
                    ItemStack existingStack = fuel.getItem();

                    try (Transaction mainTx = Transaction.openRoot()) {

                        try (Transaction subTx = Transaction.open(mainTx)) {

                            long extracted = itemHandler.extract(i, itemResource, 64, subTx);

                            if (extracted > 0) {

                                int currentCount = existingStack.getCount();
                                int maxStackSize = existingStack.getMaxStackSize();
                                int roomLeft = maxStackSize - currentCount; // 슬롯에 남은 빈 공간

                                // 실제 들어갈 수 있는 양 결정 (꺼낸 양과 빈 공간 중 작은 값)
                                long fillAmount = Math.min(extracted, roomLeft);

                                if (fillAmount > 0) {
                                    // 3. 들어갈 수 있는 만큼만 메뉴 슬롯에 추가합니다.
                                    if (existingStack.isEmpty()) {
                                        fuel.set(new ItemStack(Items.LAPIS_LAZULI, (int) fillAmount));
                                    } else {
                                        existingStack.grow((int) fillAmount);
                                    }

                                    // 4. 시뮬레이션은 커밋하지 않고 그대로 닫습니다. (메인에서 필요한 만큼만 뺄 것이므로)
                                    // simTx.commit(); 을 호출하지 않으면 상자에서 꺼냈던 64개는 가상세계에서 다시 롤백됩니다.

                                    // 5. 이제 메인 트랜잭션에서 "실제로 들어간 양(fillAmount)"만 최종 추출합니다.
                                    itemHandler.extract(i, itemResource, (int) fillAmount, mainTx);

                                    // 6. 메인 트랜잭션을 최종 확정(Commit)합니다.
                                    mainTx.commit();
                                    fuel.setChanged();

                                    break;
                                }
                            }
                        }
                    }

                }
            }

        }
    }

    // 책가져오기
    public static void bringBook(TableBlockEntity tableBlockEntity, EnchantMenu em) {
        if (tableBlockEntity.chestPos.isEmpty() || tableBlockEntity.tableLevel() == null)
            return;

        BlockPos pos = tableBlockEntity.chestPos.get();
        Level level = tableBlockEntity.tableLevel();

        ResourceHandler<ItemResource> itemHandler = level.getCapability(Capabilities.Item.BLOCK, pos, Direction.DOWN);
        if (itemHandler == null)
            return;

        for (int i = 0; i < itemHandler.size(); i++) {
            ItemResource itemResource = itemHandler.getResource(i);
            if (!itemResource.isEmpty()) {
                if (itemResource.is(Items.LAPIS_LAZULI)) {

                    Slot fuel = em.getSlot(0);
                    ItemStack existingStack = fuel.getItem();

                    try (Transaction mainTx = Transaction.openRoot()) {

                        try (Transaction subTx = Transaction.open(mainTx)) {

                            long extracted = itemHandler.extract(i, itemResource, 64, subTx);

                            if (extracted > 0) {

                                int currentCount = existingStack.getCount();
                                int maxStackSize = existingStack.getMaxStackSize();
                                int roomLeft = maxStackSize - currentCount; // 슬롯에 남은 빈 공간

                                // 실제 들어갈 수 있는 양 결정 (꺼낸 양과 빈 공간 중 작은 값)
                                long fillAmount = Math.min(extracted, roomLeft);

                                if (fillAmount > 0) {
                                    // 3. 들어갈 수 있는 만큼만 메뉴 슬롯에 추가합니다.
                                    if (existingStack.isEmpty()) {
                                        fuel.set(new ItemStack(Items.BOOK, (int) fillAmount));
                                    } else {
                                        existingStack.grow((int) fillAmount);
                                    }

                                    // 4. 시뮬레이션은 커밋하지 않고 그대로 닫습니다. (메인에서 필요한 만큼만 뺄 것이므로)
                                    // simTx.commit(); 을 호출하지 않으면 상자에서 꺼냈던 64개는 가상세계에서 다시 롤백됩니다.

                                    // 5. 이제 메인 트랜잭션에서 "실제로 들어간 양(fillAmount)"만 최종 추출합니다.
                                    itemHandler.extract(i, itemResource, (int) fillAmount, mainTx);

                                    // 6. 메인 트랜잭션을 최종 확정(Commit)합니다.
                                    mainTx.commit();
                                    fuel.setChanged();

                                    break;
                                }
                            }
                        }
                    }

                }
            }

        }
    }
}