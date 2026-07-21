package dev.rdf453.ApothicAutoEnchant.MIxin;

//TODO:플레이어의 상호작용에 반응하여 ui의 입력부에 넣기
import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(EnchLibraryBlock.class)
/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 서버 사이드에서 우클릭 이벤트를 받고 LibraryTransfer 버퍼를 조회하는 골격이 구현되어 있다.
 *   2) 추출 결과를 순회하지만 실제 목적지(테이블 입력/자동화 큐) 반영은 아직 비어 있다.
 * - 다음 작업:
 *   1) 추출 아이템을 TableBlockEntity 또는 메뉴 입력 슬롯으로 전달하는 경로를 연결한다.
 *   2) 이벤트 등록 방식(@SubscribeEvent + Mixin 병행 사용)의 실제 동작 여부를 검증하고 단일 패턴으로 정리한다.
 * - 리스크/주의:
 *   1) 현재 구현은 추출만 수행하고 소비 경로가 없어 아이템 유실/중복 처리 정책이 정의되지 않았다.
 */
public class LibraryBlockMixin {

    @SubscribeEvent
    public void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof LibraryTransfer transfer) {
            List<ItemStack> extracted = transfer.AutoEnch$extractArray();
            for (ItemStack stack : extracted) {
                if (!stack.isEmpty()) {
                    // 나중에 UI 입력칸에 넣는 로직으로 연결할 자리.
                }
            }
        }
    }
}
