/*
 * Copyright (c) 2022, @Author Alban098
 *
 * Code licensed under MIT license.
 */
package rendering.entities;

import java.util.*;
import rendering.entities.component.Component;

public abstract class Entity {

  protected final String name;
  protected final Map<Class<? extends Component>, Component> components;
  protected final List<Entity> children;
  protected Entity parent;

  public Entity() {
    this(null);
  }

  public Entity(String name) {
    this.components = new HashMap<>();
    this.children = new ArrayList<>();
    this.name = name == null ? Integer.toHexString(hashCode()) : name;
  }

  public final void addChild(Entity entity) {
    children.add(entity);
    entity.setParent(this);
  }

  public final void removeChild(Entity entity) {
    children.remove(entity);
    entity.setParent(null);
  }

  public final List<Entity> getChildren() {
    return children;
  }

  public final Entity getParent() {
    return parent;
  }

  private void setParent(Entity entity) {
    this.parent = entity;
  }

  public final Entity addComponent(Component component) {
    if (hasComponent(component.getClass())) {
      throw new IllegalArgumentException(
          "Entity already has a component of type " + component.getClass().getSimpleName());
    }
    this.components.put(component.getClass(), component);
    return this;
  }

  public final <T extends Component> T getComponent(Class<T> type) {
    if (hasComponent(type)) {
      Component component = components.get(type);
      if (type.isInstance(component)) {
        return type.cast(component);
      }
    }
    return null;
  }

  public final boolean hasComponent(Class<? extends Component> type) {
    return components.containsKey(type);
  }

  public final void updateInternal(double elapsedTime) {
    components.values().forEach(c -> c.update(this));
    update(elapsedTime);
  }

  public final void cleanUpInternal() {
    components.values().forEach(Component::cleanUp);
    cleanUp();
  }

  protected abstract void update(double elapsedTime);

  protected abstract void cleanUp();

  public String getName() {
    return name;
  }

  public Collection<Component> getComponents() {
    return components.values();
  }
}
