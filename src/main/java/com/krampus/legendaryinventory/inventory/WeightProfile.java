package com.krampus.legendaryinventory.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record WeightProfile(int bonus, boolean rowCueShown) {

    public static final WeightProfile DEFAULT = new WeightProfile(0, false);

    public static final Codec<WeightProfile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.fieldOf("bonus").forGetter(WeightProfile::bonus),
        Codec.BOOL.fieldOf("row_cue_shown").forGetter(WeightProfile::rowCueShown)
    ).apply(instance, WeightProfile::new));

    public WeightProfile withBonus(int value) {
        return new WeightProfile(value, rowCueShown);
    }

    public WeightProfile withRowCueShown(boolean value) {
        return new WeightProfile(bonus, value);
    }
}
