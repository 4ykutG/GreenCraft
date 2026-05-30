package com.example.client.entity;

import com.example.entity.NpcEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class NpcRenderer extends MobRenderer<NpcEntity, HumanoidModel<NpcEntity>> {

    // Her NPC rolü için ayrı doku — vanilla köylü dokusunu geçici kullanıyoruz,
    // src/main/resources/assets/bsb-airquest/textures/entity/ altına kendi PNG'lerini ekleyebilirsin.
    private static final ResourceLocation TEX_FABRIKA  =
        ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final ResourceLocation TEX_BELEDIYE =
        ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final ResourceLocation TEX_MAHALLE  =
        ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");
    private static final ResourceLocation TEX_DEFAULT  =
        ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public NpcRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.ZOMBIE)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity entity) {
        var name = entity.getCustomName();
        if (name == null) return TEX_DEFAULT;
        return switch (name.getString()) {
            case "Fabrika Müdürü"   -> TEX_FABRIKA;
            case "Belediye Başkanı" -> TEX_BELEDIYE;
            case "Mahalle Muhtarı"  -> TEX_MAHALLE;
            default                 -> TEX_DEFAULT;
        };
    }
}
