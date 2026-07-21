package dev.rdf453.ApothicAutoEnchant.table;

//TODO:블럭의 속성 설정

import dev.shadowsoffire.apothic_enchanting.table.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) ApothEnchantingTableBlock 상속 골격만 있고 커스텀 동작 오버라이드는 아직 없다.
 * - 다음 작업:
 *   1) newBlockEntity/getTicker를 구현해 TableBlockEntity와 서버 tick 루프를 연결한다.
 *   2) use 상호작용 시 EnchantMenu 오픈 경로를 추가한다.
 *   3) 필요 시 자동화 상태 시각화용 BlockState 프로퍼티와 파괴 드롭 정책을 확정한다.
 * - 리스크/주의:
 *   1) 현재 import 다수가 미사용이며 실제 구현 전까지는 컴파일 경고 관리가 필요하다.
 */
public class AutoEnchantingTableBlock extends ApothEnchantingTableBlock {

    public AutoEnchantingTableBlock(Block.Properties prop) {
        super(prop);
    }

    
}