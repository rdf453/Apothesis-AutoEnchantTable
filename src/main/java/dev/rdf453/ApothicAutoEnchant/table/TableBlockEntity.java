package dev.rdf453.ApothicAutoEnchant.table;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import dev.rdf453.ApothicAutoEnchant.util.FindBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.item.ItemStack;


/*
 * 설계 메모 (2026-07-22 기준)
 * - 현재 상태:
 *   1) 자동화 상태, 비용 선택, XP 탱크, 도서관 좌표 캐시 저장/복원 골격이 구현되어 있다.
 *   2) doEnchant 본체와 버퍼 전송 분리가 되어 있어 FakePlayer 경유 자동 인챈트 흐름을 잡아둘 수 있다.
 * - 다음 작업:
 *   1) static serverTick 연결과 블록 getTicker 연결을 마무리한다.
 *   2) 슬롯 1 결과물 전송과 비우기 경로를 유지하면서 실패 시 재시도 정책을 확정한다.
 *   3) 화면 표시 동기화용 상태값을 Screen 쪽으로 노출한다.
 * - 리스크/주의:
 *   1) XpTank 저장 타입과 loadAdditional 읽기 타입의 일관성이 필요하다.
 *   2) libraryPos는 Optional.empty() 초기화를 유지해야 탐색 재시도가 가능하다.
 */
public class TableBlockEntity extends EnchantingTableBlockEntity {
    
    
    static final GameProfile gp = new GameProfile(UUID.fromString("eab7b8eb-83a5-eb85-b8ec-9888ec9e8400"), "춘식이");
    boolean setAutoEnabled = false;
    int toggleCost = 3;
    long xpTank = 0;
    Optional<BlockPos> libraryPos = Optional.empty();
    Optional<BlockPos> chestPos = Optional.empty();

    
    //바닐라 인첸트 테이블 블럭엔티티 불러오기
    public TableBlockEntity(BlockPos Pos, BlockState State) {
        super(Pos, State);
    }
    //데이터를 NBT로 저장
    @Override
    protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);
        //커스텀 이름 설정
        if (this.hasCustomName()) {
            output.storeNullable("CustomName", ComponentSerialization.CODEC, this.getCustomName());
        }
        output.putInt("ToggleCost", this.toggleCost);
        output.putBoolean("SetAutoEnabled", this.setAutoEnabled);
        output.putLong("XpTank", this.xpTank);
        output.storeNullable("LibraryPos", BlockPos.CODEC, this.libraryPos.orElse(null));

        
    }
    //NBT데이터를 불러오기
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);

        Component loadedName = input.read("CustomName", ComponentSerialization.CODEC).orElse(null);

        if (loadedName != null) {
            // 부모 클래스에 이름 데이터 세팅
            this.setCustomName(loadedName);
        }
        this.setAutoEnabled = input.getBooleanOr("SetAutoEnabled", false);
        this.toggleCost = input.getIntOr("ToggleCost",0);
        this.xpTank = input.getLongOr("XpTank", 0L);
        this.libraryPos = input.read("LibraryPos", BlockPos.CODEC);
    }

    public void costSetter(int id) {
        AutomationUtils.costSetter(this, id);
    }

    public void toggleAutoEnabled() {
        AutomationUtils.toggleAutoEnabled(this);
    }

    public void inject10Lv(net.minecraft.world.entity.player.Player player) {
        AutomationUtils.inject10Lv(this, player);
    }

    public void injectAllLv(net.minecraft.world.entity.player.Player player) {
        AutomationUtils.injectAllLv(this, player);
    }

    public void eject10Lv(net.minecraft.world.entity.player.Player player) {
        AutomationUtils.eject10Lv(this, player);
    }

    public void ejectAllLv(net.minecraft.world.entity.player.Player player) {
        AutomationUtils.ejectAllLv(this, player);
    }

    public Level tableLevel() {
        return this.level;
    }

    
    private void doEnchant() {
        if(!this.setAutoEnabled) return;
        if(this.libraryPos.isEmpty()&&this.level != null) this.libraryPos = FindBlock.findLibraryPos(this.getBlockPos(),this.level);
        if(this.chestPos.isEmpty()&&this.level != null) this.chestPos = FindBlock.findChestPos(this.getBlockPos(), this.level);

        //서버레벨로 캐스팅
        if(this.level instanceof ServerLevel serverLevel){
            //춘식이 소환
            FakePlayer fp = FakePlayerFactory.get(serverLevel,gp);
            //춘식이 고정
            fp.setPosRaw(
                this.worldPosition.getX(),
                this.worldPosition.getY(),
                this.worldPosition.getZ()
            );
            //임시 메뉴 생성
            EnchantMenu Em = new EnchantMenu(0,fp.getInventory() , this.getBlockPos());
            fp.giveExperiencePoints((int) this.xpTank);
            if(Em.getSlot(1).getItem().getCount()<3) AutomationUtils.bringFuel(this, Em);
            if(!Em.getSlot(0).hasItem()) AutomationUtils.bringBook(this,Em);
            AutomationUtils.doTransfer(this, Em);

            //인첸트 진행
            boolean success = Em.clickMenuButton(fp, toggleCost);

            if(success){
                //춘식이 xp 반환
                this.xpTank = fp.totalExperience;

                
                AutomationUtils.doTransfer(this, Em);
                this.setChanged();
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EnchantingTableBlockEntity blockEntity) {
        if (blockEntity != null) {
            if (blockEntity instanceof TableBlockEntity tableBlockEntity) {
                tableBlockEntity.doEnchant();
            }
        }
    }
}