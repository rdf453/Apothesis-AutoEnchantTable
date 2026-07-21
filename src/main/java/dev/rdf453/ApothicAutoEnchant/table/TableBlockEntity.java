package dev.rdf453.ApothicAutoEnchant.table;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

//블럭 엔티티 설정
import dev.rdf453.ApothicAutoEnchant.table.EnchantMenu;
import dev.rdf453.ApothicAutoEnchant.util.FindLibrary;
import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.rdf453.ApothicAutoEnchant.util.XpTransfer;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 자동화 상태(setAutoEnabled), 비용 선택(toggleCost), XP 탱크(xpTank), 도서관 좌표 캐시(libraryPos) 저장/복원 골격이 구현되어 있다.
 *   2) doEnchant 본체가 추가되어 FakePlayer + EnchantMenu 경유 인챈트 시도가 가능하다.
 * - 다음 작업:
 *   1) 서버 tick 연결(static/serverTick + 블록 getTicker)로 doEnchant 호출 경로를 완성한다.
 *   2) 슬롯 1 결과물 복사/비우기와 도서관 버퍼 전송 경로를 doEnchant 후단에 붙인다.
 *   3) 메뉴 표시 동기화용 상태값을 Screen 쪽으로 노출한다.
 * - 리스크/주의:
 *   1) 현재 XpTank 저장은 putLong/readInt 조합이라 타입 일관성 점검이 필요하다.
 *   2) libraryPos는 Optional.empty() 초기화를 유지해야 NPE 없이 탐색 재시도가 가능하다.
 */
public class TableBlockEntity extends EnchantingTableBlockEntity {
    public boolean setAutoEnabled = false; 
    private int toggleCost=3;
    private int xpTank=0;
    private Optional<BlockPos> libraryPos = Optional.empty();
    
    static final GameProfile gp = new GameProfile(UUID.fromString("eab7b8eb-83a5-eb85-b8ec-9888ec9e8400"), "춘식이");
    
    //바닐라 인첸트 테이블 블럭엔티티 불러오기
    public TableBlockEntity(BlockPos Pos, BlockState State) {
        super(Pos,State);
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
        this.xpTank = input.getIntOr("XpTank", 0);
        this.libraryPos = input.read("LibraryPos", BlockPos.CODEC);
    }

    //id 매핑
    public void costSetter(int id) {
        this.toggleCost=id-3;
        setChanged();
    }
    //자동화 토글
    public void toggleAutoEnabled() {
        this.setAutoEnabled = !this.setAutoEnabled;
        setChanged();
        }
    //10레벨 주입
    public void inject10Lv(Player player) {

        this.xpTank += -XpTransfer.getXpNeedPoint(player, Math.max(0,player.experienceLevel-10));

        player.giveExperiencePoints((int)XpTransfer.getXpNeedPoint(player, Math.max(0,player.experienceLevel-10)));
        setChanged();
    }    
    //모든 레벨 주입
    public void injectAllLv(Player player) {
        if(player.experienceLevel <= 0) return;
        this.xpTank -= XpTransfer.getXpNeedPoint(player, 0);

        player.giveExperiencePoints((int)XpTransfer.getXpNeedPoint(player, 0));
        setChanged();
    }
    //10레벨 회수
    public void eject10Lv(Player player) {
        if(this.xpTank < 0) return;
        int Tlqkf = (int)XpTransfer.getXpNeedPoint(player, player.experienceLevel+10);

        if(this.xpTank < Tlqkf) return;
        this.xpTank -= Tlqkf;
        player.giveExperiencePoints(Tlqkf);
        
        setChanged();
    }
    //레벨 전부 회수
    public void ejectAllLv(Player player) {
        player.giveExperiencePoints((int)this.xpTank);
        this.xpTank = 0;
        setChanged();
        
    }

    private ItemStack copyResult(EnchantMenu em){
        return em.getSlot(1).getItem().copy();
    }
    private void ClearSlot(EnchantMenu em) {
        em.getSlot(1).set(ItemStack.EMPTY);
    }

    private void doEnchant() {
        if(!this.setAutoEnabled) return;
        if(this.libraryPos.isEmpty()&&this.level != null) this.libraryPos = FindLibrary.findLibraryPos(this.getBlockPos(),this.level);

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
            fp.giveExperienceLevels(this.xpTank); 

            //인첸트 진행
            boolean success = Em.clickMenuButton(fp, toggleCost);

            if(success){
                this.xpTank = fp.totalExperience;
                this.setChanged();
            }
        }
    }

    //도서관으로 전송
}