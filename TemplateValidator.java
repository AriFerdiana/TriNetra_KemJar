import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class TemplateValidator {
    public static void main(String[] args) {
        TemplateEngine templateEngine = new TemplateEngine();
        FileTemplateResolver resolver = new FileTemplateResolver();
        resolver.setPrefix("src/main/resources/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        templateEngine.setTemplateResolver(resolver);

        Context context = new Context();
        try {
            templateEngine.process("admin/dashboard", context);
            System.out.println("TEMPLATE PARSED SUCCESSFULLY!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
