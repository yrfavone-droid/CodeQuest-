import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver;
import com.codequest.academy.database.AppDatabase;
import com.codequest.academy.shared.data.AccountResult;
import com.codequest.academy.shared.data.ProgressRepository;
import java.nio.file.*;
import java.util.Properties;
import java.util.UUID;

/** Creates a clearly labelled, fresh local QA profile; never modifies a real profile. */
public final class PrepareLabVerificationProfile {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args[0]).toAbsolutePath().normalize();
        Path allowed = Path.of("D:/coding academy/build/verification").toRealPath();
        if (!root.startsWith(allowed) || !root.getFileName().toString().startsWith("labs-qa-") || Files.exists(root))
            throw new IllegalArgumentException("Expected a new labs-qa-* verification directory");
        Path database = root.resolve(".nous-ai-academy/nous_ai_academy.db");
        Files.createDirectories(database.getParent());
        JdbcSqliteDriver driver = new JdbcSqliteDriver("jdbc:sqlite:" + database, new Properties());
        try {
            AppDatabase.Companion.getSchema().create(driver);
            ProgressRepository repository = new ProgressRepository(driver);
            AccountResult result = repository.createLocalAccount("Local QA Profile", "local-qa@example.invalid", UUID.randomUUID() + "Aa9!");
            if (!(result instanceof AccountResult.Success)) throw new IllegalStateException(result.toString());
            repository.setSetting("appearance_mode", "light");
            repository.setSetting("reduced_motion", "true");
            if (!repository.hasActiveSession()) throw new IllegalStateException("QA session was not created");
            System.out.println("Fresh local-only QA profile: " + root);
        } finally { driver.close(); }
    }
}
