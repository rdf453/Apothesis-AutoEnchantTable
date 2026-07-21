package dev.rdf453.ApothicAutoEnchant.util;

import net.minecraft.world.entity.player.Player;

/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 레벨 구간별 누적 XP 계산식과 목표 레벨까지 필요한 포인트 계산 함수가 구현되어 있다.
 * - 다음 작업:
 *   1) TableBlockEntity 주입/회수 시나리오에 맞춘 래퍼 함수를 분리해 호출부 부호 실수를 줄인다.
 *   2) 경계값(0레벨, 고레벨, 부분 경험치 바) 테스트 케이스를 추가한다.
 * - 리스크/주의:
 *   1) getXpNeedPoint는 targetLevel <= currentLevel일 때 0을 반환하므로 감소/회수 계산 용도와 분리해 써야 한다.
 */
public class XpTransfer {
    
    


    private static int getXpForLevel(int level) {
        if (level <= 0) return 0;
        if (level <= 16) return level * level + 6 * level;
        if (level <= 31) return (int) (2.5 * level * level - 40.5 * level + 360);
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    
    public static long getXpNeedPoint(Player player, int targetLevel) {
        int currentLevel = player.experienceLevel;

        if (targetLevel <= currentLevel) return 0;

        // 목표 레벨까지의 단순 레벨 차이 XP
        long xpForLevels = getXpForLevel(targetLevel) - getXpForLevel(currentLevel);

        // 현재 경험치 바에 이미 채워져 있는 XP 포인트 (이미 얻은 거니까 차감해야 함)
        long currentBarXp = Math.round(player.getXpNeededForNextLevel() * player.experienceProgress);

        return xpForLevels - currentBarXp;
    }
}



    
