package dev.rdf453.ApothicAutoEnchant.table;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

//블럭 엔티티 설정
import dev.rdf453.ApothicAutoEnchant.table.EnchantMenu;
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


/*
 * 설계 메모 (2026-07-21 기준)
 * - 현재 상태:
 *   1) 자동화 상태(setAutoEnabled), 비용 선택(toggleCost), XP 탱크(xpTank) 필드와 NBT 저장/복원 골격이 구현되어 있다.
 *   2) doEnchant 본체가 추가되어 FakePlayer + EnchantMenu 경유 인챈트 시도가 가능하다.
 * - 다음 작업:
 *   1) 서버 tick 연결(static/serverTick + 블록 getTicker)로 doEnchant 호출 경로를 완성한다.
 *   2) 주변 컨테이너/도서관 연동 단계(입력 공급/결과 전송)를 순차적으로 붙인다.
 *   3) 메뉴 표시 동기화를 위한 getter 또는 DataSlot 연결을 추가한다.
 * - 리스크/주의:
 *   1) 현재 XpTank 저장은 putLong/readInt 조합이라 타입 일관성 점검이 필요하다.
 *   2) LibraryTransfer import는 아직 미사용이며 도서관 연동 구현 시점까지 정리 대상이다.
 */
public class TableBlockEntity extends EnchantingTableBlockEntity {
    public boolean setAutoEnabled = false; 
    private int toggleCost=3;
    private int xpTank=0;
    
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
    }

    
    public void costSetter(int id) {
        this.toggleCost=id-3;
        setChanged();
    }

    public void toggleAutoEnabled() {
        this.setAutoEnabled = !this.setAutoEnabled;
        setChanged();
        }
    
    public void inject10Lv(Player player) {

        this.xpTank += -XpTransfer.getXpNeedPoint(player, Math.max(0,player.experienceLevel-10));

        player.giveExperiencePoints((int)XpTransfer.getXpNeedPoint(player, Math.max(0,player.experienceLevel-10)));
        setChanged();
    }    

    public void injectAllLv(Player player) {
        if(player.experienceLevel <= 0) return;
        this.xpTank -= XpTransfer.getXpNeedPoint(player, 0);

        player.giveExperiencePoints((int)XpTransfer.getXpNeedPoint(player, 0));
        setChanged();
    }

    public void eject10Lv(Player player) {
        if(this.xpTank < 0) return;
        int Tlqkf = (int)XpTransfer.getXpNeedPoint(player, player.experienceLevel+10);

        if(this.xpTank < Tlqkf) return;
        this.xpTank -= Tlqkf;
        player.giveExperiencePoints(Tlqkf);
        
        setChanged();
    }

    public void ejectAllLv(Player player) {
        player.giveExperiencePoints((int)this.xpTank);
        this.xpTank = 0;
        setChanged();
        
    }

    private void doEnchant() {
        if(!this.setAutoEnabled) return;

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