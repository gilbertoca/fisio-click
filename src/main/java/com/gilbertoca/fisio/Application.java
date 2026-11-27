package com.gilbertoca.fisio;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import com.gilbertoca.fisio.infra.DataSourceManager;

/**
 * Bootstrap do servidor embarcado.
 *
 * A webapp vai dentro do jar (<code>/webapp/</code>) e é servida direto do
 * classpath. O jar é distribuído dentro de uma pasta com o banco H2
 * (<code>data/</code>), o <code>run.bat</code> e o
 * <code>application.properties</code>; o build monta essa pasta em
 * <code>shade/</code>. O banco fica em <code>&lt;appHome&gt;/data</code>, onde
 * <code>appHome</code> é a pasta do jar (ou <code>APP_HOME</code>). Essa pasta
 * não é criada aqui: se faltar, a aplicação para e avisa. Para usar outro
 * banco/pasta, defina <code>DB_URL</code>.
 *
 * Configuração: variável de ambiente &gt; <code>data/application.properties</code>
 * &gt; padrões empacotados no jar. O arquivo da instância fica em
 * <code>data/</code>, junto do banco, e não é versionado (segredos e
 * certificado ficam fora do git); o modelo fica em
 * <code>src/main/config</code>.
 *
 * HTTPS é opcional e substitui o HTTP na mesma <code>PORT</code> quando
 * <code>tls.keystore</code>/<code>TLS_KEYSTORE</code> aponta para um keystore
 * PKCS12; a senha vem de <code>tls.password</code>/<code>TLS_PASSWORD</code>.
 */
public class Application {

