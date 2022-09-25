/*
 * Copyright (c) 2022, @Author Alban098
 *
 * Code licensed under MIT license.
 */
package rendering;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rendering.renderers.MasterRenderer;
import rendering.scene.Camera;
import rendering.scene.Scene;

/** This class implements base methods of a Logic that can be run by the engine */
public abstract class ConcreteLogic implements ILogic {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConcreteLogic.class);

  protected final MasterRenderer renderer;
  protected final Camera camera;

  private boolean paused = false;

  protected Scene scene;

  /** Create a new Logic Initialize Camera and Renderer */
  public ConcreteLogic() {
    renderer = new MasterRenderer();
    camera = new Camera(new Vector2f());
  }

  /**
   * Initialize the Logic by creating the scene, the lights and the skybox
   *
   * @param window the Window to render to
   * @throws Exception thrown if the skybox model or texture couldn't be loaded
   */
  @Override
  public void init(Window window) throws Exception {
    renderer.init();
    camera.adjustProjection(window.getAspectRatio());
    scene = new Scene(renderer);
  }

  /**
   * Methods used to check to user inputs
   *
   * @param window the Window where the scene is renderer to
   * @param mouseInput the MouseInput containing cursor information
   */
  @Override
  public void input(Window window, MouseInput mouseInput) {
    if (window.isKeyPressed(GLFW_KEY_ENTER)) {
      glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
      glDisable(GL_TEXTURE_2D);
    } else {
      glPolygonMode(GL11.GL_FRONT_AND_BACK, GL_FILL);
      glEnable(GL_TEXTURE_2D);
    }
  }

  /**
   * Update the Camera's position and scale
   *
   * @param window the windows where the scene is rendered to
   * @param mouseInput the MouseInput to use for camera rotation
   */
  @Override
  public void updateCamera(Window window, MouseInput mouseInput) {
    if (window.isResized()) {
      camera.adjustProjection(window.getAspectRatio());
    }

    if (mouseInput.isLeftButtonPressed()) {
      Vector2f pan =
          mouseInput.getDisplacementVector().div(window.getHeight()).mul(camera.getZoom());
      pan.x = -pan.x;
      camera.move(pan);
    }

    if (mouseInput.isRightButtonPressed()) {
      float rotation = mouseInput.getDisplacementVector().y;
      camera.rotate((float) (rotation / Math.PI / 128f));
    }

    if (mouseInput.getScrollOffset() != 0) {
      camera.zoom(1 - mouseInput.getScrollOffset() / 10);
    }
  }

  @Override
  public void process(Window window, double elapsedTime) {
    // If the simulation is running, update all objects
    if (!paused) {
      preUpdate(window, elapsedTime);
      update(window, elapsedTime);
      postUpdate(window, elapsedTime);
    }
  }

  /**
   * Called before all the scene element will be updated, may be called multiple time per frame
   * Entities and components are automatically updated after this call
   *
   * @param window the Window where the simulation is rendered
   * @param elapsedTime time elapsed since last update in seconds
   */
  protected abstract void preUpdate(Window window, double elapsedTime);

  protected abstract void update(Window window, double elapsedTime);

  /**
   * Called after all the scene element have been updated, may be called multiple time per frame
   * Entities and components are automatically updated before this call
   *
   * @param window the Window where the simulation is rendered
   * @param elapsedTime time elapsed since last update in seconds
   */
  protected abstract void postUpdate(Window window, double elapsedTime);

  /**
   * Render the scene to the screen, called once every frame
   *
   * @param window the Window ro render to
   */
  @Override
  public void render(Window window) {
    renderer.render(window, camera, scene);
  }

  /** Pause the simulation */
  @Override
  public void pause() {
    paused = true;
  }

  /** Resume the simulation */
  @Override
  public void resume() {
    paused = false;
  }

  /** Clear the memory used by the scene, and it's meshes */
  @Override
  public void cleanup() {
    renderer.cleanUp();
    scene.cleanUp();
  }
}
