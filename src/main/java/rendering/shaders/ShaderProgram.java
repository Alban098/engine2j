/*
 * Copyright (c) 2022-2023, @Author Alban098
 *
 * Code licensed under MIT license.
 */
package rendering.shaders;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

import java.util.*;
import org.lwjgl.opengl.GL20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rendering.ResourceLoader;
import rendering.shaders.data.VertexArrayObject;
import rendering.shaders.data.uniform.*;

/** Represent a Shader program loaded into the GPU */
public final class ShaderProgram {

  private static final Logger LOGGER = LoggerFactory.getLogger(ShaderProgram.class);

  /** The id of the Shader as provided by OpenGL */
  private final int programId;
  /** The id of the Vertex Shader as provided by OpenGL */
  private final int vertexShader;
  /** The path of the Vertex Shader file */
  private final String vertexFile;
  /** The id of the Geometry Shader as provided by OpenGL */
  private final int geometryShader;
  /** The path of the Geometry Shader file */
  private final String geometryFile;
  /** The id of the Fragment Shader as provided by OpenGL */
  private final int fragmentShader;
  /** The path of the Fragment Shader file */
  private final String fragmentFile;

  /** A List of all {@link ShaderAttribute}s declared for this Shader */
  private final List<ShaderAttribute> attributes;
  /** A Map of all {@link Uniform}s declared in this Shader, indexed by their name */
  private final Map<String, Uniform<?>> uniforms;

  /**
   * Create a new Shader program
   *
   * @param vertex path of the vertex shader
   * @param fragment path of the fragment shader
   */
  public ShaderProgram(
      String vertex,
      String geometry,
      String fragment,
      ShaderAttribute[] attributes,
      Uniform<?>[] uniforms) {
    this.programId = glCreateProgram();
    this.uniforms = new HashMap<>();

    this.vertexShader = glCreateShader(GL_VERTEX_SHADER);
    this.vertexFile = vertex;
    GL20.glShaderSource(vertexShader, ResourceLoader.loadFile(vertex));
    compile(vertexShader);

    this.geometryShader = glCreateShader(GL_GEOMETRY_SHADER);
    this.geometryFile = geometry;
    GL20.glShaderSource(geometryShader, ResourceLoader.loadFile(geometry));
    compile(geometryShader);

    this.fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
    this.fragmentFile = fragment;
    glShaderSource(fragmentShader, ResourceLoader.loadFile(fragment));
    compile(fragmentShader);

    glAttachShader(programId, vertexShader);
    if (geometry != null) {
      glAttachShader(programId, geometryShader);
    }
    glAttachShader(programId, fragmentShader);

    this.attributes = new ArrayList<>(List.of(ShaderAttributes.INDEX));
    this.attributes.addAll(List.of(attributes));

    for (ShaderAttribute attribute : this.attributes) {
      glBindAttribLocation(programId, attribute.getLocation(), attribute.getName());
    }

    glLinkProgram(programId);
    if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
      LOGGER.error("{}", glGetProgramInfoLog(programId));
      System.exit(-1);
    }

    storeAllUniformLocations(uniforms);

    glValidateProgram(programId);
    if (glGetProgrami(programId, GL_VALIDATE_STATUS) == GL_FALSE) {
      LOGGER.error("{}", glGetProgramInfoLog(programId));
      System.exit(-1);
    }
    LOGGER.debug(
        "Created Shader with id {} with {} attributes and {} uniforms",
        this.programId,
        this.attributes.size(),
        this.uniforms.size());
  }

  /** Allocate the memory on the GPU's RAM for all the Uniforms variables of this shader */
  public void storeAllUniformLocations(Uniform<?>[] uniforms) {
    for (Uniform<?> uniform : uniforms) {
      this.uniforms.put(uniform.getName(), uniform);
      uniform.storeUniformLocation(programId);
    }
  }

  /**
   * Retrieves a {@link Uniform} by its name cast to a type, if present in this Shader
   *
   * @param name the name of the {@link Uniform} to retrieve
   * @param type the class type of the {@link Uniform} to retrieve
   * @return The retrieved {@link Uniform} cast, null if not present
   * @param <T> the type of the {@link Uniform} to retrieve
   */
  public <T extends Uniform<?>> T getUniform(String name, Class<T> type) {
    return (T) uniforms.get(name);
  }

  /**
   * Compiles a shader program and checks for error
   *
   * @param id the id of the program to compile
   */
  private void compile(int id) {
    glCompileShader(id);
    if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
      LOGGER.error("{}", glGetShaderInfoLog(id));
      System.exit(1);
    }
  }

  /** Bind the shader for further use */
  public void bind() {
    glUseProgram(programId);
  }

  /** Unbind the shader */
  public void unbind() {
    glUseProgram(0);
  }

  /** Cleanup the Shader */
  public void cleanUp() {
    glDeleteShader(vertexShader);
    glDeleteShader(geometryShader);
    glDeleteShader(fragmentShader);
    glDeleteProgram(programId);
    LOGGER.debug("Shader {} cleaned up", programId);
  }

  /**
   * Create a {@link VertexArrayObject} that can be used to load objects to this Shader, with all
   * the right data structures initialized (VBOs & SSBOs)
   *
   * @param maxQuadCapacity the number of quads this VAO must be able to batch
   * @param withSSBO does a Transform {@link rendering.shaders.data.ShaderStorageBufferObject} is
   *     necessary
   * @return a compatible {@link VertexArrayObject} fully initialized and usable immediatlly
   */
  public VertexArrayObject createCompatibleVao(int maxQuadCapacity, boolean withSSBO) {
    VertexArrayObject vao = new VertexArrayObject(maxQuadCapacity, withSSBO);
    attributes.forEach(vao::createVBO);
    return vao;
  }

  /**
   * Returns the id of the Shader as provided by OpenGL
   *
   * @return the id of the Shader as provided by OpenGL
   */
  public int getProgramId() {
    return programId;
  }

  /**
   * Returns the id of the Vertex Shader as provided by OpenGL
   *
   * @return the id of the Vertex Shader as provided by OpenGL
   */
  public int getVertexShader() {
    return vertexShader;
  }

  /**
   * Returns the id of the Geometry Shader as provided by OpenGL
   *
   * @return the id of the Geometry Shader as provided by OpenGL
   */
  public int getGeometryShader() {
    return geometryShader;
  }

  /**
   * Returns the id of the Fragment Shader as provided by OpenGL
   *
   * @return the id of the Fragment Shader as provided by OpenGL
   */
  public int getFragmentShader() {
    return fragmentShader;
  }

  /**
   * Returns the path of the Vertex Shader file
   *
   * @return the path of the Vertex Shader file
   */
  public String getVertexFile() {
    return vertexFile;
  }

  /**
   * Returns the path of the Geometry Shader file
   *
   * @return the path of the Geometry Shader file
   */
  public String getGeometryFile() {
    return geometryFile;
  }

  /**
   * Returns the path of the Fragment Shader file
   *
   * @return the path of the Fragment Shader file
   */
  public String getFragmentFile() {
    return fragmentFile;
  }

  /**
   * Returns a Collection of all {@link ShaderAttribute}s declared in the Shader
   *
   * @return a Collection of all {@link ShaderAttribute}s declared in the Shader
   */
  public List<ShaderAttribute> getAttributes() {
    return attributes;
  }

  /**
   * Returns a Collection of all {@link Uniform}s declared in Shader
   *
   * @return a Collection of all {@link Uniform}s declared in Shader
   */
  public Map<String, Uniform<?>> getUniforms() {
    return uniforms;
  }
}
