package com.example.client;

import com.example.ModEntities;
import com.example.client.entity.NpcRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class BsbairquestClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(ModEntities.FABRIKA_MUDURU,   NpcRenderer::new);
        EntityRendererRegistry.register(ModEntities.BELEDIYE_BASKANI, NpcRenderer::new);
        EntityRendererRegistry.register(ModEntities.MAHALLE_MUHTARI,  NpcRenderer::new);
    }
}
