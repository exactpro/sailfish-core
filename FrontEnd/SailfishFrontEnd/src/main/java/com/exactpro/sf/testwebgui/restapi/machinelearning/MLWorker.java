/*******************************************************************************
 * Copyright 2009-2019 Exactpro (Exactpro Systems Limited)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 ******************************************************************************/

package com.exactpro.sf.testwebgui.restapi.machinelearning;

import com.exactpro.sf.center.impl.SFLocalContext;
import com.exactpro.sf.common.messages.IMessage;
import com.exactpro.sf.common.messages.MsgMetaData;
import com.exactpro.sf.common.util.EPSCommonException;
import com.exactpro.sf.embedded.machinelearning.JsonEntityParser;
import com.exactpro.sf.embedded.machinelearning.MLPredictor;
import com.exactpro.sf.embedded.machinelearning.entities.FailedAction;
import com.exactpro.sf.embedded.machinelearning.entities.MessageEntry;
import com.exactpro.sf.embedded.machinelearning.entities.MessageParticipant;
import com.exactpro.sf.embedded.machinelearning.entities.MessageType;
import com.exactpro.sf.embedded.machinelearning.entities.SimpleValue;
import com.exactpro.sf.scriptrunner.StatusType;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Action;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Message;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Parameter;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.ReportRoot;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCase;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.TestCaseMetadata;
import com.exactpro.sf.scriptrunner.impl.jsonreport.beans.Verification;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.PredictionResultEntry;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.PredictionResultEntry.ClassValueEnum;
import com.exactpro.sf.testwebgui.restapi.machinelearning.model.ReportMessageDescriptor;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.io.FileUtils;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MLWorker {

    private static final TypeReference<?> SET_TYPE_REFERENCE = new TypeReference<Set<ReportMessageDescriptor>>() {};
    private static final TypeReference<?> MAP_TYPE_REFERENCE = new TypeReference<Map<String, ?>>() {};
    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final String TMP_DIR_FOR_EXTRACTING_REPORTS = "unpacked";

    private final Logger logger = LoggerFactory.getLogger(this.getClass().getName() + "@" + Integer.toHexString(hashCode()));


    private final MLPredictor predictor = SFLocalContext.getDefault().getMachineLearningService().getMlPredictor();

    /**
     * Parse the report and find failed actions with similar messages at specified testcase then make predictions if possible
     */
    public List<PredictionResultEntry> processReport(Integer testCaseId, InputStream streamOfZip) {

        Path tmpDir = null;

        try {

            tmpDir = Files.createTempDirectory(TMP_DIR_FOR_EXTRACTING_REPORTS);

            try (ZipInputStream zipInputStream = new ZipInputStream(streamOfZip)) {
                ZipEntry entry;
                while ((entry = zipInputStream.getNextEntry()) != null) {
                    Path path = tmpDir.resolve(Paths.get(entry.getName()));
                    path.getParent().toFile().mkdirs();
                    Files.copy(zipInputStream, path);
                }
            }

            File reportData = Files.walk(tmpDir)
                    .map(Path::toFile)
                    .filter(file -> "reportData".equals(file.getName()))
                    .findFirst()
                    .orElseThrow(() -> new EPSCommonException("Unsupported report supplied. JSON data  dir not found"));

            ReportRoot jsonReport = OBJECT_MAPPER.readValue(new File(reportData, "report.json"), ReportRoot.class);

            List<PredictionResultEntry> predictions = new ArrayList<>();

            for (TestCaseMetadata testCaseMetadata : jsonReport.getMetadata()) {

                if (testCaseId != null &&  !testCaseId.equals(Integer.valueOf(testCaseMetadata.getId()))) {
                    continue;
                }

                analyzeTestCase(reportData, testCaseMetadata)
                        .stream()
                        .map(failedAction -> new Tuple2<>(failedAction, predictor.classifyFailedAction(failedAction)))
                        .flatMap(tuple2 -> getPredictionResultEntries((int)tuple2.v1.getId(), tuple2.v2))
                        .forEach(predictions::add);
            }

            return predictions;
        } catch (Exception e) {
            throw new EPSCommonException("Cant handle testcase " + testCaseId, e);
        } finally {
            FileUtils.deleteQuietly(tmpDir.toFile());
        }
    }

    private Stream<PredictionResultEntry> getPredictionResultEntries(Integer actionId, Map<?, ?> map) {
        logger.info("mlplugin returns prediction {}", map);
        return map.entrySet().stream().map(entry -> convertOldFormatPrediction(actionId, entry));
    }

    private PredictionResultEntry convertOldFormatPrediction(Integer actionId, Map.Entry<?,?> entry) {
        int id = ((Number) entry.getKey()).intValue();
        Map<?, ?> stats = (Map<?, ?>) entry.getValue();

        PredictionResultEntry predictionResult = new PredictionResultEntry();
        predictionResult.setActionId(actionId);
        predictionResult.setMessageId(id);
        predictionResult.setClassValue(ClassValueEnum.fromValue((String) stats.get("classValue")));
        predictionResult.setPredictedClassProbability(Float.parseFloat((String) stats.get(predictionResult.getClassValue().value())));

        return predictionResult;
    }

    private List<FailedAction> analyzeTestCase(File reportData, TestCaseMetadata testCaseMetadata) throws IOException {

        TestCase testCase = OBJECT_MAPPER.readValue(new File(reportData, testCaseMetadata.getJsonFileName()), TestCase.class);
        File checkedFile = new File(reportData, MLPersistenceManager.ML_SUBMITS_FOR_REPORT);

        Set<ReportMessageDescriptor> checkedMessages = checkedFile.exists()
                ? OBJECT_MAPPER.readValue(checkedFile, SET_TYPE_REFERENCE)
                : Collections.emptySet();

        return testCase.getActions()
                .stream()
                .filter(node -> node instanceof Action)
                .map(node -> (Action)node )
                .filter(this::checkActionApplicable)
                .map(action -> convertJsonActionToMLFailedAction(testCase, checkedMessages, action))
                .collect(Collectors.toList());

    }

    private FailedAction convertJsonActionToMLFailedAction(TestCase testCase, Set<ReportMessageDescriptor> checkedMessages, Action realAction) {
        String protocol = testCase.getMessages().stream()
                .filter(message -> realAction.getRelatedMessages().contains(message.getId()))
                .map(this::getMessageProtocol)
                .findAny()
                .orElseThrow(() -> new EPSCommonException("Can't detect target protocol"));

        Stream<Message> participants = testCase.getMessages().stream()
                .filter(m -> protocol.equals(getMessageProtocol(m)));

        //TODO old API do parseFull by dictionary but
        MachineLearnMessageAlias expected = new MachineLearnMessageAlias();
        createExpectedMessage(realAction.getParameters(), expected);
        //Too dodgy
        MessageEntry expectedResult = expected.getEntries().get(0);
        expected = MachineLearnMessageAlias.wrap(expectedResult.getMessage());

        MessageParticipant[] actualMessages = participants
                .map(this::getMachineLearnMessageAlias)
                .map(msg -> buildMessageParticipant(checkedMessages, realAction, msg))
                .toArray(MessageParticipant[]::new);

        FailedAction failedAction = new FailedAction(expected, actualMessages);
        failedAction.setId(realAction.getId());

        return failedAction;
    }

    private String getMessageProtocol(Message message) {
        Map<String, ?> messageContent = parseMessageFromContent(message.getContent());
        return (String) messageContent.get("protocol");
    }

    private Map<String, ?> parseMessageFromContent(String content) {
        try {
            return OBJECT_MAPPER.readValue(content, MAP_TYPE_REFERENCE);
        } catch (IOException e) {
            throw new EPSCommonException("Can't parse message content: " + content, e);
        }
    }

    private MachineLearnMessageAlias getMachineLearnMessageAlias(Message message) {
        Map<String, Object> sfMessage = (Map<String, Object>) parseMessageFromContent(message.getContent());

        JsonFactory factory = new JsonFactory();

        try {
            JsonParser parser = factory.createParser(message.getContent());
            com.exactpro.sf.embedded.machinelearning.entities.Message parseMessage = JsonEntityParser.parseMessage(null, parser, sfMessage.entrySet()
                    .stream()
                    .filter(o -> !(o instanceof Map))
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> Objects.toString(entry.getValue()))), true);

            parseMessage.setId(message.getId());

            return MachineLearnMessageAlias.wrap(parseMessage);
        } catch (IOException e) {
            throw new EPSCommonException("Message from json report can't be parsed",  e);
        }
    }

    private MessageParticipant buildMessageParticipant(Set<ReportMessageDescriptor> checkedMessages, Action realAction, MachineLearnMessageAlias m) {
        boolean problemExplanation = false;

        if (m.getId() == realAction.getId()) {

            problemExplanation = checkedMessages
                    .stream()
                    .anyMatch(descriptor -> m.getId() == descriptor.getActionId());
        }

        MessageParticipant messageParticipant = new MessageParticipant(problemExplanation, m);
        messageParticipant.setId(m.getId());

        return messageParticipant;
    }

    private boolean checkActionApplicable(Action action) {

        boolean failed = action.getStatus().getStatus() == StatusType.FAILED;
        boolean verificationsFound = action.getSubNodes().stream().anyMatch(node -> node instanceof Verification);

        return failed && verificationsFound;
    }

    private void createExpectedMessage(List<Parameter> p, MachineLearnMessageAlias target) {

        BiFunction<Parameter, MsgMetaData, MachineLearnMessageAlias> complexParameterConverter = (param, msgMetaData) -> {
            MachineLearnMessageAlias nested = new MachineLearnMessageAlias();
            nested.setType(new MessageType(param.getName(),
                    Objects.toString(msgMetaData.getDictionaryURI(), ""),
                    Objects.toString(msgMetaData.getProtocol(), "")));
            createExpectedMessage(param.getSubParameters(), nested);

            return nested;
        };

        for (Parameter subParameter : p) {

            boolean collection = subParameter.getType().startsWith(List.class.getSimpleName());
            boolean complex = IMessage.class.getSimpleName().equals(subParameter.getType());

            MessageEntry messageEntry;
            MsgMetaData msgMetaData = subParameter.getMsgMetadata();

            if (collection) {
                if (complex) {
                    MachineLearnMessageAlias[] messageList = subParameter.getSubParameters()
                            .stream()
                            .map(parameter -> complexParameterConverter.apply(parameter, msgMetaData))
                            .toArray(MachineLearnMessageAlias[]::new);

                    messageEntry = new MessageEntry(subParameter.getName(), messageList);
                } else {
                    SimpleValue[] entryCollection = subParameter.getSubParameters()
                            .stream()
                            .map(e -> new SimpleValue(e.getType(), e.getValue()))
                            .toArray(SimpleValue[]::new);

                    messageEntry = new MessageEntry(subParameter.getName(), entryCollection);
                }
            } else {
                if (complex) {
                    MachineLearnMessageAlias nested = complexParameterConverter.apply(subParameter, msgMetaData);
                    messageEntry = new MessageEntry(subParameter.getName(), nested);
                } else {
                    messageEntry = new MessageEntry(subParameter.getName(), new SimpleValue(subParameter.getType(), subParameter.getValue()));
                }
            }

            target.addEntry(messageEntry);
        }

    }

    //Fake class to preserve typing full class name (
    private static final class MachineLearnMessageAlias extends com.exactpro.sf.embedded.machinelearning.entities.Message {
        public MachineLearnMessageAlias(MessageType messageType, MessageEntry[] toArray) {
            super(messageType, toArray);
        }

        public MachineLearnMessageAlias() {
            super();
        }

        public static MachineLearnMessageAlias wrap(com.exactpro.sf.embedded.machinelearning.entities.Message wrapped) {

            MachineLearnMessageAlias tmp = new MachineLearnMessageAlias(wrapped.getType(), wrapped.getEntries().toArray(new MessageEntry[0]));
            tmp.setId(wrapped.getId());

            return tmp;
        }

    }

}
