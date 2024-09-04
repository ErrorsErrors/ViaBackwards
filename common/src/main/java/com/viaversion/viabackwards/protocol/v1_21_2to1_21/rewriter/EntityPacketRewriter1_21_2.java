/*
 * This file is part of ViaBackwards - https://github.com/ViaVersion/ViaBackwards
 * Copyright (C) 2016-2024 ViaVersion and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.viaversion.viabackwards.protocol.v1_21_2to1_21.rewriter;

import com.viaversion.viabackwards.api.rewriters.EntityRewriter;
import com.viaversion.viabackwards.protocol.v1_21_2to1_21.Protocol1_21_2To1_21;
import com.viaversion.viaversion.api.minecraft.RegistryEntry;
import com.viaversion.viaversion.api.minecraft.entities.EntityType;
import com.viaversion.viaversion.api.minecraft.entities.EntityTypes1_20_5;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.remapper.PacketHandlers;
import com.viaversion.viaversion.api.type.Types;
import com.viaversion.viaversion.api.type.types.version.Types1_21;
import com.viaversion.viaversion.api.type.types.version.Types1_21_2;
import com.viaversion.viaversion.protocols.v1_20_3to1_20_5.packet.ServerboundPackets1_20_5;
import com.viaversion.viaversion.protocols.v1_20_5to1_21.packet.ClientboundConfigurationPackets1_21;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPacket1_21_2;
import com.viaversion.viaversion.protocols.v1_21to1_21_2.packet.ClientboundPackets1_21_2;
import com.viaversion.viaversion.util.Key;
import java.util.BitSet;

import static com.viaversion.viaversion.protocols.v1_21to1_21_2.rewriter.EntityPacketRewriter1_21_2.updateEnchantmentAttributes;

public final class EntityPacketRewriter1_21_2 extends EntityRewriter<ClientboundPacket1_21_2, Protocol1_21_2To1_21> {

    public EntityPacketRewriter1_21_2(final Protocol1_21_2To1_21 protocol) {
        super(protocol, Types1_21.ENTITY_DATA_TYPES.optionalComponentType, Types1_21.ENTITY_DATA_TYPES.booleanType);
    }

    @Override
    public void registerPackets() {
        registerTrackerWithData1_19(ClientboundPackets1_21_2.ADD_ENTITY, EntityTypes1_20_5.FALLING_BLOCK);
        registerSetEntityData(ClientboundPackets1_21_2.SET_ENTITY_DATA, Types1_21_2.ENTITY_DATA_LIST, Types1_21.ENTITY_DATA_LIST);
        registerRemoveEntities(ClientboundPackets1_21_2.REMOVE_ENTITIES);

        protocol.registerClientbound(ClientboundConfigurationPackets1_21.REGISTRY_DATA, wrapper -> {
            final String registryKey = Key.stripMinecraftNamespace(wrapper.passthrough(Types.STRING));
            final RegistryEntry[] entries = wrapper.passthrough(Types.REGISTRY_ENTRY_ARRAY);
            if (registryKey.equals("enchantment")) {
                updateEnchantmentAttributes(entries, protocol.getMappingData().getAttributeMappings());
            }

            handleRegistryData1_20_5(wrapper.user(), registryKey, entries);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.LOGIN, new PacketHandlers() {
            @Override
            public void register() {
                map(Types.INT); // Entity id
                map(Types.BOOLEAN); // Hardcore
                map(Types.STRING_ARRAY); // World List
                map(Types.VAR_INT); // Max players
                map(Types.VAR_INT); // View distance
                map(Types.VAR_INT); // Simulation distance
                map(Types.BOOLEAN); // Reduced debug info
                map(Types.BOOLEAN); // Show death screen
                map(Types.BOOLEAN); // Limited crafting
                map(Types.VAR_INT); // Dimension key
                map(Types.STRING); // World
                map(Types.LONG); // Seed
                map(Types.BYTE); // Gamemode
                map(Types.BYTE); // Previous gamemode
                map(Types.BOOLEAN); // Debug
                map(Types.BOOLEAN); // Flat
                map(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
                map(Types.VAR_INT); // Portal cooldown
                handler(worldDataTrackerHandlerByKey1_20_5(3));
                handler(playerTrackerHandler());
                read(Types.VAR_INT); // Sea level
            }
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.RESPAWN, wrapper -> {
            final int dimensionId = wrapper.passthrough(Types.VAR_INT);
            final String world = wrapper.passthrough(Types.STRING);
            wrapper.passthrough(Types.LONG); // Seed
            wrapper.passthrough(Types.BYTE); // Gamemode
            wrapper.passthrough(Types.BYTE); // Previous gamemode
            wrapper.passthrough(Types.BOOLEAN); // Debug
            wrapper.passthrough(Types.BOOLEAN); // Flat
            wrapper.passthrough(Types.OPTIONAL_GLOBAL_POSITION); // Last death location
            wrapper.passthrough(Types.VAR_INT); // Portal cooldown

            wrapper.read(Types.VAR_INT); // Sea level
            trackWorldDataByKey1_20_5(wrapper.user(), dimensionId, world);
        });

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_POSITION, wrapper -> {
            final int teleportId = wrapper.read(Types.VAR_INT);

            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z

            double movementX = wrapper.read(Types.DOUBLE);
            double movementY = wrapper.read(Types.DOUBLE);
            double movementZ = wrapper.read(Types.DOUBLE);

            // Unpack y and x rot
            final byte yaw = wrapper.read(Types.BYTE);
            final byte pitch = wrapper.read(Types.BYTE);
            wrapper.write(Types.FLOAT, yaw * 360F / 256F);
            wrapper.write(Types.FLOAT, pitch * 360F / 256F);

            // Just keep the new values in there
            final int relativeArguments = wrapper.read(Types.INT);
            wrapper.write(Types.BYTE, (byte) relativeArguments);
            wrapper.write(Types.VAR_INT, teleportId);

            if (true) {
                return; // TODO
            }

            // Send alongside separate entity motion
            wrapper.send(Protocol1_21_2To1_21.class);
            wrapper.cancel();

            if ((relativeArguments & 1 << 4) != 0) {
                // Rotate delta
            }

            // Delta x, y, z
            if ((relativeArguments & 1 << 5) != 0) {
                //movementX += currentMovementX;
            }
            if ((relativeArguments & 1 << 6) != 0) {
                //movementY += currentMovementY;
            }
            if ((relativeArguments & 1 << 7) != 0) {
                //movementZ += currentMovementZ;
            }

            final PacketWrapper entityMotionPacket = wrapper.create(ClientboundPackets1_21_2.MOVE_ENTITY_POS);
            entityMotionPacket.write(Types.VAR_INT, tracker(wrapper.user()).clientEntityId());
            entityMotionPacket.write(Types.SHORT, (short) (movementX * 8000));
            entityMotionPacket.write(Types.SHORT, (short) (movementY * 8000));
            entityMotionPacket.write(Types.SHORT, (short) (movementZ * 8000));
            entityMotionPacket.send(Protocol1_21_2To1_21.class);
        });

        // Now also sent by the player if not in a vehicle, but we can't emulate that here, and otherwise only used in predicates
        protocol.registerServerbound(ServerboundPackets1_20_5.PLAYER_INPUT, wrapper -> {
            final float sideways = wrapper.read(Types.FLOAT);
            final float forward = wrapper.read(Types.FLOAT);
            final byte flags = wrapper.read(Types.BYTE);

            byte updatedFlags = 0;
            if (forward < 0) {
                updatedFlags |= 1;
            } else if (forward > 0) {
                updatedFlags |= 1 << 1;
            }

            if (sideways < 0) {
                updatedFlags |= 1 << 2;
            } else if (sideways > 0) {
                updatedFlags |= 1 << 3;
            }

            if ((flags & 1) != 0) {
                updatedFlags |= 1 << 4;
            }
            if ((flags & 2) != 0) {
                updatedFlags |= 1 << 5;
            }

            // Sprinting we don't know...

            wrapper.write(Types.BYTE, updatedFlags);
        });

        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            fixOnGround(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_POS_ROT, wrapper -> {
            wrapper.passthrough(Types.DOUBLE); // X
            wrapper.passthrough(Types.DOUBLE); // Y
            wrapper.passthrough(Types.DOUBLE); // Z
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.passthrough(Types.FLOAT); // Pitch
            fixOnGround(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_ROT, wrapper -> {
            wrapper.passthrough(Types.FLOAT); // Yaw
            wrapper.passthrough(Types.FLOAT); // Pitch
            fixOnGround(wrapper);
        });
        protocol.registerServerbound(ServerboundPackets1_20_5.MOVE_PLAYER_STATUS_ONLY, this::fixOnGround);

        protocol.registerClientbound(ClientboundPackets1_21_2.PLAYER_INFO_UPDATE, wrapper -> {
            final BitSet actions = wrapper.passthrough(Types.PROFILE_ACTIONS_ENUM);
            final int entries = wrapper.passthrough(Types.VAR_INT);
            for (int i = 0; i < entries; i++) {
                wrapper.passthrough(Types.UUID);
                if (actions.get(0)) {
                    wrapper.passthrough(Types.STRING); // Player Name

                    final int properties = wrapper.passthrough(Types.VAR_INT);
                    for (int j = 0; j < properties; j++) {
                        wrapper.passthrough(Types.STRING); // Name
                        wrapper.passthrough(Types.STRING); // Value
                        wrapper.passthrough(Types.OPTIONAL_STRING); // Signature
                    }
                }
                if (actions.get(1) && wrapper.passthrough(Types.BOOLEAN)) {
                    wrapper.passthrough(Types.UUID); // Session UUID
                    wrapper.passthrough(Types.PROFILE_KEY);
                }
                if (actions.get(2)) {
                    wrapper.passthrough(Types.VAR_INT); // Gamemode
                }
                if (actions.get(3)) {
                    wrapper.passthrough(Types.BOOLEAN); // Listed
                }
                if (actions.get(4)) {
                    wrapper.passthrough(Types.VAR_INT); // Latency
                }
                if (actions.get(5)) {
                    wrapper.passthrough(Types.TAG); // Display name
                }

                // New one
                if (actions.get(6)) {
                    actions.clear(6);
                    wrapper.read(Types.VAR_INT); // List order
                }
            }
        });
    }

    private void fixOnGround(final PacketWrapper wrapper) {
        final boolean data = wrapper.read(Types.BOOLEAN);
        wrapper.write(Types.UNSIGNED_BYTE, data ? (short) 1 : 0); // Carries more data now
    }

    @Override
    protected void registerRewrites() {
        filter().mapDataType(Types1_21.ENTITY_DATA_TYPES::byId);
        registerEntityDataTypeHandler1_20_3(
            Types1_21.ENTITY_DATA_TYPES.itemType,
            Types1_21.ENTITY_DATA_TYPES.blockStateType,
            Types1_21.ENTITY_DATA_TYPES.optionalBlockStateType,
            Types1_21.ENTITY_DATA_TYPES.particleType,
            Types1_21.ENTITY_DATA_TYPES.particlesType,
            Types1_21.ENTITY_DATA_TYPES.componentType,
            Types1_21.ENTITY_DATA_TYPES.optionalComponentType
        );
        registerBlockStateHandler(EntityTypes1_20_5.ABSTRACT_MINECART, 11);

        filter().type(EntityTypes1_20_5.SALMON).removeIndex(17); // Data type
        filter().type(EntityTypes1_20_5.DOLPHIN).removeIndex(16); // Baby
        filter().type(EntityTypes1_20_5.GLOW_SQUID).removeIndex(16); // Baby
        filter().type(EntityTypes1_20_5.SQUID).removeIndex(16); // Baby
    }

    @Override
    public EntityType typeFromId(final int type) {
        return EntityTypes1_20_5.getTypeFromId(type);
    }
}
