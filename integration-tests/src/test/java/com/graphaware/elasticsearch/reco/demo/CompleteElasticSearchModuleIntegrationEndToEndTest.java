
package com.graphaware.elasticsearch.reco.demo;

import com.graphaware.elasticsearch.util.TestUtil;
import com.graphaware.integration.es.test.ElasticSearchClient;
import com.graphaware.integration.es.test.ElasticSearchServer;
import com.graphaware.integration.es.test.EmbeddedElasticSearchServer;
import com.graphaware.integration.es.test.JestElasticSearchClient;
import com.graphaware.test.data.DatabasePopulator;
import com.graphaware.test.data.GraphgenPopulator;
import com.graphaware.test.integration.GraphAwareApiTest;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static com.graphaware.elasticsearch.util.TestUtil.*;
import static org.junit.Assert.assertEquals;

public class CompleteElasticSearchModuleIntegrationEndToEndTest extends GraphAwareApiTest {

    private static final String ES_HOST = "localhost";
    private static final String ES_PORT = "9201";
    private static final String ES_CONN = String.format("http://%s:%s", ES_HOST, ES_PORT);
    private static final String ES_INDEX = "neo4jes";

    private ElasticSearchClient esClient;
    private ElasticSearchServer esServer;

    private static final Logger LOG = LoggerFactory.getLogger(CompleteElasticSearchModuleIntegrationEndToEndTest.class);

    //@Override
    protected String neo4jConfigFile() {
        return "neo4j-elasticsearch-reco.properties";
    }

    protected String propertiesFile() {
        return "src/test/resources/" + neo4jConfigFile();
    }

    //@Override
    protected String neo4jServerConfigFile() {
        return "neo4j-server-es.properties";
    }

    @Override
    public String baseUrl() {
        return "http://localhost:7575";
    }

    @Before
    public void setUp() throws Exception {
        deleteDataDirectory();
        esServer = new EmbeddedElasticSearchServer();
        esServer.start();
        esClient = new JestElasticSearchClient(ES_HOST, ES_PORT);
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        esClient.shutdown();
        esServer.stop();
        super.tearDown();
        deleteDataDirectory();
    }

    protected DatabasePopulator databasePopulator() {
        return new GraphgenPopulator() {
            @Override
            protected String file() throws IOException {
                return new ClassPathResource("demo-data.cyp").getFile().getAbsolutePath();
            }

//      @Override
//      public void populate(GraphDatabaseService database)
//      {
//        String separator = separator();
//
//        String[] statementGroups = statementGroups();
//        if (statementGroups == null)
//          return;
//        
//        for (String statementGroup : statementGroups)
//            for (String statement : statementGroup.split(separator))
//              httpClient.executeCypher(baseUrl(), statement);
//      }
        };
    }

    protected void populateDatabase(GraphDatabaseService database) {
        DatabasePopulator populator = databasePopulator();
        if (populator != null) {
            populator.populate(database);
        }
    }

    @Test
    public void test() throws IOException {
        waitFor(1000);

        String executeCypher = httpClient.executeCypher(baseUrl(), "MATCH (p:Person {firstname:'Kelly', lastname:'Krajcik'}) return p");
        String response = httpClient.get(ES_CONN + "/" + ES_INDEX + "/Person/_search?q=firstname:Kelly", HttpStatus.OK_200);

//    String query = "{\n" +
//"  \"bool\" : {\n" +
//"    \"must\" : {\n" +
//"      \"match_all\" : { }\n" +
//"    },\n" +
//"    \"should\" : {\n" +
//"      \"match\" : {\n" +
//"        \"__forUser\" : {\n" +
//"          \"query\" : \"1000\",\n" +
//"          \"type\" : \"boolean\"\n" +
//"        }\n" +
//"      }\n" +
//"    }\n" +
//"  }\n" +
//"}";
//    
//    String query = "{" +
//          "\"match_all\" : {}" +
//        "}";
        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-booster\" :{" +
                    "          \"name\": \"GARecommenderBooster\"," +
                    "          \"recoTarget\": \"Durgan%20LLC\"" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();


            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
            assertEquals("Estefania Bashirian", hits.get(1).source.getName());
            assertEquals("Wilton Emmerich", hits.get(0).source.getName());
            assertEquals("Emilie Bins", hits.get(2).source.getName());
        }

        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-booster\" :{" +
                    "          \"name\": \"GARecommenderBooster\"," +
                    "          \"recoTarget\": \"Durgan%20LLC\"," +
                    "          \"maxResultSize\": 50" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();


            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
        }

        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-booster\" :{" +
                    "          \"name\": \"GARecommenderMixedBooster\"," +
                    "          \"recoTarget\": \"Durgan%20LLC\"" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();


            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
//      assertEquals("Wilton Emmerich", hits.get(0).source.getName());
//      assertEquals("Emilie Bins", hits.get(1).source.getName());
//      assertEquals("Keegan Wolf", hits.get(2).source.getName());
        }


        {
            String query = "{" +
                    "   \"filter\": {" +
                    "      \"bool\": {" +
                    "         \"must\": [" +
                    "            {" +
                    "                  \"match_all\": {}" +
                    "            }" +
                    "         ]" +
                    "      }" +
                    "   }," +
                    "   \"ga-filter\" :{" +
                    "          \"name\": \"GACypherQueryFilter\"," +
                    "          \"query\": \"MATCH (n:Person) RETURN n.uuid\"" +
                    "      }" +
                    "}";
            Search search = new Search.Builder(query)
                    // multiple index or types can be added.
                    .addIndex(ES_INDEX)
                    .addType("Person")
                    .build();
            SearchResult result = esClient.execute(search);

            List<SearchResult.Hit<JestPersonResult, Void>> hits = result.getHits(JestPersonResult.class);
            Assert.assertEquals(10, hits.size());
//      assertEquals("148", hits.get(0).source.getDocumentId());
//      assertEquals("197", hits.get(1).source.getDocumentId());
//      assertEquals("102", hits.get(2).source.getDocumentId());
        }


        //String response = httpClient.get(ES_CONN + "/" + ES_INDEX + "/Company/_search?q=firstname:Kelly", HttpStatus.OK_200);
        String result1 = httpClient.get(baseUrl() + "/graphaware/recommendation/filter/Durgan%20LLC?limit=10&ids=148,197,27,4,5,6,7,8,9&keyProperty=uuid", HttpStatus.OK_200);

        //boolean res = response.contains("total\": 1");
        //assertEquals(res, true);

//    Get get = new Get.Builder(ES_INDEX, nodeId).type(car.name()).build();
//    JestResult result = null;
//
//    try
//    {
//      result = client.execute(get);
//    }
//    catch (IOException e)
//    {
//      e.printStackTrace();
//    }
//
//    notNull(result);
//    isTrue(result.isSucceeded());
    }
}