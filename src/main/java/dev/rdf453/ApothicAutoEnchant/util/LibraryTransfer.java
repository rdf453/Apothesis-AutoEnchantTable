package dev.rdf453.ApothicAutoEnchant.util;

import net.minecraft.world.item.ItemStack;

import java.util.List;

// 동적 바인드를 위하여 인터페이스로 구현
/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 라이브러리 버퍼에 대한 삽입/전체추출 추상화 인터페이스가 정착되어 Mixin 경유 호출이 가능하다.
 * - 다음 작업:
 *   1) 요청 수량 기반 부분 추출/삽입 API를 확장한다.
 *   2) 실패 사유(용량부족/타입불일치/권한실패)를 구분할 반환 규약을 설계한다.
 * - 리스크/주의:
 *   1) 현재 메서드 명명은 충돌 회피 목적 접두 사용 중이므로 API 안정화 시점에 정리 필요하다.
 */
public interface LibraryTransfer {
    boolean AutoEnch$insertArray(ItemStack stack);

    List<ItemStack> AutoEnch$extractArray();
}