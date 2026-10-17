// Disposable browser acceptance only; not application/test source or packaged code.
import com.qubigo.qubigobackend.QubigoBackendKtApplication;
import com.qubigo.qubigobackend.common.domain.Success;
import com.qubigo.qubigobackend.common.domain.messaging.MessageEngine;
import com.qubigo.qubigobackend.notifications.domain.NotificationService;
import kotlin.Unit;
import org.springframework.boot.SpringApplication;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

public class InvitationBackend {
    public static void main(String[] args) throws Exception {
        var app = new SpringApplication(QubigoBackendKtApplication.class);
        app.addInitializers(context -> context.addBeanFactoryPostProcessor(factory -> {
            var registry = (BeanDefinitionRegistry) factory;
            // No external provider calls: retain real commands, security, users and SQLite.
            registry.removeBeanDefinition("firebaseCLoudMessagingBasedNotificationService");
            factory.registerSingleton("acceptanceNotifications", Proxy.newProxyInstance(
                NotificationService.class.getClassLoader(), new Class[]{NotificationService.class},
                (proxy, method, params) -> new Success<>(Unit.INSTANCE)));
            registry.removeBeanDefinition("messageEngine");
            factory.registerSingleton("acceptanceEmail", (MessageEngine) messenger -> {
                try {
                    Files.writeString(Path.of(System.getenv("ACCEPTANCE_EMAIL_FILE")), messenger.messages().get(0).getBody());
                } catch (Exception error) { throw new IllegalStateException("Email capture failed", error); }
                return new Success<>(Unit.INSTANCE);
            });
        }));
        var context = app.run(args);
        context.getBean(JdbcTemplate.class).update(
            "INSERT INTO users(user_id,email,name,password,role,created_at) VALUES(?,?,?,?,?,?)",
            "11111111-2222-3333-4444-555555555555", "organizer@example.test", "Organizer",
            context.getBean(PasswordEncoder.class).encode("Ab12345678"), "ADMIN", java.sql.Timestamp.valueOf("2026-10-16 00:00:00"));
        Files.writeString(Path.of(System.getenv("ACCEPTANCE_READY_FILE")), "ready");
    }
}
