/*
 * Copyright (c) 2022-2023, @Author Alban098
 *
 * Code licensed under MIT license.
 */
package simulation.entities.components;

import org.alban098.engine2j.objects.entities.Entity;
import org.alban098.engine2j.objects.entities.component.Component;

public class RotationProviderComponent extends Component {

  private final float value;

  public RotationProviderComponent(float value) {
    this.value = value;
  }

  @Override
  public void cleanUp() {}

  @Override
  public void update(Entity entity) {}

  public float getValue() {
    return value;
  }
}
