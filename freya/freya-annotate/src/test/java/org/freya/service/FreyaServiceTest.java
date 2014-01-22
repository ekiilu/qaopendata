package org.freya.service;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.freya.model.service.FreyaResponse;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.DispatcherServlet;

import com.google.common.collect.Maps;

public class FreyaServiceTest {
    private static final Logger log = LoggerFactory.getLogger(FreyaServiceTest.class);
    ObjectMapper objectMapper = new ObjectMapper();
    private static ApplicationContext applicationContext;

    @BeforeClass
    public static void setUpUnitTest() throws Exception {
        applicationContext = new MockWebAppContext("src/main/webapp", "freya");
    }

    @Test
    public void getSparql() throws Exception {
    	/*
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("query", "City california.");

        FreyaResponse expected = new FreyaResponse();
        //expected.setRepositoryId("mooney-native");
        //expected.setRepositoryUrl("http://localhost:8080/openrdf-sesame");
        expected.setSparqlQuery("prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> prefix xsd: <http://www.w3.org/2001/XMLSchema#> select distinct ?firstJoker0 where { ?firstJoker0  ?p0 ?d1 .  FILTER ( ?p0=<http://www.mooney.net/geo#cityPopulation>  ) .  FILTER REGEX(str(?d1), \"^California$\",\"i\") .  FILTER (?p0=<http://www.w3.org/2000/01/rdf-schema#label>)} LIMIT 10000");

        FreyaResponse actual =
                        objectMapper.readValue(send(GET, "/getSparql", parameters).getContentAsString(), FreyaResponse.class);
        //assertEquals(expected.getRepositoryId(), actual.getRepositoryId());
        //assertEquals(expected.getRepositoryUrl(), actual.getRepositoryUrl());
        assertTrue(actual.getPreciseSparql().contains("i1"));
        */
    }

    @Test
    public void ask() throws Exception {
    	/*
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("query", "List capitals.");

        FreyaResponse actual =
                        objectMapper.readValue(send(GET, "/ask", parameters).getContentAsString(), FreyaResponse.class);
        log.info(actual.getTextResponse().toString());
        // assertTrue( actual.getTextResponse());
         * *
         */
    }

    @Test
    public void askNoDialogue() throws Exception {
    	/*
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("query", "rivers in texas");
        MockHttpServletResponse response = send(POST, "/askNoDialog", parameters);
        FreyaResponse actual =
                        objectMapper.readValue(response.getContentAsString(), FreyaResponse.class);

       // assertTrue(actual.getTextResponse().toString().toLowerCase().contains("joker"));
        * 
        */
    }

    @Test
    public void analyse() throws Exception {
    	/*
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("query", "what is california?");
        MockHttpServletResponse response = send(GET, "/analyse", parameters);

        String stringContent = response.getContentAsString();
        FreyaResponse actual =
                        objectMapper.readValue(stringContent, FreyaResponse.class);

        assertEquals(1, actual.getAnnotations().size());

        log.info(actual.getAnnotations().toString());

        assertEquals("california", actual.getAnnotations().get(0).getText());
        assertEquals("http://www.mooney.net/geo#california", actual.getAnnotations().get(0).getUri());
        //assertEquals("", actual.getAnnotations().get(0).getType());
         * 
         */
    }

    public void load() throws Exception {
    	/*
        Map<String, String> parameters = Maps.newHashMap();
        parameters.put("query", "List capitals.");

        FreyaResponse actual =
                        objectMapper.readValue(send(GET, "/load", parameters).getContentAsString(), FreyaResponse.class);
        log.info(actual.getTextResponse().toString());
        // assertTrue( actual.getTextResponse());
         * 
         */
    }

    
    /*
    
    
    
    private MockHttpServletResponse send(HttpMethod method, String path) throws Exception {
        return send(method, path, null);
    }

    private MockHttpServletResponse send(HttpMethod method, String path, Map<String, String> parameters)
                    throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method.name(), path);
        request.addHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        request.addHeader("Content-type", MediaType.APPLICATION_JSON_VALUE);
        if (parameters != null) {
            request.addParameters(parameters);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();

        DispatcherServlet servlet = applicationContext.getBean(DispatcherServlet.class);
        servlet.service(request, response);

        return response;
    }

    private void asserts(MockHttpServletResponse res, String expected) throws Exception {
        assertThat(res.getStatus(), is(HttpStatus.OK.value()));
        assertThat(res.getContentType(), is(MediaType.APPLICATION_JSON_VALUE));
        String content = res.getContentAsString();
        log.info("expected:{}", expected);
        log.info("content :{}", content);
        assertEquals(expected, content);
    }
    
    */

}
