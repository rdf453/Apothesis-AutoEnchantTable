package dev.rdf453.ApothicAutoEnchant.table;
//TODO:메뉴 구현

/*
 * 설계 메모 (2026-07-23 기준)
 * - 목표 UX(화면 요구사항):
 *   1) 입력 슬롯에 아이템이 없을 때, 원본 레벨 선택 구역 Shift+클릭으로 "자동화 토글 모드" 진입.
 *   2) 토글 모드 진입 시 좌측 빈 영역에 ON/OFF, +1/+10/-10/ALL 버튼을 노출.
 *   3) 토글된 레벨 선택 버튼을 다시 누르면 원래 원본 레벨 선택 UI로 복귀.
 *   4) 토글 모드에서 선택된 레벨 버튼은 강조 애니메이션(하단 주석안)으로 표시.
 * - Menu 재설계 방향:
 *   1) "토글 모드 여부"를 메뉴 동기화 상태값으로 유지하고 Screen이 동일 값으로 렌더링하게 한다.
 *   2) 버튼 ID 체계를 "기본 인챈트 버튼"과 "자동화 전용 버튼"으로 명시 분리한다.
 *   3) Shift+클릭 입력은 Screen에서 판별하되, 최종 상태 전이는 Menu 서버 분기에서 확정한다.
 *   4) 입력 슬롯 비어 있음 조건 검증은 서버(Menu/BE)에서도 재검증해 클라 단독 오동작을 막는다.
 * - 리스크/주의:
 *   1) 원본 버튼 ID(0~2) 충돌 시 기존 인챈트 동작이 깨질 수 있으므로 ID/분기 테이블 문서화가 필요하다.
 *   2) 토글 모드 상태 동기화 누락 시 클라이언트 표시와 서버 처리 불일치가 발생한다.
 *   3) 가짜 플레이어/자동화 경로에서는 BE 해석 실패 가능성을 계속 고려한다.
 */

import dev.shadowsoffire.apothic_enchanting.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apothic_enchanting.table.EnchantmentTableItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnchantMenu extends ApothEnchantmentMenu {
    private final BlockPos tablePos;
    private TableBlockEntity be;

    public EnchantMenu(int id, Inventory inv, BlockPos pos) {
        super(id, inv, pos);
        this.tablePos = pos;
        // 메뉴 오픈 시점에 위치 기반으로 BE를 해석해 NPE를 사전에 차단한다.
        this.be = resolveTableBe(inv.player, pos);
        
    }

    public EnchantMenu(int id, Inventory inv, ContainerLevelAccess wPos, EnchantmentTableItemHandler teInv, BlockPos pos) {
        super(id, inv, wPos, teInv, pos);
        this.tablePos = pos;
        // 자동화/가짜 플레이어 경로도 동일한 초기화 규칙을 사용한다.
        this.be = resolveTableBe(inv.player, pos);
    }

    private static TableBlockEntity resolveTableBe(Player player, BlockPos pos) {
        if (player == null || player.level() == null) return null;

        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (blockEntity instanceof TableBlockEntity tableBlockEntity) {
            return tableBlockEntity;
        }
        return null;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0 || id >= 11) {
            Util.logAndPauseIfInIde(player.getName() + " pressed invalid button id: " + id);
            return false;
        }

        //* 버튼 id 3~5 자동부여 레벨 선택
        //       id 6~9 xp 삽입 추출
        //       id 10 자동화 기능 토글   */
        if (id >= 0 && id <= 2) return super.clickMenuButton(player, id);

        if (this.be == null) {
            Util.logAndPauseIfInIde("TableBlockEntity is null at " + this.tablePos + " for player " + player.getName().getString());
            return false;
        }

        switch (id) {
            case 3, 4, 5:
                this.be.costSetter(id);
                break;
            //TODO:xp주입 회수 요청
            case 6:
                this.be.injectAllLv(player);
                break;

            case 7:
                this.be.inject10Lv(player);
                break;
            case 8:
                this.be.eject10Lv(player);
                break;
            case 9:
                this.be.ejectAllLv(player);
                break;

            case 10:
                this.be.toggleAutoEnabled();
                break;

            default:
                return false;

        }
        return true;
    }
}
