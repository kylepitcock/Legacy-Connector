package ic2.api;

/** Test double of IC2 1.103's storage interface (the same three methods as the real one). */
public interface IEnergyStorage {
    int getStored();

    int getCapacity();

    int getOutput();
}
