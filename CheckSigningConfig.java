
import java.lang.reflect.*;

public class CheckSigningConfig {
    public static void main(String[] args) {
        try {
            Class<?> clazz = Class.forName("com.android.build.api.dsl.SigningConfig");
            for (Method m : clazz.getMethods()) {
                if (m.getName().toLowerCase().contains("v1") || m.getName().toLowerCase().contains("v2") || m.getName().toLowerCase().contains("enable")) {
                    System.out.println(m);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
