package eu.pollux28.imggen.world.terrain;

import com.mojang.serialization.MapCodec;
import eu.pollux28.imggen.ImgGen;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseRouter;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * A vanilla climate parameter (continentalness, erosion or weirdness) bent towards the biome image.
 *
 * <p>These functions only ever live inside the settings a generator builds for itself at runtime; the
 * world's saved settings keep pointing at the untouched noise settings, so nothing here is serialised.
 */
public record ImageClimateFunction(DensityFunction input, ImageTerrain.Parameter parameter, ImageTerrain terrain) implements DensityFunction {
    /**
     * Replaces the climate parameters everywhere they are used in the router. Vanilla derives terrain
     * height, steepness and jaggedness from exactly these three functions, so steering them reshapes the
     * land while every other part of the noise settings (caves, aquifers, ore veins) keeps working.
     */
    public static NoiseRouter steer(NoiseRouter router, ImageTerrain terrain) {
        Map<DensityFunction, ImageTerrain.Parameter> targets = new HashMap<>();
        addTarget(targets, router.continents(), ImageTerrain.Parameter.CONTINENTALNESS);
        addTarget(targets, router.erosion(), ImageTerrain.Parameter.EROSION);
        addTarget(targets, router.ridges(), ImageTerrain.Parameter.WEIRDNESS);

        Set<ImageTerrain.Parameter> missing = EnumSet.allOf(ImageTerrain.Parameter.class);
        missing.removeAll(targets.values());
        if (!missing.isEmpty()) {
            ImgGen.LOGGER.warn("The noise settings have no usable {} function; terrain will not follow the biome image there", missing);
        }
        if (targets.isEmpty()) {
            return router;
        }

        return router.mapAll(function -> {
            ImageTerrain.Parameter parameter = targets.get(function);
            return parameter == null
                    ? function
                    : DensityFunctions.flatCache(new ImageClimateFunction(function, parameter, terrain));
        });
    }

    /**
     * Mapping a router rebuilds every function bottom-up and drops registry references on the way, so a
     * function can only be recognised by structure. Passing the wanted function through an identity
     * mapping gives it the same shape the visitor will see later.
     */
    private static void addTarget(Map<DensityFunction, ImageTerrain.Parameter> targets, DensityFunction function, ImageTerrain.Parameter parameter) {
        if (function.minValue() == function.maxValue()) {
            return; // A constant, as in the Nether and End settings: nothing to steer.
        }

        DensityFunction normalized = function.mapAll(identity -> identity);
        if (normalized instanceof DensityFunctions.HolderHolder(var holder)) {
            normalized = holder.value();
        }
        targets.putIfAbsent(normalized, parameter);
    }

    @Override
    public double compute(FunctionContext context) {
        return terrain.apply(parameter, input.compute(context), context.blockX(), context.blockZ());
    }

    @Override
    public void fillArray(double[] values, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(values, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new ImageClimateFunction(input.mapAll(visitor), parameter, terrain));
    }

    @Override
    public double minValue() {
        return Math.min(input.minValue(), -1.0);
    }

    @Override
    public double maxValue() {
        return Math.max(input.maxValue(), 1.0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return KeyDispatchDataCodec.of(MapCodec.unit(this));
    }
}
