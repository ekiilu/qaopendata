package org.freya.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.freya.dialogue.DialogManager;
import org.freya.format.JsonCreator;
import org.freya.learning.LearningModelHelper;
import org.freya.mapping.Consolidator;
import org.freya.mapping.Mapper;
import org.freya.model.OntologyElement;
import org.freya.model.POC;
import org.freya.model.Question;
import org.freya.model.ResultGraph;
import org.freya.model.SemanticConcept;
import org.freya.model.SuggestionPair;
import org.freya.model.Vote;
import org.freya.model.service.FreyaResponse;
import org.freya.oc.OCAnalyser;
import org.freya.oc.OCUtil;
import org.freya.rdf.SesameRepositoryManager;
import org.freya.rdf.query.CATConstants;
import org.freya.util.FreyaConstants;
import org.freya.util.NumberUtils;
import org.freya.util.OntologyElementsUtil;
import org.freya.util.ProfilerUtil;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.config.RepositoryConfigException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FreyaServiceHelper {
    protected final Log logger = LogFactory.getLog(getClass());

    @Autowired private Mapper mapper;

    @Autowired Consolidator consolidator;

    @Autowired private DialogManager dialogManager;

    @Autowired private OCAnalyser ocAnalyser;

    @Autowired private OCUtil ocUtil;

    @Autowired private JsonCreator jsonCreator;

    @Autowired private LearningModelHelper learningModelHelper;
    @Qualifier("rdfRepository") @Autowired SesameRepositoryManager repository;

    @Value("${org.freya.force.dialogue}") String forceDialogue;
    @Value("${org.freya.prefer.longer}") String preferLonger;
    @Value("${org.freya.add.none}") String addNoneToOCDialog;
    @Value("${org.freya.force.dialogue.thresholds}") String forceDialogThreshold;



    /**
     * this method just goes on with the selecting automatically all first options
     * 
     * @param query
     * 
     * @return
     */
    public FreyaResponse processQuestionAutomatically(String query) {
        // Map result = new HashMap();
        FreyaResponse response = new FreyaResponse();
        Question question = processQuestion(query);
        boolean questionRequiresDialog = true;
        while (questionRequiresDialog) {
            long startTime = System.currentTimeMillis();
            SuggestionPair pair = null;
            boolean resolvingOc = false;
            if (dialogManager.requiresOCDialog(question, new Boolean(forceDialogue))) {
                resolvingOc = true;
                pair = dialogManager.generateDisambiguationDialog(question, new Boolean(forceDialogue));
            } else if (dialogManager.requiresPOCDialog(question)) {
                resolvingOc = false;
                pair = dialogManager.generateMappingPocToOcDialog(question);
            } else {
                resolvingOc = false;
                questionRequiresDialog = false;
                if (question.getSemanticConcepts().size() > 5) {
                    logger.error("More than 5 columns!? Probably something wrong...for question:"
                                    + question.getSyntaxTree());
                    return null;
                }
                response = returnAnswer(question, query);
                return response;
            }
            Vote firstVote = pair.getVote().get(0);
            List<SemanticConcept> cocs = pair.getKey().getNearestNeighbours();
            String coc = "";
            for (SemanticConcept sc : cocs) {
                String genericElement =
                                (String) learningModelHelper.findGenericElement(sc
                                                .getOntologyElement());
                coc = coc + genericElement;
            }
            List<SemanticConcept> candidates = new ArrayList<SemanticConcept>();
            candidates.add(firstVote.getCandidate());
            question =
                            removePocOrOcFromQuestion(pair.getCurrentDialogSubject(), candidates,
                                            question);
            long endTime = System.currentTimeMillis();
            logger.info("Generating answer for " + (endTime - startTime) + " ms.");
        }
        return null;// "Something went wrong!!!!!!!";
    }



    /**
     * @param query
     * @return
     */
    public Question processQuestion(String query) {
        return processQuestion(query, null, null);
    }

    public Question processQuestion(String query, Boolean forceTheDialog) {
        return processQuestion(query, forceTheDialog, null);
    }

    /**
     * configure different modes and process question
     * 
     * @param query
     * @param forceTheDialog
     * @param thePreferLonger
     * @return
     */
    public Question processQuestion(String query, Boolean forceTheDialog, Boolean thePreferLonger) {

        logger.debug("Sumbitted query:" + query);

        // setConfiguration(forceTheDialog, thePreferLonger);

        return mapper.processQuestionLucene(query, new Boolean(forceDialogue).booleanValue(),
                        new Long(forceDialogThreshold), new Boolean(preferLonger).booleanValue(), new Boolean(
                                        addNoneToOCDialog));
    }


    /**
     * 
     * @param forceTheDialog
     * @param thePreferLonger
     */
    // void setConfiguration(Boolean forceTheDialog, Boolean thePreferLonger) {
    // read prop file
    // InputStream is = this.getClass().getResourceAsStream("/Service.properties");
    // Properties ps = new Properties();
    // try {
    // ps.load(is);
    // } catch (IOException e1) {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    // if (forceTheDialog == null)
    // forceDialog = new Boolean(ps.getProperty("forceDialog"));
    // else
    // forceDialog = forceTheDialog;
    // if (thePreferLonger == null)
    // preferLonger = new Boolean(ps.getProperty("preferLonger"));
    // else
    // preferLonger = thePreferLonger;
    // addNoneToOCDialog = new Boolean(ps.getProperty("addNoneToOCDialog"));
    // forceDialogThreshold = new Long(ps.getProperty("forceDialogThreshold"));
    // }

    /**
     * this method checks whether the dialog needs to be modelled and if not it generates results; if yes it generates
     * suggestionPair for the dialog
     * 
     * @param question
     * @return
     */
    FreyaResponse dialogOrNot(Question question, String query, HttpSession session) {
        // Map result = new HashMap();
        FreyaResponse response = new FreyaResponse();
        Object answer = null;
        try {
            if (dialogManager.requiresOCDialog(question, new Boolean(forceDialogue))) {
                int counter = 1;
                int steps = 2;
                long startTime = System.currentTimeMillis();
                SuggestionPair pair =
                                dialogManager.generateDisambiguationDialog(question, new Boolean(forceDialogue));

                long endTime = System.currentTimeMillis();
                logger.debug("Generating dialog for ambiguous ocs (" + pair.getVote().size()
                                + " suggestions) for " + (endTime - startTime) + " ms." + counter
                                + "/" + steps);
                counter++;
                session.setAttribute("PAIR|" + query, pair);
                startTime = System.currentTimeMillis();
                answer = jsonCreator.fromSuggestionsToJson(pair);
                response.setTextResponse(answer);
                // result.put(FreyaConstants.ANSWER, answer);
                endTime = System.currentTimeMillis();
                logger.debug("Generating json suggestions for " + (endTime - startTime) + " ms."
                                + counter + "/" + steps);
            } else if (dialogManager.requiresPOCDialog(question)) {
                int counter = 1;
                int steps = 2;
                long startTime = System.currentTimeMillis();
                SuggestionPair pair = dialogManager.generateMappingPocToOcDialog(question);
                long endTime = System.currentTimeMillis();
                logger.debug("Generating dialog for a POC (" + pair.getVote().size()
                                + " suggestions) for " + (endTime - startTime) + " ms." + counter
                                + "/" + steps);
                counter++;
                session.setAttribute("PAIR|" + query, pair);
                startTime = System.currentTimeMillis();
                answer = jsonCreator.fromSuggestionsToJson(pair);
                // result.put(FreyaConstants.ANSWER, answer);
                response.setTextResponse(answer);
                endTime = System.currentTimeMillis();
                logger.debug("Generating json suggestions for " + (endTime - startTime) + " ms."
                                + counter + "/" + steps);
            } else {
                response = returnAnswer(question, query);
                session.invalidate();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }

    /**
     * this method is called when all OCs are resolved and it generates sparql executes it generates graph etc
     * 
     * @param question
     * @param query
     * @return
     */
    FreyaResponse returnAnswer(Question question, String query) {
        // String answer = null;
        FreyaResponse response = new FreyaResponse();
        int counter = 1;
        int steps = 5;
        long startTime = System.currentTimeMillis();
        question = consolidator.consolidateTheAnswerType(question);
        long endTime = System.currentTimeMillis();
        logger.debug("Consolidating the answer type for " + (endTime - startTime) + " ms."
                        + counter + "/" + steps);
        counter++;
        if (question.getAnswerType() != null)
            logger.info("Answer type: "
                            + OntologyElementsUtil.beautifyListOfOntologyElements(question
                                            .getAnswerType()));
        else
            logger.info("Answer type is null");
        // logger.info("Generating SPARQL");
        // check consistency first:
        // each column need to have the same type of elements
        // otherwise return some warning or something and do not generate sparql
        if (!ocAnalyser.elementsConsistent(question.getSemanticConcepts())) {
            String message =
                            "Could not proceed to generating SPARQL/answer because elements are not consistent!";
            logger.info(message);
            return null;
        }
        startTime = System.currentTimeMillis();
        HashMap map = ocAnalyser.getResultMap(question.getSemanticConcepts(), question);
        String repositoryUrl = (String) map
                        .get(FreyaConstants.REPOSITORY_URL);
        String repositoryId = (String) map.get(FreyaConstants.REPOSITORY_ID);
        response.setRepositoryId(repositoryId);
        response.setRepositoryUrl(repositoryUrl);
        counter++;
        // logger.info("Sparql:\n"+sparql);
        List<List<String>> table = (List<List<String>>) map.get(FreyaConstants.TABLE);
        List<List<String>> list2DOfResults = (List<List<String>>) map.get(FreyaConstants.TABLE);
        int numberOfResults = 0;
        if (list2DOfResults != null && list2DOfResults.size() > 0) {
            numberOfResults = list2DOfResults.size() - 1;// -1 zato sto je prvi
                                                         // red
                                                         // variable
        }
        List<List<OntologyElement>> elementsTable =
                        (List<List<OntologyElement>>) map.get(FreyaConstants.ELEMENTS);
        Map mapWithNumberOfResultsAndTriples = ocAnalyser.getResultGraph(map);
        Integer numberForLimit =
                        (Integer) mapWithNumberOfResultsAndTriples
                                        .get(FreyaConstants.NUMBER_OF_RESULTS);
        String preciseSparql = (String) map.get(FreyaConstants.PRECISE_SPARQL);
        response.setPreciseSparql(preciseSparql);

        if (numberForLimit != null && numberOfResults != numberForLimit.intValue()) {
            // promeni limit
            preciseSparql = preciseSparql.substring(0, preciseSparql.indexOf("LIMIT"));
            preciseSparql = preciseSparql + " LIMIT " + numberForLimit;
        }
        // String sparql = (String)map.get("sparql");
        logger.info("PreciseSparql:\n" + preciseSparql + " " + counter + "/" + steps);
        List<ResultGraph> aGraph = null;
        if (numberOfResults < 1000) {
            aGraph =
                            (List<ResultGraph>) mapWithNumberOfResultsAndTriples
                                            .get(FreyaConstants.RESULTS_GRAPH);
            logger.debug("ovde cipo uradi paging sa offset i limit a ne ovako...");
        } else
            logger.info("Skipped generating graph, there were too many (" + numberOfResults
                            + ") results.");
        endTime = System.currentTimeMillis();
        logger.debug("Generating SPARQL and then graph for " + (endTime - startTime) + " ms."
                        + counter + "/" + steps);
        counter++;
        startTime = System.currentTimeMillis();
        if (aGraph != null) try {
            response = jsonCreator.createJsonNodeFromResultGraph(aGraph, table, response);
            // map.put(FreyaConstants.ANSWER, answer);



        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        endTime = System.currentTimeMillis();
        logger.debug("Preparing json graph from the resulted graph for " + (endTime - startTime)
                        + " ms." + counter + "/" + steps);
        counter++;
        startTime = endTime;

        endTime = System.currentTimeMillis();
        logger.debug("Saved sparql to file for " + (endTime - startTime) + " ms." + counter + "/"
                        + steps);
        // map.put(FreyaConstants.QUESTION, question);
        return response;
        // return map;
    }

    /**
     * find the selected vote based on the voteId
     * 
     * @param votes
     * @param voteIdString
     * @return
     */
    public List<Vote> findTheSelectedVoteAndRewardIt(List<Vote> votes, String[] voteIdString) {
        List<Vote> theOnes = new ArrayList<Vote>();
        for (String oneVoteIdString : voteIdString) {
            if (!"".equals(oneVoteIdString)) {
                for (Vote vote : votes) {
                    long voteId = vote.getId();
                    Long userSelectedVoteId = new Long(oneVoteIdString);
                    if (userSelectedVoteId.longValue() == voteId) {
                        double newVote =
                                        NumberUtils.roundTwoDecimals(vote.getVote()
                                                        + (double) FreyaConstants.REINFORCEMENT_REWARD);
                        vote.setVote(newVote);
                        theOnes.add(vote);
                        break;
                    }
                }
            }
        }
        return theOnes;
    }

    /**
     * @param query
     * @param username
     * @param voteIdString
     * @return
     */
    public FreyaResponse refineQuestion(String query, String[] voteIdString, HttpSession session) {
        long globalStartTime = System.currentTimeMillis();
        FreyaResponse response = null;
        // Map map = new HashMap();
        String answer = null;
        Question question = (Question) session.getAttribute(query);
        SuggestionPair pair = (SuggestionPair) session.getAttribute("PAIR|" + query);
        long startTime = System.currentTimeMillis();
        List<Vote> theOnes = findTheSelectedVoteAndRewardIt(pair.getVote(), voteIdString);
        long endTime = System.currentTimeMillis();
        logger.debug("Located the vote on the server and added positive reward for "
                        + (endTime - startTime) + " ms.");
        List<SemanticConcept> candidates = new ArrayList<SemanticConcept>();
        startTime = System.currentTimeMillis();
        Integer[] ranks = null;
        if (theOnes != null) {
            ranks = new Integer[theOnes.size()];
            for (int i = 0; i < theOnes.size(); i++) {
                Vote theOne = theOnes.get(i);
                candidates.add(theOne.getCandidate());
                Integer index = pair.getVote().indexOf(theOne);
                ranks[i] = index + 1;
            }
            // which rank was selected?

            // add negative reward
            for (Vote vote : pair.getVote()) {
                for (Vote theOne : theOnes) {
                    if (!vote.equals(theOne)) {
                        double negVoteScore =
                                        NumberUtils.roundTwoDecimals(vote.getVote()
                                                        + (double) FreyaConstants.REINFORCEMENT_NEGATIVE_REWARD);
                        vote.setVote(negVoteScore);
                    }
                }
            }
        }
        endTime = System.currentTimeMillis();
        logger.debug("Added negative reward for " + (endTime - startTime) + " ms.");
        // updating the learning model here
        startTime = System.currentTimeMillis();
        learningModelHelper.updateTheLearningModel(pair);
        endTime = System.currentTimeMillis();
        logger.debug("Updated the learning model for " + (endTime - startTime) + " ms.");
        if (candidates == null) {
            logger.info("!!!!!!!!!!!!!!!!!!ERROR!!!!!!!!!!!!!!!!!!!!!!!");
            logger.info("Candidate is null! This should never happen!!!");
            logger.info("!!!!!!!!!!!!!!!!!!ERROR!!!!!!!!!!!!!!!!!!!!!!!");
        } else {
            Object dialogSubject = pair.getCurrentDialogSubject();
            // dialogSubject is null when OCs are in the dialog, otherwise they
            // hold POC
            question = removePocOrOcFromQuestion(dialogSubject, candidates, question);
            startTime = System.currentTimeMillis();
            question = consolidator.consolidateTheAnswerType(question);
            endTime = System.currentTimeMillis();
            logger.debug("Consolidating the answer type for " + (endTime - startTime) + " ms.");
            session.setAttribute(query, question);
        }
        // call the method dialogOrNot which returns the answer/dialog
        startTime = System.currentTimeMillis();
        response = dialogOrNot(question, query, session);
        endTime = System.currentTimeMillis();
        logger.debug("DialogOrNot method took " + (endTime - startTime) + " ms.");

        long stopTime = System.currentTimeMillis();
        long runTime = stopTime - startTime;
        logger.info(ProfilerUtil.profileSelection(runTime, session.getId(), query, candidates,
                        ranks));
        return response;
    }

    /**
     * removes pocss or ocs after they have been resolved by the user
     * 
     * @param dialogSubject
     * @param candidate
     * @param question
     * @return
     */
    public Question removePocOrOcFromQuestion(Object dialogSubject,
                    List<SemanticConcept> candidates, Question question) {
        if (dialogSubject == null) {
            logger.debug("User has resolved OC. Removing all except: " + candidates.toString());
            List<List<SemanticConcept>> table = question.getSemanticConcepts();
            // iterate through the list and remove all ocs in the same
            // column but this one
            long startTime = System.currentTimeMillis();
            HashMap<Integer, List<SemanticConcept>> toRemove =
                            dialogManager.findSemanticConceptsToRemove(table, candidates);
            long endTime = System.currentTimeMillis();
            logger.debug("Marked which sem. concepts needs to be removed for "
                            + (endTime - startTime) + " ms. ");
            startTime = System.currentTimeMillis();
            question = ocUtil.removeMarkedOcs(question, toRemove);
            endTime = System.currentTimeMillis();
            logger.debug("Removed the sem. concepts for " + (endTime - startTime) + " ms.");
            /* now resolve any pocs as a result of resolved oc * */
            startTime = System.currentTimeMillis();
            question = mapper.resolvePocsWhichOverlapWithOcsLucene(question);
            endTime = System.currentTimeMillis();
            logger.debug("Resolved POCs which overlap with OCs for " + (endTime - startTime)
                            + " ms.");
            logger.debug("question has:" + question.getSemanticConcepts().size() + " columns.");
            for (int i = 0; i < question.getSemanticConcepts().size(); i++) {
                logger.debug("col " + i + " has " + question.getSemanticConcepts().get(i).size()
                                + " elements.");
            }
        } else {
            long startTime = System.currentTimeMillis();
            POC selectedOne = (POC) dialogSubject;
            logger.debug("User has resolved POC. Removing: "
                            + selectedOne.getAnnotation().getText());
            question.getPocs().remove(selectedOne);
            for (SemanticConcept sc : candidates) {
                sc.setVerified(true);
            }
            question.getSemanticConcepts().add(candidates);
            long endTime = System.currentTimeMillis();
            logger.debug("Removing POC for " + (endTime - startTime) + " ms.");
        }
        return question;
    }



    public void load(String repositoryURL, String repositoryId, String source, InputStream preloadInputStream)
                    throws RepositoryException {
        if (repositoryURL != repository.getRepositoryURL()) {
            repository.setRepositoryURL(repositoryURL);
        }
        if (repositoryId != repository.getRepositoryId()) {
            repository.setRepositoryId(repositoryId);
            try {
                repository.afterPropertiesSet();
            } catch (RepositoryConfigException e) {
                logger.error(e.getMessage());
            } catch (RepositoryException e) {
                logger.error(e.getMessage());
            }
        }

        repository.loadByInputStream(source, preloadInputStream);

    }



    public FreyaResponse extractAnnotationsFromQuestion(Question question) {
        FreyaResponse response = new FreyaResponse();
        List<List<SemanticConcept>> table = question.getSemanticConcepts();
        for (List<SemanticConcept> row : table) {
            for (SemanticConcept concept : row) {

                org.freya.model.ui.Annotation ann = new org.freya.model.ui.Annotation();
                ann.setStartOffset(
                                concept.getOntologyElement().getAnnotation().getStartOffset());
                ann.setEndOffset(
                                concept.getOntologyElement().getAnnotation().getEndOffset());

                ann.setText(
                                concept.getOntologyElement().getAnnotation().getText());
                response.getAnnotations().add(ann
                                );
                String uri = (String) concept.getOntologyElement().getAnnotation().getFeatures()
                                .get(CATConstants.FEATURE_URI);
                if (uri == null)
                    uri = (String) concept.getOntologyElement().getAnnotation().getFeatures()
                                    .get(CATConstants.FEATURE_INSTANCE_URI);

                ann.setUri(uri);

                String type = (String) concept.getOntologyElement().getAnnotation().getFeatures()
                                .get(CATConstants.FEATURE_TYPE_CLASS);
                ann.setType(type);

                ann.setScore(new Double((Float) concept.getOntologyElement().getAnnotation().getFeatures()
                                .get(FreyaConstants.SCORE)));
            }

        }
        return response;
    }
}
