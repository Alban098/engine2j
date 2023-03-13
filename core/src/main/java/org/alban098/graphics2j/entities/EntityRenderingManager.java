/*
 * Copyright (c) 2022-2023, @Author Alban098
 *
 * Code licensed under MIT license.
 */
package org.alban098.graphics2j.entities;

import static org.lwjgl.opengl.GL11.*;

import java.util.*;
import org.alban098.graphics2j.common.Cleanable;
import org.alban098.graphics2j.common.RenderingMode;
import org.alban098.graphics2j.common.Window;
import org.alban098.graphics2j.common.components.Camera;
import org.alban098.graphics2j.entities.renderers.DefaultEntityRenderer;
import org.alban098.graphics2j.entities.renderers.EntityRenderer;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for managing and dispatching rendering for all {@link Entity}s to be rendered by {@link
 * EntityRenderer}s
 */
public final class EntityRenderingManager implements Cleanable {

  /** Just a Logger to log events */
  private static final Logger LOGGER = LoggerFactory.getLogger(EntityRenderingManager.class);

  /**
   * A Map of all registered {@link EntityRenderer} that will render {@link Entity}, indexed by
   * Entity type
   */
  private final Map<Class<? extends Entity>, EntityRenderer<? extends Entity>> entityRenderers;

  /** A List of all registered {@link EntityRenderer}s, used for debugging interfaces */
  private final Set<EntityRenderer<? extends Entity>> rendererList;
  /** The default {@link RenderingMode} */
  private RenderingMode renderingMode = RenderingMode.FILL;

  /** Initializes the Manager and create all mandatory {@link EntityRenderer}s */
  public EntityRenderingManager() {
    this.entityRenderers = new HashMap<>();
    this.rendererList = new HashSet<>();

    // default renderer
    registerRenderer(Entity.class, new DefaultEntityRenderer());

    LOGGER.info("Successfully initialized RendererManager");
  }

  /**
   * Changes the current {@link RenderingMode}
   *
   * @param mode the requested {@link RenderingMode}
   */
  public void setRenderingMode(RenderingMode mode) {
    renderingMode = mode;
    LOGGER.debug("Rendering mode changed to {}", renderingMode);
  }

  /**
   * Attaches a {@link EntityRenderer} to an {@link Entity} forType
   *
   * @param forType the {@link Entity} class forType to attach to
   * @param renderer the {@link EntityRenderer} to attach
   * @param <T> the {@link Entity } forType to attach to
   */
  public <T extends Entity> void registerRenderer(
      Class<T> forType, EntityRenderer<? extends Entity> renderer) {
    entityRenderers.put(forType, renderer);
    rendererList.add(renderer);
    LOGGER.info(
        "Registered new renderer of forType [{}] for entities of forType [{}]",
        renderer.getClass().getName(),
        forType.getName());
  }

  /**
   * Renders a Scene to the screen
   *
   * @param window the {@link Window} to render into
   * @param camera the {@link Camera} to render from
   */
  public void render(Window window, Camera camera) {
    switch (renderingMode) {
      case FILL -> {
        glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        glEnable(GL_TEXTURE_2D);
      }
      case WIREFRAME -> {
        glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        glDisable(GL_TEXTURE_2D);
      }
    }

    // Render objects
    for (EntityRenderer<? extends Entity> renderer : entityRenderers.values()) {
      renderer.render(window, camera);
    }
  }

  /** Clears the Manager by clearing every mapped {@link EntityRenderer} */
  @Override
  public void cleanUp() {
    for (EntityRenderer<? extends Entity> renderer : rendererList) {
      renderer.cleanUp();
    }
  }

  /**
   * Register an {@link Entity} to be rendered until unregistered
   *
   * @param entity the {@link Entity} to register
   */
  public void add(Entity entity) {
    EntityRenderer<Entity> renderer =
        (EntityRenderer<Entity>) entityRenderers.get(entity.getClass());
    if (renderer != null) {
      renderer.register(entity);
    } else {
      EntityRenderer<Entity> defaultRenderer =
          (EntityRenderer<Entity>) entityRenderers.get(Entity.class);
      defaultRenderer.register(entity);
    }
  }

  /**
   * Unregister an {@link Entity} to no longer be rendered
   *
   * @param entity the {@link Entity} to unregister
   */
  public void remove(Entity entity) {
    EntityRenderer<Entity> renderer =
        (EntityRenderer<Entity>) entityRenderers.get(entity.getClass());
    if (renderer != null) {
      renderer.unregister(entity);
    } else {
      EntityRenderer<Entity> defaultRenderer =
          (EntityRenderer<Entity>) entityRenderers.get(Entity.class);
      defaultRenderer.unregister(entity);
    }
    LOGGER.debug(
        "Unregistered an Entity of type [{}] with name {}",
        entity.getClass().getSimpleName(),
        entity.getName());
  }
}
