/*
 * Copyright (c) 2022, @Author Alban098
 *
 * Code licensed under MIT license.
 */
package rendering.renderers;

import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rendering.Texture;
import rendering.Window;
import rendering.data.Vao;
import rendering.entities.RenderableObject;
import rendering.entities.component.Renderable;
import rendering.scene.Camera;
import rendering.scene.Scene;
import rendering.shaders.ShaderProgram;
import rendering.shaders.uniform.UniformBoolean;
import rendering.shaders.uniform.UniformMat4;
import rendering.shaders.uniform.UniformVec4;
import rendering.shaders.uniform.Uniforms;

public abstract class Renderer<T extends RenderableObject> {

  private static final Logger LOGGER = LoggerFactory.getLogger(Renderer.class);

  protected final Vao vao;
  protected final ShaderProgram shader;
  // Work with untextured object because of Hashmap null key
  protected final Map<Texture, List<T>> registered = new HashMap<>();
  private final Vector4f wireframeColor;

  protected Renderer(ShaderProgram shader, Vector4f wireframeColor) {
    this.shader = shader;
    this.wireframeColor = wireframeColor;
    this.vao = shader.createCompatibleVao(8096);
  }

  public int render() {
    int drawCall = 0;
    for (Map.Entry<Texture, List<T>> entry : registered.entrySet()) {
      if (entry.getKey() != null) {
        entry.getKey().bind();
      }
      for (T object : entry.getValue()) {
        if (!vao.batch(object.getRenderable())) {
          // If the VAO is full, draw it and start a new batch
          drawVao();
          vao.batch(object.getRenderable());
        }
      }
      drawCall += drawVao();
    }
    shader.unbind();
    return drawCall;
  }

  public void setWireframeColor(Vector4f wireframeColor) {
    this.wireframeColor.set(wireframeColor);
  }

  public Vector4f getWireframeColor() {
    return wireframeColor;
  }

  private int drawVao() {
    vao.draw();
    return 1;
  }

  public abstract void loadUniforms(Window window, Camera camera, Scene scene);

  public abstract void cleanUp();

  public void unregister(T object) {
    Renderable renderable = object.getRenderable();
    List<T> list = registered.get(renderable.getTexture());
    if (list.remove(object)) {
      if (list.isEmpty()) {
        registered.remove(renderable.getTexture());
      }
      LOGGER.debug("Unregistered an object");
    }
  }

  public void register(T object) {
    Renderable renderable = object.getRenderable();
    registered.computeIfAbsent(renderable.getTexture(), t -> new ArrayList<>());
    registered.get(renderable.getTexture()).add(object);
    LOGGER.debug("Registered an object");
  }

  void renderNative(Window window, Camera camera, Scene scene, RenderingMode mode) {
    preRender();
    loadUniformsNative(window, camera, scene, mode);
    render();
    postRender();
  }

  protected void loadUniformsNative(Window window, Camera camera, Scene scene, RenderingMode mode) {
    ((UniformMat4) shader.getUniform(Uniforms.VIEW_MATRIX)).loadMatrix(camera.getViewMatrix());
    ((UniformMat4) shader.getUniform(Uniforms.PROJECTION_MATRIX))
        .loadMatrix(camera.getProjectionMatrix());
    ((UniformBoolean) shader.getUniform(Uniforms.WIREFRAME))
        .loadBoolean(mode == RenderingMode.WIREFRAME);
    ((UniformVec4) shader.getUniform(Uniforms.WIREFRAME_COLOR)).loadVec4(wireframeColor);
    loadUniforms(window, camera, scene);
  }

  private void preRender() {
    shader.bind();
    glActiveTexture(GL_TEXTURE0);
  }

  private void postRender() {
    shader.unbind();
  }

  void cleanUpNative() {
    vao.cleanUp();
    shader.cleanUp();
    cleanUp();
  }
}
