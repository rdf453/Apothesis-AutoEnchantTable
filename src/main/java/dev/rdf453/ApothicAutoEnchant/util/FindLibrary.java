package dev.rdf453.ApothicAutoEnchant.util;

import java.util.Optional;


import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class FindLibrary {
    

    public static Optional<BlockPos> findLibraryPos(BlockPos pos, Level level) {
        // 인첸트 테이블 기준 반경 5칸 이내에서 도서관 블럭 엔티티의 좌표를 찾는다.
        BlockPos minPos = pos.offset(-5,-5,-5);
        BlockPos maxPos = pos.offset(5,5,5);

        
        for(BlockPos targetPos: BlockPos.betweenClosed(minPos,maxPos)) {
            BlockEntity be = level.getBlockEntity(targetPos);

            if(be instanceof EnchLibraryTile) return Optional.of(targetPos.immutable());


        }
        // 도서관을 찾지 못하면 비어 있음으로 반환해 토글 차단/재탐색에 사용한다.
        return Optional.empty();
    }
}
