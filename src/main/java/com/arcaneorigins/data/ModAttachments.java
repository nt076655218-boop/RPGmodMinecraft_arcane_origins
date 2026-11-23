package com.arcaneorigins.data;

import com.arcaneorigins.ArcaneOriginsMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArcaneOriginsMod.MOD_ID);

    public static final Supplier<AttachmentType<PlayerClassData>> CLASS_DATA = ATTACHMENTS.register(
            "class_data",
            () -> AttachmentType.builder(PlayerClassData::new)
                    .serialize(PlayerClassData.CODEC)
                    .copyOnDeath()
                    .build()
    );
}
