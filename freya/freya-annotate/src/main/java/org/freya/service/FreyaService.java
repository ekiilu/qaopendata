package org.freya.service;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.model.Question;
import org.freya.model.service.FreyaResponse;
import org.freya.util.FreyaConstants;
import org.freya.util.ProfilerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class FreyaService {
    protected final Log logger = LogFactory.getLog(getClass());

    /**
     * 
     * @return Test with:
     * 
     * <pre>curl --data "query=What is a city?" http://localhost:8080/freya/ask</pre>
     */

    @Autowired FreyaServiceHelper helper;

    /**
     * This method is called for loading ontology files to RDF repository. Client should set ontology source data in
     * request body as InputStream.
     * 
     * @param repositoryURL Sesame repository server.
     * @param repositoryId Sesame repository ID.
     * @param source Ontology source name that needs to load into repository.
     * @return String message.
     */
    @ResponseBody
    @RequestMapping(value = "/load", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public String load(@RequestParam(value = "url") String repositoryURL,
                    @RequestParam(value = "id") String repositoryId,
                    @RequestParam(value = "src") String source, HttpServletRequest request) {

        logger.info(String.format("params: { url => %s, id => %s, src => %s }",
                        repositoryURL, repositoryId, source));

        if (StringUtils.isEmpty(repositoryURL)
                        || StringUtils.isEmpty(repositoryId)) {
            return "query params: url, id and src cannot be empty!";
        }

        try {
            helper.load(repositoryURL, repositoryId, source, request.getInputStream());
        } catch (Exception e) {
            logger.error(e);
            return "Exception: " + e.getLocalizedMessage();
        }

        return String.format("Updating from %s to %s.", source, repositoryURL);
    }

    /**
     * this method is called for the query processing at the click of Submit
     * 
     * @param query
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/getSparql", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse getSparql(@RequestParam(value = "query") String query, HttpServletRequest request

                    )
                                    throws Exception {
        HttpSession session = request.getSession(true);
        long startTime = System.currentTimeMillis();
        FreyaResponse response = helper.processQuestionAutomatically(query);
        //String preciseSparql = (String) result.get(FreyaConstants.PRECISE_SPARQL);
        // String repositoryUrl = (String) result
        // .get(FreyaConstants.REPOSITORY_URL);
        // String repositoryId = (String) result.get(FreyaConstants.REPOSITORY_ID);
        //
        // StringBuffer answer = new StringBuffer("repositoryId:\n")
        // .append(repositoryId).append(" repositoryURL:\n")
        // .append(repositoryUrl).append(" SPARQL query:\n")
        // .append(preciseSparql);

        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;
        logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
                        null));

        // FreyaResponse response = new FreyaResponse();
        // response.setRepositoryId(repositoryId);
        // response.setRepositoryUrl(repositoryUrl);
        // response.setSparqlQuery(preciseSparql);
        return response;
    }

    /**
     * this method is called for the query processing at the click of Submit
     * 
     * @param query
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/ask", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse ask(@RequestParam(value = "query") String query,
                    HttpServletRequest request
                    ) throws Exception {
        HttpSession session = request.getSession(true);
        long startTime = System.currentTimeMillis();
        Question question = helper.processQuestion(query);
        session.setAttribute(query, question);
        FreyaResponse response = helper.dialogOrNot(question, query, session);
        // Object answer = result.get(FreyaConstants.ANSWER);
        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;
        logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
                        null));
        // FreyaResponse response = new FreyaResponse();
        // response.setTextResponse(answer);
        return response;
    }

    /**
     * this method is called when the user selects one of the suggestions
     * 
     * @param query
     * @param request
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/refine", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse refine(@RequestParam(value = "query") String query,
                    @RequestParam(value = "voteId") String[] voteIds,

                    HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(true);
        //Object answer = null;
        FreyaResponse response;
        if (voteIds != null) {
            response = helper.refineQuestion(query, voteIds, session);
            // answer = (String) result.get(FreyaConstants.ANSWER);
            // answer = (Object) result.get(FreyaConstants.ANSWER);

        } else
            throw new Exception("Vote id is null! This should never happen!");
        // FreyaResponse response = new FreyaResponse();
        // response.setTextResponse(answer);
        return response;
        // return answer;
    }

    /**
     * this method is called for the query processing at the click of Submit when freya is in auto mode; it will
     * simulate that all the first options are being clicked and generate some result based on them
     * 
     * @param query
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/askNoDialog", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse askNoDialog(@RequestParam(value = "query") String query,
                    HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(true);
        long startTime = System.currentTimeMillis();

        FreyaResponse response = helper.processQuestionAutomatically(query);
        
        logger.info("THIS IS FREYA CONSTANT ANSWER"+ FreyaConstants.ANSWER);
        
       // Object answer = (Object) result.get(FreyaConstants.ANSWER);
        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;
        logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
                        null));
       // FreyaResponse response = new FreyaResponse();
       // response.setTextResponse(answer);
        return response;
    }

    /**
     * this method sets true for forceDialog
     * 
     * @param query
     * @param request
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/askForceDialog", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse askForceDialog(@RequestParam(value = "query") String query,
                    HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(true);
        long startTime = System.currentTimeMillis();

        Question question = null;
        question = helper.processQuestion(query, true);
        session.setAttribute(query, question);
        FreyaResponse response = helper.dialogOrNot(question, query, session);
       // String answer = (String) map.get(FreyaConstants.ANSWER);

        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;

        logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
                        null));
        return response;
    }

    /**
     * this method sets preferLonger to false
     * 
     * @param query
     * @param request
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/askNoFilter", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse askNoFilter(@RequestParam(value = "query") String query,
                    HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(true);
        long startTime = System.currentTimeMillis();
        Question question = null;
        // force dialog and no filter
        question = helper.processQuestion(query, true, false);
        session.setAttribute(query, question);
        FreyaResponse response = helper.dialogOrNot(question, query, session);
        //String answer = (String) map.get(FreyaConstants.ANSWER);

        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;
        logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
                        null));
        return response;
    }

    /**
     * analysis of input with subject predicate object
     * 
     * @param query
     * @param request
     * @return
     * @throws Exception
     */
    @ResponseBody
    @RequestMapping(value = "/analyse", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public FreyaResponse analyse(@RequestParam(value = "query") String query,
                    HttpServletRequest request) throws Exception {
        HttpSession session = request.getSession(true);
        long startTime = System.currentTimeMillis();
        // force dialog and no filter
        Question question = helper.processQuestion(query);

        // Question question = (Question) map.get(FreyaConstants.QUESTION);

        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;
        logger.info(ProfilerUtil.profileString(session.getId(), query, runTime,
                        null));

        FreyaResponse freyaResponse = helper.extractAnnotationsFromQuestion(question);
        return freyaResponse;
    }

}
