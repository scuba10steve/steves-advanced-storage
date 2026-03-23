package io.github.scuba10steve.s3.advanced.blockentity;

import net.neoforged.neoforge.energy.EnergyStorage;

/** Energy storage for generator blocks: no external receive, can push/extract. */
public class GeneratorEnergyStorage extends EnergyStorage {

    public GeneratorEnergyStorage(int capacity, int maxExtract) {
        super(capacity, 0, maxExtract);
    }

    /** Add generated energy to the buffer. Returns actual amount stored. */
    public int generate(int amount) {
        int generated = Math.min(amount, capacity - energy);
        energy += generated;
        return generated;
    }

    public void setEnergy(int amount) {
        this.energy = Math.min(amount, capacity);
    }
}
