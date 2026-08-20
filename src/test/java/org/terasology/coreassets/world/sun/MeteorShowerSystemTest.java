// Copyright 2026 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.coreassets.world.sun;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.terasology.engine.core.ComponentSystemManager;
import org.terasology.engine.entitySystem.entity.EntityManager;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.systems.ComponentSystem;
import org.terasology.engine.integrationenvironment.jupiter.IntegrationEnvironment;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.particles.components.ParticleDataSpriteComponent;
import org.terasology.engine.particles.components.ParticleEmitterComponent;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.sun.MeteorShowerSystem;

import java.util.List;

import static com.google.common.truth.Truth.assertThat;

/**
 * Regression/behavior test for #97's meteor shower implementation. {@link MeteorShowerSystem} lives in
 * the engine (it's a general sky/celestial-event feature, only referencing the particle effect prefab by
 * urn - the same "stealth dependency on CoreAssets" pattern {@code BlockEntitySystem}'s dust effect
 * already uses), but exercising it against a real, loaded {@code CoreAssets:meteorShowerParticleEffect}
 * prefab needs to happen from a test that actually depends on CoreAssets - hence this lives here rather
 * than in engine-tests.
 */
@IntegrationEnvironment(dependencies = "CoreAssets")
public class MeteorShowerSystemTest {

    @In
    private EntityManager entityManager;

    @In
    private ComponentSystemManager componentSystemManager;

    @Test
    public void prefabIsAWellFormedParticleEmitter() {
        EntityRef meteor = entityManager.create("CoreAssets:meteorShowerParticleEffect", new Vector3f());
        try {
            assertThat(meteor.exists()).isTrue();
            assertThat(meteor.hasComponent(LocationComponent.class)).isTrue();
            assertThat(meteor.hasComponent(ParticleEmitterComponent.class)).isTrue();
            assertThat(meteor.hasComponent(ParticleDataSpriteComponent.class)).isTrue();
            assertThat(meteor.getComponent(ParticleDataSpriteComponent.class).texture).isNotNull();
        } finally {
            meteor.destroy();
        }
    }

    @Test
    public void spawnShowerAroundCreatesMeteorsWithinTheDocumentedBounds() {
        MeteorShowerSystem system = findRegisteredSystem();

        Vector3f playerPosition = new Vector3f(100, 64, -200);
        List<EntityRef> meteors = system.spawnShowerAround(playerPosition);
        try {
            assertThat(meteors.size()).isAtLeast(3);
            assertThat(meteors.size()).isAtMost(7);

            for (EntityRef meteor : meteors) {
                assertThat(meteor.exists()).isTrue();
                assertThat(meteor.hasComponent(ParticleEmitterComponent.class)).isTrue();

                Vector3f pos = meteor.getComponent(LocationComponent.class).getWorldPosition(new Vector3f());
                float heightAbovePlayer = pos.y - playerPosition.y;
                assertThat(heightAbovePlayer).isAtLeast(40f);
                assertThat(heightAbovePlayer).isAtMost(80f);
                assertThat(Math.abs(pos.x - playerPosition.x)).isAtMost(60f);
                assertThat(Math.abs(pos.z - playerPosition.z)).isAtMost(60f);
            }
        } finally {
            meteors.forEach(EntityRef::destroy);
        }
    }

    /** The engine auto-registers one real, fully @In-injected instance; use that rather than a fresh one. */
    private MeteorShowerSystem findRegisteredSystem() {
        for (ComponentSystem componentSystem : componentSystemManager.getAllSystems()) {
            if (componentSystem instanceof MeteorShowerSystem) {
                return (MeteorShowerSystem) componentSystem;
            }
        }
        throw new AssertionError("MeteorShowerSystem was not registered - is it @RegisterSystem(RegisterMode.AUTHORITY)?");
    }
}
