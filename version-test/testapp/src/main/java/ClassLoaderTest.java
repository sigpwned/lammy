import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import com.example.liba.ExampleA;

public class ClassLoaderTest {
    public static void main(String[] args) throws Exception {
        new ExampleA().printExampleBVersion();

        // Assuming libB_v2's JAR is built and located at ../libB_v2/target/libB_v2-1.0-SNAPSHOT.jar
        File libBv2Jar = new File("../libB_v2/target/libB_v2-1.0-SNAPSHOT.jar");
        if (!libBv2Jar.exists()) {
            System.err.println("libB_v2 JAR not found: " + libBv2Jar.getAbsolutePath());
            return;
        }
        URL libBv2URL = libBv2Jar.toURI().toURL();
        URL[] urls = { libBv2URL };

        // Create a URLClassLoader (child) with the system class loader as parent.
        // URLClassLoader childClassLoader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());
        URLClassLoader childClassLoader = new URLClassLoader(urls, null); // Use bootstrap as parent

        // Load ExampleA using the child class loader.
        // ExampleA is in libA, which is on the system classpath.
        Class<?> exampleAClass = childClassLoader.loadClass("com.example.liba.ExampleA");
        Object exampleAInstance = exampleAClass.getDeclaredConstructor().newInstance();

        // Invoke printExampleBVersion to see which version of ExampleB is used.
        exampleAClass.getMethod("printExampleBVersion").invoke(exampleAInstance);

        Class<?> exampleBClass = childClassLoader.loadClass("com.example.libb.ExampleB");
        Object exampleBInstance = exampleBClass.getDeclaredConstructor().newInstance();
        System.out.println(exampleBClass.getMethod("getVersion").invoke(exampleBInstance));
    }
}
