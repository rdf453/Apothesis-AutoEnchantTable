package dev.rdf453.ApothicAutoEnchant.MIxin;

//TODO:플레이어의 상호작용에 반응하여 ui의 입력부에 넣기
import dev.rdf453.ApothicAutoEnchant.util.LibraryTransfer;
import dev.shadowsoffire.apothic_enchanting.library.EnchLibraryBlock;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(EnchLibraryBlock.class)
public class LibraryBlockMixin {

    @SubscribeEvent
    public void onUse(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return;
        }

        BlockEntity blockEntity = event.getLevel().getBlockEntity(event.getPos());
        if (blockEntity instanceof LibraryTransfer transfer) {
            List<ItemStack> extracted = transfer.AutoEnch$extractArray();
            for (ItemStack stack : extracted) {
                if (!stack.isEmpty()) {
                    // 나중에 UI 입력칸에 넣는 로직으로 연결할 자리.
                }
            }
        }
    }
}
