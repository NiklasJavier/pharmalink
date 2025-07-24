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
public class NoSqlTemplateInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(NoSqlTemplateInitializer.class);
    private final MongoTemplate mongoTemplate;

    public NoSqlTemplateInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(String... args) {
        createUnitPassportView();
        createIpfsUsageSummaryView();
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

        createOrReplaceView("template.unit_deep_dive", "pharmalink.units", pipeline);
    }

    private void createIpfsUsageSummaryView() {
        GroupOperation group = Aggregation.group("ipfsHash")
                .count().as("usageCount")
                .first("lastAccessed").as("lastAccessed")
                .first("createdAt").as("createdAt");

        ProjectionOperation project = Aggregation.project("usageCount", "lastAccessed", "createdAt")
                .and("_id").as("ipfsHash");

        List<Document> pipeline = Aggregation.newAggregation(group, project)
                .toPipeline(Aggregation.DEFAULT_CONTEXT);

        createOrReplaceView("template.stats_ipfs_usage", "pharmalink.ipfs_cache", pipeline);
    }
}