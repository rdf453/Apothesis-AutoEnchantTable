package dev.rdf453.ApothicAutoEnchant.MIxin;

//TODO:플레이어의 상호작용에 반응하여 ui의 입력부에 넣기
import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EnchLibraryBlock.class)
/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 서버 사이드 우클릭 시 LibraryTransfer.flushBufferToInput()를 호출하는 골격이 구현되어 있다.
 *   2) 도서관 블럭 엔티티를 직접 읽어서 플레이어 상호작용을 입력 전송 트리거로 쓰는 구조다.
 * - 다음 작업:
 *   1) 도서관 좌표 캐시가 생기면 onUse에서 재탐색 없이 곧바로 처리할 수 있게 연결한다.
 *   2) 이벤트 등록 방식(@SubscribeEvent + Mixin 병행 사용)의 실제 동작 여부를 검증하고 단일 패턴으로 정리한다.
 * - 리스크/주의:
 *   1) 현재 구현은 서버 우클릭에 의존하므로, 자동화 틱 경로와 중복 호출 정책을 분리해 둘 필요가 있다.
 */
public class LibraryBlockMixin {

    @SubscribeEvent
    public void onUse(PlayerInteractEvent.RightClickBlock event) {
        // 서버에서만 버퍼 비우기를 수행한다.
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof LibraryTransfer transfer) {
            // 플레이어 상호작용 시점에 버퍼 아이템을 입력 슬롯으로 최대한 밀어넣는다.
            transfer.AutoEnch_flushBufferToInput();
        }
    }
}
