package dev.rdf453.ApothicAutoEnchant.table;
//TODO:메뉴 구현

/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 커스텀 버튼 ID(3~10) 서버 분기와 TableBlockEntity 호출 경로가 구현되어 있다.
 *   2) 생성자에서 BlockPos 기반으로 BE를 해석하고 null 가드로 NPE를 방지한다.
 * - 다음 작업:
 *   1) 메뉴 타입 등록과 Screen 매핑 등록을 완료해 실제 UI 진입 경로를 연결한다.
 *   2) 버튼 처리 전에 거리/블록 일치 권한 검증을 공통 분기로 추가한다.
 *   3) 화면 동기화용 상태 값(자동화 ON/OFF, xpTank, 비용 레벨) 노출 경로를 정리한다.
 * - 리스크/주의:
 *   1) 자동화 경로에서 가짜 플레이어 인벤토리 기반 생성 시 BE 해석 실패 가능성은 로그로만 처리되므로 재시도 정책이 필요할 수 있다.
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
