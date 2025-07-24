package de.jklein.pharmalink.runner;

import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Component
public class NoSqlViewInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(NoSqlViewInitializer.class);
    private final MongoTemplate mongoTemplate;

    public NoSqlViewInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        createUnitPassportView();
        createMedikamentSupplySummaryView();
        createFullTransferHistoryView();
    }

    private void createOrReplaceView(String viewName, String sourceCollection, List<Document> pipeline) {
        if (mongoTemplate.getCollectionNames().contains(viewName)) {
            mongoTemplate.getDb().getCollection(viewName).drop();
            logger.info("Bestehende View '{}' wurde entfernt.", viewName);
        }

        Document createViewCommand = new Document("create", viewName)
                .append("viewOn", sourceCollection)
                .append("pipeline", pipeline);

        mongoTemplate.getDb().runCommand(createViewCommand);
        logger.info("View '{}' erfolgreich erstellt.", viewName);
    }

    private void createUnitPassportView() {
        LookupOperation lookupMedikament = Aggregation.lookup("pharmalink.medikamente", "medikament._id", "_id", "medikamentInfo");
        UnwindOperation unwindMedikament = Aggregation.unwind("$medikamentInfo", true);

        LookupOperation lookupHersteller = Aggregation.lookup("pharmalink.actors", "medikamentInfo.hersteller._id", "_id", "herstellerInfo");
        UnwindOperation unwindHersteller = Aggregation.unwind("$herstellerInfo", true);

        LookupOperation lookupOwner = Aggregation.lookup("pharmalink.actors", "currentOwner._id", "_id", "ownerInfo");
        UnwindOperation unwindOwner = Aggregation.unwind("$ownerInfo", true);

        ProjectionOperation project = Aggregation.project("unitId", "chargeBezeichnung", "isConsumed", "ipfsLink", "transferHistory", "temperatureReadings")
                .and("$medikamentInfo").as("medikament")
                .and("$herstellerInfo").as("hersteller")
                .and("$ownerInfo").as("owner");

        List<Document> pipeline = Aggregation.newAggregation(
                lookupMedikament, unwindMedikament,
                lookupHersteller, unwindHersteller,
                lookupOwner, unwindOwner,
                project
        ).toPipeline(Aggregation.DEFAULT_CONTEXT);

        createOrReplaceView("template.unit_passport", "pharmalink.units", pipeline);
    }

    private void createMedikamentSupplySummaryView() {
        LookupOperation lookupMedikament = Aggregation.lookup("pharmalink.medikamente", "medikament._id", "_id", "medikamentInfo");
        UnwindOperation unwindMedikament = Aggregation.unwind("$medikamentInfo");

        GroupOperation group = Aggregation.group("medikamentInfo")
                .count().as("unitCount");

        ProjectionOperation project = Aggregation.project("unitCount")
                .and("_id").as("medikament");

        List<Document> pipeline = Aggregation.newAggregation(lookupMedikament, unwindMedikament, group, project)
                .toPipeline(Aggregation.DEFAULT_CONTEXT);

        createOrReplaceView("template.medikament_supply_summary", "pharmalink.units", pipeline);
    }

    private void createFullTransferHistoryView() {
        UnwindOperation unwindHistory = Aggregation.unwind("transferHistory");

        LookupOperation lookupMedikament = Aggregation.lookup("pharmalink.medikamente", "medikament._id", "_id", "medikamentInfo");
        LookupOperation lookupFromActor = Aggregation.lookup("pharmalink.actors", "transferHistory.fromActorId", "_id", "fromActorInfo");
        LookupOperation lookupToActor = Aggregation.lookup("pharmalink.actors", "transferHistory.toActorId", "_id", "toActorInfo");

        UnwindOperation unwindFrom = Aggregation.unwind("$fromActorInfo", true);
        UnwindOperation unwindTo = Aggregation.unwind("$toActorInfo", true);
        UnwindOperation unwindMed = Aggregation.unwind("$medikamentInfo", true);

        ProjectionOperation project = Aggregation.project("unitId")
                .and("transferHistory.timestamp").as("transferTimestamp")
                .and("$medikamentInfo").as("medikament")
                .and("$fromActorInfo").as("fromActor")
                .and("$toActorInfo").as("toActor");

        List<Document> pipeline = Aggregation.newAggregation(unwindHistory, lookupMedikament, lookupFromActor, lookupToActor, unwindFrom, unwindTo, unwindMed, project)
                .toPipeline(Aggregation.DEFAULT_CONTEXT);

        createOrReplaceView("template.full_transfer_history", "pharmalink.units", pipeline);
    }
}