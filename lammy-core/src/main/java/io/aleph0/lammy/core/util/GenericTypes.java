/*-
 * =================================LICENSE_START==================================
 * lammy-core
 * ====================================SECTION=====================================
 * Copyright (C) 2023 Andy Boothe
 * ====================================SECTION=====================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================LICENSE_END===================================
 */
package io.aleph0.lammy.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Map;
import java.util.Optional;
import io.leangen.geantyref.GenericTypeReflector;
import io.leangen.geantyref.TypeFactory;

/**
 * Utilities for working with generic types. Stolen shamelessly from JDBI. All credit to JDBI team.
 *
 * https://raw.githubusercontent.com/jdbi/jdbi/94d15a4be5b927e937fde6c6596fad946ef64fbb/core/src/main/java/org/jdbi/v3/core/generic/GenericTypes.java
 */
@SuppressWarnings("rawtypes")
public final class GenericTypes {
  private static final TypeVariable<Class<Map>> KEY;
  private static final TypeVariable<Class<Map>> VALUE;

  static {
    TypeVariable<Class<Map>>[] mapParams = Map.class.getTypeParameters();
    KEY = mapParams[0];
    VALUE = mapParams[1];
  }

  private GenericTypes() {}

  /**
   * Returns the erased class for the given type.
   *
   * <p>
   * Example: if type is <code>List&lt;String&gt;</code>, returns <code>List.class</code>
   * </p>
   * parameter
   *
   * @param type the type
   * @return the erased class
   */
  public static Class<?> getErasedType(Type type) {
    return GenericTypeReflector.erase(type);
  }

  /**
   * Same as {@link #findGenericParameter(Type, Class, int)} with n = 0.
   *
   * @param type the type
   * @param parameterizedSupertype the parameterized supertype
   * @return the first parameter type
   * @see #findGenericParameter(Type, Class, int)
   */
  public static Optional<Type> findGenericParameter(Type type, Class<?> parameterizedSupertype) {
    return findGenericParameter(type, parameterizedSupertype, 0);
  }

  /**
   * For the given type which extends parameterizedSupertype, returns the nth generic parameter for
   * the parameterized supertype, if concretely expressed.
   *
   * <p>
   * Example:
   * </p>
   * <ul>
   * <li>if {@code type} is {@code ArrayList<String>}, {@code parameterizedSupertype} is
   * {@code List.class}, and {@code n} is {@code 0}, returns {@code Optional.of(String.class)}.</li>
   *
   * <li>if {@code type} is {@code Map<String, Integer>}, {@code parameterizedSupertype} is
   * {@code Map.class}, and {@code n} is {@code 1}, returns {@code Optional.of(Integer.class)}.</li>
   *
   * <li>if {@code type} is {@code ArrayList.class} (raw), {@code parameterizedSupertype} is
   * {@code List.class}, and {@code n} is {@code 0}, returns {@code Optional.empty()}.</li>
   * </ul>
   *
   * @param type the subtype of parameterizedSupertype
   * @param parameterizedSupertype the parameterized supertype from which we want the generic
   *        parameter
   * @param n the index in {@code Foo<X, Y, Z, ...>}
   * @return the parameter on the supertype, if it is concretely defined.
   * @throws ArrayIndexOutOfBoundsException if n &gt; the number of type variables the type has
   */
  public static Optional<Type> findGenericParameter(Type type, Class<?> parameterizedSupertype,
      int n) {
    if (type instanceof Class) {
      Type genericSuper = ((Class<?>) type).getGenericSuperclass();
      if (genericSuper instanceof ParameterizedType) {
        ParameterizedType pt = (ParameterizedType) genericSuper;
        if (pt.getRawType() == parameterizedSupertype) {
          return Optional.ofNullable(pt.getActualTypeArguments()[n]);
        }
      }
    }

    // fall back to the type reflector
    return Optional.ofNullable(
        GenericTypeReflector.getTypeParameter(type, parameterizedSupertype.getTypeParameters()[n]));
  }

  /**
   * Resolves the {@code type} parameter in the context of {@code contextType}. For example, if
   * {@code type} is {@code List.class.getMethod("get", int.class).getGenericReturnType()}, and
   * {@code contextType} is {@code List<String>}, this method returns {@code String.class}
   *
   * @param type the type to be resolved in the scope of <code>contextType</code>
   * @param contextType the context type in which <code>type</code> is interpreted to resolve the
   *        type.
   * @return the resolved type.
   */
  public static Type resolveType(Type type, Type contextType) {
    return GenericTypeReflector.resolveType(type, contextType);
  }

  /**
   * Checks whether a given {@link Type} is an Array Type.
   *
   * @param type a type
   * @return whether the given {@link Type} is an Array type.
   */
  public static boolean isArray(Type type) {
    return type instanceof Class<?> && ((Class<?>) type).isArray();
  }

  /**
   * Returns the element type for an Array Type.
   *
   * @param type the array's element type
   * @return the array Type
   */
  public static Type arrayType(Type type) {
    return TypeFactory.arrayOf(type);
  }

  /**
   * Given a subtype of {@code Map<K,V>}, returns the corresponding map entry type
   * {@code Map.Entry<K,V>}.
   *
   * @param mapType the map subtype
   * @return the map entry type
   */
  public static Type resolveMapEntryType(Type mapType) {
    Type keyType = resolveType(KEY, mapType);
    Type valueType = resolveType(VALUE, mapType);
    return resolveMapEntryType(keyType, valueType);
  }

  /**
   * Given a key and value type, returns the map entry type {@code Map.Entry<keyType,valueType>}.
   *
   * @param keyType the key type
   * @param valueType the value type
   * @return the map entry type
   */
  public static Type resolveMapEntryType(Type keyType, Type valueType) {
    return TypeFactory.parameterizedClass(Map.Entry.class, keyType, valueType);
  }

  /**
   * Creates a type of class {@code clazz} with {@code arguments} as type arguments.
   * <p>
   * For example: {@code parameterizedClass(Map.class, Integer.class, String.class)} returns the
   * type {@code Map<Integer, String>}.
   *
   * @param clazz Type class of the type to create
   * @param arguments Type arguments for the variables of {@code clazz}, or null if these are not
   *        known.
   * @return A {@link ParameterizedType}, or simply {@code clazz} if {@code arguments} is
   *         {@code null} or empty.
   */
  public static Type parameterizeClass(Class<?> clazz, Type... arguments) {
    return TypeFactory.parameterizedClass(clazz, arguments);
  }

  /**
   * Perform boxing conversion on a {@code Type}, if it is a primitive type. Otherwise return the
   * input argument.
   *
   * @param type the type to box
   * @return the boxed type, or the input type if it is not a primitive
   */
  public static Type box(Type type) {
    return GenericTypeReflector.box(type);
  }

  /**
   * Tests whether a given type is a supertype of another type
   *
   * @param superType The supertype to check.
   * @param subType The subtype to check.
   * @return True if supertype is a supertype of subtype.
   */
  public static boolean isSuperType(Type superType, Type subType) {
    return GenericTypeReflector.isSuperType(superType, subType);
  }
}
