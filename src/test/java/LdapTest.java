import org.junit.*;
import org.neo4j.driver.v1.*;
import org.neo4j.harness.junit.EnterpriseNeo4jRule;
import org.neo4j.harness.junit.Neo4jRule;

import java.util.concurrent.TimeUnit;

public class LdapTest {

    public Driver driver;

    private final static Integer TIME_SEC = 1;

    @Rule
    public Neo4jRule neo4j = new EnterpriseNeo4jRule()
            .withConfig("dbms.security.auth_enabled", "true")
            .withConfig("dbms.security.auth_provider", "ldap").withConfig("dbms.security.ldap.host", "ec2-52-52-200-170.us-west-1.compute.amazonaws.com")
            .withConfig("dbms.security.ldap.authentication.user_dn_template", "cn={0},cn=users,dc=example,dc=com")
            .withConfig("dbms.security.ldap.authorization.user_search_base", "cn=users,dc=example,dc=com")
            .withConfig("dbms.security.ldap.authorization.user_search_filter", "(&(objectClass=*)(cn={0}))")
            .withConfig("dbms.security.ldap.authorization.group_to_role_mapping", " 'cn=Neo4j Administrator,cn=Users,dc=example,dc=com'  = admin")
            .withConfig("dbms.security.ldap.authentication.cache_enabled", "true").withConfig("dbms.security.auth_cache_ttl", TIME_SEC + "s");

    @Before
    public void initDriver() {
        Config.ConfigBuilder cb = Config.build()
                .withoutEncryption()
                .withMaxIdleSessions(1)
                .withConnectionLivenessCheckTimeout(0, TimeUnit.MILLISECONDS);
        this.driver = GraphDatabase.driver(neo4j.boltURI(), AuthTokens.basic("Neo4j Admin", "neo4j"), cb.toConfig());

    }

    @After
    public void closeDriver(){
        this.driver.close();
        this.driver = null;
    }

    public void query_should_succeed() {
        try (Session session = this.driver.session()) {
            StatementResult rs = session.run("RETURN 1");
            Assert.assertEquals(1, rs.single().get(0).asInt());
        }
    }

    @Test public void serial_queries_lower_than_ttl_should_work() throws InterruptedException {
        this.query_should_succeed();
        Thread.sleep(TIME_SEC * 1000 / 2);
        this.query_should_succeed();
    }

    @Test public void serial_queries_upper_than_ttl_should_work() throws InterruptedException {
        this.query_should_succeed();
        Thread.sleep(TIME_SEC * 1000 + 1);
        this.query_should_succeed();
    }
}
