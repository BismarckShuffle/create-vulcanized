package com.bismarckshuffle.createvulcanized.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ProgressionComponent(int progress, int maxProgress, int stackCount) {

    public ProgressionComponent(int progress, int maxProgress) {
        this(progress, maxProgress, 1);
    }

    public static final Codec<ProgressionComponent> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("progress").forGetter(ProgressionComponent::progress),
                    Codec.INT.fieldOf("maxProgress").forGetter(ProgressionComponent::maxProgress),
                    Codec.INT.fieldOf("stackCount").forGetter(ProgressionComponent::stackCount)
            ).apply(instance, ProgressionComponent::new));


    public static final StreamCodec<FriendlyByteBuf, ProgressionComponent> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> { // Encoder/Writer first
                        buf.writeVarInt(value.progress());
                        buf.writeVarInt(value.maxProgress());
                        buf.writeVarInt(value.stackCount());
                    },
                    buf -> new ProgressionComponent( // Decoder/Reader second
                            buf.readVarInt(),
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );
}