    public static void main(String[] args) throws Exception {
        Properties defaults = loadDefaults();

        Path appHome = resolveAppHome();
        Path dataDir = appHome.resolve("data");
        Path configFile = dataDir.resolve("application.properties");
        Properties config = loadConfig(configFile);

        int port = Integer.parseInt(setting("PORT", "server.port", config, defaults, "8080"));

        String jdbcUrl = setting("DB_URL", "db.url", config, defaults, null);
        if (jdbcUrl == null) {
            if (!Files.isDirectory(dataDir)) {
                throw new IllegalStateException(
                        "Pasta de dados não encontrada: " + dataDir
                        + ". Crie a pasta ou defina DB_URL.");
            }
            jdbcUrl = "jdbc:h2:file:" + dataDir.resolve("fisio").toAbsolutePath()
                    + ";MODE=PostgreSQL;INIT=CREATE SCHEMA IF NOT EXISTS FISIO;DATABASE_TO_LOWER=TRUE";
        }

        String username = setting("DB_USER", "db.user", config, defaults, "sa");
        String password = setting("DB_PASS", "db.password", config, defaults, "sa");
        String driver = setting("DB_DRIVER", "db.driver", config, defaults, "org.h2.Driver");
        String keyStore = setting("TLS_KEYSTORE", "tls.keystore", config, defaults, null);
        String keyStorePassword = setting("TLS_PASSWORD", "tls.password", config, defaults, null);

        System.out.println("--------------------------------------------------");
        System.out.println("Starting Application Server Core...");
        System.out.println("Application home:    " + appHome);
        System.out.println("Config file:         "
                + (Files.isRegularFile(configFile) ? configFile : "(nenhum, usando padrões)"));
        System.out.println("Port:                " + port + (blank(keyStore) ? " (HTTP)" : " (HTTPS)"));
        System.out.println("Database URL:        " + jdbcUrl);
        System.out.println("Database Driver:     " + driver);
        System.out.println("--------------------------------------------------");

        if ("sa".equals(username) && "sa".equals(password)) {
            System.err.println("AVISO: credenciais padrão do banco (sa/sa) em uso.");
            System.err.println("AVISO: defina DB_USER/DB_PASS (ambiente ou data/application.properties).");
        }

        // 1. Infra antes de abrir o socket
        DataSourceManager.initialize(jdbcUrl, username, password, driver);
        DataSourceManager.runMigrations();

        // 2. Webapp embutida no jar
        URL webapp = Application.class.getResource("/webapp/");
        if (webapp == null) {
            throw new IllegalStateException(
                    "Webapp não encontrada no classpath (/webapp/). O build precisa empacotar "
                    + "src/main/webapp como resource (ver <resources> no pom.xml).");
        }

        WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setBaseResourceAsString(webapp.toExternalForm());
        context.setParentLoaderPriority(true);

        // 3. HTTP, ou HTTPS na mesma porta quando há keystore
        Server server = new Server();
        HttpConfiguration httpConfig = new HttpConfiguration();
        if (blank(keyStore)) {
            server.addConnector(connector(server, port, new HttpConnectionFactory(httpConfig)));
        } else {
            httpConfig.addCustomizer(new SecureRequestCustomizer());
            Path keyStoreFile = Paths.get(keyStore);
            if (!keyStoreFile.isAbsolute()) {
                keyStoreFile = appHome.resolve(keyStoreFile);
            }
            if (keyStorePassword == null) {
                throw new IllegalStateException(
                        "tls.keystore definido, mas tls.password/TLS_PASSWORD não foi informado.");
            }
            SslContextFactory.Server ssl = new SslContextFactory.Server();
            ssl.setKeyStorePath(keyStoreFile.toString());
            ssl.setKeyStorePassword(keyStorePassword);
            ssl.setKeyStoreType("PKCS12");
            server.addConnector(connector(server, port,
                    new SslConnectionFactory(ssl, "http/1.1"),
                    new HttpConnectionFactory(httpConfig)));
            System.out.println("TLS keystore:        " + keyStoreFile);
        }

        server.setHandler(context);
        server.setStopTimeout(30000);
        server.setStopAtShutdown(true);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Container terminating. Closing pool streams...");
            DataSourceManager.shutdown();
        }));

        server.start();
        server.join();
    }

    private static ServerConnector connector(Server server, int port, ConnectionFactory... factories) {
        ServerConnector connector = new ServerConnector(server, factories);
        connector.setPort(port);
        return connector;
    }

    private static Properties loadDefaults() {
        Properties defaults = new Properties();
        try (InputStream input = Application.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                defaults.load(input);
            }
        } catch (Exception e) {
            System.err.println("Warning: application.properties could not be loaded. Falling back to code defaults.");
        }
        return defaults;
    }

    /** application.properties externo, se existir. */
    private static Properties loadConfig(Path file) {
        Properties config = new Properties();
        if (!Files.isRegularFile(file)) {
            return config;
        }
        try (InputStream input = Files.newInputStream(file)) {
            config.load(input);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível ler " + file + ": " + e.getMessage(), e);
        }
        return config;
    }

    /**
     * Ordem de precedência: variável de ambiente &gt; data/application.properties
     * &gt; padrão empacotado no jar ({@code default.*}).
     */
    private static String setting(String envName, String key,
            Properties config, Properties defaults, String fallback) {
        return env(envName, config.getProperty(key,
                defaults.getProperty("default." + key, fallback)));
    }

    /**
     * Pasta da aplicação: APP_HOME explícito ou a pasta que contém o jar em
     * execução. Rodando de target/classes (dev), usa o diretório corrente.
     */
    private static Path resolveAppHome() {
        String env = System.getenv("APP_HOME");
        if (env != null && !env.isBlank()) {
            return Paths.get(env).toAbsolutePath().normalize();
        }
        try {
            Path location = Paths.get(Application.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            if (Files.isRegularFile(location)) {
                return location.getParent().toAbsolutePath().normalize();
            }
        } catch (Exception e) {
            System.err.println("Warning: não foi possível resolver a pasta do jar: " + e.getMessage());
        }
        return Paths.get("").toAbsolutePath().normalize();
    }

    private static String env(String name, String fallback) {
        String value = System.getenv(name);
        return blank(value) ? fallback : value;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
