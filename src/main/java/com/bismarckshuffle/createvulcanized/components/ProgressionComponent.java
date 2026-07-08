package com.bismarckshuffle.createvulcanized.components;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public record ProgressionComponent(int progress, int maxProgress) {

    public static final Codec<ProgressionComponent> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.fieldOf("progress").forGetter(ProgressionComponent::progress),
                    Codec.INT.fieldOf("maxProgress").forGetter(ProgressionComponent::maxProgress)
            ).apply(instance, ProgressionComponent::new));


    public static final StreamCodec<FriendlyByteBuf, ProgressionComponent> STREAM_CODEC =
            StreamCodec.of(
                    (buf, value) -> { // Encoder/Writer first
                        buf.writeVarInt(value.progress());
                        buf.writeVarInt(value.maxProgress());
                    },
                    buf -> new ProgressionComponent( // Decoder/Reader second
                            buf.readVarInt(),
                            buf.readVarInt()
                    )
            );

    public boolean isComplete() {
        return progress >= maxProgress;
    }

    public ProgressionComponent withProgress(int newProgress) {
        return new ProgressionComponent(newProgress, maxProgress);
    }
}
