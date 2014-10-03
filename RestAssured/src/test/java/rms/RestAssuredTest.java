package rms; /**
 * Created by Haemimont on 9/30/2014.
 */
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;
import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;
import static com.jayway.restassured.RestAssured.*;
import static com.jayway.restassured.matcher.RestAssuredMatchers.matchesXsd;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.xml.HasXPath.hasXPath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class RestAssuredTest {
    @Rule
    public ContiPerfRule i = new ContiPerfRule();

    @Test
    @PerfTest(invocations = 300, threads = 30)
    /**
     * Test that validates json response with added contiperf annotation.
     */
    public void testGetSingleUser() {
        when().
                get("http://193.178.152.147:8080/webservice/single-user").
        then().
                statusCode(200).
                contentType(ContentType.JSON).
                body(
                        "email", equalTo("peter@rms.com"),
                        "firstName", equalTo("Peter"),
                        "lastName", equalTo("Coleman"),
                        "id", equalTo("1"));
    }

    @Test
    /**
     * Test that validates json response, using JsonPath.
     */
    public void testGetSingleUserProgrammatic() {
        Response res = get("/webservice/single-user");
        assertEquals(200, res.getStatusCode());
        String json = res.asString();
        JsonPath jp = new JsonPath(json);
        assertEquals("1", jp.get("id"));
        assertEquals("Peter", jp.get("firstName"));
        assertEquals("Coleman", jp.get("lastName"));
        assertEquals("peter@rms.com", jp.get("email"));
    }

    @Test
    /**
     * Test that validates xml response.
     */
    public void testGetSingleUserAsXml() {
        when().
                get("/webservice/single-user/xml").
        then().
                statusCode(200).
                body(
                        "user.email", equalTo("peter@rms.com"),
                        "user.firstName", equalTo("Peter"),
                        "user.lastName", equalTo("Coleman"),
                        "user.id", equalTo("1"));
    }

    @Test
    /**
     * Test that validates xml response, using XPath.
     */
    public void testGetPersons() {
        when().get("/webservice/users/xml").
        then().
                statusCode(200).
                body(hasXPath("//*[self::person and self::person[@id='1'] and self::person/email[text()='peter@rms.com'] and self::person/firstName[text()='Peter'] and self::person/lastName[text()='Coleman']]")).
                body(hasXPath("//*[self::person and self::person[@id='20'] and self::person/email[text()='dev@rms.com'] and self::person/firstName[text()='Sara'] and self::person/lastName[text()='Stevens']]")).
                body(hasXPath("//*[self::person and self::person[@id='11'] and self::person/email[text()='devnull@rms.com'] and self::person/firstName[text()='Mark'] and self::person/lastName[text()='Mustache']]"));
    }

    @Test
    /**
     * Test that validates xml response schema, using defined xsd schema.
     */
    public void testGetSingleUserAgainstSchema() {
        InputStream xsd = getClass().getResourceAsStream("/user.xsd");
        assertNotNull(xsd);
        when().
                get("/webservice/single-user/xml").
        then().
                statusCode(200).
                body(
                        matchesXsd(xsd));
    }

    @Test
    /**
     * Test that validates json response, using parameters.
     */
    public void testCreateUser() {
        final String email = "peter@rms.com";
        final String firstName = "Peter";
        final String lastName = "Coleman";

        given().
                parameters(
                        "email", email,
                        "firstName", firstName,
                        "lastName", lastName).
        when().
                get("/webservice/user/create").
        then().
                body("email", equalTo(email)).
                body("firstName", equalTo(firstName)).
                body("lastName", equalTo(lastName));
    }

    @Test
    /**
     * Test that validates authentication.
     */
    public void testAuthenticationWorking() {
        // we're not authenticated, service returns "401 Unauthorized"
        when().
                get("/webservice/secure/user").
        then().
                statusCode(401);

        // with authentication it is working
        given().
                with().
                authentication().basic("test", "test").
        when().
                get("/webservice/secure/user").
        then().
                statusCode(200);

        authentication = basic("test", "test");
        try {
            when()
                    .get("/webservice/secure/user").
            then()
                    .statusCode(200);
        } finally {
            RestAssured.reset();
        }
    }

    @Test
    /**
     * Test that validates json response, using JsonPath and Groovy Closure.
     */
    public void testFindUsingGroovyClosure() {
        String json = get("/webservice/users/json").asString();
        JsonPath jp = new JsonPath(json);
        jp.setRoot("user");
        Map person = jp.get("find {e -> e.email =~ /peter@/}");
        assertEquals("peter@rms.com", person.get("email"));
        assertEquals("Peter", person.get("firstName"));
        assertEquals("Coleman", person.get("lastName"));
    }
}
