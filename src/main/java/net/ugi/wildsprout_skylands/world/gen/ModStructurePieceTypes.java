package net.ugi.wildsprout_skylands.world.gen;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.ugi.wildsprout_skylands.WildsproutSkylands;
import net.ugi.wildsprout_skylands.world.gen.structure.piece.FloatingIslandBridgePiece;
import net.ugi.wildsprout_skylands.world.gen.structure.piece.FloatingIslandPiece;

import java.util.function.Supplier;

public class ModStructurePieceTypes {
    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, WildsproutSkylands.MODID);

        public static final Supplier<StructurePieceType> FLOATING_ISLAND_PIECE =
            STRUCTURE_PIECE_TYPES.register("floating_island_piece",
                    () -> (StructurePieceType.ContextlessType) FloatingIslandPiece::new);

        public static final Supplier<StructurePieceType> FLOATING_ISLAND_BRIDGE =
            STRUCTURE_PIECE_TYPES.register("floating_island_bridge",
                    () -> (StructurePieceType.ContextlessType) FloatingIslandBridgePiece::new);


    public static void register(IEventBus eventBus) {
        STRUCTURE_PIECE_TYPES.register(eventBus);
    }
}
